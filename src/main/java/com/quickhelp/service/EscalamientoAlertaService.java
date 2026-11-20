
package com.quickhelp.service;

import com.quickhelp.entity.Alerta; // Emergencia registrada.
import com.quickhelp.entity.EstadoAlerta; // Estado PENDIENTE.
import com.quickhelp.entity.HistorialAlerta; // Registro del escalamiento.
import com.quickhelp.entity.Notificacion; // Avisos programados.
import com.quickhelp.entity.ParametroSistema; // Configuración del sistema.
import com.quickhelp.entity.RedApoyo; // Integrantes de apoyo.
import com.quickhelp.entity.Usuario; // Administradores y destinatarios.
import com.quickhelp.repository.AlertaRepository; // Consulta y bloqueo de alertas.
import com.quickhelp.repository.EstadoAlertaRepository; // Catálogo de estados.
import com.quickhelp.repository.HistorialAlertaRepository; // Historial de la alerta.
import com.quickhelp.repository.NotificacionRepository; // Persistencia de avisos.
import com.quickhelp.repository.ParametroSistemaRepository; // Parámetros de PostgreSQL.
import com.quickhelp.repository.RedApoyoRepository; // Consulta de red de apoyo.
import com.quickhelp.repository.RespuestaAlertaRepository; // Respuestas de cada asistente.
import org.springframework.beans.factory.annotation.Value; // Configuración de activación.
import org.springframework.scheduling.annotation.Scheduled; // Revisión automática.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.PlatformTransactionManager; // Transacciones.
import org.springframework.transaction.support.TransactionTemplate; // Bloqueo transaccional.
import java.time.Duration; // Tiempo transcurrido desde la emergencia.
import java.time.LocalDateTime; // Fechas de programación.
import java.util.List; // Listados de destinatarios.

@Service // Reenvía la emergencia a los asistentes que todavía no responden.
public class EscalamientoAlertaService {

    // Intervalo predeterminado si todavía no existe el parámetro.
    private static final long INTERVALO_PREDETERMINADO = 2;

    // Clave del intervalo configurable en PostgreSQL.
    private static final String CLAVE_INTERVALO = "INTERVALO_ESCALAMIENTO_SOS";

    private static final String TIPO_REAVISO = "REAVISO_RED";

    // Cantidad máxima de reavisos para cada asistente sin respuesta.
    private static final int MAXIMO_REAVISOS = 5;

    // Estados en que la emergencia sigue abierta (los reavisos continúan en todos).
    private static final List<String> ESTADOS_ABIERTOS =
            List.of("ACTIVA", "PENDIENTE", "EN_ATENCION");

    private final AlertaRepository alertaRepository;
    private final EstadoAlertaRepository estadoAlertaRepository;
    private final HistorialAlertaRepository historialAlertaRepository;
    private final RedApoyoRepository redApoyoRepository;
    private final NotificacionRepository notificacionRepository;
    private final RespuestaAlertaRepository respuestaAlertaRepository;
    private final ParametroSistemaRepository parametroRepository;
    private final ProcesamientoNotificacionService procesamientoNotificacionService;
    private final TransactionTemplate transactionTemplate;
    private final LocalDateTime fechaActivacion;

    // Inyecta los servicios y repositorios existentes.
    public EscalamientoAlertaService(
            AlertaRepository alertaRepository,
            EstadoAlertaRepository estadoAlertaRepository,
            HistorialAlertaRepository historialAlertaRepository,
            RedApoyoRepository redApoyoRepository,
            NotificacionRepository notificacionRepository,
            RespuestaAlertaRepository respuestaAlertaRepository,
            ParametroSistemaRepository parametroRepository,
            ProcesamientoNotificacionService procesamientoNotificacionService,
            PlatformTransactionManager transactionManager,
            @Value("${quickhelp.escalamiento.fecha-activacion:}") String fechaConfigurada) {

        this.alertaRepository = alertaRepository;
        this.estadoAlertaRepository = estadoAlertaRepository;
        this.historialAlertaRepository = historialAlertaRepository;
        this.redApoyoRepository = redApoyoRepository;
        this.notificacionRepository = notificacionRepository;
        this.respuestaAlertaRepository = respuestaAlertaRepository;
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
                    .findByEstadoActual_CodigoInOrderByFechaCreacionDesc(ESTADOS_ABIERTOS);

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

    // Reenvía la emergencia cada intervalo a quien no ha respondido, hasta 5 veces.
    private void generarEscalamiento(Integer idAlerta) {

        // Bloquea la alerta para evitar operaciones simultáneas.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElse(null);

        if (alerta == null || alerta.getFechaCreacion() == null) return;

        // Solo se procesan emergencias que continúan abiertas.
        String codigoEstado = alerta.getEstadoActual().getCodigo();
        if (!ESTADOS_ABIERTOS.contains(codigoEstado)) return;

        // Evita escalar emergencias antiguas.
        if (fechaActivacion == null
                || alerta.getFechaCreacion().isBefore(fechaActivacion)) return;

        // Usa el intervalo fijado al crear la alerta o, si falta, el parámetro actual.
        long intervaloMinutos = alerta.getIntervaloEscalamientoMinutos() != null
                ? alerta.getIntervaloEscalamientoMinutos()
                : obtenerIntervaloMinutos();

        // Regla de escalamiento (aparte de los reavisos): si sigue ACTIVA al cumplirse
        // el intervalo, nadie dijo que va y pasa a PENDIENTE para el administrador.
        if ("ACTIVA".equals(codigoEstado)
                && Duration.between(alerta.getFechaCreacion(), LocalDateTime.now()).toMinutes()
                        >= intervaloMinutos) {
            escalarAPendiente(alerta, intervaloMinutos);
        }

        // Calcula qué reaviso corresponde ahora (1 a 5).
        long minutosTranscurridos = Duration.between(
                alerta.getFechaCreacion(), LocalDateTime.now()).toMinutes();
        long reaviso = Math.min(minutosTranscurridos / intervaloMinutos, MAXIMO_REAVISOS);

        if (reaviso < 1) return;

        // Programa solo el reaviso vigente para no enviar varios juntos si el servidor despierta tarde.
        LocalDateTime fechaReaviso = alerta.getFechaCreacion()
                .plusMinutes(intervaloMinutos * reaviso);

        Integer idAdultoMayor = alerta.getAdultoMayor().getIdUsuario();

        List<RedApoyo> integrantes = redApoyoRepository
                .findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(idAdultoMayor);

        for (RedApoyo integrante : integrantes) {

            // Omite integrantes deshabilitados.
            if (!Boolean.TRUE.equals(integrante.getEstado())) continue;

            Usuario asistente = integrante.getIntegrante();

            // Cualquier respuesta, incluso "No puedo asistir", detiene sus reavisos.
            if (respuestaAlertaRepository.existsByAlerta_IdAlertaAndIntegrante_IdUsuario(
                    idAlerta, asistente.getIdUsuario())) continue;

            registrarNotificacion(alerta, asistente, fechaReaviso, TIPO_REAVISO);
        }
    }

    // Cambia la emergencia a PENDIENTE (Activa/Pendiente) y lo deja en el historial.
    private void escalarAPendiente(Alerta alerta, long intervaloMinutos) {
        EstadoAlerta pendiente = estadoAlertaRepository.findByCodigo("PENDIENTE").orElse(null);
        if (pendiente == null) return; // Falta el estado en la base de datos.

        EstadoAlerta estadoAnterior = alerta.getEstadoActual();
        alerta.setEstadoActual(pendiente);
        alertaRepository.save(alerta);

        HistorialAlerta historial = new HistorialAlerta();
        historial.setAlerta(alerta);
        historial.setUsuarioActor(null); // Lo hace el sistema.
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(pendiente);
        historial.setAccion("ESCALAMIENTO_ALERTA");
        historial.setObservacion("Nadie respondió que va en " + intervaloMinutos
                + " minutos: la emergencia se escaló al administrador.");
        historial.setFechaHora(LocalDateTime.now());
        historialAlertaRepository.save(historial);
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
        notificacion.setTipoAviso(tipoAviso); // Conserva el tipo de recordatorio.
        notificacion.setEstadoEnvio("PENDIENTE");
        notificacion.setFechaProgramada(fechaProgramada);
        notificacion.setFechaEnvio(null);
        notificacion.setResultado(null); // Se completará con el resultado de Firebase.

        notificacionRepository.save(notificacion);
    }
}
