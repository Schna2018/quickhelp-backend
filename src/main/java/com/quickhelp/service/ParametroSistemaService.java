
package com.quickhelp.service;

import com.quickhelp.entity.Auditoria; // Registro de cambios.
import com.quickhelp.entity.ParametroSistema; // Configuración del sistema.
import com.quickhelp.entity.Usuario; // Usuario administrador.
import com.quickhelp.repository.AuditoriaRepository; // Persistencia de auditoría.
import com.quickhelp.repository.ParametroSistemaRepository; // Acceso a parámetros.
import com.quickhelp.repository.UsuarioRepository; // Acceso a usuarios.
import java.time.LocalDateTime; // Fecha de actualización.
import java.util.List; // Listado de parámetros.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@Service // Administra las configuraciones de QuickHelp.
public class ParametroSistemaService {

    private final ParametroSistemaRepository parametroRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    // Inyecta los repositorios existentes.
    public ParametroSistemaService(
            ParametroSistemaRepository parametroRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaRepository auditoriaRepository) {

        this.parametroRepository = parametroRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    // Lista los parámetros para un administrador autorizado.
    @Transactional(readOnly = true)
    public List<ParametroSistema> listar(Authentication authentication) {
        validarAdministrador(authentication);
        return parametroRepository.findAll();
    }

    // Actualiza el valor de un parámetro existente.
    @Transactional
    public ParametroSistema actualizar(
            Integer idParametro,
            String nuevoValor,
            Authentication authentication) {

        Usuario administrador = validarAdministrador(authentication);

        ParametroSistema parametro = parametroRepository.findById(idParametro)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Parámetro no encontrado."));

        if (nuevoValor == null || nuevoValor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El valor no puede estar vacío.");
        }

        String valorValidado = nuevoValor.trim();

        // Comprueba que el valor coincida con el tipo de dato.
        validarTipoDato(parametro.getTipoDato(), valorValidado);
        validarReglasParametro(parametro.getClave(), valorValidado); // Comprueba límites de configuración.

        String valorAnterior = parametro.getValor();

        parametro.setValor(valorValidado);
        parametro.setUsuarioActualiza(administrador);
        parametro.setFechaActualizacion(LocalDateTime.now());

        ParametroSistema actualizado = parametroRepository.save(parametro);

        // Registra el cambio en auditoría.
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(administrador);
        auditoria.setEntidad("parametrosistema");
        auditoria.setIdRegistro(idParametro);
        auditoria.setAccion("ACTUALIZACION_PARAMETRO");
        auditoria.setDetalle("Parámetro: " + parametro.getClave()
                + ". Valor anterior: " + valorAnterior
                + ". Valor nuevo: " + valorValidado);
        auditoriaRepository.save(auditoria);

        return actualizado;
    }

    // Valida los tipos de datos permitidos.
    private void validarTipoDato(String tipoDato, String valor) {
        switch (tipoDato.toUpperCase()) {
            case "INTEGER":
                try {
                    Integer.parseInt(valor);
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Se requiere un número entero.");
                }
                break;
            case "BOOLEAN":
                if (!"true".equalsIgnoreCase(valor)
                        && !"false".equalsIgnoreCase(valor)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Se requiere true o false.");
                }
                break;
            case "STRING":
                break;
            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Tipo de dato no admitido.");
        }
    }

    
    
    // Permite modificar únicamente parámetros reconocidos por QuickHelp.
    private void validarReglasParametro(String clave, String valor) {
        switch (clave) {
            case "NOTIFICACIONES_HABILITADAS":
                if (!"true".equalsIgnoreCase(valor)
                        && !"false".equalsIgnoreCase(valor)) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El valor debe ser true o false.");
                }
                break;
            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Este parámetro no está habilitado para modificación.");
        }
    }

    // Comprueba que el usuario autenticado sea administrador activo.
    private Usuario validarAdministrador(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token inválido.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario no autorizado."));

        if (!Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())
                || !"ADMINISTRADOR".equals(usuario.getRol().getCodigo())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador activo puede modificar parámetros.");
        }

        return usuario;
    }
}
