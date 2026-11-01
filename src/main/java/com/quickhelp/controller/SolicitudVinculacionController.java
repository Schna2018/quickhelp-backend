
package com.quickhelp.controller;

import com.quickhelp.service.SolicitudVinculacionService; // Lógica de vinculación.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.PathVariable; // Identificador de solicitud.
import org.springframework.web.bind.annotation.PostMapping; // Peticiones POST.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta principal.
import org.springframework.web.bind.annotation.ResponseStatus; // Estado de respuesta.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.

@RestController
@RequestMapping("/api/vinculaciones")
public class SolicitudVinculacionController {

    private final SolicitudVinculacionService solicitudService;

    public SolicitudVinculacionController(SolicitudVinculacionService solicitudService) {
        this.solicitudService = solicitudService; // Inyección del servicio.
    }

    @PostMapping("/{idSolicitud}/aprobar") // Aprueba una solicitud pendiente.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aprobar(@PathVariable Integer idSolicitud, Authentication authentication) {
        solicitudService.aprobarSolicitud(idSolicitud, authentication);
    }

    @PostMapping("/{idSolicitud}/rechazar") // Rechaza una solicitud pendiente.
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazar(@PathVariable Integer idSolicitud, Authentication authentication) {
        solicitudService.rechazarSolicitud(idSolicitud, authentication);
    }
}
