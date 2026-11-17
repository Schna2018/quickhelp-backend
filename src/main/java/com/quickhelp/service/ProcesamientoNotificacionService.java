
package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessagingException; // Errores de Firebase.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.repository.AlertaRepository; // Consulta de estados de alertas.
import com.quickhelp.repository.NotificacionRepository; // Repositorio existente.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import java.time.LocalDateTime; // Fecha y hora del envío.
import java.util.List; // Colección de notificaciones.

@Service
public class ProcesamientoNotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final AlertaRepository alertaRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final NotificacionFinalizacionService notificacionFinalizacionService;
    private final NotificacionReservaService notificacionReservaService;

    public ProcesamientoNotificacionService(
            NotificacionRepository notificacionRepository,
            AlertaRepository alertaRepository,
            FirebaseMessagingService firebaseMessagingService,
            NotificacionReservaService notificacionReservaService,
            NotificacionFinalizacionService notificacionFinalizacionService) {
        this.notificacionRepository = notificacionRepository;
        this.alertaRepository = alertaRepository;
        this.firebaseMessagingService = firebaseMessagingService;
        this.notificacionReservaService = notificacionReservaService;
        this.notificacionFinalizacionService = notificacionFinalizacionService;
    }

    // Procesa las notificaciones pendientes de una alerta específica.
    public void procesarPendientes(Integer idAlerta) {

        List<Notificacion> notificaciones = notificacionRepository
                .findByAlerta_IdAlertaOrderByIdNotificacionDesc(idAlerta);

        for (Notificacion notificacion : notificaciones) {

            // Solo procesa notificaciones pendientes.
            if (!"PENDIENTE".equals(notificacion.getEstadoEnvio())) {
                continue;
            }

            // Respeta la fecha programada de cada notificación.
            if (notificacion.getFechaProgramada() != null
                    && notificacion.getFechaProgramada().isAfter(LocalDateTime.now())) {
                continue;
            }

            // No envía reavisos cuando la alerta ya no está activa.
            if (!alertaActiva(idAlerta)) {
                continue;
            }

            // Reserva el envío para evitar procesamientos simultáneos.
            if (!notificacionReservaService.reservar(notificacion.getIdNotificacion())) {
                continue;
            }

            String estado = "ERROR";
            String resultado;

            try {
                // Vuelve a comprobar el estado antes de llamar a Firebase.
                if (!alertaActiva(idAlerta)) {
                    estado = "CANCELADA";
                    resultado = "Notificación omitida: la alerta ya no está activa.";
                } else {
                    String tokenFcm = notificacion.getDestinatario().getTokenFcm();

                    resultado = firebaseMessagingService.enviarNotificacion(
                            tokenFcm,
                            idAlerta,
                            "¡Alerta de emergencia!",
                            "Un integrante de tu red de apoyo necesita asistencia.");

                    estado = "ENVIADA";
                }

            } catch (FirebaseMessagingException | RuntimeException e) {
                resultado = e.getMessage() != null
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
            }

            // Guarda el resultado del procesamiento en PostgreSQL.
            boolean finalizada = notificacionFinalizacionService.finalizar(
                    notificacion.getIdNotificacion(),
                    estado,
                    "ENVIADA".equals(estado) ? LocalDateTime.now() : null,
                    resultado);

            if (!finalizada) {
                throw new IllegalStateException(
                        "No se pudo finalizar la notificación "
                                + notificacion.getIdNotificacion());
            }
        }
    }

    // Procesa las notificaciones programadas cuya fecha ya se cumplió.
    public void procesarPendientesProgramadas() {

        List<Notificacion> notificaciones = notificacionRepository
                .buscarPendientesProgramadas(LocalDateTime.now());

        for (Notificacion notificacion : notificaciones) {
            procesarPendientes(notificacion.getAlerta().getIdAlerta());
        }
    }

    // Comprueba que la alerta siga activa antes de enviar.
    private boolean alertaActiva(Integer idAlerta) {

        return alertaRepository.findById(idAlerta)
                .map(alerta -> "ACTIVA".equals(
                        alerta.getEstadoActual().getCodigo()))
                .orElse(false);
    }
}
