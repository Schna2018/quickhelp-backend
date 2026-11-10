
package com.quickhelp.service;

import com.quickhelp.entity.Usuario; // Entidad de usuarios.
import com.quickhelp.repository.RolPermisoRepository; // Consulta los permisos de cada rol.
import com.quickhelp.repository.UsuarioRepository; // Consulta usuarios en PostgreSQL.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Identidad autenticada.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones de lectura.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@Service // Centraliza las validaciones de permisos del backend.
public class PermisoService {
    private final UsuarioRepository usuarioRepository;
    private final RolPermisoRepository rolPermisoRepository;

    public PermisoService(UsuarioRepository usuarioRepository,
                          RolPermisoRepository rolPermisoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolPermisoRepository = rolPermisoRepository;
    }

    @Transactional(readOnly = true) // Consulta el permiso sin modificar registros.
    public boolean tienePermiso(Authentication authentication, String codigoPermiso) {
        if (codigoPermiso == null || codigoPermiso.isBlank()) {
       return false; // Rechaza permisos vacíos o no definidos.
      }
        if (authentication == null || !authentication.isAuthenticated()) {
            return false; // Rechaza solicitudes sin autenticación.
        }

        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(authentication.getName()); // ID obtenido del JWT.
        } catch (NumberFormatException | NullPointerException e) {
            return false; // El identificador no es válido.
        }

        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {
            return false; // Usuario o rol inexistente o inactivo.
        }

        return rolPermisoRepository.existsByRol_IdRolAndPermiso_Codigo(
                usuario.getRol().getIdRol(), codigoPermiso); // Consulta la asignación real.
    }

    @Transactional(readOnly = true) // Exige el permiso para continuar.
    public void exigirPermiso(Authentication authentication, String codigoPermiso) {
        if (!tienePermiso(authentication, codigoPermiso)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta operación.");
        }
    }
}
