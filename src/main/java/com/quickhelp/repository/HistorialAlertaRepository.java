
package com.quickhelp.repository;

import com.quickhelp.entity.HistorialAlerta; // Entidad del historial.
import java.util.List; // Colección de registros.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones JPA.
import org.springframework.data.jpa.repository.Query; // Consultas JPQL.
import org.springframework.data.repository.query.Param; // Parámetros de consultas.

public interface HistorialAlertaRepository extends JpaRepository<HistorialAlerta, Integer> {

    // Consulta el historial de una alerta, desde el evento más reciente.
    List<HistorialAlerta> findByAlerta_IdAlertaOrderByFechaHoraDesc(Integer idAlerta);

    /**
     * PROTECCIÓN DEL HISTORIAL DE EMERGENCIAS.
     * Comprueba si un usuario realizó alguna acción registrada.
     * Incluye atención, rechazo, resolución y otras acciones históricas.
     * No modifica ni elimina registros de PostgreSQL.
     */
    @Query("""
            SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END
            FROM HistorialAlerta h
            WHERE h.usuarioActor.idUsuario = :idUsuario
            """)
    boolean existeHistorialRelacionadoConUsuario(
            @Param("idUsuario") Integer idUsuario);
}
