
package com.quickhelp.dto;

// Recibe la respuesta seleccionada por el asistente desde la app móvil.
public record RespuestaAlertaRequestDTO(
        String tipoRespuesta, // VOY_EN_CAMINO, NO_PUEDO_ASISTIR, VOY_A_DEMORAR o PERSONALIZADA.
        String observacion // Mensaje opcional, obligatorio si es PERSONALIZADA.
) {}
