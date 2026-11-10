
package com.quickhelp.dto;

import java.time.LocalDateTime; // Fecha de modificación.

public record ParametroSistemaResponseDTO(
        Integer idParametro,
        String clave,
        String valor,
        String tipoDato,
        String descripcion,
        Integer idUsuarioActualiza,
        LocalDateTime fechaActualizacion
) {
}
