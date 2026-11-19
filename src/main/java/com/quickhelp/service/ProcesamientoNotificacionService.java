
package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessagingException; // Errores de Firebase.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.repository.AlertaRepository; // Consulta de estados de alertas.
import com.quickhelp.repository.NotificacionRepository; // Repositorio existente.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import java.time.Duration; // Número del recordatorio.
import java.time.LocalDateTime; // Fecha y hora del envío.
import java.util.HashSet; // Evita procesar una alerta varias veces.
import java.util.List; // Colección de notificaciones.
import java.util.Set; // Identificadores de alertas procesadas.

@Service
public class ProcesamientoNotificacionService {

    private static final String TIPO_ADMIN = "ESCALAMIENTO_ADMIN";
    private static final String TIPO_REAVISO = "REAVISO_RED";

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

        // Obtiene los datos necesarios para construir los avisos.
        AlertaRepository.DatosAvisoAlerta datosAlerta =
                alertaRepository.buscarDatosAviso(idAlerta).orElse(null);

        String nombre = datosAlerta != null
                && datosAlerta.getNombreAdulto() != null
                && !datosAlerta.getNombreAdulto().isBlank()
                ? datosAlerta.getNombreAdulto().trim()
                : "Una persona de tu red";

        for (Notificacion notificacion : notificaciones) {

            // Procesa únicamente las notificaciones pendientes.
            if (!"PENDIENTE".equals(notificacion.getEstadoEnvio())) {
                continue;
            }

            // Respeta la fecha programada de cada notificación.
            if (notificacion.getFechaProgramada() != null
                    && notificacion.getFechaProgramada().isAfter(LocalDateTime.now())) {
                continue;
            }

            // No procesa avisos de emergencias que dejaron de estar activas.
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

                // Comprueba nuevamente que la emergencia esté activa.
                if (!alertaActiva(idAlerta)) {

                    // Conserva el estado compatible con el flujo actual.
                    resultado = "Envío omitido: la alerta ya no está activa.";

                } else if (notificacion.getDestinatario() == null) {

                    // Identifica registros sin un destinatario asociado.
                    resultado = "La notificación no tiene destinatario registrado.";

                } else {

                    String tokenFcm = notificacion.getDestinatario().getTokenFcm();

                    // Verifica que el dispositivo tenga un token FCM.
                    if (tokenFcm == null || tokenFcm.isBlank()) {

                        resultado = "El destinatario no tiene token FCM.";

                    } else {

                        // Recupera el tipo de aviso registrado.
                        String tipoAviso = notificacion.getResultado();

                        // Construye el mensaje de emergencia normal.
                        String titulo = "🚨 Emergencia de " + nombre;
                        String mensaje = nombre
                                + " pidió ayuda con el botón SOS. Toca para responder.";

                        // Mensaje especial de escalamiento administrativo.
                        if (TIPO_ADMIN.equals(tipoAviso)) {

                            titulo = "¡SOS sin atención!";
                            mensaje = "Una emergencia requiere intervención administrativa.";

                        } else if (TIPO_REAVISO.equals(tipoAviso)) {

                            // Mensaje especial para recordatorios de la red.
                            titulo = "🚨 " + nombre + " sigue esperando ayuda";
                            mensaje = "Recordatorio "
                                    + numeroRecordatorio(datosAlerta, notificacion)
                                    + " de 5 · Toca para responder.";
                        }

                        // Envía el mensaje mediante Firebase Cloud Messaging.
                        resultado = firebaseMessagingService.enviarNotificacion(
                                tokenFcm.trim(),
                                idAlerta,
                                titulo,
                                mensaje);

                        // ENVIADA significa que Firebase aceptó el mensaje.
                        estado = "ENVIADA";
                    }
                }

            } catch (FirebaseMessagingException e) {

                // Conserva el código técnico de Firebase para diagnóstico.
                resultado = "Firebase FCM: "
                        + (e.getErrorCode() != null
                                ? e.getErrorCode().toString() + " - "
                                : "")
                        + (e.getMessage() != null
                                ? e.getMessage()
                                : e.getClass().getSimpleName());

            } catch (RuntimeException e) {

                // Registra errores inesperados sin marcar el aviso como enviado.
                resultado = e.getMessage() != null
                        ? e.getMessage()
                        : e.getClass().getSimpleName();
            }

            // Guarda el resultado del intento en PostgreSQL.
            boolean finalizada = notificacionFinalizacionService.finalizar(
                    notificacion.getIdNotificacion(),
                    estado,
                    "ENVIADA".equals(estado) ? LocalDateTime.now() : null,
                    resultado);

            // Informa si no se pudo finalizar la notificación reservada.
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

        // Evita procesar repetidamente la misma alerta durante el ciclo.
        Set<Integer> alertasProcesadas = new HashSet<>();

        for (Notificacion notificacion : notificaciones) {

            Integer idAlerta = notificacion.getAlerta().getIdAlerta();

            if (alertasProcesadas.add(idAlerta)) {
                procesarPendientes(idAlerta);
            }
        }
    }

    // Calcula el número del recordatorio (1 a 5).
    private long numeroRecordatorio(
            AlertaRepository.DatosAvisoAlerta datosAlerta,
            Notificacion notificacion) {

        if (datosAlerta == null
                || datosAlerta.getFechaCreacion() == null
                || notificacion.getFechaProgramada() == null) {
            return 1;
        }

        // Obtiene el intervalo configurado para los recordatorios.
        int intervalo = datosAlerta.getIntervalo() != null
                && datosAlerta.getIntervalo() > 0
                ? datosAlerta.getIntervalo()
                : 2;

        // Calcula los minutos desde la creación de la emergencia.
        long minutos = Duration.between(
                datosAlerta.getFechaCreacion(),
                notificacion.getFechaProgramada()).toMinutes();

        // Limita el número del recordatorio entre 1 y 5.
        return Math.max(1, Math.min(5, minutos / intervalo));
    }

    // Comprueba que la emergencia continúe abierta (activa, pendiente o en atención).
    private boolean alertaActiva(Integer idAlerta) {

        return alertaRepository
                .existsByIdAlertaAndEstadoActual_CodigoIn(idAlerta, java.util.List.of("ACTIVA", "PENDIENTE", "EN_ATENCION"));
    }
}
