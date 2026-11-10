
package com.quickhelp.controller;

import com.quickhelp.service.SolicitudVinculacionService; // Lógica de vinculación.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.PathVariable; // Identificador de solicitud.
import org.springframework.web.bind.annotation.PostMapping; // Peticiones POST.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta principal.
import org.springframework.web.bind.annotation.ResponseStatus; // Estado de respuesta.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.
import com.quickhelp.dto.SolicitudVinculacionResponseDTO; // Datos seguros de las solicitudes.
import org.springframework.web.bind.annotation.GetMapping; // Permite crear endpoints GET.
import java.util.List; // Permite devolver una lista de solicitudes.

@RestController
@RequestMapping("/api/vinculaciones")
public class SolicitudVinculacionController {

    private final SolicitudVinculacionService solicitudService;

    public SolicitudVinculacionController(SolicitudVinculacionService solicitudService) {
        this.solicitudService = solicitudService; // Inyección del servicio.
    }

    @GetMapping("/pendientes") // Consulta las solicitudes pendientes del adulto mayor.
    public List<SolicitudVinculacionResponseDTO> listarPendientes(Authentication authentication) {
        return solicitudService.listarPendientes(authentication); // Obtiene los datos filtrados por JWT.
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
