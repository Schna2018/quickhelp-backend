
package com.quickhelp.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.quickhelp.dto.AuthRequestDTO;
import com.quickhelp.dto.AuthResponseDTO;
import com.quickhelp.service.AuthService;
import org.springframework.http.HttpStatus; // Define el estado HTTP de la respuesta.
import org.springframework.web.bind.annotation.ResponseStatus; // Configura el estado HTTP del endpoint.
import com.quickhelp.dto.RegistroUsuarioDTO; // Recibe los datos del nuevo usuario.

@RestController // Expone el endpoint de autenticación.
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login") // POST /api/auth/login
    public AuthResponseDTO login(@Valid @RequestBody AuthRequestDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/registro/adulto") // POST /api/auth/registro/adulto.
    @ResponseStatus(HttpStatus.CREATED) // Devuelve HTTP 201 al registrar correctamente.
    public void registrarAdulto(@Valid @RequestBody RegistroUsuarioDTO dto) {
        authService.registrarUsuario(dto, "ADULTO_MAYOR"); // Asigna el rol de adulto mayor.
    }

    @PostMapping("/registro/asistente") // POST /api/auth/registro/asistente.
    @ResponseStatus(HttpStatus.CREATED) // Devuelve HTTP 201 al registrar correctamente.
    public void registrarAsistente(@Valid @RequestBody RegistroUsuarioDTO dto) {
        authService.registrarUsuario(dto, "ASISTENTE"); // Asigna el rol de asistente.
    }

}
