package com.quickhelp.repository;

import com.quickhelp.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> { // Permite CRUD sobre la tabla usuario usando id_usuario como Integer.

    Optional<Usuario> findByCorreo(String correo); // Busca un usuario por su correo; servirá más adelante para login y validaciones.

    boolean existsByCorreo(String correo); // Verifica si ya existe un usuario con ese correo antes de registrarlo.
}