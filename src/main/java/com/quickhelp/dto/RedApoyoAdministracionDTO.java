
package com.quickhelp.dto;

import java.time.LocalDateTime; // Fecha de vinculación.

// Datos de la red de apoyo para el panel administrativo.
public record RedApoyoAdministracionDTO(
        Integer idRedApoyo, // Identificador de la vinculación.
        Integer idAdultoMayor, // Identificador del adulto mayor.
        String nombreAdultoMayor, // Nombre del adulto mayor.
        Integer idIntegrante, // Identificador del asistente.
        String nombreIntegrante, // Nombre del asistente.
        Integer prioridad, // Orden de contacto.
        Boolean estado, // Estado de la vinculación.
        LocalDateTime fechaAsociacion // Fecha de asociación.
) {
}
