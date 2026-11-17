
package com.quickhelp.repository;

import com.quickhelp.entity.Usuario; // Entidad que representa un usuario.
import jakarta.persistence.LockModeType; // Tipo de bloqueo de base de datos.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD de JPA.
import org.springframework.data.jpa.repository.Lock; // Permite bloquear un registro.
import org.springframework.data.jpa.repository.Query; // Consultas JPQL.
import org.springframework.data.repository.query.Param; // Parámetros de consultas.
import java.util.List; // Permite devolver listas de usuarios.
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

    // Obtiene los usuarios activos de un rol para enviar notificaciones.
    // Se utilizará para identificar administradores en el escalamiento SOS.
    List<Usuario> findByRol_CodigoAndEstadoTrue(String codigoRol);

    // Coordina la creación de emergencias del mismo adulto.
    // Debe utilizarse dentro de una transacción del servicio.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.idUsuario = :idUsuario")
    Optional<Usuario> buscarPorIdConBloqueo(
            @Param("idUsuario") Integer idUsuario);
}
