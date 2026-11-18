
package com.quickhelp.controller;

import com.quickhelp.dto.NotificacionAdminResponseDTO; // Datos para el panel administrativo.
import com.quickhelp.dto.NotificacionResponseDTO; // Datos para la aplicación móvil.
import com.quickhelp.service.NotificacionService; // Lógica de notificaciones.
import java.util.List; // Listado de notificaciones.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.*; // Endpoints REST.

@RestController // Controlador REST de notificaciones.
@RequestMapping("/api/notificaciones") // Ruta base.
public class NotificacionController {

    private final NotificacionService notificacionService;

    // Inyecta el servicio de notificaciones.
    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    // Devuelve las notificaciones del usuario autenticado.
    @GetMapping
    public List<NotificacionResponseDTO> listarMisNotificaciones(
            Authentication authentication) {
        return notificacionService.listarMisNotificaciones(authentication);
    }

    // Consulta las notificaciones de una alerta para administración.
    @GetMapping("/alerta/{idAlerta}")
    public List<NotificacionAdminResponseDTO> listarNotificacionesPorAlerta(
            @PathVariable Integer idAlerta,
            Authentication authentication) {
        return notificacionService.listarNotificacionesPorAlerta(
                idAlerta, authentication);
    }
}
