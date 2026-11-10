package com.quickhelp.repository;

import com.quickhelp.entity.Auditoria; // Entidad de auditoría.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD.

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
}