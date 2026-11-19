
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

        // Nombre del adulto y datos para numerar los recordatorios.
        AlertaRepository.DatosAvisoAlerta datosAlerta =
                alertaRepository.buscarDatosAviso(idAlerta).orElse(null);
        String nombre = datosAlerta != null
                && datosAlerta.getNombreAdulto() != null
                && !datosAlerta.getNombreAdulto().isBlank()
                ? datosAlerta.getNombreAdulto().trim()
                : "Una persona de tu red";

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

            // No envía avisos cuando la alerta deja de estar activa.
            if (!alertaActiva(idAlerta)) {
                continue;
            }

            // Reserva el envío para impedir procesamientos simultáneos.
            if (!notificacionReservaService.reservar(notificacion.getIdNotificacion())) {
                continue;
            }

            String estado = "ERROR";
            String resultado;

            try {
                // Comprueba nuevamente el estado antes del envío.
                if (!alertaActiva(idAlerta)) {
                    resultado = "Envío omitido: la alerta ya no está activa.";
                } else {
                    String tokenFcm = notificacion.getDestinatario().getTokenFcm();

                    // Identifica el tipo de notificación antes de finalizarla.
                    String tipoAviso = notificacion.getResultado();

                    // Muestra quién pidió ayuda sin necesidad de abrir la app.
                    String titulo = "🚨 Emergencia de " + nombre;
                    String mensaje = nombre + " pidió ayuda con el botón SOS. Toca para responder.";

                    if (TIPO_ADMIN.equals(tipoAviso)) {
                        titulo = "¡SOS sin atención!";
                        mensaje = "Una emergencia requiere intervención administrativa.";
                    } else if (TIPO_REAVISO.equals(tipoAviso)) {
                        titulo = "🚨 " + nombre + " sigue esperando ayuda";
                        mensaje = "Recordatorio " + numeroRecordatorio(datosAlerta, notificacion)
                                + " de 5 · Toca para responder.";
                    }

                    // Envía la notificación mediante Firebase Cloud Messaging.
                    resultado = firebaseMessagingService.enviarNotificacion(
                            tokenFcm,
                            idAlerta,
                            titulo,
                            mensaje);

                    estado = "ENVIADA";
                }

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

    // Procesa las notificaciones programadas cuya fecha ya se cumplió.
    public void procesarPendientesProgramadas() {

        List<Notificacion> notificaciones = notificacionRepository
                .buscarPendientesProgramadas(LocalDateTime.now());

        // Evita consultar repetidamente la misma alerta durante el ciclo.
        Set<Integer> alertasProcesadas = new HashSet<>();

        for (Notificacion notificacion : notificaciones) {
            Integer idAlerta = notificacion.getAlerta().getIdAlerta();

            if (alertasProcesadas.add(idAlerta)) {
                procesarPendientes(idAlerta);
            }
        }
    }

    // Calcula el número del recordatorio (1 a 5) a partir de su hora programada.
    private long numeroRecordatorio(
            AlertaRepository.DatosAvisoAlerta datosAlerta, Notificacion notificacion) {

        if (datosAlerta == null || datosAlerta.getFechaCreacion() == null
                || notificacion.getFechaProgramada() == null) {
            return 1;
        }

        int intervalo = datosAlerta.getIntervalo() != null && datosAlerta.getIntervalo() > 0
                ? datosAlerta.getIntervalo() : 2;
        long minutos = Duration.between(
                datosAlerta.getFechaCreacion(), notificacion.getFechaProgramada()).toMinutes();

        return Math.max(1, Math.min(5, minutos / intervalo));
    }

    // Comprueba que la alerta siga activa antes de enviar.
    private boolean alertaActiva(Integer idAlerta) {

        return alertaRepository.existsByIdAlertaAndEstadoActual_Codigo(idAlerta, "ACTIVA");
    }
}
