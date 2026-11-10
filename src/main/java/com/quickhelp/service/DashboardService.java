
package com.quickhelp.service;

import com.quickhelp.dto.DashboardResponseDTO; // DTO con los indicadores.
import com.quickhelp.repository.AlertaRepository; // Consultas de emergencias.
import com.quickhelp.repository.UsuarioRepository; // Consultas de usuarios.
import org.springframework.stereotype.Service; // Registra el servicio.
import org.springframework.transaction.annotation.Transactional; // Transacciones de lectura.

@Service // Servicio del Dashboard administrativo.
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final AlertaRepository alertaRepository;

    // Inyecta los repositorios necesarios.
    public DashboardService(
            UsuarioRepository usuarioRepository,
            AlertaRepository alertaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.alertaRepository = alertaRepository;
    }

    @Transactional(readOnly = true) // Consulta sin modificar la base de datos.
    public DashboardResponseDTO obtenerIndicadores() {

        // Obtiene los indicadores de usuarios.
        long totalUsuarios = usuarioRepository.count();
        long usuariosActivos = usuarioRepository.countByEstadoTrue();
        long adultosMayoresActivos =
                usuarioRepository.countByRol_CodigoAndEstadoTrue("ADULTO_MAYOR");
        long asistentesActivos =
                usuarioRepository.countByRol_CodigoAndEstadoTrue("ASISTENTE");

        // Obtiene los indicadores de emergencias.
        long alertasActivas = alertaRepository.countByEstadoActual_Codigo("ACTIVA");
        long alertasEnAtencion = alertaRepository.countByEstadoActual_Codigo("EN_ATENCION");
        long alertasResueltas = alertaRepository.countByEstadoActual_Codigo("RESUELTA");
        long totalAlertas = alertaRepository.count();

        // Devuelve los resultados al controlador.
        return new DashboardResponseDTO(
                totalUsuarios,
                usuariosActivos,
                adultosMayoresActivos,
                asistentesActivos,
                alertasActivas,
                alertasEnAtencion,
                alertasResueltas,
                totalAlertas
        );
    }
}
