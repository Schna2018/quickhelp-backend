
package com.quickhelp.dto;

import java.time.LocalDateTime; // Fecha y hora de la respuesta.

// Datos que devuelve el backend al consultar una respuesta.
public record RespuestaAlertaResponseDTO(
        Integer idRespuesta, // Identificador de la respuesta.
        Integer idAlerta, // Emergencia asociada.
        Integer idIntegrante, // Asistente que respondió.
        String tipoRespuesta, // Opción seleccionada.
        String observacion, // Mensaje adicional.
        LocalDateTime fechaHora // Momento de la respuesta.
) {}
