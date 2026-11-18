
package com.quickhelp.service;

import com.quickhelp.entity.Alerta; // Entidad de emergencia.
import com.quickhelp.entity.Notificacion; // Entidad de notificación.
import com.quickhelp.entity.ParametroSistema; // Parámetros configurables.
import com.quickhelp.entity.RedApoyo; // Integrantes de la red de apoyo.
import com.quickhelp.entity.Usuario; // Usuarios destinatarios.
import com.quickhelp.repository.AlertaRepository; // Consulta y bloquea alertas.
import com.quickhelp.repository.NotificacionRepository; // Registra notificaciones.
import com.quickhelp.repository.ParametroSistemaRepository; // Consulta configuración.
import com.quickhelp.repository.RedApoyoRepository; // Consulta red de apoyo.
import com.quickhelp.repository.UsuarioRepository; // Consulta administradores.
import java.time.LocalDateTime; // Manejo de fechas.
import java.util.List; // Listados de entidades.
import org.springframework.beans.factory.annotation.Value; // Configuración externa.
import org.springframework.scheduling.annotation.Scheduled; // Ejecución programada.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.PlatformTransactionManager; // Transacciones.
import org.springframework.transaction.support.TransactionTemplate; // Control transaccional.

@Service
public class EscalamientoAlertaService {

    private static final int INTERVALO_PREDETERMINADO = 2;
    private static final String CLAVE_INTERVALO = "INTERVALO_ESCALAMIENTO_SOS";
    private static final String CODIGO_ADMIN = "ADMIN";
    private static final String TIPO_REAVISO = "REAVISO_RED";
    private static final String TIPO_ADMIN = "ESCALAMIENTO_ADMIN";

    private final AlertaRepository alertaRepository;
    private final RedApoyoRepository redApoyoRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParametroSistemaRepository parametroRepository;
    private final ProcesamientoNotificacionService procesamientoNotificacionService;
    private final TransactionTemplate transactionTemplate;
    private final LocalDateTime fechaActivacion;

    public EscalamientoAlertaService(
            AlertaRepository alertaRepository,
            RedApoyoRepository redApoyoRepository,
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository,
            ParametroSistemaRepository parametroRepository,
            ProcesamientoNotificacionService procesamientoNotificacionService,
            PlatformTransactionManager transactionManager,
            @Value("${quickhelp.escalamiento.fecha-activacion:}") String fechaConfigurada) {

        this.alertaRepository = alertaRepository;
        this.redApoyoRepository = redApoyoRepository;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.parametroRepository = parametroRepository;
        this.procesamientoNotificacionService = procesamientoNotificacionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.fechaActivacion = fechaConfigurada.isBlank()
                ? null : LocalDateTime.parse(fechaConfigurada);
    }

    // Revisa las emergencias activas cada 30 segundos.
    @Scheduled(fixedDelay = 30000)
    public void revisarAlertasActivas() {
        if (fechaActivacion != null) {
            List<Alerta> alertas = alertaRepository
                    .findByEstadoActual_CodigoOrderByFechaCreacionDesc("ACTIVA");

            for (Alerta alerta : alertas) {
                if (alerta.getFechaCreacion() == null
                        || alerta.getFechaCreacion().isBefore(fechaActivacion)) {
                    continue;
                }

                transactionTemplate.executeWithoutResult(
                        status -> generarEscalamiento(alerta.getIdAlerta()));
            }
        }

        procesamientoNotificacionService.procesarPendientesProgramadas();
    }

    // Obtiene el intervalo global para utilizarlo como valor de respaldo.
    private int obtenerIntervaloConfigurado() {
        return parametroRepository.findByClave(CLAVE_INTERVALO)
                .map(ParametroSistema::getValor)
                .map(valor -> {
                    try {
                        int minutos = Integer.parseInt(valor);
                        if (minutos == 1 || minutos == 2
                                || minutos == 3 || minutos == 5) {
                            return minutos;
                        }
                    } catch (NumberFormatException e) {
                        // Utiliza el intervalo predeterminado.
                    }
                    return INTERVALO_PREDETERMINADO;
                })
                .orElse(INTERVALO_PREDETERMINADO);
    }

    // Mantiene fijo el intervalo de cada emergencia.
    private int obtenerIntervaloAlerta(Alerta alerta) {
        Integer intervalo = alerta.getIntervaloEscalamientoMinutos();

        if (intervalo != null && intervalo > 0) {
            return intervalo;
        }

        // Compatibilidad con emergencias creadas antes de la migración.
        return INTERVALO_PREDETERMINADO;
    }

    // Genera los reavisos y el escalamiento al administrador.
    private void generarEscalamiento(Integer idAlerta) {
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElse(null);

        if (alerta == null || alerta.getFechaCreacion() == null) {
            return;
        }

        if (!"ACTIVA".equals(alerta.getEstadoActual().getCodigo())) {
            return;
        }

        if (fechaActivacion == null
                || alerta.getFechaCreacion().isBefore(fechaActivacion)) {
            return;
        }

        int intervaloMinutos = obtenerIntervaloAlerta(alerta);
        Integer idAdultoMayor = alerta.getAdultoMayor().getIdUsuario();

        List<RedApoyo> integrantes = redApoyoRepository
                .findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(idAdultoMayor);

        int posicion = 0;
        LocalDateTime ahora = LocalDateTime.now();

        for (RedApoyo integrante : integrantes) {
            if (!Boolean.TRUE.equals(integrante.getEstado())) {
                continue;
            }

            posicion++;

            LocalDateTime fechaReaviso = alerta.getFechaCreacion()
                    .plusMinutes((long) intervaloMinutos * posicion);

            if (ahora.isBefore(fechaReaviso)) {
                continue;
            }

            registrarNotificacion(
                    alerta,
                    integrante.getIntegrante(),
                    fechaReaviso,
                    TIPO_REAVISO);
        }

        LocalDateTime fechaAdministrador = alerta.getFechaCreacion()
                .plusMinutes((long) intervaloMinutos * (posicion + 1));

        if (ahora.isBefore(fechaAdministrador)) {
            return;
        }

        List<Usuario> administradores = usuarioRepository
                .findByRol_CodigoAndEstadoTrue(CODIGO_ADMIN);

        for (Usuario administrador : administradores) {
            registrarNotificacion(
                    alerta,
                    administrador,
                    fechaAdministrador,
                    TIPO_ADMIN);
        }
    }

    // Registra un aviso únicamente si todavía no existe.
    private void registrarNotificacion(
            Alerta alerta,
            Usuario destinatario,
            LocalDateTime fechaProgramada,
            String tipoAviso) {

        Integer idAlerta = alerta.getIdAlerta();
        Integer idDestinatario = destinatario.getIdUsuario();

        boolean existe = notificacionRepository
                .existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndTipoAviso(
                        idAlerta,
                        idDestinatario,
                        "PUSH",
                        tipoAviso);

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
        notificacion.setResultado(null);
        notificacion.setTipoAviso(tipoAviso);

        notificacionRepository.save(notificacion);
    }
}
