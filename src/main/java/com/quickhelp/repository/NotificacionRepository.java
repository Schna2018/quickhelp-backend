
package com.quickhelp.repository;

import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import java.time.LocalDateTime; // Fecha y hora del envío.
import java.util.List; // Manejo de listas.
import org.springframework.data.jpa.repository.EntityGraph; // Carga relaciones necesarias.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD.
import org.springframework.data.jpa.repository.Modifying; // Permite ejecutar UPDATE.
import org.springframework.data.jpa.repository.Query; // Consultas JPQL.
import org.springframework.data.repository.query.Param; // Vincula parámetros.

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    // Consulta las notificaciones de un usuario, desde la más reciente.
    List<Notificacion> findByDestinatario_IdUsuarioOrderByIdNotificacionDesc(
            Integer idDestinatario);

    // Carga el destinatario y su token FCM junto con cada notificación.
    @EntityGraph(attributePaths = {"destinatario"})
    List<Notificacion> findByAlerta_IdAlertaOrderByIdNotificacionDesc(
            Integer idAlerta);

    // Obtiene las notificaciones según su estado de envío.
    List<Notificacion> findByEstadoEnvioOrderByIdNotificacionAsc(
            String estadoEnvio);

    // Consulta las notificaciones de un destinatario según su estado.
    List<Notificacion> findByDestinatario_IdUsuarioAndEstadoEnvioOrderByIdNotificacionDesc(
            Integer idDestinatario, String estadoEnvio);

    // Comprueba si ya existe una notificación para usuario, alerta y canal.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanal(
            Integer idAlerta, Integer idDestinatario, String canal);

    // Reserva la notificación, incrementa el intento y registra la hora de inicio.
    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.estadoEnvio = 'EN_PROCESO',
                n.numeroIntento = n.numeroIntento + 1,
                n.fechaInicioProceso = CURRENT_TIMESTAMP
            WHERE n.idNotificacion = :idNotificacion
              AND n.estadoEnvio = 'PENDIENTE'
            """)
    int reservarNotificacion(
            @Param("idNotificacion") Integer idNotificacion);

    // Guarda el resultado únicamente si la notificación continúa reservada.
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

    // Comprueba si existe un aviso para el mismo contacto y fecha programada.
    // Se utiliza para controlar los reavisos del escalamiento automático.
    boolean existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndFechaProgramada(
            Integer idAlerta,
            Integer idDestinatario,
            String canal,
            LocalDateTime fechaProgramada);

    // Consulta los avisos programados de emergencias activas o pendientes.
    // Mantiene los reavisos cuando la emergencia pasa a PENDIENTE.
    // Excluye EN_ATENCION para detener los reavisos tras confirmar atención.
    @EntityGraph(attributePaths = {"destinatario", "alerta", "alerta.estadoActual"})
    @Query("""
            SELECT n FROM Notificacion n
            WHERE n.estadoEnvio = 'PENDIENTE'
              AND n.fechaProgramada <= :ahora
              AND n.alerta.estadoActual.codigo IN ('ACTIVA', 'PENDIENTE')
            ORDER BY n.fechaProgramada ASC, n.idNotificacion ASC
            """)
    List<Notificacion> buscarPendientesProgramadas(
            @Param("ahora") LocalDateTime ahora);
}
