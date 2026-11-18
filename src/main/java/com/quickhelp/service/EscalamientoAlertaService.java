
package com.quickhelp.service;

import com.quickhelp.entity.Alerta; // Entidad de alertas.
import com.quickhelp.entity.Notificacion; // Entidad de notificaciones.
import com.quickhelp.entity.RedApoyo; // Integrantes de la red de apoyo.
import com.quickhelp.entity.Usuario; // Usuarios administradores.
import com.quickhelp.repository.AlertaRepository; // Consulta de alertas.
import com.quickhelp.repository.NotificacionRepository; // Registro de reavisos.
import com.quickhelp.repository.RedApoyoRepository; // Consulta de prioridades.
import com.quickhelp.repository.UsuarioRepository; // Consulta de administradores.
import org.springframework.scheduling.annotation.Scheduled; // Tareas automáticas.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import org.springframework.transaction.PlatformTransactionManager; // Transacciones.
import org.springframework.transaction.support.TransactionTemplate; // Control transaccional.
import java.time.LocalDateTime; // Fechas programadas.
import java.util.List; // Colecciones.

@Service
public class EscalamientoAlertaService {

    private static final long INTERVALO_MINUTOS = 2;
    private static final String CODIGO_ADMIN = "ADMIN";
    private static final String TIPO_REAVISO = "REAVISO_RED";
    private static final String TIPO_ADMIN = "ESCALAMIENTO_ADMIN";

    private final AlertaRepository alertaRepository;
    private final RedApoyoRepository redApoyoRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcesamientoNotificacionService procesamientoNotificacionService;
    private final TransactionTemplate transactionTemplate;

    public EscalamientoAlertaService(
            AlertaRepository alertaRepository,
            RedApoyoRepository redApoyoRepository,
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository,
            ProcesamientoNotificacionService procesamientoNotificacionService,
            PlatformTransactionManager transactionManager) {
        this.alertaRepository = alertaRepository;
        this.redApoyoRepository = redApoyoRepository;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.procesamientoNotificacionService = procesamientoNotificacionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // Revisa las alertas activas cada 30 segundos.
    @Scheduled(fixedDelay = 30000)
    public void revisarAlertasActivas() {

        List<Alerta> alertas = alertaRepository
                .findByEstadoActual_CodigoOrderByFechaCreacionDesc("ACTIVA");

        for (Alerta alerta : alertas) {

            // Procesa cada alerta dentro de una transacción independiente.
            transactionTemplate.executeWithoutResult(status ->
                    generarEscalamiento(alerta.getIdAlerta()));
        }

        // Procesa las notificaciones después de confirmar los registros.
        procesamientoNotificacionService.procesarPendientesProgramadas();
    }

    // Registra los reavisos y el aviso administrativo según el tiempo transcurrido.
    private void generarEscalamiento(Integer idAlerta) {

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

            // Considera únicamente integrantes activos.
            if (!Boolean.TRUE.equals(integrante.getEstado())) {
                continue;
            }

            posicion++;

            // Prioridad 1: 2 minutos; prioridad 2: 4; prioridad 3: 6.
            LocalDateTime fechaReaviso = alerta.getFechaCreacion()
                    .plusMinutes(INTERVALO_MINUTOS * posicion);

            if (ahora.isBefore(fechaReaviso)) {
                continue;
            }

            registrarNotificacion(
                    alerta,
                    integrante.getIntegrante(),
                    fechaReaviso,
                    TIPO_REAVISO);
        }

        // Programa el aviso administrativo después del último contacto.
        LocalDateTime fechaAdministrador = alerta.getFechaCreacion()
                .plusMinutes(INTERVALO_MINUTOS * (posicion + 1));

        if (ahora.isBefore(fechaAdministrador)) {
            return;
        }

        // Consulta los administradores activos del sistema.
        List<Usuario> administradores = usuarioRepository
                .findByRol_CodigoAndEstadoTrue(CODIGO_ADMIN);

        for (Usuario administrador : administradores) {

            // Registra el aviso administrativo sin duplicarlo.
            registrarNotificacion(
                    alerta,
                    administrador,
                    fechaAdministrador,
                    TIPO_ADMIN);
        }
    }

    // Registra una notificación con su identificador de tipo.
    private void registrarNotificacion(
            Alerta alerta,
            Usuario destinatario,
            LocalDateTime fechaProgramada,
            String tipoAviso) {

        Integer idAlerta = alerta.getIdAlerta();
        Integer idDestinatario = destinatario.getIdUsuario();

        // Comprueba si el aviso ya fue registrado para esa fecha.
        boolean existe = notificacionRepository
                .existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndFechaProgramada(
                        idAlerta,
                        idDestinatario,
                        "PUSH",
                        fechaProgramada);

        if (existe) {
            return;
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setAlerta(alerta);
        notificacion.setDestinatario(destinatario);
        notificacion.setNumeroIntento(0);
        notificacion.setCanal("PUSH");
        notificacion.setEstadoEnvio("PENDIENTE");
        notificacion.setFechaProgramada(fechaProgramada);
        notificacion.setFechaEnvio(null);

        // Identifica el aviso mientras se encuentra pendiente.
        notificacion.setResultado(tipoAviso);

        notificacionRepository.save(notificacion);
    }
}
