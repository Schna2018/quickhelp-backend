
package com.quickhelp.dto;

import java.time.LocalDateTime; // Maneja la fecha de vinculación.

// Datos que el backend entrega sobre cada integrante de la red de apoyo.
public record RedApoyoResponseDTO(
        Integer idRedApoyo, // Identificador de la relación.
        Integer idIntegrante, // Identificador del asistente.
        String nombreIntegrante, // Nombre del asistente.
        Integer prioridad, // Orden de contacto.
        Boolean estado, // Indica si la relación está activa.
        LocalDateTime fechaAsociacion // Fecha de vinculación.
) {
}
