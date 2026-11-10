
package com.quickhelp.controller;

import com.quickhelp.dto.NotificacionResponseDTO; // Datos de notificación.
import com.quickhelp.service.NotificacionService; // Lógica de notificaciones.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.*; // Endpoints REST.
import java.util.List; // Lista de notificaciones.

@RestController // Controlador REST.
@RequestMapping("/api/notificaciones") // Ruta de notificaciones.
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
}
