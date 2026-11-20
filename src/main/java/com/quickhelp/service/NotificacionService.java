
package com.quickhelp.service;

import com.quickhelp.dto.NotificacionAdminResponseDTO; // Datos del panel administrativo.
import com.quickhelp.dto.NotificacionResponseDTO; // Datos de notificaciones móviles.
import com.quickhelp.entity.Alerta; // Emergencia asociada.
import com.quickhelp.entity.Notificacion; // Registro de notificación.
import com.quickhelp.entity.RedApoyo; // Integrantes de apoyo.
import com.quickhelp.entity.Rol; // Rol del administrador.
import com.quickhelp.entity.Usuario; // Usuario y destinatario.
import com.quickhelp.repository.NotificacionRepository; // Repositorio de notificaciones.
import com.quickhelp.repository.RedApoyoRepository; // Repositorio de red de apoyo.
import com.quickhelp.repository.UsuarioRepository; // Repositorio de usuarios.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Identidad JWT.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.
import java.time.LocalDateTime; // Fechas de programación.
import java.util.List; // Listas de registros.

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final RedApoyoRepository redApoyoRepository;
    private final UsuarioRepository usuarioRepository;

    // Inyecta los repositorios necesarios.
    public NotificacionService(
            NotificacionRepository notificacionRepository,
            RedApoyoRepository redApoyoRepository,
            UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.redApoyoRepository = redApoyoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional // Registra las notificaciones dentro de una transacción.
    public void generarNotificacionesEmergencia(Alerta alerta) {

        // Obtiene el adulto mayor que activó la emergencia.
        Integer idAdultoMayor = alerta.getAdultoMayor().getIdUsuario();

        // Recupera su red de apoyo ordenada por prioridad.
        List<RedApoyo> integrantes = redApoyoRepository
                .findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(idAdultoMayor);

        // Recorre los integrantes vinculados.
        for (RedApoyo integrante : integrantes) {

            // Omite las vinculaciones desactivadas.
            if (!Boolean.TRUE.equals(integrante.getEstado())) {
                continue;
            }

            // Obtiene el identificador del destinatario.
            Integer idDestinatario = integrante.getIntegrante().getIdUsuario();

            // Evita generar otra notificación inicial para el mismo usuario.
            boolean existe = notificacionRepository
                    .existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanal(
                            alerta.getIdAlerta(), idDestinatario, "PUSH");

            if (existe) {
                continue;
            }

            // Construye la notificación inicial pendiente.
            Notificacion notificacion = new Notificacion();
            notificacion.setAlerta(alerta);
            notificacion.setDestinatario(integrante.getIntegrante());
            notificacion.setNumeroIntento(0);
            notificacion.setCanal("PUSH");
            notificacion.setTipoAviso("ALERTA_INICIAL"); // Conserva el tipo de aviso.
            notificacion.setEstadoEnvio("PENDIENTE");
            notificacion.setFechaProgramada(LocalDateTime.now());
            notificacion.setFechaEnvio(null);
            notificacion.setResultado(null);

            // Guarda el registro en PostgreSQL.
            notificacionRepository.save(notificacion);
        }
    }

    @Transactional(readOnly = true) // Consulta sin modificar la base de datos.
    public List<NotificacionResponseDTO> listarMisNotificaciones(
            Authentication authentication) {

        Integer idUsuario = obtenerIdAutenticado(authentication);

        // Consulta exclusivamente las notificaciones del usuario autenticado.
        return notificacionRepository
                .findByDestinatario_IdUsuarioOrderByIdNotificacionDesc(idUsuario)
                .stream()
                .map(notificacion -> new NotificacionResponseDTO(
                        notificacion.getIdNotificacion(),
                        notificacion.getAlerta().getIdAlerta(),
                        notificacion.getAlerta().getAdultoMayor().getIdUsuario(),
                        notificacion.getAlerta().getEstadoActual().getCodigo(),
                        notificacion.getCanal(),
                        notificacion.getEstadoEnvio(),
                        notificacion.getFechaProgramada(),
                        notificacion.getFechaEnvio()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacionAdminResponseDTO> listarNotificacionesPorAlerta(
            Integer idAlerta, Authentication authentication) {

        Usuario administrador = usuarioRepository.findById(obtenerIdAutenticado(authentication))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario no autorizado."));

        // Solo un administrador activo puede consultar avisos de otros usuarios.
        Rol rol = administrador.getRol();
        if (!Boolean.TRUE.equals(administrador.getEstado())
                || rol == null
                || !Boolean.TRUE.equals(rol.getEstado())
                || !"ADMIN".equals(rol.getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Acceso exclusivo para administradores.");
        }

        return notificacionRepository.findByAlerta_IdAlertaOrderByIdNotificacionDesc(idAlerta)
                .stream()
                .map(this::convertirNotificacionAdministrativa)
                .toList();
    }

    private NotificacionAdminResponseDTO convertirNotificacionAdministrativa(Notificacion notificacion) {
        Usuario destinatario = notificacion.getDestinatario();
        String resultado = notificacion.getResultado();
        String tipoAviso = notificacion.getTipoAviso(); // Clasificación independiente del resultado.

        // Compatibilidad con registros anteriores que guardaban el tipo en resultado.
        if (tipoAviso == null
                && ("ESCALAMIENTO_ADMIN".equals(resultado) || "REAVISO_RED".equals(resultado))) {
            tipoAviso = resultado;
            resultado = null;
        }

        return new NotificacionAdminResponseDTO(
                notificacion.getIdNotificacion(),
                notificacion.getAlerta().getIdAlerta(),
                destinatario.getIdUsuario(),
                destinatario.getNombreApellido(),
                tipoAviso,
                notificacion.getCanal(),
                notificacion.getEstadoEnvio(),
                notificacion.getNumeroIntento(),
                notificacion.getFechaProgramada(),
                notificacion.getFechaEnvio(),
                resultado
        );
    }

    // Identifica al usuario mediante el sujeto del JWT.
    private Integer obtenerIdAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de usuario no válido.");
        }
    }
}
