package com.quickhelp.dto;

import jakarta.validation.constraints.NotBlank; // Rechaza mensajes vacíos.
import jakarta.validation.constraints.NotNull; // Exige la clave de envío.
import jakarta.validation.constraints.Size; // Limita el tamaño del mensaje.
import java.util.UUID; // Clave generada por la aplicación.

// El autor se obtiene del JWT, no del formulario.
public record MensajeAlertaRequestDTO(
        @NotBlank(message = "Debe escribir un mensaje.")
        @Size(max = 2000, message = "El mensaje admite hasta 2000 caracteres.")
        String texto,

        @NotNull(message = "Debe indicar la clave del mensaje.")
        UUID clave
) {
}