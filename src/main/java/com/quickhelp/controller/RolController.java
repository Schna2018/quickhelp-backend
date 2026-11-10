
package com.quickhelp.controller;

import com.quickhelp.dto.RolResponseDTO; // Datos seguros del rol.
import com.quickhelp.entity.Rol; // Entidad de PostgreSQL.
import com.quickhelp.service.RolService; // Lógica de roles.
import java.util.List; // Colección de resultados.
import org.springframework.web.bind.annotation.GetMapping; // Endpoint GET.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta base.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.

@RestController // Expone los endpoints HTTP.
@RequestMapping("/api/roles") // Ruta base de los roles.
public class RolController {

    private final RolService rolService; // Servicio existente.

    public RolController(RolService rolService) {
        this.rolService = rolService; // Inyección del servicio.
    }

    @GetMapping // GET /api/roles.
    public List<RolResponseDTO> listarRoles() {
        return rolService.listarRoles().stream()
                .map(this::convertirDTO)
                .toList(); // Devuelve únicamente los campos necesarios.
    }

    // Convierte una entidad Rol en un DTO.
    private RolResponseDTO convertirDTO(Rol rol) {
        return new RolResponseDTO(
                rol.getIdRol(),
                rol.getCodigo(),
                rol.getNombre(),
                rol.getEstado()
        );
    }
}
