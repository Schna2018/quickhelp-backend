package com.quickhelp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Service
public class GeocodificacionService {

    private static final long PAUSA_POR_CUOTA_MS = 60_000L;
    private static final long ESPERA_ENTRE_CONSULTAS_MS = 1_000L;
    private static final int MAXIMO_GUARDADAS = 500;
    // OpenStreetMap pide identificar la aplicación en cada consulta.
    private static final String AGENTE = "QuickHelp/1.0 (+https://github.com/Schna2018/quickhelp-backend)";
    private final String clave;
    private final RestClient cliente;
    private final RestClient clienteRespaldo;
    private final AtomicLong reintentarDesde = new AtomicLong();
    private final AtomicLong reintentarRespaldoDesde = new AtomicLong();
    private final AtomicLong ultimaConsultaRespaldo = new AtomicLong();
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

        // Respaldo gratuito y sin clave: Nominatim de OpenStreetMap.
        clienteRespaldo = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .requestFactory(peticiones)
                .defaultHeader("User-Agent", AGENTE)
                .build();
    }

    public JsonNode buscarDireccion(BigDecimal latitud, BigDecimal longitud) {
        // Reutiliza la dirección ya obtenida, aunque esté vigente la pausa por cuota.
        String llave = latitud.toPlainString() + "," + longitud.toPlainString();
        JsonNode guardada = guardadas.get(llave);
        if (guardada != null) {
            return guardada;
        }

        // Primero Google; si no responde con datos, se usa OpenStreetMap.
        JsonNode resultado = buscarEnGoogle(latitud, longitud);
        if (resultado == null) {
            resultado = buscarEnOpenStreetMap(latitud, longitud);
        }

        // Guarda solo direcciones con datos para poder reintentar las vacías.
        if (resultado.path("results").size() > 0) {
            // Limita la memoria usada por las direcciones guardadas.
            if (guardadas.size() >= MAXIMO_GUARDADAS) {
                guardadas.clear();
            }
            guardadas.put(llave, resultado);
        }
        return resultado;
    }

    // Devuelve null cuando Google no está disponible para pasar al respaldo.
    private JsonNode buscarEnGoogle(BigDecimal latitud, BigDecimal longitud) {
        if (clave.isEmpty() || System.currentTimeMillis() < reintentarDesde.get()) {
            return null;
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

            if (resultado != null && resultado.isObject() && resultado.path("results").size() > 0) {
                return resultado;
            }
        } catch (HttpClientErrorException.TooManyRequests fallo) {
            // Pausa Google en esta instancia mientras responde el respaldo.
            reintentarDesde.accumulateAndGet(
                    System.currentTimeMillis() + PAUSA_POR_CUOTA_MS, Math::max);
        } catch (RestClientException fallo) {
            // Cualquier otra falla de Google también pasa al respaldo.
        }
        return null;
    }

    private JsonNode buscarEnOpenStreetMap(BigDecimal latitud, BigDecimal longitud) {
        long ahora = System.currentTimeMillis();
        if (ahora < reintentarRespaldoDesde.get()) {
            throw limiteDeCuota();
        }
        // Respeta el máximo de una consulta por segundo de OpenStreetMap.
        long anterior = ultimaConsultaRespaldo.get();
        if (ahora - anterior < ESPERA_ENTRE_CONSULTAS_MS
                || !ultimaConsultaRespaldo.compareAndSet(anterior, ahora)) {
            throw limiteDeCuota();
        }

        try {
            RespuestaNominatim respuesta = clienteRespaldo.get()
                    .uri("/reverse?format=jsonv2&addressdetails=1&accept-language=es&lat={latitud}&lon={longitud}",
                            latitud.toPlainString(), longitud.toPlainString())
                    .retrieve()
                    .body(RespuestaNominatim.class);

            if (respuesta != null) {
                return convertir(respuesta.address());
            }
        } catch (HttpClientErrorException.TooManyRequests | HttpClientErrorException.Forbidden fallo) {
            // Comparte la pausa del respaldo entre las consultas de esta instancia.
            reintentarRespaldoDesde.accumulateAndGet(
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

    // Mantiene el formato que ya lee la app: results[].addressComponents[] con longText y types.
    private static JsonNode convertir(Map<String, String> direccion) {
        ObjectNode raiz = JsonNodeFactory.instance.objectNode();
        ArrayNode resultados = raiz.putArray("results");
        if (direccion == null || direccion.isEmpty()) {
            return raiz; // Sin datos: la tarjeta mantiene el mapa y ofrece reintentar.
        }

        ArrayNode componentes = resultados.addObject().putArray("addressComponents");
        agregar(componentes, "route", direccion.get("road"));
        agregar(componentes, "street_number", direccion.get("house_number"));
        // En Chile la comuna llega como ciudad, pueblo o sector según la zona.
        agregar(componentes, "administrative_area_level_3", primero(direccion,
                "city", "town", "village", "municipality", "suburb"));
        return raiz;
    }

    private static void agregar(ArrayNode componentes, String tipo, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        ObjectNode componente = componentes.addObject();
        componente.put("longText", texto.trim());
        componente.putArray("types").add(tipo);
    }

    private static String primero(Map<String, String> direccion, String... campos) {
        for (String campo : campos) {
            String valor = direccion.get(campo);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private static ResponseStatusException limiteDeCuota() {
        return new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS, "El proveedor de direcciones limitó temporalmente las consultas.");
    }

    // Solo se leen los campos de dirección de la respuesta de OpenStreetMap.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaNominatim(Map<String, String> address) {}
}
