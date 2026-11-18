
package com.quickhelp.dto;

import java.time.LocalDateTime; // Maneja fechas y horas.

// Datos que recibe la aplicación móvil al consultar notificaciones.
public record NotificacionResponseDTO(
        Integer idNotificacion, // Identificador de la notificación.
        Integer idAlerta, // Emergencia relacionada.
        Integer idAdultoMayor, // Adulto mayor que activó la alerta.
        String estadoAlerta, // Estado actual de la emergencia.
        String canal, // Canal de notificación.
        String estadoEnvio, // Estado del envío.
        LocalDateTime fechaProgramada, // Fecha de programación.
        LocalDateTime fechaEnvio // Fecha efectiva de envío.
) {}
