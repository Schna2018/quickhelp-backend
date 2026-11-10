
package com.quickhelp.dto;

import java.time.LocalDateTime; // Maneja fechas y horas.

// Define los datos de una alerta que se enviarán al frontend.
public record AlertaResponseDTO(
        Integer idAlerta, // Identificador de la emergencia.
        Integer idAdultoMayor, // Identificador del adulto mayor.
        String nombreAdultoMayor, // Nombre del adulto mayor.
        String estado, // Código del estado actual.
        String tipoActivacion, // Forma de activación.
        LocalDateTime fechaCreacion, // Fecha de generación.
        LocalDateTime fechaAtencion, // Inicio de atención.
        LocalDateTime fechaCierre // Fecha de finalización.
) {
}
