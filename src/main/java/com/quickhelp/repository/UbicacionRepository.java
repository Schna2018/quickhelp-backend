
package com.quickhelp.repository;

import com.quickhelp.entity.Ubicacion; // Entidad de ubicación GPS.
import java.util.Optional; // Permite representar una ubicación inexistente.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD.

public interface UbicacionRepository extends JpaRepository<Ubicacion, Integer> {

    // Obtiene la ubicación más reciente registrada para una emergencia.
    Optional<Ubicacion> findFirstByAlerta_IdAlertaOrderByFechaHoraDescIdUbicacionDesc(
            Integer idAlerta);
}
