
package com.quickhelp.controller;

import com.quickhelp.service.CodigoVinculacionService; // Servicio de generación del código.
import org.springframework.http.ResponseEntity; // Respuestas HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado mediante JWT.
import org.springframework.web.bind.annotation.GetMapping; // Endpoint GET.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta principal.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.
import java.util.Map; // Estructura de respuesta JSON.

@RestController // Expone los endpoints REST.
@RequestMapping("/api/vinculacion") // Ruta principal de vinculación.
public class CodigoVinculacionController {

    private final CodigoVinculacionService codigoVinculacionService; // Servicio de vinculación.

    public CodigoVinculacionController(CodigoVinculacionService codigoVinculacionService) {
        this.codigoVinculacionService = codigoVinculacionService; // Inyección del servicio.
    }

    @GetMapping("/codigo") // GET /api/vinculacion/codigo
    public ResponseEntity<Map<String, String>> obtenerCodigo(Authentication authentication) {

        String codigo = codigoVinculacionService.generarCodigo(authentication); // Obtiene o genera el código.

        return ResponseEntity.ok(Map.of(
            "codigoVinculacion", codigo // Devuelve el código al perfil del adulto mayor.
        ));
    }
}
