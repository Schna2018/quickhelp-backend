
package com.quickhelp.repository;

import com.quickhelp.entity.Usuario; // Entidad que representa un usuario.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD de JPA.
import java.util.Optional; // Permite devolver resultados opcionales.

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Busca un usuario por correo electrónico.
    Optional<Usuario> findByCorreo(String correo);

    // Comprueba si ya existe un correo registrado.
    boolean existsByCorreo(String correo);

    // Comprueba si un código de vinculación ya está registrado.
    boolean existsByCodigoVinculacion(String codigoVinculacion);

    // Busca un usuario mediante su código de vinculación.
    Optional<Usuario> findByCodigoVinculacion(String codigoVinculacion);

    // Cuenta únicamente los usuarios que se encuentran activos.
    long countByEstadoTrue();

    // Cuenta los usuarios activos que pertenecen a un rol específico.
    long countByRol_CodigoAndEstadoTrue(String codigoRol);
}
