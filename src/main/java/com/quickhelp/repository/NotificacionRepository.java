
package com.quickhelp.repository;

import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import java.time.LocalDateTime; // Manejo de fechas.
import java.util.List; // Listado de notificaciones.
import org.springframework.data.jpa.repository.EntityGraph; // Carga relaciones.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD.
import org.springframework.data.jpa.repository.Modifying; // Consultas de actualización.
import org.springframework.data.jpa.repository.Query; // Consultas personalizadas.
import org.springframework.data.repository.query.Param; // Parámetros de consultas.

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    // Obtiene las notificaciones de un destinatario.
    List<Notificacion> findByDestinatario_IdUsuarioOrderByIdNotificacionDesc(
            Integer idDestinatario);

    // Obtiene las notificaciones de una alerta.
    @EntityGraph(attributePaths = {"destinatario"})
    List<Notificacion> findByAlerta_IdAlertaOrderByIdNotificacionDesc(
            Integer idAlerta);

    // Obtiene notificaciones según su estado.
    List<Notificacion> findByEstadoEnvioOrderByIdNotificacionAsc(
            String estadoEnvio);

    // Obtiene notificaciones de un usuario según su estado.
    List<Notificacion> findByDestinatario_IdUsuarioAndEstadoEnvioOrderByIdNotificacionDesc(
            Integer idDestinatario, String estadoEnvio);

    // Comprueba notificaciones existentes.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanal(
            Integer idAlerta, Integer idDestinatario, String canal);

    // Evita duplicados del mismo tipo de aviso.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndTipoAviso(
            Integer idAlerta, Integer idDestinatario, String canal, String tipoAviso);

    // Reserva una notificación e incrementa el contador de forma segura.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'EN_PROCESO',
                n.numeroIntento = COALESCE(n.numeroIntento, 0) + 1,
                n.fechaInicioProceso = CURRENT_TIMESTAMP
            WHERE n.idNotificacion = :idNotificacion
              AND n.estadoEnvio = 'PENDIENTE'
              AND COALESCE(n.numeroIntento, 0) < 3
              AND (n.fechaProgramada IS NULL
                   OR n.fechaProgramada <= CURRENT_TIMESTAMP)
              AND n.alerta.idAlerta IN (
                  SELECT a.idAlerta
                  FROM Alerta a
                  WHERE a.estadoActual.codigo = 'ACTIVA'
              )
            """)
    int reservarNotificacion(@Param("idNotificacion") Integer idNotificacion);

    // Finaliza un envío reservado.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = :estado,
                n.fechaEnvio = :fechaEnvio,
                n.resultado = :resultado,
                n.fechaInicioProceso = null
            WHERE n.idNotificacion = :idNotificacion
              AND n.estadoEnvio = 'EN_PROCESO'
            """)
    int finalizarEnvio(
            @Param("idNotificacion") Integer idNotificacion,
            @Param("estado") String estado,
            @Param("fechaEnvio") LocalDateTime fechaEnvio,
            @Param("resultado") String resultado);

    // Reprograma un envío temporalmente fallido.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'PENDIENTE',
                n.fechaProgramada = :fechaReintento,
                n.fechaInicioProceso = null,
                n.resultado = :resultado
            WHERE n.idNotificacion = :idNotificacion
              AND n.estadoEnvio = 'EN_PROCESO'
              AND COALESCE(n.numeroIntento, 0) < 3
            """)
    int reprogramarNotificacion(
            @Param("idNotificacion") Integer idNotificacion,
            @Param("fechaReintento") LocalDateTime fechaReintento,
            @Param("resultado") String resultado);

    // Recupera envíos interrumpidos que todavía admiten reintentos.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'PENDIENTE',
                n.fechaProgramada = :fechaReintento,
                n.fechaInicioProceso = null,
                n.resultado = 'Recuperada tras interrupción del procesamiento.'
            WHERE n.estadoEnvio = 'EN_PROCESO'
              AND n.fechaInicioProceso <= :fechaLimite
              AND COALESCE(n.numeroIntento, 0) < 3
              AND n.alerta.idAlerta IN (
                  SELECT a.idAlerta
                  FROM Alerta a
                  WHERE a.estadoActual.codigo = 'ACTIVA'
              )
            """)
    int recuperarNotificacionesBloqueadas(
            @Param("fechaLimite") LocalDateTime fechaLimite,
            @Param("fechaReintento") LocalDateTime fechaReintento);

    // Cierra envíos interrumpidos sin posibilidad de reintento.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'ERROR',
                n.resultado = 'Recuperación cerrada: límite de intentos o alerta inactiva.',
                n.fechaInicioProceso = null
            WHERE n.estadoEnvio = 'EN_PROCESO'
              AND n.fechaInicioProceso <= :fechaLimite
              AND (
                  COALESCE(n.numeroIntento, 0) >= 3
                  OR n.alerta.idAlerta NOT IN (
                      SELECT a.idAlerta
                      FROM Alerta a
                      WHERE a.estadoActual.codigo = 'ACTIVA'
                  )
              )
            """)
    int cerrarNotificacionesBloqueadas(
            @Param("fechaLimite") LocalDateTime fechaLimite);

    // Cierra notificaciones pendientes que agotaron sus intentos.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'ERROR',
                n.resultado = 'Se alcanzó el máximo de tres intentos.',
                n.fechaInicioProceso = null
            WHERE n.estadoEnvio = 'PENDIENTE'
              AND COALESCE(n.numeroIntento, 0) >= 3
            """)
    int cerrarPendientesAgotadas();

    // Comprueba si existe una notificación en una fecha determinada.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndFechaProgramada(
            Integer idAlerta,
            Integer idDestinatario,
            String canal,
            LocalDateTime fechaProgramada);

    // Obtiene notificaciones programadas de alertas activas.
    @EntityGraph(attributePaths = {"destinatario", "alerta", "alerta.estadoActual"})
    @Query("""
            SELECT n
            FROM Notificacion n
            WHERE n.estadoEnvio = 'PENDIENTE'
              AND (n.fechaProgramada IS NULL OR n.fechaProgramada <= :ahora)
              AND n.alerta.estadoActual.codigo = 'ACTIVA'
            ORDER BY n.fechaProgramada ASC, n.idNotificacion ASC
            """)
    List<Notificacion> buscarPendientesProgramadas(
            @Param("ahora") LocalDateTime ahora);
}
