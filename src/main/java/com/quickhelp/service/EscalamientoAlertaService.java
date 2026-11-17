
package com.quickhelp.service;

import com.quickhelp.entity.Alerta; // Entidad de alertas.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.entity.RedApoyo; // Integrantes de la red de apoyo.
import com.quickhelp.repository.AlertaRepository; // Consulta de alertas.
import com.quickhelp.repository.NotificacionRepository; // Registro de reavisos.
import com.quickhelp.repository.RedApoyoRepository; // Consulta de prioridades.
import org.springframework.scheduling.annotation.Scheduled; // Tareas automáticas.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import org.springframework.transaction.PlatformTransactionManager; // Transacciones.
import org.springframework.transaction.support.TransactionTemplate; // Control transaccional.
import java.time.LocalDateTime; // Fechas programadas.
import java.util.List; // Colecciones.

@Service
public class EscalamientoAlertaService {

    private static final long INTERVALO_MINUTOS = 2;

    private final AlertaRepository alertaRepository;
    private final RedApoyoRepository redApoyoRepository;
    private final NotificacionRepository notificacionRepository;
    private final ProcesamientoNotificacionService procesamientoNotificacionService;
    private final TransactionTemplate transactionTemplate;

    public EscalamientoAlertaService(
            AlertaRepository alertaRepository,
            RedApoyoRepository redApoyoRepository,
            NotificacionRepository notificacionRepository,
            ProcesamientoNotificacionService procesamientoNotificacionService,
            PlatformTransactionManager transactionManager) {
        this.alertaRepository = alertaRepository;
        this.redApoyoRepository = redApoyoRepository;
        this.notificacionRepository = notificacionRepository;
        this.procesamientoNotificacionService = procesamientoNotificacionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Revisa las alertas activas cada 30 segundos.
    @Scheduled(fixedDelay = 30000)
    public void revisarAlertasActivas() {

        List<Alerta> alertas = alertaRepository
                .findByEstadoActual_CodigoOrderByFechaCreacionDesc("ACTIVA");

        for (Alerta alerta : alertas) {

            // Cada alerta se procesa dentro de una transacción independiente.
            transactionTemplate.executeWithoutResult(status ->
                    generarReavisos(alerta.getIdAlerta()));
        }

        // Procesa los envíos después de confirmar los registros.
        procesamientoNotificacionService.procesarPendientesProgramadas();
    }

    // Registra los reavisos según el orden de prioridad de la red.
    private void generarReavisos(Integer idAlerta) {

        // Bloquea la alerta para evitar registros simultáneos duplicados.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElse(null);

        if (alerta == null || alerta.getFechaCreacion() == null) {
            return;
        }

        // Detiene el escalamiento si la alerta ya no está activa.
        if (!"ACTIVA".equals(alerta.getEstadoActual().getCodigo())) {
            return;
        }

        Integer idAdultoMayor = alerta.getAdultoMayor().getIdUsuario();

        List<RedApoyo> integrantes = redApoyoRepository
                .findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(idAdultoMayor);

        int posicion = 0;
        LocalDateTime ahora = LocalDateTime.now();

        for (RedApoyo integrante : integrantes) {

            // Solo considera integrantes activos.
            if (!Boolean.TRUE.equals(integrante.getEstado())) {
                continue;
            }

            posicion++;

            // Prioridad 1: 2 min; prioridad 2: 4 min; prioridad 3: 6 min.
            LocalDateTime fechaReaviso = alerta.getFechaCreacion()
                    .plusMinutes(INTERVALO_MINUTOS * posicion);

            // Todavía no corresponde generar este reaviso.
            if (ahora.isBefore(fechaReaviso)) {
                continue;
            }

            Integer idDestinatario = integrante.getIntegrante().getIdUsuario();

            // Impide registrar nuevamente el mismo reaviso.
            boolean existe = notificacionRepository
                    .existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndFechaProgramada(
                            idAlerta, idDestinatario, "PUSH", fechaReaviso);

            if (existe) {
                continue;
            }

            // Crea un nuevo registro para el reaviso.
            Notificacion notificacion = new Notificacion();
            notificacion.setAlerta(alerta);
            notificacion.setDestinatario(integrante.getIntegrante());
            notificacion.setNumeroIntento(0);
            notificacion.setCanal("PUSH");
            notificacion.setEstadoEnvio("PENDIENTE");
            notificacion.setFechaProgramada(fechaReaviso);
            notificacion.setFechaEnvio(null);
            notificacion.setResultado(null);

            notificacionRepository.save(notificacion);
        }
    }
}
