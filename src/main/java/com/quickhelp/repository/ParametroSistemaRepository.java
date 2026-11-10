
package com.quickhelp.repository;

import com.quickhelp.entity.ParametroSistema; // Entidad de configuración.
import java.util.Optional; // Resultado que puede estar vacío.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD.

public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Integer> {

    Optional<ParametroSistema> findByClave(String clave); // Busca una configuración por su clave.
}
