package com.quickhelp.controller;

import com.quickhelp.entity.Usuario;
import com.quickhelp.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MiPerfilController {

    private final UsuarioRepository usuarios;

    public MiPerfilController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public record Perfil(
            Integer idUsuario,
            String nombreApellido,
            String correo,
            String telefono,
            String rol
    ) {}

    @GetMapping("/api/mi-perfil")
    public Perfil consultar(@AuthenticationPrincipal Jwt jwt) {
        // El login de Quick Help guarda el ID del usuario en el subject del JWT.
        Integer id;
        try {
            id = Integer.valueOf(jwt == null ? null : jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!Boolean.TRUE.equals(usuario.getEstado())
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return new Perfil(
                usuario.getIdUsuario(),
                usuario.getNombreApellido(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol().getCodigo()
        );
    }
}
