
package com.quickhelp.dto;

// Recibe la observación enviada al resolver una emergencia.
public record ResolverAlertaRequestDTO(
        String observacion // Explica cómo se resolvió la emergencia.
) {
}
