package com.quickhelp.repository;

import com.quickhelp.entity.RolPermiso; // Relación entre roles y permisos.
import java.util.List; // Colección de permisos.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones JPA.

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Integer> {

    // Obtiene los permisos asignados a un rol.
    List<RolPermiso> findByRol_IdRol(Integer idRol);

    // Comprueba si un rol tiene asignado un permiso específico.
    boolean existsByRol_IdRolAndPermiso_Codigo(Integer idRol, String codigo);
}