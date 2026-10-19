package com.quickhelp.controller;

import com.quickhelp.entity.Rol;
import com.quickhelp.service.RolService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController // Indica que esta clase atiende solicitudes HTTP y devuelve respuestas, normalmente en JSON.
@RequestMapping("/api/roles") // Define la ruta base de esta API: http://localhost:8080/api/roles
public class RolController {

    private final RolService rolService; // Servicio que contiene la lógica relacionada con los roles.

    public RolController(RolService rolService) {
        this.rolService = rolService; // Spring inyecta automáticamente una instancia de RolService.
    }

    @GetMapping // Atiende solicitudes HTTP GET realizadas a /api/roles.
    public List<Rol> listarRoles() {
        return rolService.listarRoles(); // Obtiene todos los roles y los devuelve al frontend en formato JSON.
    }
}