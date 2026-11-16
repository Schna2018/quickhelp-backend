package com.quickhelp.dto;

import java.time.LocalDateTime; // Fecha enviada al chat.

// Datos que recibe la aplicación al consultar los mensajes.
public record MensajeAlertaResponseDTO(
        Integer idMensaje,
        Integer idAutor,
        String nombreAutor,
        String texto,
        LocalDateTime fechaHora
) {
}