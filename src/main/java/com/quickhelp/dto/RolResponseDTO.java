
package com.quickhelp.dto;

// Datos públicos de un rol que puede consultar el frontend.
public record RolResponseDTO(
        Integer idRol,
        String codigo,
        String nombre,
        Boolean estado
) {}
