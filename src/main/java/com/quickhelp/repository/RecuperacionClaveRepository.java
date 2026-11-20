package com.quickhelp.repository;

import com.quickhelp.entity.RecuperacionClave;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecuperacionClaveRepository extends JpaRepository<RecuperacionClave, Integer> {

    // Último código sin usar de la cuenta.
    Optional<RecuperacionClave> findFirstByUsuario_IdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(
            Integer idUsuario);

    // Al pedir un código nuevo, los anteriores dejan de servir.
    @Modifying
    @Query("""
            UPDATE RecuperacionClave r
            SET r.usado = true
            WHERE r.usuario.idUsuario = :idUsuario AND r.usado = false
            """)
    int invalidarPendientes(@Param("idUsuario") Integer idUsuario);
}
