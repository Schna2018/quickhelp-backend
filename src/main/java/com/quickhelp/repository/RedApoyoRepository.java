
package com.quickhelp.repository;

import com.quickhelp.entity.RedApoyo; // Entidad que representa la red de apoyo.
import org.springframework.data.jpa.repository.JpaRepository; // Proporciona operaciones CRUD.
import org.springframework.data.jpa.repository.Query; // Consulta personalizada.
import org.springframework.data.repository.query.Param; // Parámetro de consulta.

public interface RedApoyoRepository extends JpaRepository<RedApoyo, Integer> {
    // Permite guardar, consultar, actualizar y eliminar vinculaciones.

    boolean existsByAdultoMayor_IdUsuarioAndIntegrante_IdUsuario(
            Integer idAdultoMayor, Integer idIntegrante); // Comprueba si la relación ya existe.

    @Query("SELECT COALESCE(MAX(r.prioridad), 0) FROM RedApoyo r WHERE r.adultoMayor.idUsuario = :idAdulto")
    Integer findMaxPrioridadByAdultoMayorId(@Param("idAdulto") Integer idAdulto); // Obtiene la prioridad máxima.

}
