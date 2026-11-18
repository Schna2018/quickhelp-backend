
package com.quickhelp.repository;

import com.quickhelp.entity.HistorialAlerta; // Entidad de historial.
import java.util.List; // Listado de eventos.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones JPA.
import org.springframework.stereotype.Repository; // Repositorio Spring.

@Repository // Acceso al historial persistido en PostgreSQL.
public interface HistorialAlertaRepository
        extends JpaRepository<HistorialAlerta, Integer> {

    // Obtiene los eventos de una alerta en orden cronológico.
    List<HistorialAlerta> findByAlerta_IdAlertaOrderByFechaHoraAsc(
            Integer idAlerta);
}
