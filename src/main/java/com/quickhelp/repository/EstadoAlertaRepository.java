
package com.quickhelp.repository;

import com.quickhelp.entity.EstadoAlerta; // Entidad de estados.
import java.util.List; // Colección de estados.
import java.util.Optional; // Resultado que puede no existir.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones JPA.

public interface EstadoAlertaRepository extends JpaRepository<EstadoAlerta, Integer> {

    // Busca un estado mediante su código.
    Optional<EstadoAlerta> findByCodigo(String codigo);

    // Obtiene los estados ordenados para mostrarlos en la aplicación.
    List<EstadoAlerta> findAllByOrderByOrdenAsc();
}
