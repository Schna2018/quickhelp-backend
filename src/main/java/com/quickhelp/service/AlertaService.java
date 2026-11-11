
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
import com.quickhelp.repository.UbicacionRepository; // Acceso a ubicaciones.
import com.quickhelp.repository.UsuarioRepository; // Acceso a usuarios.
import java.math.BigDecimal; // Validación de coordenadas.
import java.time.LocalDateTime; // Fecha y hora.
import java.util.LinkedHashMap; // Respuesta JSON ordenada.
import java.util.List; // Listado de alertas.
import java.util.Map; // Datos de ubicación.
import org.springframework.context.ApplicationEventPublisher; // Eventos Spring.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
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
    private final AuditoriaRepository auditoriaRepository; // Guarda acciones auditadas.

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
            AuditoriaRepository auditoriaRepository) {

        this.alertaRepository = alertaRepository;
        this.estadoAlertaRepository = estadoAlertaRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialAlertaRepository = historialAlertaRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.redApoyoAutorizacionService = redApoyoAutorizacionService;
        this.notificacionService = notificacionService;
        this.eventPublisher = eventPublisher;
        this.auditoriaRepository = auditoriaRepository; // Inyecta auditoría.
    }

    // Mantiene la compatibilidad con llamadas sin coordenadas GPS.
    @Transactional
    public Integer crearAlerta(Authentication authentication) {
        return crearAlerta(authentication, null);
    }

    // Crea una alerta y registra su ubicación cuando está disponible.
    @Transactional
    public Integer crearAlerta(
            Authentication authentication,
            CrearAlertaRequestDTO request) {

        Integer idAdulto = obtenerIdAutenticado(authentication);

        // Recupera al usuario que activó la emergencia.
        Usuario adulto = usuarioRepository.findById(idAdulto)
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

        validarCoordenadas(request); // GPS opcional, pero válido si se envía.

        // Recupera el estado inicial de la emergencia.
        EstadoAlerta estadoInicial = estadoAlertaRepository.findByCodigo("ACTIVA")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No existe el estado ACTIVA."));

        // Construye y guarda la emergencia.
        Alerta alerta = new Alerta();
        alerta.setAdultoMayor(adulto);
        alerta.setEstadoActual(estadoInicial);
        alerta.setTipoActivacion("MANUAL");
        alerta.setFechaCreacion(LocalDateTime.now());

        Alerta alertaGuardada = alertaRepository.save(alerta);

        // Registra las coordenadas si el teléfono las proporcionó.
        if (request != null
                && request.latitud() != null
                && request.longitud() != null) {

            Ubicacion ubicacion = new Ubicacion();
            ubicacion.setAlerta(alertaGuardada);
            ubicacion.setLatitud(request.latitud());
            ubicacion.setLongitud(request.longitud());
            ubicacion.setPrecisionMetros(request.precisionMetros());
            ubicacion.setFechaHora(LocalDateTime.now());

            ubicacionRepository.save(ubicacion);
        }

        // Registra la creación en el historial.
        HistorialAlerta historial = new HistorialAlerta();
        historial.setAlerta(alertaGuardada);
        historial.setUsuarioActor(adulto);
        historial.setEstadoAnterior(null);
        historial.setEstadoNuevo(estadoInicial);
        historial.setAccion("CREACION_ALERTA");
        historial.setObservacion(
                "Emergencia activada manualmente por el adulto mayor.");
        historial.setFechaHora(LocalDateTime.now());

        historialAlertaRepository.save(historial);

        // Audita la creación de la emergencia.
        registrarAuditoria(
                adulto,
                alertaGuardada.getIdAlerta(),
                "CREACION_ALERTA",
                "El adulto mayor activó una emergencia.");

        // Genera las notificaciones de la red de apoyo.
        notificacionService.generarNotificacionesEmergencia(alertaGuardada);

        // Publica el evento para procesar las notificaciones tras el commit.
        eventPublisher.publishEvent(
                new AlertaCreadaEvent(alertaGuardada.getIdAlerta()));

        return alertaGuardada.getIdAlerta();
    }

    // Comprueba las coordenadas sin exigir que exista GPS.
    private void validarCoordenadas(CrearAlertaRequestDTO request) {
        if (request == null) return;

        boolean tieneLatitud = request.latitud() != null;
        boolean tieneLongitud = request.longitud() != null;

        // No permite coordenadas incompletas.
        if (tieneLatitud != tieneLongitud) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes enviar latitud y longitud juntas.");
        }

        // La precisión solo puede enviarse con coordenadas.
        if (!tieneLatitud && request.precisionMetros() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La precisión requiere coordenadas GPS.");
        }

        if (!tieneLatitud) return; // La emergencia puede crearse sin GPS.

        // Comprueba el rango válido de latitud.
        if (request.latitud().compareTo(BigDecimal.valueOf(-90)) < 0
                || request.latitud().compareTo(BigDecimal.valueOf(90)) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Latitud GPS inválida.");
        }

        // Comprueba el rango válido de longitud.
        if (request.longitud().compareTo(BigDecimal.valueOf(-180)) < 0
                || request.longitud().compareTo(BigDecimal.valueOf(180)) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Longitud GPS inválida.");
        }

        // La precisión GPS no puede ser negativa.
        if (request.precisionMetros() != null
                && request.precisionMetros().signum() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Precisión GPS inválida.");
        }
    }

    // Consulta las alertas del adulto mayor autenticado.
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAlertas(
            Authentication authentication) {

        Integer idAdulto = obtenerIdAutenticado(authentication);

        Usuario adulto = usuarioRepository.findById(idAdulto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        // Verifica que el usuario y su rol estén activos.
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

    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarAlertasDeMiRed(Authentication authentication) {
        Integer idAsistente = redApoyoAutorizacionService.obtenerIdAsistenteActivo(authentication);
        return alertaRepository.buscarAlertasDeMiRed(idAsistente).stream()
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

        // Busca la emergencia solicitada.
        Alerta alerta = alertaRepository.findById(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        // Comprueba que el usuario tenga una cuenta y un rol activos.
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario no autorizado."));

        if (!Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Usuario inactivo o sin rol válido.");
        }

        Integer idAdulto = alerta.getAdultoMayor().getIdUsuario();

        // El adulto mayor propietario puede consultar su propia alerta.
        boolean esPropietario = idAdulto.equals(idUsuario)
                && "ADULTO_MAYOR".equals(usuario.getRol().getCodigo());

        // Si no es propietario, debe ser un asistente vinculado.
        if (!esPropietario) {
            redApoyoAutorizacionService.validarAccesoAsistente(
                    idAdulto, authentication);
        }

        // Busca la ubicación más reciente de la emergencia.
        var ubicacionEncontrada = ubicacionRepository
                .findFirstByAlerta_IdAlertaOrderByFechaHoraDescIdUbicacionDesc(
                        idAlerta);

        // Prepara una respuesta que admite valores nulos.
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("idAlerta", idAlerta);
        respuesta.put("disponible", ubicacionEncontrada.isPresent());

        if (ubicacionEncontrada.isEmpty()) {
            // La emergencia existe, pero no tiene coordenadas registradas.
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

        // Bloquea la alerta para evitar atenciones simultáneas.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        // Comprueba que el asistente esté vinculado al adulto mayor.
        redApoyoAutorizacionService.validarAccesoAsistente(
                alerta.getAdultoMayor().getIdUsuario(), authentication);

        if (!"ACTIVA".equals(alerta.getEstadoActual().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La alerta no está disponible para iniciar atención.");
        }

        // Obtiene el estado EN_ATENCION desde PostgreSQL.
        EstadoAlerta nuevoEstado = estadoAlertaRepository.findByCodigo("EN_ATENCION")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No existe el estado EN_ATENCION."));

        Usuario asistente = usuarioRepository.findById(idAsistente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Asistente no encontrado."));

        EstadoAlerta estadoAnterior = alerta.getEstadoActual();

        // Actualiza el estado y asigna al responsable.
        alerta.setEstadoActual(nuevoEstado);
        alerta.setResponsableAtencion(asistente);
        alerta.setFechaAtencion(LocalDateTime.now());

        alertaRepository.save(alerta);

        // Registra el inicio de atención en el historial.
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

        // Audita el inicio de atención.
        registrarAuditoria(
                asistente,
                alerta.getIdAlerta(),
                "INICIO_ATENCION",
                "El asistente inició la atención de la emergencia.");
    }

    // Permite al asistente responsable resolver una emergencia.
    @Transactional
    public void resolverAlerta(
            Integer idAlerta,
            String observacion,
            Authentication authentication) {

        Integer idAsistente = obtenerIdAutenticado(authentication);

        // Exige una observación para documentar la resolución.
        if (observacion == null || observacion.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar una observación de cierre.");
        }

        // Bloquea la alerta durante el cierre.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        Usuario responsable = alerta.getResponsableAtencion();

        // Solo el asistente asignado puede resolver la emergencia.
        if (responsable == null
                || !responsable.getIdUsuario().equals(idAsistente)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No eres el responsable de esta alerta.");
        }

        // Comprueba que la vinculación siga activa.
        redApoyoAutorizacionService.validarAccesoAsistente(
                alerta.getAdultoMayor().getIdUsuario(), authentication);

        if (!"EN_ATENCION".equals(alerta.getEstadoActual().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La alerta no se encuentra en atención.");
        }

        // Recupera el estado final desde PostgreSQL.
        EstadoAlerta estadoResuelta = estadoAlertaRepository.findByCodigo("RESUELTA")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "No existe el estado RESUELTA."));

        EstadoAlerta estadoAnterior = alerta.getEstadoActual();
        LocalDateTime fechaCierre = LocalDateTime.now();

        // Actualiza la emergencia.
        alerta.setEstadoActual(estadoResuelta);
        alerta.setFechaCierre(fechaCierre);
        alerta.setObservacionCierre(observacion.trim());

        alertaRepository.save(alerta);

        // Registra la resolución en el historial.
        HistorialAlerta historial = new HistorialAlerta();
        historial.setAlerta(alerta);
        historial.setUsuarioActor(responsable);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoResuelta);
        historial.setAccion("RESOLUCION_ALERTA");
        historial.setObservacion(observacion.trim());
        historial.setFechaHora(fechaCierre);

        historialAlertaRepository.save(historial);

        // Audita la resolución de la emergencia.
        registrarAuditoria(
                responsable,
                alerta.getIdAlerta(),
                "RESOLUCION_ALERTA",
                "El asistente resolvió la emergencia.");
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
        auditoria.setUsuario(usuario); // Usuario responsable.
        auditoria.setEntidad("alerta"); // Entidad afectada.
        auditoria.setIdRegistro(idAlerta); // ID de la emergencia.
        auditoria.setAccion(accion); // Acción realizada.
        auditoria.setDetalle(detalle); // Descripción del evento.
        auditoria.setFechaHora(LocalDateTime.now()); // Fecha del registro.

        auditoriaRepository.save(auditoria); // Guarda en PostgreSQL.
    }
}
