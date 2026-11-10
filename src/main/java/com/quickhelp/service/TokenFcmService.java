
package com.quickhelp.service;

import com.quickhelp.dto.TokenFcmRequestDTO; // DTO que recibe el token FCM.
import com.quickhelp.entity.Usuario; // Entidad usuario.
import com.quickhelp.repository.UsuarioRepository; // Acceso a PostgreSQL.
import org.springframework.security.access.AccessDeniedException; // Error de autorización.
import org.springframework.security.core.Authentication; // Usuario autenticado mediante JWT.
import org.springframework.stereotype.Service; // Servicio de Spring Boot.
import org.springframework.transaction.annotation.Transactional; // Manejo de transacciones.

@Service // Registra el servicio en Spring.
public class TokenFcmService {

    private final UsuarioRepository usuarioRepository;

    public TokenFcmService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional // Guarda el cambio en una transacción.
    public void registrarToken(TokenFcmRequestDTO request, Authentication authentication) {

        // Obtiene el ID del usuario autenticado desde el JWT.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }

        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Identidad de usuario inválida.");
        }

        // Busca al usuario que inició sesión.
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // Impide registrar tokens para usuarios desactivados.
        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new AccessDeniedException("El usuario está desactivado.");
        }

        // Actualiza el token FCM del dispositivo móvil.
        usuario.setTokenFcm(request.tokenFcm().trim());

        // Guarda el cambio en PostgreSQL.
        usuarioRepository.save(usuario);
    }
}
