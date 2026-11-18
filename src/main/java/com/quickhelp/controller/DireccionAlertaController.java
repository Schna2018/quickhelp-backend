package com.quickhelp.controller;

import com.quickhelp.service.AlertaService; // Consulta la ubicación autorizada de la alerta.
import com.quickhelp.service.GeocodificacionService; // Convierte las coordenadas en una dirección.
import java.math.BigDecimal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/alertas")
public class DireccionAlertaController {

    private final AlertaService alertas;
    private final GeocodificacionService geocodificacion;

    public DireccionAlertaController(
            AlertaService alertas, GeocodificacionService geocodificacion) {
        this.alertas = alertas;
        this.geocodificacion = geocodificacion;
    }

    @GetMapping("/{idAlerta}/ubicacion/direccion")
    public ResponseEntity<RespuestaDireccion> consultarDireccion(
            @PathVariable Integer idAlerta, Authentication authentication) {
        // Reutiliza la autorización y las coordenadas más recientes de la alerta.
        var ubicacion = alertas.consultarUbicacion(idAlerta, authentication);
        if (!(ubicacion.get("latitud") instanceof BigDecimal latitud)
                || !(ubicacion.get("longitud") instanceof BigDecimal longitud)) {
            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        }

        // La tarjeta puede comprobar que la dirección corresponde al mismo punto de su mapa.
        var datos = geocodificacion.buscarDireccion(latitud, longitud);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new RespuestaDireccion(latitud, longitud, datos));
    }

    public record RespuestaDireccion(
            BigDecimal latitud, BigDecimal longitud, JsonNode geocodificacion) {}
}
