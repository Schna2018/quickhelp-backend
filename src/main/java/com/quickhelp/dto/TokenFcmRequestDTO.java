
package com.quickhelp.dto;

import jakarta.validation.constraints.NotBlank; // Valida que el token no esté vacío.

public record TokenFcmRequestDTO(
    @NotBlank(message = "El token FCM es obligatorio")
    String tokenFcm // Token generado por Firebase en el dispositivo móvil.
) {}
