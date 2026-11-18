package com.quickhelp.service;

import com.google.firebase.messaging.AndroidConfig; // Configuración de entrega en Android.
import com.google.firebase.messaging.AndroidNotification; // Canal de notificación en Android.
import com.google.firebase.messaging.ApnsConfig; // Configuración de entrega en iOS.
import com.google.firebase.messaging.Aps; // Contenido y sonido de la notificación de iOS.
import com.google.firebase.messaging.ApsAlert; // Título y cuerpo visible en iOS.
import com.google.firebase.messaging.FirebaseMessaging; // Servicio de envío de Firebase.
import com.google.firebase.messaging.FirebaseMessagingException; // Errores del envío.
import com.google.firebase.messaging.Message; // Mensaje enviado al dispositivo.
import com.google.firebase.messaging.Notification; // Título y contenido visible.
import org.springframework.stereotype.Service; // Registra el servicio en Spring.

@Service
public class FirebaseMessagingService {

    // La app crea estos canales y empaqueta un WAV para cada tipo de aviso.
    private static final String CANAL_SOS = "quickhelp_sos_v1";
    private static final String CANAL_CHAT = "quickhelp_chat_v1";
    private static final String SONIDO_SOS = "quickhelp_sos";
    private static final String SONIDO_CHAT = "quickhelp_chat";

    // Conserva el método utilizado por el procesamiento y escalamiento SOS.
    public String enviarNotificacion(
            String tokenFcm,
            Integer idAlerta,
            String titulo,
            String cuerpo) throws FirebaseMessagingException {

        return enviar(
                tokenFcm,
                idAlerta,
                null,
                "ALERTA_EMERGENCIA",
                CANAL_SOS,
                SONIDO_SOS,
                titulo,
                cuerpo);
    }

    // Prepara un aviso de chat asociado a un mensaje concreto.
    public String enviarMensajeChat(
            String tokenFcm,
            Integer idAlerta,
            Integer idMensaje,
            String titulo,
            String cuerpo) throws FirebaseMessagingException {

        if (idMensaje == null || idMensaje <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del mensaje no es válido.");
        }

        return enviar(
                tokenFcm,
                idAlerta,
                idMensaje,
                "MENSAJE_CHAT",
                CANAL_CHAT,
                SONIDO_CHAT,
                titulo,
                cuerpo);
    }

    // Centraliza la construcción y el envío para ambos tipos de aviso.
    private String enviar(
            String tokenFcm,
            Integer idAlerta,
            Integer idMensaje,
            String tipo,
            String canal,
            String sonido,
            String titulo,
            String cuerpo) throws FirebaseMessagingException {

        if (tokenFcm == null || tokenFcm.isBlank()) {
            throw new IllegalArgumentException(
                    "El destinatario no tiene token FCM.");
        }

        if (idAlerta == null || idAlerta <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la alerta no es válido.");
        }

        if (titulo == null || titulo.isBlank()
                || cuerpo == null || cuerpo.isBlank()) {
            throw new IllegalArgumentException(
                    "La notificación debe tener título y contenido.");
        }

        AndroidNotification notificacionAndroid =
                AndroidNotification.builder()
                        .setChannelId(canal) // Selecciona el canal SOS o chat.
                        .setSound(sonido) // Recurso res/raw para Android sin canales.
                        .build();

        AndroidConfig configuracionAndroid = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH) // Solicita entrega inmediata.
                .setNotification(notificacionAndroid)
                .build();

        Message.Builder mensaje = Message.builder()
                .setToken(tokenFcm.trim())
                .setNotification(Notification.builder()
                        .setTitle(titulo)
                        .setBody(cuerpo)
                        .build())
                .setAndroidConfig(configuracionAndroid)
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setAlert(ApsAlert.builder().setTitle(titulo).setBody(cuerpo).build())
                                .setSound(sonido + ".wav") // Archivo incluido en iOS.
                                .build())
                        .build())
                .putData("tipo", tipo) // Permite distinguir emergencia y chat.
                .putData("idAlerta", idAlerta.toString());

        if (idMensaje != null) {
            mensaje.putData("idMensaje", idMensaje.toString());
        }

        // Devuelve el identificador aceptado por Firebase.
        return FirebaseMessaging.getInstance().send(mensaje.build());
    }
}