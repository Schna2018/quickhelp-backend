
package com.quickhelp.service;

import com.quickhelp.entity.Usuario; // Entidad de usuarios.
import com.quickhelp.repository.RedApoyoRepository; // Consulta las vinculaciones.
import com.quickhelp.repository.UsuarioRepository; // Consulta las cuentas.
import org.springframework.http.HttpStatus; // Códigos de respuesta HTTP.
import org.springframework.security.core.Authentication; // Identidad autenticada.
import org.springframework.stereotype.Service; // Servicio de Spring.
import org.springframework.transaction.annotation.Transactional; // Transacción de lectura.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@Service
public class RedApoyoAutorizacionService {

    private final RedApoyoRepository redApoyoRepository;
    private final UsuarioRepository usuarioRepository;

    // Recibe los repositorios necesarios para comprobar los permisos.
    public RedApoyoAutorizacionService(RedApoyoRepository redApoyoRepository,
            UsuarioRepository usuarioRepository) {
        this.redApoyoRepository = redApoyoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true) // Solo consulta información.
    public void validarAccesoAsistente(Integer idAdultoMayor,
            Authentication authentication) {

        // Verifica que exista una sesión autenticada.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        // Obtiene el ID del asistente autenticado mediante JWT.
        Integer idAsistente;
        try {
            idAsistente = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }

        // Verifica que la cuenta del asistente esté activa.
        Usuario asistente = usuarioRepository.findById(idAsistente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Acceso no autorizado."));

        if (!Boolean.TRUE.equals(asistente.getEstado())
                || asistente.getRol() == null
                || !Boolean.TRUE.equals(asistente.getRol().getEstado())
                || !"ASISTENTE".equals(asistente.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso no autorizado.");
        }

        // Comprueba que el adulto mayor exista y tenga su cuenta activa.
        Usuario adulto = usuarioRepository.findById(idAdultoMayor)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Acceso no autorizado."));

        if (!Boolean.TRUE.equals(adulto.getEstado())
                || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso no autorizado.");
        }

        // Solo autoriza relaciones activas entre ambos usuarios.
        boolean vinculado = redApoyoRepository
                .existsByAdultoMayor_IdUsuarioAndIntegrante_IdUsuarioAndEstadoTrue(
                        idAdultoMayor, idAsistente);

        if (!vinculado) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes una vinculación activa con este adulto mayor.");
        }
    }
}
