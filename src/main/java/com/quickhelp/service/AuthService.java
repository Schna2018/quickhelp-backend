
package com.quickhelp.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.quickhelp.dto.AuthRequestDTO;
import com.quickhelp.dto.AuthResponseDTO;
import com.quickhelp.entity.Usuario;
import com.quickhelp.repository.UsuarioRepository;
import com.quickhelp.dto.RegistroUsuarioDTO; // Datos recibidos durante el registro.
import com.quickhelp.entity.Rol; // Entidad que representa los roles.
import com.quickhelp.repository.RolRepository; // Permite buscar roles en PostgreSQL.
import org.springframework.transaction.annotation.Transactional; // Mantiene el registro y la solicitud en una transacción.

@Service // Gestiona la autenticación de usuarios.
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RolRepository rolRepository; // Consulta el rol del nuevo usuario.
    private final SolicitudVinculacionService solicitudVinculacionService; // Gestiona las solicitudes de vinculación.

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder, RolRepository rolRepository,
            SolicitudVinculacionService solicitudVinculacionService) {
        this.usuarioRepository = usuarioRepository; // Acceso a usuarios.
        this.passwordEncoder = passwordEncoder; // Cifrado de contraseñas.
        this.jwtEncoder = jwtEncoder; // Generación de JWT.
        this.rolRepository = rolRepository; // Acceso a roles.
        this.solicitudVinculacionService = solicitudVinculacionService; // Gestión de solicitudes.
    }

    public AuthResponseDTO login(AuthRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByCorreo(dto.getCorreo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        if (!Boolean.TRUE.equals(usuario.getEstado()) || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario o rol inactivo");
        }

        Instant ahora = Instant.now();
        String rol = usuario.getRol().getCodigo(); // Rol registrado en PostgreSQL.

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("quickhelp")
                .subject(usuario.getIdUsuario().toString()) // Identifica al usuario.
                .issuedAt(ahora)
                .expiresAt(ahora.plus(2, ChronoUnit.HOURS)) // Token válido durante 2 horas.
                .claim("roles", java.util.List.of(rol)) // Autoridades para Spring Security.
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new AuthResponseDTO(token, usuario.getIdUsuario(), usuario.getNombreApellido(), rol);
    }

    @Transactional // Guarda el usuario y su solicitud como una sola operación.
    public void registrarUsuario(RegistroUsuarioDTO dto, String codigoRol) {
        // Normaliza el correo para evitar diferencias por mayúsculas y espacios.
        String correo = dto.getCorreo().trim().toLowerCase(java.util.Locale.ROOT);

        // Comprueba que el correo no esté registrado.
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        // Solo permite registrar los roles habilitados para la aplicación móvil.
        if (!"ADULTO_MAYOR".equals(codigoRol) && !"ASISTENTE".equals(codigoRol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol de registro no permitido");
        }

        // Busca el rol correspondiente en PostgreSQL.
        Rol rol = rolRepository.findByCodigo(codigoRol)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Rol no configurado"));

        // Verifica que el rol esté activo.
        if (!Boolean.TRUE.equals(rol.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol está inactivo");
        }

        // Construye el usuario con los datos validados.
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombreApellido(dto.getNombreApellido().trim());
        usuario.setCorreo(correo);
        usuario.setTelefono(dto.getTelefono() == null ? null : dto.getTelefono().trim());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword())); // Genera el hash BCrypt.
        usuario.setVozActiva(false); // Desactiva inicialmente la activación por voz.
        usuario.setEstado(true); // Habilita la cuenta al registrarse.
        // Configura la palabra predeterminada únicamente para adultos mayores.
        usuario.setFraseActivacion("ADULTO_MAYOR".equals(codigoRol) ? "Ayuda" : null);

        usuarioRepository.save(usuario); // Registra la cuenta en PostgreSQL.

        // Los asistentes deben proporcionar un código para solicitar la vinculación.
        if ("ASISTENTE".equals(codigoRol)) {
            String codigo = dto.getCodigoVinculacion();

            if (codigo == null || codigo.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El código de vinculación es obligatorio.");
            }

            solicitudVinculacionService.crearSolicitud(
                    usuario, codigo.trim().toUpperCase(java.util.Locale.ROOT)); // Crea la solicitud PENDIENTE, sin vincular automáticamente.
        }

    }

}
