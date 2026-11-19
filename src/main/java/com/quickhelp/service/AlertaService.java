
package com.quickhelp.service;

import com.quickhelp.dto.AlertaResponseDTO; // Respuesta de emergencias.
import com.quickhelp.dto.CrearAlertaRequestDTO; // Datos GPS opcionales.
import com.quickhelp.entity.Alerta; // Entidad de emergencias.
import com.quickhelp.entity.Auditoria; // Registro de acciones.
import com.quickhelp.entity.EstadoAlerta; // Estados de alertas.
import com.quickhelp.entity.HistorialAlerta; // Historial de emergencias.
import com.quickhelp.entity.Ubicacion; // Ubicación GPS.
import com.quickhelp.entity.Usuario; // Usuarios del sistema.
import com.quickhelp.event.AlertaCreadaEvent; // Evento de nueva alerta.
import com.quickhelp.repository.AlertaRepository; // Acceso a alertas.
import com.quickhelp.repository.AuditoriaRepository; // Acceso a auditoría.
import com.quickhelp.repository.EstadoAlertaRepository; // Acceso a estados.
import com.quickhelp.repository.HistorialAlertaRepository; // Acceso al historial.
import com.quickhelp.repository.ParametroSistemaRepository; // Parámetros SOS.
import com.quickhelp.repository.UbicacionRepository; // Acceso a ubicaciones.
import com.quickhelp.repository.UsuarioRepository; // Acceso a usuarios.
import java.math.BigDecimal; // Validación de coordenadas.
import java.time.LocalDateTime; // Fecha y hora.
import java.util.LinkedHashMap; // Respuesta JSON ordenada.
import java.util.List; // Listados.
import java.util.Map; // Datos JSON.
import org.springframework.context.ApplicationEventPublisher; // Eventos.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Autenticación.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@Service // Gestiona el ciclo de vida de las emergencias.
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final EstadoAlertaRepository estadoAlertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialAlertaRepository historialAlertaRepository;
    private final UbicacionRepository ubicacionRepository;
    private final RedApoyoAutorizacionService redApoyoAutorizacionService;
    private final NotificacionService notificacionService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditoriaRepository auditoriaRepository;
    private final ParametroSistemaRepository parametroSistemaRepository;

    // Inyecta los repositorios y servicios existentes.
    public AlertaService(
            AlertaRepository alertaRepository,
            EstadoAlertaRepository estadoAlertaRepository,
            UsuarioRepository usuarioRepository,
            HistorialAlertaRepository historialAlertaRepository,
            UbicacionRepository ubicacionRepository,
            RedApoyoAutorizacionService redApoyoAutorizacionService,
            NotificacionService notificacionService,
            ApplicationEventPublisher eventPublisher,
            AuditoriaRepository auditoriaRepository,
            ParametroSistemaRepository parametroSistemaRepository) {

        this.alertaRepository = alertaRepository;
        this.estadoAlertaRepository = estadoAlertaRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialAlertaRepository = historialAlertaRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.redApoyoAutorizacionService = redApoyoAutorizacionService;
        this.notificacionService = notificacionService;
        this.eventPublisher = eventPublisher;
        this.auditoriaRepository = auditoriaRepository;
        this.parametroSistemaRepository = parametroSistemaRepository;
    }

    // Mantiene la compatibilidad con llamadas sin coordenadas GPS.
    @Transactional
    public Integer crearAlerta(Authentication authentication) {
        return crearAlerta(authentication, null);
    }

    // Reutiliza una emergencia abierta o crea una nueva.
    @Transactional
    public Integer crearAlerta(
            Authentication authentication,
            CrearAlertaRequestDTO request) {

        Integer idAdulto = obtenerIdAutenticado(authentication);

        // Bloquea al usuario para coordinar solicitudes simultáneas.
        Usuario adulto = usuarioRepository.buscarPorIdConBloqueo(idAdulto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        // Solo permite generar alertas a adultos mayores activos.
        if (!Boolean.TRUE.equals(adulto.getEstado())
                || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un adulto mayor activo puede generar alertas.");
        }

        validarCoordenadas(request);

        // Busca una emergencia abierta del mismo adulto.
        var emergenciaAbierta = alertaRepository
                .findFirstByAdultoMayor_IdUsuarioAndEstadoActual_CodigoInOrderByFechaCreacionDescIdAlertaDesc(
                        idAdulto, List.of("ACTIVA", "PENDIENTE", "EN_ATENCION"));

        if (emergenciaAbierta.isPresent()) {
            Alerta existente = emergenciaAbierta.get();
            guardarUbicacion(existente, request);
            return existente.getIdAlerta();
        }

        EstadoAlerta estadoInicial = estadoAlertaRepository.findByCodigo("ACTIVA")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No existe el estado ACTIVA."));

        // Construye y guarda la emergencia.
        Alerta alerta = new Alerta();
        alerta.setAdultoMayor(adulto);
        alerta.setEstadoActual(estadoInicial);
        alerta.setTipoActivacion(tipoActivacion(request));
        alerta.setFechaCreacion(LocalDateTime.now());
        alerta.setIntervaloEscalamientoMinutos(obtenerIntervaloEscalamientoMinutos());

        Alerta alertaGuardada = alertaRepository.save(alerta);
        guardarUbicacion(alertaGuardada, request);

        // Registra la creación en el historial.
        HistorialAlerta historial = new HistorialAlerta();
        historial.setAlerta(alertaGuardada);
        historial.setUsuarioActor(adulto);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(estadoInicial);
        historial.setAccion("CREACION_ALERTA");
        historial.setObservacion(
                describirActivacion(alertaGuardada.getTipoActivacion()));
        historial.setFechaHora(LocalDateTime.now());
        historialAlertaRepository.save(historial);

        // Audita la creación de la emergencia.
        registrarAuditoria(
                adulto,
                alertaGuardada.getIdAlerta(),
                "CREACION_ALERTA",
                "El adulto mayor activó una emergencia.");

        // Genera y programa las notificaciones SOS.
        notificacionService.generarNotificacionesEmergencia(alertaGuardada);
        eventPublisher.publishEvent(
                new AlertaCreadaEvent(alertaGuardada.getIdAlerta()));

        return alertaGuardada.getIdAlerta();
    }

    // Formas de activar una SOS: botón de la app, widget, panel rápido o voz.
    private static final java.util.Set<String> TIPOS_ACTIVACION =
            java.util.Set.of("MANUAL", "WIDGET", "PANEL", "VOZ");

    // Usa el tipo enviado por la app; si falta o no es válido, queda MANUAL.
    private String tipoActivacion(CrearAlertaRequestDTO request) {
        if (request == null || request.tipoActivacion() == null) return "MANUAL";
        String tipo = request.tipoActivacion().strip().toUpperCase(java.util.Locale.ROOT);
        return TIPOS_ACTIVACION.contains(tipo) ? tipo : "MANUAL";
    }

    // Texto del historial según cómo se activó la emergencia.
    private String describirActivacion(String tipo) {
        return switch (tipo) {
            case "VOZ" -> "Emergencia activada por voz por el adulto mayor.";
            case "WIDGET" -> "Emergencia activada desde el widget por el adulto mayor.";
            case "PANEL" -> "Emergencia activada desde el panel rápido por el adulto mayor.";
            default -> "Emergencia activada manualmente por el adulto mayor.";
        };
    }

    // Obtiene el intervalo configurado para el escalamiento SOS.
    private Integer obtenerIntervaloEscalamientoMinutos() {
        return parametroSistemaRepository.findByClave("INTERVALO_ESCALAMIENTO_SOS")
                .map(parametro -> parametro.getValor())
                .map(valor -> {
                    try {
                        int minutos = Integer.parseInt(valor);
                        return (minutos == 1 || minutos == 2 || minutos == 3 || minutos == 5)
                                ? minutos : 2;
                    } catch (NumberFormatException ex) {
                        return 2;
                    }
                })
                .orElse(2);
    }

    // Guarda coordenadas sin sobrescribir ubicaciones anteriores.
    private void guardarUbicacion(
            Alerta alerta,
            CrearAlertaRequestDTO request) {

        if (request == null
                || request.latitud() == null
                || request.longitud() == null) {
            return;
        }

        Ubicacion ubicacion = new Ubicacion();
        ubicacion.setAlerta(alerta);
        ubicacion.setLatitud(request.latitud());
        ubicacion.setLongitud(request.longitud());
        ubicacion.setPrecisionMetros(request.precisionMetros());
        ubicacion.setFechaHora(LocalDateTime.now());
        ubicacionRepository.save(ubicacion);
    }

    // Valida las coordenadas GPS opcionales.
    private void validarCoordenadas(CrearAlertaRequestDTO request) {
        if (request == null) return;

        boolean tieneLatitud = request.latitud() != null;
        boolean tieneLongitud = request.longitud() != null;

        if (tieneLatitud != tieneLongitud) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes enviar latitud y longitud juntas.");
        }

        if (!tieneLatitud && request.precisionMetros() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La precisión requiere coordenadas GPS.");
        }

        if (!tieneLatitud) return;

        if (request.latitud().compareTo(BigDecimal.valueOf(-90)) < 0
                || request.latitud().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Latitud GPS inválida.");
        }

        if (request.longitud().compareTo(BigDecimal.valueOf(-180)) < 0
                || request.longitud().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Longitud GPS inválida.");
        }

        if (request.precisionMetros() != null
                && request.precisionMetros().signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Precisión GPS inválida.");
        }
    }

    // Consulta todas las emergencias para el portal administrativo.
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAlertasAdministracion(
            Authentication authentication) {

        Integer idUsuario = obtenerIdAutenticado(authentication);

        Usuario administrador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario no autorizado."));

        if (!Boolean.TRUE.equals(administrador.getEstado())
                || administrador.getRol() == null
                || !Boolean.TRUE.equals(administrador.getRol().getEstado())
                || !"ADMIN".equals(administrador.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Acceso exclusivo para administradores.");
        }

        return alertaRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .map(this::convertirAlertaDTO)
                .toList();
    }

    // Consulta el historial real de una emergencia para ADMIN.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> consultarHistorialAdministracion(
            Integer idAlerta,
            Authentication authentication) {

        Integer idUsuario = obtenerIdAutenticado(authentication);

        Usuario administrador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario no autorizado."));

        if (!Boolean.TRUE.equals(administrador.getEstado())
                || administrador.getRol() == null
                || !Boolean.TRUE.equals(administrador.getRol().getEstado())
                || !"ADMIN".equals(administrador.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Acceso exclusivo para administradores.");
        }

        // Comprueba que la emergencia exista en PostgreSQL.
        if (!alertaRepository.existsById(idAlerta)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Alerta no encontrada.");
        }

        // Recupera los eventos registrados, del más reciente al más antiguo.
        return historialAlertaRepository
                .findByAlerta_IdAlertaOrderByFechaHoraDesc(idAlerta)
                .stream()
                .map(historial -> {
                    Map<String, Object> evento = new LinkedHashMap<>();
                    evento.put("accion", historial.getAccion());
                    evento.put("observacion", historial.getObservacion());
                    evento.put("fechaHora", historial.getFechaHora());
                    evento.put("idUsuarioActor",
                            historial.getUsuarioActor() != null
                                    ? historial.getUsuarioActor().getIdUsuario()
                                    : null);
                    evento.put("estadoAnterior",
                            historial.getEstadoAnterior() != null
                                    ? historial.getEstadoAnterior().getCodigo()
                                    : null);
                    evento.put("estadoNuevo",
                            historial.getEstadoNuevo() != null
                                    ? historial.getEstadoNuevo().getCodigo()
                                    : null);
                    return evento;
                })
                .toList();
    }

    // Consulta las alertas del adulto mayor autenticado.
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAlertas(
            Authentication authentication) {

        Integer idAdulto = obtenerIdAutenticado(authentication);

        Usuario adulto = usuarioRepository.findById(idAdulto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!Boolean.TRUE.equals(adulto.getEstado())
                || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo el adulto mayor puede consultar sus alertas.");
        }

        return alertaRepository
                .findByAdultoMayor_IdUsuarioOrderByFechaCreacionDesc(idAdulto)
                .stream()
                .map(this::convertirAlertaDTO)
                .toList();
    }

    // Consulta las alertas de los adultos vinculados al asistente.
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAlertasDeMiRed(
            Authentication authentication) {

        Integer idAsistente = redApoyoAutorizacionService
                .obtenerIdAsistenteActivo(authentication);

        return alertaRepository.buscarAlertasDeMiRed(idAsistente)
                .stream()
                .map(this::convertirAlertaDTO)
                .toList();
    }

    // Consulta las alertas de un adulto mayor vinculado.
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAlertasAdultoMayor(
            Integer idAdultoMayor,
            Authentication authentication) {

        redApoyoAutorizacionService.validarAccesoAsistente(
                idAdultoMayor, authentication);

        return alertaRepository
                .findByAdultoMayor_IdUsuarioOrderByFechaCreacionDesc(idAdultoMayor)
                .stream()
                .map(this::convertirAlertaDTO)
                .toList();
    }

    // Consulta la ubicación GPS más reciente de una emergencia.
    @Transactional(readOnly = true)
    public Map<String, Object> consultarUbicacion(
            Integer idAlerta,
            Authentication authentication) {

        Integer idUsuario = obtenerIdAutenticado(authentication);

        Alerta alerta = alertaRepository.findById(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario no autorizado."));

        // Exige que la cuenta y su rol estén activos.
        if (!Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Usuario inactivo o sin rol válido.");
        }

        Integer idAdulto = alerta.getAdultoMayor().getIdUsuario();

        // El adulto mayor puede consultar su propia emergencia.
        boolean esPropietario = idAdulto.equals(idUsuario)
                && "ADULTO_MAYOR".equals(usuario.getRol().getCodigo());

        // Permite consultar el GPS al administrador activo.
        boolean esAdministrador = "ADMIN".equals(usuario.getRol().getCodigo());

        // Los asistentes deben conservar su vinculación autorizada.
        if (!esPropietario && !esAdministrador) {
            redApoyoAutorizacionService.validarAccesoAsistente(
                    idAdulto, authentication);
        }

        // Busca la última ubicación registrada.
        var ubicacionEncontrada = ubicacionRepository
                .findFirstByAlerta_IdAlertaOrderByFechaHoraDescIdUbicacionDesc(
                        idAlerta);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("idAlerta", idAlerta);
        respuesta.put("disponible", ubicacionEncontrada.isPresent());

        if (ubicacionEncontrada.isEmpty()) {
            respuesta.put("latitud", null);
            respuesta.put("longitud", null);
            respuesta.put("precisionMetros", null);
            respuesta.put("fechaHora", null);
            return respuesta;
        }

        Ubicacion ubicacion = ubicacionEncontrada.get();
        respuesta.put("latitud", ubicacion.getLatitud());
        respuesta.put("longitud", ubicacion.getLongitud());
        respuesta.put("precisionMetros", ubicacion.getPrecisionMetros());
        respuesta.put("fechaHora", ubicacion.getFechaHora());

        return respuesta;
    }

    // Permite que un asistente vinculado inicie una atención.
    @Transactional
    public void iniciarAtencion(
            Integer idAlerta,
            Authentication authentication) {

        Integer idAsistente = obtenerIdAutenticado(authentication);

        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        redApoyoAutorizacionService.validarAccesoAsistente(
                alerta.getAdultoMayor().getIdUsuario(), authentication);

        if (!"ACTIVA".equals(alerta.getEstadoActual().getCodigo())
                && !"PENDIENTE".equals(alerta.getEstadoActual().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La alerta no está disponible para iniciar atención.");
        }

        EstadoAlerta nuevoEstado = estadoAlertaRepository
                .findByCodigo("EN_ATENCION")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No existe el estado EN_ATENCION."));

        Usuario asistente = usuarioRepository.findById(idAsistente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Asistente no encontrado."));

        EstadoAlerta estadoAnterior = alerta.getEstadoActual();

        alerta.setEstadoActual(nuevoEstado);
        alerta.setResponsableAtencion(asistente);
        alerta.setFechaAtencion(LocalDateTime.now());
        alertaRepository.save(alerta);

        // Registra el inicio de atención.
        HistorialAlerta historial = new HistorialAlerta();
        historial.setAlerta(alerta);
        historial.setUsuarioActor(asistente);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(nuevoEstado);
        historial.setAccion("INICIO_ATENCION");
        historial.setObservacion(
                "El asistente inició la atención de la emergencia.");
        historial.setFechaHora(LocalDateTime.now());
        historialAlertaRepository.save(historial);

        registrarAuditoria(
                asistente,
                alerta.getIdAlerta(),
                "INICIO_ATENCION",
                "El asistente inició la atención de la emergencia.");
    }

    // Permite resolver una emergencia al asistente responsable.
    @Transactional
    public void resolverAlerta(
            Integer idAlerta,
            String observacion,
            Authentication authentication) {

        Integer idAsistente = obtenerIdAutenticado(authentication);

        if (observacion == null || observacion.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar una observación de cierre.");
        }

        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        Usuario responsable = alerta.getResponsableAtencion();

        if (responsable == null
                || !responsable.getIdUsuario().equals(idAsistente)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No eres el responsable de esta alerta.");
        }

        redApoyoAutorizacionService.validarAccesoAsistente(
                alerta.getAdultoMayor().getIdUsuario(), authentication);

        if (!"EN_ATENCION".equals(alerta.getEstadoActual().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La alerta no se encuentra en atención.");
        }

        EstadoAlerta estadoResuelta = estadoAlertaRepository
                .findByCodigo("RESUELTA")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No existe el estado RESUELTA."));

        EstadoAlerta estadoAnterior = alerta.getEstadoActual();
        LocalDateTime fechaCierre = LocalDateTime.now();

        alerta.setEstadoActual(estadoResuelta);
        alerta.setFechaCierre(fechaCierre);
        alerta.setObservacionCierre(observacion.trim());
        alertaRepository.save(alerta);

        // Registra la resolución.
        HistorialAlerta historial = new HistorialAlerta();
        historial.setAlerta(alerta);
        historial.setUsuarioActor(responsable);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoResuelta);
        historial.setAccion("RESOLUCION_ALERTA");
        historial.setObservacion(observacion.trim());
        historial.setFechaHora(fechaCierre);
        historialAlertaRepository.save(historial);

        registrarAuditoria(
                responsable,
                alerta.getIdAlerta(),
                "RESOLUCION_ALERTA",
                "El asistente resolvió la emergencia.");
    }

    // Permite al administrador finalizar una emergencia activa, pendiente o en atención.
    @Transactional
    public void resolverAlertaAdministracion(
            Integer idAlerta,
            String observacion,
            Authentication authentication) {

        Integer idAdministrador = obtenerIdAutenticado(authentication);

        // Exige una observación para justificar el cierre administrativo.
        if (observacion == null || observacion.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar una observación de cierre.");
        }

        // Comprueba que el administrador y su rol estén activos.
        Usuario administrador = usuarioRepository.findById(idAdministrador)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario no autorizado."));

        if (!Boolean.TRUE.equals(administrador.getEstado())
                || administrador.getRol() == null
                || !Boolean.TRUE.equals(administrador.getRol().getEstado())
                || !"ADMIN".equals(administrador.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso exclusivo para administradores.");
        }

        // Bloquea la emergencia para evitar cierres simultáneos.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        EstadoAlerta estadoAnterior = alerta.getEstadoActual();
        String codigoAnterior = estadoAnterior.getCodigo();

        // Solo se pueden cerrar emergencias todavía abiertas.
        if (!"ACTIVA".equals(codigoAnterior)
                && !"PENDIENTE".equals(codigoAnterior)
                && !"EN_ATENCION".equals(codigoAnterior)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La emergencia ya está cerrada o no admite cierre administrativo.");
        }

        EstadoAlerta estadoResuelta = estadoAlertaRepository
                .findByCodigo("RESUELTA")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No existe el estado RESUELTA."));

        LocalDateTime fechaCierre = LocalDateTime.now();
        String observacionFinal = observacion.trim();

        // Conserva el responsable de atención y registra el cierre.
        alerta.setEstadoActual(estadoResuelta);
        alerta.setFechaCierre(fechaCierre);
        alerta.setObservacionCierre(observacionFinal);
        alertaRepository.save(alerta);

        // Identifica al administrador que realizó la operación.
        HistorialAlerta historial = new HistorialAlerta();
        historial.setAlerta(alerta);
        historial.setUsuarioActor(administrador);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoResuelta);
        historial.setAccion("RESOLUCION_ADMINISTRATIVA");
        historial.setObservacion(observacionFinal);
        historial.setFechaHora(fechaCierre);
        historialAlertaRepository.save(historial);

        // Conserva una traza adicional de la operación administrativa.
        registrarAuditoria(
                administrador,
                alerta.getIdAlerta(),
                "RESOLUCION_ADMINISTRATIVA",
                "El administrador finalizó la emergencia. Motivo: "
                        + observacionFinal);
    }

    // Obtiene el identificador del usuario desde el JWT.
    private Integer obtenerIdAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token inválido.");
        }
    }

    // Convierte una alerta al DTO de respuesta existente.
    private AlertaResponseDTO convertirAlertaDTO(Alerta alerta) {
        return new AlertaResponseDTO(
                alerta.getIdAlerta(),
                alerta.getAdultoMayor().getIdUsuario(),
                alerta.getAdultoMayor().getNombreApellido(),
                alerta.getEstadoActual().getCodigo(),
                alerta.getTipoActivacion(),
                alerta.getFechaCreacion(),
                alerta.getFechaAtencion(),
                alerta.getFechaCierre()
        );
    }

    // Registra una acción importante sobre una emergencia.
    private void registrarAuditoria(
            Usuario usuario,
            Integer idAlerta,
            String accion,
            String detalle) {

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(usuario);
        auditoria.setEntidad("alerta");
        auditoria.setIdRegistro(idAlerta);
        auditoria.setAccion(accion);
        auditoria.setDetalle(detalle);
        auditoria.setFechaHora(LocalDateTime.now());
        auditoriaRepository.save(auditoria);
    }
}
