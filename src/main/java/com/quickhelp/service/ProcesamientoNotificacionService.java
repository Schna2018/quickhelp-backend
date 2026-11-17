
package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessagingException; // Errores de Firebase.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.repository.NotificacionRepository; // Repositorio existente.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import java.time.LocalDateTime; // Fecha y hora del envío.
import java.util.List; // Colección de notificaciones.

@Service
public class ProcesamientoNotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final NotificacionFinalizacionService notificacionFinalizacionService;
    private final NotificacionReservaService notificacionReservaService;

    public ProcesamientoNotificacionService(
            NotificacionRepository notificacionRepository,
            FirebaseMessagingService firebaseMessagingService,
            NotificacionReservaService notificacionReservaService,
            NotificacionFinalizacionService notificacionFinalizacionService) {
        this.notificacionRepository = notificacionRepository;
        this.firebaseMessagingService = firebaseMessagingService;
        this.notificacionReservaService = notificacionReservaService;
        this.notificacionFinalizacionService = notificacionFinalizacionService;
    }

    // Procesa las notificaciones pendientes de una alerta específica.
    public void procesarPendientes(Integer idAlerta) {

        List<Notificacion> notificaciones = notificacionRepository
                .findByAlerta_IdAlertaOrderByIdNotificacionDesc(idAlerta);

        for (Notificacion notificacion : notificaciones) {

            // Omite las notificaciones que no están pendientes.
            if (!"PENDIENTE".equals(notificacion.getEstadoEnvio())) {
                continue;
            }

            // Evita enviar notificaciones antes de su fecha programada.
            if (notificacion.getFechaProgramada() != null
                    && notificacion.getFechaProgramada().isAfter(LocalDateTime.now())) {
                continue;
            }

            // Reserva el envío para evitar procesarlo simultáneamente.
            if (!notificacionReservaService.reservar(notificacion.getIdNotificacion())) {
                continue;
            }

            String estado = "ERROR";
            String resultado;

            try {
                String tokenFcm = notificacion.getDestinatario().getTokenFcm();

                resultado = firebaseMessagingService.enviarNotificacion(
                        tokenFcm,
                        idAlerta,
                        "¡Alerta de emergencia!",
                        "Un integrante de tu red de apoyo necesita asistencia.");

                estado = "ENVIADA";

            } catch (FirebaseMessagingException | RuntimeException e) {
                resultado = e.getMessage() != null
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
            }

            // Registra el resultado del intento en PostgreSQL.
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

    // Procesa las notificaciones pendientes cuya fecha programada ya se cumplió.
    // Se utilizará para ejecutar los reavisos automáticos.
    public void procesarPendientesProgramadas() {

        List<Notificacion> notificaciones = notificacionRepository
                .buscarPendientesProgramadas(LocalDateTime.now());

        for (Notificacion notificacion : notificaciones) {
            procesarPendientes(notificacion.getAlerta().getIdAlerta());
        }
    }
}
