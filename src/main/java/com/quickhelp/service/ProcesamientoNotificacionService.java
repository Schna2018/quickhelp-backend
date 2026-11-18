
package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessagingException; // Errores de Firebase.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.repository.AlertaRepository; // Consulta estados de alertas.
import com.quickhelp.repository.NotificacionRepository; // Repositorio de notificaciones.
import java.time.LocalDateTime; // Fecha y hora.
import java.util.HashSet; // Evita procesamientos repetidos.
import java.util.List; // Listado de notificaciones.
import java.util.Set; // Identificadores de alertas.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import org.springframework.transaction.PlatformTransactionManager; // Gestor transaccional.
import org.springframework.transaction.support.TransactionTemplate; // Transacciones independientes.

@Service
public class ProcesamientoNotificacionService {

    private static final String TIPO_INICIAL = "INICIAL_SOS";
    private static final String TIPO_ADMIN = "ESCALAMIENTO_ADMIN";
    private static final String TIPO_REAVISO = "REAVISO_RED";
    private static final int MAX_INTENTOS = 3;
    private static final int ESPERA_REINTENTO_MINUTOS = 2;
    private static final int TIEMPO_RECUPERACION_MINUTOS = 5;

    private final NotificacionRepository notificacionRepository;
    private final AlertaRepository alertaRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final NotificacionFinalizacionService notificacionFinalizacionService;
    private final NotificacionReservaService notificacionReservaService;
    private final TransactionTemplate transactionTemplate;

    public ProcesamientoNotificacionService(
            NotificacionRepository notificacionRepository,
            AlertaRepository alertaRepository,
            FirebaseMessagingService firebaseMessagingService,
            NotificacionReservaService notificacionReservaService,
            NotificacionFinalizacionService notificacionFinalizacionService,
            PlatformTransactionManager transactionManager) {
        this.notificacionRepository = notificacionRepository;
        this.alertaRepository = alertaRepository;
        this.firebaseMessagingService = firebaseMessagingService;
        this.notificacionReservaService = notificacionReservaService;
        this.notificacionFinalizacionService = notificacionFinalizacionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Procesa las notificaciones pendientes de una alerta.
    public void procesarPendientes(Integer idAlerta) {
        List<Notificacion> notificaciones = notificacionRepository
                .findByAlerta_IdAlertaOrderByIdNotificacionDesc(idAlerta);

        for (Notificacion notificacion : notificaciones) {
            if (!"PENDIENTE".equals(notificacion.getEstadoEnvio())) continue;
            if (notificacion.getFechaProgramada() != null
                    && notificacion.getFechaProgramada().isAfter(LocalDateTime.now())) continue;
            if (!alertaActiva(idAlerta)) continue;

            Integer intentosAnteriores = notificacion.getNumeroIntento();
            int intentos = intentosAnteriores == null ? 0 : intentosAnteriores;

            // El máximo de intentos también se controla en el repositorio.
            if (intentos >= MAX_INTENTOS) continue;
            if (!notificacionReservaService.reservar(notificacion.getIdNotificacion())) continue;

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
            }

            // Reintenta errores temporales sin superar tres intentos.
            if (errorTemporal && intentoActual < MAX_INTENTOS && alertaActiva(idAlerta)) {
                LocalDateTime proximoIntento = LocalDateTime.now()
                        .plusMinutes(ESPERA_REINTENTO_MINUTOS);

                boolean reprogramada = notificacionFinalizacionService.reprogramar(
                        notificacion.getIdNotificacion(), proximoIntento, resultado);

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

    // Recupera registros que permanecieron bloqueados tras una interrupción.
    public void recuperarNotificacionesBloqueadas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaLimite = ahora.minusMinutes(TIEMPO_RECUPERACION_MINUTOS);
        LocalDateTime fechaReintento = ahora.plusMinutes(ESPERA_REINTENTO_MINUTOS);

        transactionTemplate.executeWithoutResult(status -> {
            // Cierra primero los registros sin posibilidad de reintento.
            notificacionRepository.cerrarNotificacionesBloqueadas(fechaLimite);

            // Reprograma los registros recuperables.
            notificacionRepository.recuperarNotificacionesBloqueadas(
                    fechaLimite, fechaReintento);
        });
    }

    // Recupera bloqueos y procesa las notificaciones programadas.
    public void procesarPendientesProgramadas() {
        recuperarNotificacionesBloqueadas();

        List<Notificacion> notificaciones = notificacionRepository
                .buscarPendientesProgramadas(LocalDateTime.now());

        Set<Integer> alertasProcesadas = new HashSet<>();

        for (Notificacion notificacion : notificaciones) {
            Integer idAlerta = notificacion.getAlerta().getIdAlerta();
            if (alertasProcesadas.add(idAlerta)) procesarPendientes(idAlerta);
        }
    }

    // Determina si Firebase presentó un error temporal.
    private boolean esErrorTemporalFirebase(FirebaseMessagingException e) {
        if (e.getMessagingErrorCode() != null) {
            switch (e.getMessagingErrorCode()) {
                case UNAVAILABLE:
                case INTERNAL:
                    return true;
                default:
                    return false;
            }
        }

        return e.getErrorCode() == com.google.firebase.ErrorCode.UNAVAILABLE
                || e.getErrorCode() == com.google.firebase.ErrorCode.INTERNAL
                || e.getErrorCode() == com.google.firebase.ErrorCode.RESOURCE_EXHAUSTED;
    }

    // Obtiene una descripción del error.
    private String obtenerMensajeError(Exception e) {
        return e.getMessage() != null
                ? e.getMessage()
                : e.getClass().getSimpleName();
    }

    // Confirma que el registro quedó finalizado.
    private void finalizarObligatoriamente(
            Integer idNotificacion, String estado,
            LocalDateTime fechaEnvio, String resultado) {

        boolean finalizada = notificacionFinalizacionService.finalizar(
                idNotificacion, estado, fechaEnvio, resultado);

        if (!finalizada) {
            throw new IllegalStateException(
                    "No se pudo finalizar la notificación " + idNotificacion);
        }
    }

    // Verifica que la alerta siga activa.
    private boolean alertaActiva(Integer idAlerta) {
        return alertaRepository.existsByIdAlertaAndEstadoActual_Codigo(
                idAlerta, "ACTIVA");
    }
}
