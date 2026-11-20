package com.quickhelp.controller;

import com.quickhelp.service.RecuperacionClaveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Rutas públicas para recuperar la contraseña (sin sesión iniciada).
@RestController
@RequestMapping("/api/auth/recuperar-clave")
public class RecuperacionClaveController {

    private final RecuperacionClaveService recuperacionService;

    public RecuperacionClaveController(RecuperacionClaveService recuperacionService) {
        this.recuperacionService = recuperacionService;
    }

    public record SolicitarCodigo(@NotBlank @Email @Size(max = 150) String correo) {}

    public record CambiarClave(
            @NotBlank @Email @Size(max = 150) String correo,
            @NotBlank @Pattern(regexp = "\\d{6}") String codigo,
            @NotBlank @Size(min = 8, max = 100) String nuevaClave
    ) {}

    // POST /api/auth/recuperar-clave/solicitar: siempre responde lo mismo, exista o no el correo.
    @PostMapping("/solicitar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> solicitar(@Valid @RequestBody SolicitarCodigo datos) {
        recuperacionService.solicitarCodigo(datos.correo());
        return Map.of("mensaje", "Si el correo está registrado, te enviamos un código.");
    }

    // POST /api/auth/recuperar-clave/cambiar: valida el código y guarda la contraseña nueva.
    @PostMapping("/cambiar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiar(@Valid @RequestBody CambiarClave datos) {
        recuperacionService.cambiarClave(datos.correo(), datos.codigo(), datos.nuevaClave());
    }
}
