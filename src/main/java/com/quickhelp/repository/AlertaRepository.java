
package com.quickhelp.repository;

import com.quickhelp.entity.Alerta; // Entidad que representa una emergencia.
import jakarta.persistence.LockModeType; // Tipo de bloqueo de base de datos.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD de JPA.
import org.springframework.data.jpa.repository.Lock; // Permite bloquear registros.
import org.springframework.data.jpa.repository.Query; // Permite definir consultas JPQL.
import org.springframework.data.repository.query.Param; // Identifica parámetros de consultas.
import java.util.List; // Maneja listas de alertas.
import java.util.Optional; // Representa resultados que pueden no existir.

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    // Obtiene las alertas de un adulto mayor, ordenadas desde la más reciente.
    List<Alerta> findByAdultoMayor_IdUsuarioOrderByFechaCreacionDesc(Integer idAdultoMayor);

    // Obtiene las alertas asignadas a un asistente responsable.
    List<Alerta> findByResponsableAtencion_IdUsuarioOrderByFechaCreacionDesc(Integer idResponsable);

    // Obtiene las alertas que tienen un estado específico.
    List<Alerta> findByEstadoActual_CodigoOrderByFechaCreacionDesc(String codigoEstado);

    // Cuenta las alertas que pertenecen a un estado determinado.
    long countByEstadoActual_Codigo(String codigoEstado);

    // Obtiene todas las alertas, ordenadas desde la más reciente.
    List<Alerta> findAllByOrderByFechaCreacionDesc();

    // Bloquea una alerta para impedir modificaciones simultáneas.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Alerta a WHERE a.idAlerta = :idAlerta")
    Optional<Alerta> buscarPorIdConBloqueo(@Param("idAlerta") Integer idAlerta);
}
