package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessagingException; // Errores de FCM.
import com.quickhelp.event.MensajeAlertaCreadoEvent; // Evento del mensaje nuevo.
import com.quickhelp.repository.MensajeAlertaRepository; // Destinatarios autorizados.
import java.util.List; // Tokens de los destinatarios.
import org.slf4j.Logger; // Registro de errores de envío.
import org.slf4j.LoggerFactory; // Crea el registro del componente.
import org.springframework.beans.factory.annotation.Qualifier; // Selecciona el ejecutor de Spring Boot.
import org.springframework.core.task.TaskExecutor; // Ejecuta la entrega en segundo plano.
import org.springframework.core.task.TaskRejectedException; // Error al programar el envío.
import org.springframework.dao.DataAccessException; // Errores de consulta a la base de datos.
import org.springframework.stereotype.Component; // Registra el listener en Spring.
import org.springframework.transaction.event.TransactionPhase; // Fase de la transacción.
import org.springframework.transaction.event.TransactionalEventListener; // Escucha tras confirmar el mensaje.

@Component
public class NotificacionChatEventListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificacionChatEventListener.class);

    private final MensajeAlertaRepository mensajeRepository;
    private final FirebaseMessagingService firebaseMessagingService;
    private final TaskExecutor tareas;

    public NotificacionChatEventListener(
            MensajeAlertaRepository mensajeRepository,
            FirebaseMessagingService firebaseMessagingService,
            @Qualifier("applicationTaskExecutor") TaskExecutor tareas) {
        this.mensajeRepository = mensajeRepository;
        this.firebaseMessagingService = firebaseMessagingService;
        this.tareas = tareas;
    }

    // El guardado ya se confirmó y la respuesta del chat no depende de Firebase.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificar(MensajeAlertaCreadoEvent evento) {
        try {
            tareas.execute(() -> enviarAvisos(evento));
        } catch (TaskRejectedException e) {
            logger.warn("No se pudo programar el aviso del mensaje {}", evento.idMensaje());
        }
    }

    private void enviarAvisos(MensajeAlertaCreadoEvent evento) {
        List<String> tokens;
        try {
            // Comprueba los permisos y tokens vigentes al procesar el envío.
            tokens = mensajeRepository.buscarTokensParaAviso(evento.idMensaje());
        } catch (DataAccessException e) {
            logger.warn("No se pudieron consultar los destinatarios del mensaje {}", evento.idMensaje(), e);
            return;
        }

        if (tokens.isEmpty()) return;

        // Muestra el autor y el comienzo del mensaje, como en una app de mensajería.
        String titulo = "Nuevo mensaje de Quick Help";
        String cuerpo = "Abre Quick Help para leer el mensaje.";
        try {
            var datos = mensajeRepository.buscarDatosAviso(evento.idMensaje()).orElse(null);
            if (datos != null) {
                if (datos.getNombreAutor() != null && !datos.getNombreAutor().isBlank()) {
                    titulo = datos.getNombreAutor().trim();
                }
                if (datos.getTexto() != null && !datos.getTexto().isBlank()) {
                    cuerpo = resumir(datos.getTexto());
                }
            }
        } catch (DataAccessException e) {
            logger.warn("No se pudo leer el contenido del mensaje {}", evento.idMensaje());
        }

        for (String token : tokens) {
            try {
                firebaseMessagingService.enviarMensajeChat(
                        token, evento.idAlerta(), evento.idMensaje(),
                        titulo,
                        cuerpo);
            } catch (FirebaseMessagingException e) {
                // Un token rechazado no impide avisar a las demás personas.
                logger.warn("Firebase rechazó un aviso del mensaje {}: {} / {}",
                        evento.idMensaje(), e.getErrorCode(), e.getMessagingErrorCode());
            } catch (RuntimeException e) {
                logger.warn("Falló un aviso del mensaje {}: {}",
                        evento.idMensaje(), e.getClass().getSimpleName());
            }
        }
    }

    // Une las líneas y recorta textos largos a unos 100 caracteres.
    private static String resumir(String texto) {
        String limpio = texto.strip().replaceAll("\\s+", " ");
        // Cuenta caracteres completos para no cortar un emoji por la mitad.
        if (limpio.codePointCount(0, limpio.length()) <= 100) return limpio;
        return limpio.substring(0, limpio.offsetByCodePoints(0, 99)).stripTrailing() + "…";
    }
}
