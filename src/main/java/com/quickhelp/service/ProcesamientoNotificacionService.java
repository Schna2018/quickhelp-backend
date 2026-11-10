
package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessagingException; // Errores de Firebase.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.repository.NotificacionRepository; // Repositorio existente.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
//import org.springframework.transaction.annotation.Transactional; // Control de transacciones.
import java.time.LocalDateTime; // Fecha y hora del envío.
import java.util.List; // Colección de notificaciones.

@Service // Registra el servicio en Spring Boot.
public class ProcesamientoNotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final NotificacionFinalizacionService notificacionFinalizacionService; // Guarda el resultado del envío.
    private final NotificacionReservaService notificacionReservaService; // Reserva los envíos.

    // Inyecta los servicios necesarios para procesar las notificaciones.
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

    // Procesa únicamente las notificaciones pendientes de una alerta.
    public void procesarPendientes(Integer idAlerta) {

        // Consulta las notificaciones asociadas a la emergencia.
        List<Notificacion> notificaciones = notificacionRepository
                .findByAlerta_IdAlertaOrderByIdNotificacionDesc(idAlerta);

        for (Notificacion notificacion : notificaciones) {

            // Reserva la notificación antes de enviarla.
            if (!notificacionReservaService.reservar(notificacion.getIdNotificacion())) {
                continue; // Otro proceso la reservó o ya fue procesada.
            }

            String estado = "ERROR"; // Estado inicial ante un fallo.
            String resultado;

            try {
                // Obtiene el token del asistente destinatario.
                String tokenFcm = notificacion.getDestinatario().getTokenFcm();

                resultado = firebaseMessagingService.enviarNotificacion(
                        tokenFcm,
                        idAlerta, // Utiliza el ID recibido, sin cargar nuevamente la alerta.
                        "¡Alerta de emergencia!",
                        "Un integrante de tu red de apoyo necesita asistencia.");

                estado = "ENVIADA"; // Firebase aceptó el mensaje.

          
            } catch (FirebaseMessagingException | RuntimeException e) {
                // Registra los errores de Firebase y otros fallos de ejecución.
                resultado = e.getMessage() != null
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
            }


            // Guarda el resultado sin sobrescribir numero_intento.
            boolean finalizada = notificacionFinalizacionService.finalizar(
                    notificacion.getIdNotificacion(),
                    estado,
                    "ENVIADA".equals(estado) ? LocalDateTime.now() : null,
                    resultado);

            if (!finalizada) {
                throw new IllegalStateException(
                        "No se pudo finalizar la notificación " + notificacion.getIdNotificacion());
            }
        }
    }
}
