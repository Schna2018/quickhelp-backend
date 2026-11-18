package com.quickhelp.service;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class GeocodificacionService {

    private static final long PAUSA_POR_CUOTA_MS = 60_000L;
    private static final int MAXIMO_GUARDADAS = 500;
    private final String clave;
    private final RestClient cliente;
    private final AtomicLong reintentarDesde = new AtomicLong();
    // Direcciones ya resueltas: un punto fijo no cambia de dirección.
    private final Map<String, JsonNode> guardadas = new ConcurrentHashMap<>();

    public GeocodificacionService(@Value("${QUICKHELP_MAPS_KEY:}") String clave) {
        this.clave = clave.trim(); // La credencial se lee del entorno del backend.
        var transporte = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        var peticiones = new JdkClientHttpRequestFactory(transporte);
        peticiones.setReadTimeout(Duration.ofSeconds(5));

        cliente = RestClient.builder()
                .baseUrl("https://geocode.googleapis.com")
                .requestFactory(peticiones)
                .build();
    }

    public JsonNode buscarDireccion(BigDecimal latitud, BigDecimal longitud) {
        if (clave.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "El servicio de direcciones no está configurado.");
        }

        // Reutiliza la dirección ya obtenida, aunque esté vigente la pausa por cuota.
        String llave = latitud.toPlainString() + "," + longitud.toPlainString();
        JsonNode guardada = guardadas.get(llave);
        if (guardada != null) {
            return guardada;
        }

        if (System.currentTimeMillis() < reintentarDesde.get()) {
            throw limiteDeCuota();
        }

        try {
            // Solicita los componentes que ya usa la tarjeta para armar la dirección.
            JsonNode resultado = cliente.get()
                    .uri("/v4/geocode/location/{latitud},{longitud}?languageCode=es",
                            latitud.toPlainString(), longitud.toPlainString())
                    .header("X-Goog-Api-Key", clave)
                    .header("X-Goog-FieldMask", "results.addressComponents.longText,results.addressComponents.types")
                    .retrieve()
                    .body(JsonNode.class);

            if (resultado != null && resultado.isObject()) {
                // Limita la memoria usada por las direcciones guardadas.
                if (guardadas.size() >= MAXIMO_GUARDADAS) {
                    guardadas.clear();
                }
                guardadas.put(llave, resultado);
                return resultado;
            }
        } catch (HttpClientErrorException.TooManyRequests fallo) {
            // Comparte la pausa entre las consultas que recibe esta instancia.
            reintentarDesde.accumulateAndGet(
                    System.currentTimeMillis() + PAUSA_POR_CUOTA_MS, Math::max);
            throw limiteDeCuota();
        } catch (RestClientException fallo) {
            // Devuelve un error controlado si el proveedor está caído o rechaza la consulta.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "No se pudo obtener la dirección.");
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "El servicio de direcciones devolvió una respuesta inválida.");
    }

    private static ResponseStatusException limiteDeCuota() {
        return new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS, "El proveedor de direcciones limitó temporalmente las consultas.");
    }
}
