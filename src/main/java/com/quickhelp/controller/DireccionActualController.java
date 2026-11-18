package com.quickhelp.controller;

import com.quickhelp.controller.DireccionAlertaController.RespuestaDireccion; // Mismo formato que la tarjeta.
import com.quickhelp.dto.DireccionRequestDTO; // Coordenadas recibidas.
import com.quickhelp.service.GeocodificacionService; // Google con respaldo de OpenStreetMap.
import jakarta.validation.Valid; // Valida los datos recibidos.
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ubicacion")
public class DireccionActualController {

    private final GeocodificacionService geocodificacion;

    public DireccionActualController(GeocodificacionService geocodificacion) {
        this.geocodificacion = geocodificacion;
    }

    @PostMapping("/direccion") // POST /api/ubicacion/direccion.
    public ResponseEntity<RespuestaDireccion> consultarDireccion(
            @Valid @RequestBody DireccionRequestDTO request,
            Authentication authentication) {

        // Solo el adulto mayor muestra su dirección actual en Inicio.
        boolean esAdulto = authentication.getAuthorities().stream()
                .anyMatch(rol -> "ROLE_ADULTO_MAYOR".equals(rol.getAuthority()));
        if (!esAdulto) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo el adulto mayor puede consultar su dirección actual.");
        }

        // Redondea a unos 11 metros: basta para la dirección y aprovecha las ya guardadas.
        BigDecimal latitud = request.latitud().setScale(4, RoundingMode.HALF_UP);
        BigDecimal longitud = request.longitud().setScale(4, RoundingMode.HALF_UP);

        var datos = geocodificacion.buscarDireccion(latitud, longitud);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new RespuestaDireccion(latitud, longitud, datos));
    }
}
