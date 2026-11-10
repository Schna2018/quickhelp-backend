
package com.quickhelp.dto;

// Agrupa los indicadores del Dashboard administrativo.
public record DashboardResponseDTO(
        long totalUsuarios, // Total de usuarios registrados.
        long usuariosActivos, // Usuarios habilitados.
        long adultosMayoresActivos, // Adultos mayores habilitados.
        long asistentesActivos, // Asistentes habilitados.
        long alertasActivas, // Emergencias pendientes de atención.
        long alertasEnAtencion, // Emergencias que están siendo atendidas.
        long alertasResueltas, // Emergencias finalizadas.
        long totalAlertas // Total histórico de emergencias.
) {
}
