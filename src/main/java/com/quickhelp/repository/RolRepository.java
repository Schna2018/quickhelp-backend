package com.quickhelp.repository;

import com.quickhelp.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    // JpaRepository ya incluye CRUD básico para la tabla rol
}