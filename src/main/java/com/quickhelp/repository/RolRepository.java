
package com.quickhelp.repository;

import java.util.Optional; // Permite manejar búsquedas sin resultados.
import org.springframework.data.jpa.repository.JpaRepository; // Proporciona operaciones CRUD.
import com.quickhelp.entity.Rol; // Entidad correspondiente a la tabla rol.

public interface RolRepository extends JpaRepository<Rol, Integer> {
    // Busca un rol por su código registrado en PostgreSQL.
    Optional<Rol> findByCodigo(String codigo);
}
