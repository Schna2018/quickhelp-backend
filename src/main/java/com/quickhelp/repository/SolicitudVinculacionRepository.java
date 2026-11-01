
package com.quickhelp.repository;

import com.quickhelp.entity.SolicitudVinculacion; // Entidad de solicitudes.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones de base de datos.
import java.util.List; // Colección de solicitudes.

public interface SolicitudVinculacionRepository
        extends JpaRepository<SolicitudVinculacion, Integer> {

    // Comprueba si ya existe una solicitud pendiente entre ambos usuarios.
    boolean existsByAdultoMayor_IdUsuarioAndAsistente_IdUsuarioAndEstado(
            Integer idAdultoMayor, Integer idAsistente, String estado);

    // Obtiene las solicitudes pendientes recibidas por un adulto mayor.
    List<SolicitudVinculacion> findByAdultoMayor_IdUsuarioAndEstado(
            Integer idAdultoMayor, String estado);

    // Obtiene las solicitudes enviadas por un asistente.
    List<SolicitudVinculacion> findByAsistente_IdUsuario(
            Integer idAsistente);
}
