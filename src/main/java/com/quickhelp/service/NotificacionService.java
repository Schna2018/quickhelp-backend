
package com.quickhelp.service;

import com.quickhelp.dto.NotificacionResponseDTO; // Datos de salida.
import com.quickhelp.entity.Alerta; // Emergencia que origina las notificaciones.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.entity.RedApoyo; // Vinculación con integrantes de apoyo.
import com.quickhelp.repository.NotificacionRepository; // Guarda las notificaciones.
import com.quickhelp.repository.RedApoyoRepository; // Consulta la red de apoyo.
import java.time.LocalDateTime; // Fecha de programación.
import java.util.List; // Lista de integrantes.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Identidad JWT.
import org.springframework.stereotype.Service; // Servicio administrado por Spring.
import org.springframework.transaction.annotation.Transactional; // Control transaccional.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@Service
public class NotificacionService {

    private static final String TIPO_INICIAL = "INICIAL_SOS";
    private static final String CANAL_PUSH = "PUSH";

    private final NotificacionRepository notificacionRepository;
    private final RedApoyoRepository redApoyoRepository;

    // Inyecta los repositorios necesarios.
    public NotificacionService(
            NotificacionRepository notificacionRepository,
            RedApoyoRepository redApoyoRepository) {
        this.notificacionRepository = notificacionRepository;
        this.redApoyoRepository = redApoyoRepository;
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
                            alerta.getIdAlerta(), idDestinatario, CANAL_PUSH);

            if (existe) {
                continue;
            }

            // Construye la notificación inicial pendiente.
            Notificacion notificacion = new Notificacion();
            notificacion.setAlerta(alerta);
            notificacion.setDestinatario(integrante.getIntegrante());
            notificacion.setNumeroIntento(0);
            notificacion.setCanal(CANAL_PUSH);
            notificacion.setEstadoEnvio("PENDIENTE");
            notificacion.setFechaProgramada(LocalDateTime.now());
            notificacion.setFechaInicioProceso(null);
            notificacion.setFechaEnvio(null);
            notificacion.setResultado(null);

            // Identifica permanentemente la primera notificación SOS.
            notificacion.setTipoAviso(TIPO_INICIAL);

            // Guarda el registro en PostgreSQL.
            notificacionRepository.save(notificacion);
        }
    }

    @Transactional(readOnly = true) // Consulta sin modificar la base de datos.
    public List<NotificacionResponseDTO> listarMisNotificaciones(
            Authentication authentication) {

        // Comprueba que exista una sesión autenticada.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        // Obtiene el ID del usuario desde el token JWT.
        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de usuario no válido.");
        }

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
}
