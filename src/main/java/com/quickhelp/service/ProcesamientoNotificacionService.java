
package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessagingException; // Errores de Firebase.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.repository.AlertaRepository; // Consulta estados de alertas.
import com.quickhelp.repository.NotificacionRepository; // Repositorio de notificaciones.
import java.time.LocalDateTime; // Fecha y hora de los envíos.
import java.util.HashSet; // Evita procesamientos repetidos.
import java.util.List; // Listado de notificaciones.
import java.util.Set; // Identificadores de alertas.
import org.springframework.stereotype.Service; // Servicio Spring Boot.

@Service
public class ProcesamientoNotificacionService {

    private static final String TIPO_INICIAL = "INICIAL_SOS";
    private static final String TIPO_ADMIN = "ESCALAMIENTO_ADMIN";
    private static final String TIPO_REAVISO = "REAVISO_RED";
    private static final int MAX_INTENTOS = 3;
    private static final int ESPERA_REINTENTO_MINUTOS = 2;

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
            if (!"PENDIENTE".equals(notificacion.getEstadoEnvio())) {
                continue;
            }

            if (notificacion.getFechaProgramada() != null
                    && notificacion.getFechaProgramada().isAfter(LocalDateTime.now())) {
                continue;
            }

            if (!alertaActiva(idAlerta)) {
                continue;
            }

            Integer intentosAnteriores = notificacion.getNumeroIntento();
            int intentos = intentosAnteriores == null ? 0 : intentosAnteriores;

            // No supera el máximo de intentos permitidos.
            if (intentos >= MAX_INTENTOS) {
                if (notificacionReservaService.reservar(notificacion.getIdNotificacion())) {
                    finalizarObligatoriamente(
                            notificacion.getIdNotificacion(),
                            "ERROR",
                            null,
                            "Se alcanzó el máximo de intentos permitidos.");
                }
                continue;
            }

            // Reserva el envío antes de contactar Firebase.
            if (!notificacionReservaService.reservar(notificacion.getIdNotificacion())) {
                continue;
            }

            int intentoActual = intentos + 1;
            String estado = "ERROR";
            String resultado;
            boolean errorTemporal = false;

            try {
                if (!alertaActiva(idAlerta)) {
                    resultado = "Envío omitido: la alerta ya no está activa.";
                } else {
                    String tokenFcm = notificacion.getDestinatario().getTokenFcm();

                    if (tokenFcm == null || tokenFcm.isBlank()) {
                        resultado = "El destinatario no tiene un token FCM válido.";
                    } else {
                        String tipoAviso = notificacion.getTipoAviso();
                        String titulo = "¡Alerta de emergencia!";
                        String mensaje = "Un integrante de tu red de apoyo necesita asistencia.";

                        if (TIPO_ADMIN.equals(tipoAviso)) {
                            titulo = "¡SOS sin atención!";
                            mensaje = "Una emergencia requiere intervención administrativa.";
                        } else if (TIPO_REAVISO.equals(tipoAviso)) {
                            titulo = "¡Recordatorio de emergencia!";
                            mensaje = "La alerta SOS continúa activa y necesita asistencia.";
                        } else if (TIPO_INICIAL.equals(tipoAviso) || tipoAviso == null) {
                            // Mantiene el mensaje inicial.
                        }

                        resultado = firebaseMessagingService.enviarNotificacion(
                                tokenFcm, idAlerta, titulo, mensaje);
                        estado = "ENVIADA";
                    }
                }
            } catch (FirebaseMessagingException e) {
                resultado = obtenerMensajeError(e);
                errorTemporal = esErrorTemporalFirebase(e);
            } catch (RuntimeException e) {
                resultado = obtenerMensajeError(e);
                // No reintenta automáticamente errores internos desconocidos.
            }

            // Reprograma solamente errores temporales y con intentos disponibles.
            if (errorTemporal && intentoActual < MAX_INTENTOS && alertaActiva(idAlerta)) {
                LocalDateTime proximoIntento = LocalDateTime.now()
                        .plusMinutes(ESPERA_REINTENTO_MINUTOS);

                boolean reprogramada = notificacionFinalizacionService.reprogramar(
                        notificacion.getIdNotificacion(),
                        proximoIntento,
                        resultado);

                if (!reprogramada) {
                    throw new IllegalStateException(
                            "No se pudo reprogramar la notificación "
                                    + notificacion.getIdNotificacion());
                }
                continue;
            }

            finalizarObligatoriamente(
                    notificacion.getIdNotificacion(),
                    estado,
                    "ENVIADA".equals(estado) ? LocalDateTime.now() : null,
                    resultado);
        }
    }

    // Procesa las notificaciones programadas cuya fecha ya se cumplió.
    public void procesarPendientesProgramadas() {
        List<Notificacion> notificaciones = notificacionRepository
                .buscarPendientesProgramadas(LocalDateTime.now());

        Set<Integer> alertasProcesadas = new HashSet<>();

        for (Notificacion notificacion : notificaciones) {
            Integer idAlerta = notificacion.getAlerta().getIdAlerta();

            if (alertasProcesadas.add(idAlerta)) {
                procesarPendientes(idAlerta);
            }
        }
    }

    // Identifica errores temporales de Firebase.
    private boolean esErrorTemporalFirebase(FirebaseMessagingException e) {
        if (e.getMessagingErrorCode() != null) {
            switch (e.getMessagingErrorCode()) {
                case UNAVAILABLE:
                case INTERNAL:
                case THIRD_PARTY_AUTH_ERROR:
                    return true;
                default:
                    return false;
            }
        }

        return e.getErrorCode() == com.google.firebase.ErrorCode.UNAVAILABLE
                || e.getErrorCode() == com.google.firebase.ErrorCode.INTERNAL
                || e.getErrorCode() == com.google.firebase.ErrorCode.RESOURCE_EXHAUSTED;
    }

    // Obtiene un mensaje de error sin perder el motivo del fallo.
    private String obtenerMensajeError(Exception e) {
        return e.getMessage() != null
                ? e.getMessage()
                : e.getClass().getSimpleName();
    }

    // Confirma la finalización o informa de una inconsistencia.
    private void finalizarObligatoriamente(Integer idNotificacion,
            String estado, LocalDateTime fechaEnvio, String resultado) {

        boolean finalizada = notificacionFinalizacionService.finalizar(
                idNotificacion, estado, fechaEnvio, resultado);

        if (!finalizada) {
            throw new IllegalStateException(
                    "No se pudo finalizar la notificación " + idNotificacion);
        }
    }

    // Comprueba que la emergencia continúe activa.
    private boolean alertaActiva(Integer idAlerta) {
        return alertaRepository.existsByIdAlertaAndEstadoActual_Codigo(
                idAlerta, "ACTIVA");
    }
}
