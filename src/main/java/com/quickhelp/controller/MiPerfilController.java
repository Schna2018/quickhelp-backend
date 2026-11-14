package com.quickhelp.controller;

import com.quickhelp.entity.Usuario;
import com.quickhelp.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MiPerfilController {

    private final UsuarioRepository usuarios;

    public MiPerfilController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    // Datos que recibe la app al consultar o guardar el perfil.
    public record Perfil(
            Integer idUsuario,
            String nombreApellido,
            String correo,
            String telefono,
            String rol
    ) {}

    // El formulario envía los tres datos personales. El teléfono es opcional.
    public record EditarPerfil(
            @NotBlank @Size(max = 150) String nombreApellido,
            @NotBlank @Email @Size(max = 150) String correo,
            @Size(max = 30) String telefono
    ) {
        public EditarPerfil {
            if (nombreApellido != null) nombreApellido = nombreApellido.strip();
            if (correo != null) correo = correo.strip();
            if (telefono != null) {
                telefono = telefono.strip();
                if (telefono.isEmpty()) telefono = null;
            }
        }
    }

    @GetMapping("/api/mi-perfil")
    @Transactional(readOnly = true)
    public Perfil consultar(@AuthenticationPrincipal Jwt jwt) {
        return convertirPerfil(obtenerUsuarioActivo(jwt));
    }

    @PutMapping("/api/mi-perfil")
    @Transactional
    public Perfil actualizar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EditarPerfil cambios) {

        Usuario usuario = obtenerUsuarioActivo(jwt);

        // Permite conservar el correo propio y detecta si pertenece a otra cuenta.
        if (!cambios.correo().equals(usuario.getCorreo())) {
            usuarios.findByCorreo(cambios.correo()).ifPresent(otraCuenta -> {
                if (!otraCuenta.getIdUsuario().equals(usuario.getIdUsuario())) {
                    throw correoOcupado();
                }
            });
        }

        usuario.setNombreApellido(cambios.nombreApellido());
        usuario.setCorreo(cambios.correo());
        usuario.setTelefono(cambios.telefono());

        try {
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            // PostgreSQL también comprueba la unicidad si dos cuentas guardan a la vez.
            if (ex.getMostSpecificCause() instanceof SQLException sql
                    && "23505".equals(sql.getSQLState())) {
                throw correoOcupado();
            }
            throw ex;
        }

        return convertirPerfil(usuario);
    }

    private Usuario obtenerUsuarioActivo(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Usuario usuario = usuarios.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return usuario;
    }

    private Perfil convertirPerfil(Usuario usuario) {
        return new Perfil(
                usuario.getIdUsuario(),
                usuario.getNombreApellido(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol().getCodigo()
        );
    }

    private ResponseStatusException correoOcupado() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "El correo ya está registrado por otra cuenta.");
    }
}