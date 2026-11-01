
package com.quickhelp.service;

import com.quickhelp.entity.Usuario; // Entidad de usuarios.
import com.quickhelp.repository.UsuarioRepository; // Acceso a PostgreSQL.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.stereotype.Service; // Servicio de Spring.
import org.springframework.transaction.annotation.Transactional; // Control de transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.
import java.security.SecureRandom; // Generación segura de códigos.

@Service // Registra esta clase como servicio.
public class CodigoVinculacionService {

    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Evita caracteres confusos.
    private static final SecureRandom RANDOM = new SecureRandom(); // Generador criptográficamente seguro.
    private final UsuarioRepository usuarioRepository; // Repositorio de usuarios.

    public CodigoVinculacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository; // Inyección de dependencias.
    }

    @Transactional // Guarda el código dentro de una transacción.
    public String generarCodigo(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        Integer idUsuario;

        try {
            idUsuario = Integer.valueOf(authentication.getName()); // Obtiene el ID del JWT.
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token con identificador inválido.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado.")); // Busca al usuario autenticado por su ID.

        if (!Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(usuario.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un adulto mayor activo puede generar códigos."); // Impide generar códigos desde cuentas no
                                                                           // autorizadas.
        }

        if (usuario.getCodigoVinculacion() != null
                && !usuario.getCodigoVinculacion().isBlank()) {
            return usuario.getCodigoVinculacion(); // Devuelve el código permanente existente.
        }

        String codigo;
        int intentos = 0;

        do {
            StringBuilder aleatorio = new StringBuilder("QH-"); // Prefijo de QuickHelp.

            for (int i = 0; i < 10; i++) {
                aleatorio.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
            }

            codigo = aleatorio.toString(); // Código final de 13 caracteres.
            intentos++;

            if (intentos >= 20) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible generar el código.");
            }
        } while (usuarioRepository.existsByCodigoVinculacion(codigo));

        usuario.setCodigoVinculacion(codigo); // Asigna el código al adulto mayor.
        usuarioRepository.save(usuario); // Guarda el código en PostgreSQL.

        return codigo; // Devuelve el código a la aplicación.
    }
}
