
package com.quickhelp.dto;

import java.math.BigDecimal; // Permite almacenar coordenadas GPS decimales.

// Recibe las coordenadas opcionales al crear una emergencia.
public record CrearAlertaRequestDTO(
        BigDecimal latitud, // Latitud del adulto mayor.
        BigDecimal longitud, // Longitud del adulto mayor.
        BigDecimal precisionMetros // Precisión GPS en metros.
) {
}
