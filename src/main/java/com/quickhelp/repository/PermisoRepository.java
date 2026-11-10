package com.quickhelp.repository;

import com.quickhelp.entity.Permiso; // Entidad de permisos.
import java.util.Optional; // Resultado opcional de búsqueda.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones JPA.

public interface PermisoRepository extends JpaRepository<Permiso, Integer> {

    // Busca un permiso mediante su código.
    Optional<Permiso> findByCodigo(String codigo);
}