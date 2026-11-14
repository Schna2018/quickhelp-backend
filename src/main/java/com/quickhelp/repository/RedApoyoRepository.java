
package com.quickhelp.repository;

import com.quickhelp.entity.RedApoyo; // Entidad que representa la red de apoyo.
import org.springframework.data.jpa.repository.JpaRepository; // Proporciona operaciones CRUD.
import org.springframework.data.jpa.repository.Query; // Consulta personalizada.
import org.springframework.data.repository.query.Param; // Parámetro de consulta.
import java.util.List; // Permite devolver los integrantes de la red de apoyo.

public interface RedApoyoRepository extends JpaRepository<RedApoyo, Integer> {
    // Permite guardar, consultar, actualizar y eliminar vinculaciones.

    boolean existsByAdultoMayor_IdUsuarioAndIntegrante_IdUsuario(
            Integer idAdultoMayor, Integer idIntegrante); // Comprueba si la relación ya existe.

    @Query("SELECT COALESCE(MAX(r.prioridad), 0) FROM RedApoyo r WHERE r.adultoMayor.idUsuario = :idAdulto")
    Integer findMaxPrioridadByAdultoMayorId(@Param("idAdulto") Integer idAdulto); // Obtiene la prioridad máxima.
    // Obtiene los integrantes del adulto mayor ordenados por prioridad.

    List<RedApoyo> findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(Integer idAdultoMayor);

    // Comprueba que exista una vinculación activa entre el adulto y el asistente.
    boolean existsByAdultoMayor_IdUsuarioAndIntegrante_IdUsuarioAndEstadoTrue(
            Integer idAdultoMayor, Integer idIntegrante);

            // Devuelve una fila por persona adulta con vinculación activa.
    @Query("""
            select distinct new com.quickhelp.dto.PersonaVinculadaDTO(
                adulto.idUsuario, adulto.nombreApellido
            )
            from RedApoyo red
            join red.adultoMayor adulto
            where red.integrante.idUsuario = :idAsistente
              and red.estado = true
              and adulto.estado = true
              and adulto.rol.estado = true
              and adulto.rol.codigo = 'ADULTO_MAYOR'
            order by adulto.nombreApellido, adulto.idUsuario
            """)
    List<com.quickhelp.dto.PersonaVinculadaDTO> listarPersonasVinculadas(
            @Param("idAsistente") Integer idAsistente);
 



}
