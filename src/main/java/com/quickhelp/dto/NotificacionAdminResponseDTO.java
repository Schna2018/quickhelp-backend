
package com.quickhelp.dto;

import java.time.LocalDateTime; // Manejo de fechas y horas.

// Información de seguimiento SOS para el panel administrativo.
public record NotificacionAdminResponseDTO(
        Integer idNotificacion, // Identificador de la notificación.
        Integer idAlerta, // Emergencia relacionada.
        Integer idDestinatario, // Usuario que recibe el aviso.
        String nombreDestinatario, // Nombre del destinatario.
        String tipoAviso, // Tipo de aviso SOS.
        String canal, // Canal utilizado.
        String estadoEnvio, // Estado del envío.
        Integer numeroIntento, // Cantidad de intentos realizados.
        LocalDateTime fechaProgramada, // Fecha programada.
        LocalDateTime fechaEnvio, // Fecha efectiva de envío.
        String resultado // Resultado del envío.
) {}
