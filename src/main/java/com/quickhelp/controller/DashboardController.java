
package com.quickhelp.controller;

import com.quickhelp.dto.DashboardResponseDTO; // Respuesta del Dashboard.
import com.quickhelp.service.DashboardService; // Servicio de indicadores.
import com.quickhelp.repository.UsuarioRepository; // Consulta del usuario autenticado.
import com.quickhelp.entity.Usuario; // Entidad de usuario.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Identidad JWT.
import org.springframework.web.bind.annotation.GetMapping; // Endpoint GET.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta principal.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.
import org.springframework.web.server.ResponseStatusException; // Manejo de errores.

@RestController // Expone los indicadores como JSON.
@RequestMapping("/api/dashboard") // Ruta base del Dashboard.
public class DashboardController {

    private final DashboardService dashboardService;
    private final UsuarioRepository usuarioRepository;

    // Inyecta las dependencias del controlador.
    public DashboardController(
            DashboardService dashboardService,
            UsuarioRepository usuarioRepository) {
        this.dashboardService = dashboardService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/indicadores") // GET /api/dashboard/indicadores.
    public DashboardResponseDTO obtenerIndicadores(Authentication authentication) {

        // Verifica que exista una sesión autenticada.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        // Obtiene el ID del usuario desde el token JWT.
        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de usuario no válido.");
        }

        // Consulta al usuario autenticado en PostgreSQL.
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));

        // Exige que el usuario y su rol estén activos.
        if (!Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Usuario o rol inactivo.");
        }

        // Solo permite acceder al administrador.
        if (!"ADMIN".equals(usuario.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tiene permisos para consultar el Dashboard.");
        }

        // Devuelve los indicadores calculados desde PostgreSQL.
        return dashboardService.obtenerIndicadores();
    }
}
