
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

    // Obtiene las notificaciones asociadas a una alerta.
    @EntityGraph(attributePaths = {"destinatario"})
    List<Notificacion> findByAlerta_IdAlertaOrderByIdNotificacionDesc(
            Integer idAlerta);

    // Obtiene las notificaciones según su estado.
    List<Notificacion> findByEstadoEnvioOrderByIdNotificacionAsc(
            String estadoEnvio);

    // Obtiene las notificaciones de un usuario según su estado.
    List<Notificacion> findByDestinatario_IdUsuarioAndEstadoEnvioOrderByIdNotificacionDesc(
            Integer idDestinatario, String estadoEnvio);

    // Evita duplicar la notificación inicial por canal.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanal(
            Integer idAlerta, Integer idDestinatario, String canal);

    // Evita duplicar un aviso de escalamiento del mismo tipo.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndTipoAviso(
            Integer idAlerta, Integer idDestinatario, String canal, String tipoAviso);

    // Reserva una notificación pendiente para procesarla una sola vez.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'EN_PROCESO',
                n.numeroIntento = n.numeroIntento + 1,
                n.fechaInicioProceso = CURRENT_TIMESTAMP
            WHERE n.idNotificacion = :idNotificacion
              AND n.estadoEnvio = 'PENDIENTE'
            """)
    int reservarNotificacion(@Param("idNotificacion") Integer idNotificacion);

    // Registra el resultado del envío de una notificación.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = :estado,
                n.fechaEnvio = :fechaEnvio,
                n.resultado = :resultado
            WHERE n.idNotificacion = :idNotificacion
              AND n.estadoEnvio = 'EN_PROCESO'
            """)
    int finalizarEnvio(
            @Param("idNotificacion") Integer idNotificacion,
            @Param("estado") String estado,
            @Param("fechaEnvio") LocalDateTime fechaEnvio,
            @Param("resultado") String resultado);

    // Reprograma un envío fallido sin perder el número de intentos.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'PENDIENTE',
                n.fechaProgramada = :fechaReintento,
                n.fechaInicioProceso = null,
                n.resultado = :resultado
            WHERE n.idNotificacion = :idNotificacion
              AND n.estadoEnvio = 'EN_PROCESO'
            """)
    int reprogramarNotificacion(
            @Param("idNotificacion") Integer idNotificacion,
            @Param("fechaReintento") LocalDateTime fechaReintento,
            @Param("resultado") String resultado);

    // Conserva la consulta anterior por fecha programada.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndFechaProgramada(
            Integer idAlerta,
            Integer idDestinatario,
            String canal,
            LocalDateTime fechaProgramada);

    // Busca notificaciones pendientes de alertas activas.
    @EntityGraph(attributePaths = {"destinatario", "alerta", "alerta.estadoActual"})
    @Query("""
            SELECT n
            FROM Notificacion n
            WHERE n.estadoEnvio = 'PENDIENTE'
              AND n.fechaProgramada <= :ahora
              AND n.alerta.estadoActual.codigo = 'ACTIVA'
            ORDER BY n.fechaProgramada ASC, n.idNotificacion ASC
            """)
    List<Notificacion> buscarPendientesProgramadas(
            @Param("ahora") LocalDateTime ahora);
}
