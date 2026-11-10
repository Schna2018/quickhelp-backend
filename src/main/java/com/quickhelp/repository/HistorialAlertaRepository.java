
package com.quickhelp.repository;

import com.quickhelp.entity.HistorialAlerta; // Entidad del historial.
import java.util.List; // Colección de registros.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones JPA.

public interface HistorialAlertaRepository extends JpaRepository<HistorialAlerta, Integer> {

    // Consulta el historial de una alerta, desde el evento más reciente.
    List<HistorialAlerta> findByAlerta_IdAlertaOrderByFechaHoraDesc(Integer idAlerta);
}
