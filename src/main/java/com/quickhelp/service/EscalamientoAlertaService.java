
package com.quickhelp.service;

import com.quickhelp.entity.Alerta; // Emergencia registrada.
import com.quickhelp.entity.Notificacion; // Avisos programados.
import com.quickhelp.entity.ParametroSistema; // Configuración del sistema.
import com.quickhelp.entity.RedApoyo; // Integrantes de apoyo.
import com.quickhelp.entity.Usuario; // Administradores y destinatarios.
import com.quickhelp.repository.AlertaRepository; // Consulta y bloqueo de alertas.
import com.quickhelp.repository.NotificacionRepository; // Persistencia de avisos.
import com.quickhelp.repository.ParametroSistemaRepository; // Parámetros de PostgreSQL.
import com.quickhelp.repository.RedApoyoRepository; // Consulta de red de apoyo.
import com.quickhelp.repository.UsuarioRepository; // Consulta de administradores.
import org.springframework.beans.factory.annotation.Value; // Configuración de activación.
import org.springframework.scheduling.annotation.Scheduled; // Revisión automática.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.PlatformTransactionManager; // Transacciones.
import org.springframework.transaction.support.TransactionTemplate; // Bloqueo transaccional.
import java.time.LocalDateTime; // Fechas de programación.
import java.util.List; // Listados de destinatarios.

@Service // Escala emergencias activas sin atención.
public class EscalamientoAlertaService {

    // Intervalo predeterminado si todavía no existe el parámetro.
    private static final long INTERVALO_PREDETERMINADO = 2;

    // Clave del intervalo configurable en PostgreSQL.
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

    // Inyecta los servicios y repositorios existentes.
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

        // Sin fecha configurada no se generan nuevos escalamientos.
        this.fechaActivacion = fechaConfigurada.isBlank()
                ? null : LocalDateTime.parse(fechaConfigurada);
    }

    // Revisa periódicamente las emergencias activas.
    @Scheduled(fixedDelay = 30000)
    public void revisarAlertasActivas() {

        if (fechaActivacion != null) {
            List<Alerta> alertas = alertaRepository
                    .findByEstadoActual_CodigoOrderByFechaCreacionDesc("ACTIVA");

            for (Alerta alerta : alertas) {

                // Protege las alertas anteriores a la activación.
                if (alerta.getFechaCreacion() == null
                        || alerta.getFechaCreacion().isBefore(fechaActivacion)) {
                    continue;
                }

                // Cada alerta se procesa dentro de una transacción.
                transactionTemplate.executeWithoutResult(status ->
                        generarEscalamiento(alerta.getIdAlerta()));
            }
        }

        // Procesa los avisos previamente programados.
        procesamientoNotificacionService.procesarPendientesProgramadas();
    }

    // Obtiene el intervalo autorizado desde PostgreSQL.
    private long obtenerIntervaloMinutos() {

        return parametroRepository.findByClave(CLAVE_INTERVALO)
                .map(ParametroSistema::getValor)
                .map(valor -> {
                    try {
                        long minutos = Long.parseLong(valor);

                        // Acepta solamente los valores habilitados.
                        if (minutos == 1 || minutos == 2
                                || minutos == 3 || minutos == 5) {
                            return minutos;
                        }
                    } catch (NumberFormatException e) {
                        // Se conserva el intervalo predeterminado.
                    }

                    return INTERVALO_PREDETERMINADO;
                })
                .orElse(INTERVALO_PREDETERMINADO);
    }

    // Genera reavisos secuenciales y aviso final a administradores.
    private void generarEscalamiento(Integer idAlerta) {

        // Bloquea la alerta para evitar operaciones simultáneas.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElse(null);

        if (alerta == null || alerta.getFechaCreacion() == null) return;

        // Solo se escalan emergencias que continúan activas.
        if (!"ACTIVA".equals(alerta.getEstadoActual().getCodigo())) return;

        // Evita escalar emergencias antiguas.
        if (fechaActivacion == null
                || alerta.getFechaCreacion().isBefore(fechaActivacion)) return;

        // Lee el intervalo actual de configuración.
        long intervaloMinutos = obtenerIntervaloMinutos();

        Integer idAdultoMayor = alerta.getAdultoMayor().getIdUsuario();

        // Ordena la red de apoyo por prioridad.
        List<RedApoyo> integrantes = redApoyoRepository
                .findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(idAdultoMayor);

        int posicion = 0;
        LocalDateTime ahora = LocalDateTime.now();

        for (RedApoyo integrante : integrantes) {

            // Omite integrantes deshabilitados.
            if (!Boolean.TRUE.equals(integrante.getEstado())) continue;

            posicion++;

            // Programa cada reaviso según la prioridad.
            LocalDateTime fechaReaviso = alerta.getFechaCreacion()
                    .plusMinutes(intervaloMinutos * posicion);

            if (ahora.isBefore(fechaReaviso)) continue;

            registrarNotificacion(
                    alerta,
                    integrante.getIntegrante(),
                    fechaReaviso,
                    TIPO_REAVISO);
        }

        // Escala a administradores después de recorrer la red.
        LocalDateTime fechaAdministrador = alerta.getFechaCreacion()
                .plusMinutes(intervaloMinutos * (posicion + 1));

        if (ahora.isBefore(fechaAdministrador)) return;

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

    // Evita duplicar avisos para la misma fecha y destinatario.
    private void registrarNotificacion(
            Alerta alerta,
            Usuario destinatario,
            LocalDateTime fechaProgramada,
            String tipoAviso) {

        Integer idAlerta = alerta.getIdAlerta();
        Integer idDestinatario = destinatario.getIdUsuario();

        boolean existe = notificacionRepository
                .existsByAlerta_IdAlertaAndDestinatario_IdUsuarioAndCanalAndFechaProgramada(
                        idAlerta,
                        idDestinatario,
                        "PUSH",
                        fechaProgramada);

        if (existe) return;

        // Crea una notificación pendiente.
        Notificacion notificacion = new Notificacion();
        notificacion.setAlerta(alerta);
        notificacion.setDestinatario(destinatario);
        notificacion.setNumeroIntento(0);
        notificacion.setCanal("PUSH");
        notificacion.setEstadoEnvio("PENDIENTE");
        notificacion.setFechaProgramada(fechaProgramada);
        notificacion.setFechaEnvio(null);
        notificacion.setResultado(tipoAviso);

        notificacionRepository.save(notificacion);
    }
}
