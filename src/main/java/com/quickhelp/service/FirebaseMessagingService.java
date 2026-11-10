
package com.quickhelp.service;

import com.google.firebase.messaging.FirebaseMessaging; // Servicio de mensajería Firebase.
import com.google.firebase.messaging.FirebaseMessagingException; // Errores de FCM.
import com.google.firebase.messaging.Message; // Mensaje dirigido a un dispositivo.
import com.google.firebase.messaging.Notification; // Contenido visible de la notificación.
import org.springframework.stereotype.Service; // Servicio Spring Boot.

@Service // Permite inyectar este servicio en otros componentes.
public class FirebaseMessagingService {

    // Envía una notificación push al dispositivo indicado.
    public String enviarNotificacion(
            String tokenFcm,
            Integer idAlerta,
            String titulo,
            String cuerpo) throws FirebaseMessagingException {

        // Verifica que exista un token de destino.
        if (tokenFcm == null || tokenFcm.isBlank()) {
            throw new IllegalArgumentException("El destinatario no tiene token FCM.");
        }

        // Construye el mensaje para el dispositivo móvil.
        Message mensaje = Message.builder()
                .setToken(tokenFcm.trim())
                .setNotification(Notification.builder()
                        .setTitle(titulo)
                        .setBody(cuerpo)
                        .build())
                .putData("tipo", "ALERTA_EMERGENCIA")
                .putData("idAlerta", String.valueOf(idAlerta))
                .build();

        // Firebase devuelve el identificador del mensaje aceptado.
        return FirebaseMessaging.getInstance().send(mensaje);
    }
}
