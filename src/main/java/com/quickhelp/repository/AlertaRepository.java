
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
    List<Alerta> findByAdultoMayor_IdUsuarioOrderByFechaCreacionDesc(
            Integer idAdultoMayor);

    // Busca y bloquea la emergencia abierta más reciente del adulto.
    // El servicio enviará los estados ACTIVA y EN_ATENCION.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Alerta> findFirstByAdultoMayor_IdUsuarioAndEstadoActual_CodigoInOrderByFechaCreacionDescIdAlertaDesc(
            Integer idAdultoMayor,
            List<String> codigosEstado);

    // Limita la consulta a adultos y vinculaciones activos del asistente.
    @Query("""
            select a from Alerta a
            where a.adultoMayor.estado = true
              and a.adultoMayor.rol.estado = true
              and a.adultoMayor.rol.codigo = 'ADULTO_MAYOR'
              and exists (
                  select r.idRedApoyo from RedApoyo r
                  where r.adultoMayor = a.adultoMayor
                    and r.integrante.idUsuario = :idAsistente and r.estado = true
              )
            order by a.fechaCreacion desc, a.idAlerta desc
            """)
    List<Alerta> buscarAlertasDeMiRed(
            @Param("idAsistente") Integer idAsistente);

    // Obtiene las alertas asignadas a un asistente responsable.
    List<Alerta> findByResponsableAtencion_IdUsuarioOrderByFechaCreacionDesc(
            Integer idResponsable);

    // Obtiene las alertas que tienen un estado específico.
    List<Alerta> findByEstadoActual_CodigoOrderByFechaCreacionDesc(
            String codigoEstado);

    // Cuenta las alertas que pertenecen a un estado determinado.
    long countByEstadoActual_Codigo(String codigoEstado);

    // Comprueba el estado en la base de datos, incluso en tareas sin sesión web.
    boolean existsByIdAlertaAndEstadoActual_Codigo(Integer idAlerta, String codigoEstado);

    // Obtiene todas las alertas, ordenadas desde la más reciente.
    List<Alerta> findAllByOrderByFechaCreacionDesc();

    // Bloquea una alerta para impedir modificaciones simultáneas.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Alerta a WHERE a.idAlerta = :idAlerta")
    Optional<Alerta> buscarPorIdConBloqueo(
            @Param("idAlerta") Integer idAlerta);

    // Datos que muestran las notificaciones de emergencia.
    interface DatosAvisoAlerta {
        String getNombreAdulto();
        java.time.LocalDateTime getFechaCreacion();
        Integer getIntervalo();
    }

    // Consulta solo el nombre y las fechas, sin cargar relaciones perezosas.
    @Query("""
            SELECT a.adultoMayor.nombreApellido AS nombreAdulto,
                   a.fechaCreacion AS fechaCreacion,
                   a.intervaloEscalamientoMinutos AS intervalo
            FROM Alerta a
            WHERE a.idAlerta = :idAlerta
            """)
    Optional<DatosAvisoAlerta> buscarDatosAviso(@Param("idAlerta") Integer idAlerta);

    /**
     * PROTECCIÓN DE ELIMINACIÓN DEFINITIVA.
     * Comprueba si un usuario participa en alguna emergencia registrada.
     * Incluye emergencias activas, resueltas, rechazadas y cerradas.
     * No modifica ni elimina información histórica.
     */
    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Alerta a
            WHERE a.adultoMayor.idUsuario = :idUsuario
               OR a.responsableAtencion.idUsuario = :idUsuario
               OR a.operadorAsignado.idUsuario = :idUsuario
            """)
    boolean existeAlertaRelacionadaConUsuario(
            @Param("idUsuario") Integer idUsuario);
}
