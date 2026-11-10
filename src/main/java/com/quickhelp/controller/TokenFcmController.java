
package com.quickhelp.controller;

import com.quickhelp.dto.TokenFcmRequestDTO; // Datos del token recibido.
import com.quickhelp.service.TokenFcmService; // Servicio que guarda el token.
import jakarta.validation.Valid; // Valida los datos recibidos.
import org.springframework.http.ResponseEntity; // Respuesta HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.*; // Anotaciones REST.

@RestController // Expone los endpoints REST.
@RequestMapping("/api/usuarios") // Ruta base del controlador.
public class TokenFcmController {

    private final TokenFcmService tokenFcmService;

    public TokenFcmController(TokenFcmService tokenFcmService) {
        this.tokenFcmService = tokenFcmService;
    }

    @PutMapping("/token-fcm") // PUT /api/usuarios/token-fcm.
    public ResponseEntity<Void> registrarToken(
            @Valid @RequestBody TokenFcmRequestDTO request,
            Authentication authentication) {

        // Registra el token del usuario identificado mediante JWT.
        tokenFcmService.registrarToken(request, authentication);

        return ResponseEntity.noContent().build(); // HTTP 204.
    }
}
