package com.quickhelp.dto;

import jakarta.validation.constraints.DecimalMax; // Limita el rango de las coordenadas.
import jakarta.validation.constraints.DecimalMin; // Limita el rango de las coordenadas.
import jakarta.validation.constraints.NotNull; // Exige ambas coordenadas.
import java.math.BigDecimal; // Permite recibir coordenadas GPS decimales.

// Coordenadas actuales del dispositivo para mostrar su dirección en Inicio.
public record DireccionRequestDTO(
        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90", message = "Latitud inválida")
        @DecimalMax(value = "90", message = "Latitud inválida")
        BigDecimal latitud, // Latitud del adulto mayor.

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180", message = "Longitud inválida")
        @DecimalMax(value = "180", message = "Longitud inválida")
        BigDecimal longitud // Longitud del adulto mayor.
) {
}
