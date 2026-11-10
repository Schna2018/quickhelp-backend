
package com.quickhelp.dto;

import java.time.LocalDateTime; // Representa la fecha de solicitud.

// Define los datos seguros que recibirá la aplicación del adulto mayor.
public record SolicitudVinculacionResponseDTO(
        Integer idSolicitud, // Identificador para aprobar o rechazar.
        Integer idAsistente, // Identificador del asistente solicitante.
        String nombreAsistente, // Nombre visible del asistente.
        LocalDateTime fechaSolicitud, // Fecha de creación de la solicitud.
        String estado // Estado actual de la solicitud.
) {
}
