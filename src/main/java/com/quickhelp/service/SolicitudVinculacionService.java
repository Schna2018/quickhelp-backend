
package com.quickhelp.service;


import com.quickhelp.entity.RedApoyo; // Relación aprobada.
import com.quickhelp.entity.SolicitudVinculacion; // Solicitud de vinculación.
import com.quickhelp.entity.Usuario; // Usuarios del sistema.
import com.quickhelp.repository.RedApoyoRepository; // Acceso a redapoyo.
import com.quickhelp.repository.SolicitudVinculacionRepository; // Acceso a solicitudes.
import com.quickhelp.repository.UsuarioRepository; // Acceso a usuarios.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.stereotype.Service; // Servicio de Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.
import java.time.LocalDateTime; // Fecha y hora.
import org.springframework.security.core.Authentication; // Identifica al adulto autenticado.

@Service
public class SolicitudVinculacionService {

    private final UsuarioRepository usuarioRepository;
    private final SolicitudVinculacionRepository solicitudRepository;
    private final RedApoyoRepository redApoyoRepository;

    public SolicitudVinculacionService(
            UsuarioRepository usuarioRepository,
            SolicitudVinculacionRepository solicitudRepository,
            RedApoyoRepository redApoyoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.redApoyoRepository = redApoyoRepository;
    }

    @Transactional
    public void crearSolicitud(Usuario asistente, String codigoVinculacion) {
        // Comprueba que el solicitante sea un asistente activo.
        if (asistente == null || !Boolean.TRUE.equals(asistente.getEstado())
                || asistente.getRol() == null
                || !Boolean.TRUE.equals(asistente.getRol().getEstado())
                || !"ASISTENTE".equals(asistente.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El solicitante debe ser un asistente activo.");
        }

        // Busca al adulto mayor asociado al código.
        Usuario adulto = usuarioRepository.findByCodigoVinculacion(codigoVinculacion)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Código de vinculación inválido."));

        // Verifica que el destinatario sea un adulto mayor activo.
        if (!Boolean.TRUE.equals(adulto.getEstado()) || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El adulto mayor no está disponible.");
        }

        Integer idAdulto = adulto.getIdUsuario();
        Integer idAsistente = asistente.getIdUsuario();

        // Impide solicitudes pendientes duplicadas.
        if (solicitudRepository.existsByAdultoMayor_IdUsuarioAndAsistente_IdUsuarioAndEstado(
                idAdulto, idAsistente, "PENDIENTE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una solicitud pendiente.");
        }

        // Impide solicitar otra vinculación si la relación ya existe.
        if (redApoyoRepository.existsByAdultoMayor_IdUsuarioAndIntegrante_IdUsuario(
                idAdulto, idAsistente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una relación de apoyo.");
        }

        SolicitudVinculacion solicitud = new SolicitudVinculacion();
        solicitud.setAdultoMayor(adulto);
        solicitud.setAsistente(asistente);
        solicitud.setEstado("PENDIENTE");
        solicitudRepository.save(solicitud); // Guarda la solicitud sin vincular todavía.
    }

    @Transactional
    public void aprobarSolicitud(Integer idSolicitud, Authentication authentication) {
        Integer idAdulto = obtenerIdAutenticado(authentication); // Identifica al usuario del JWT.
        SolicitudVinculacion solicitud = obtenerSolicitudPendiente(idSolicitud, idAdulto);

        Integer idAsistente = solicitud.getAsistente().getIdUsuario();

        // Evita crear dos relaciones para la misma pareja.
        if (redApoyoRepository.existsByAdultoMayor_IdUsuarioAndIntegrante_IdUsuario(
                idAdulto, idAsistente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La relación de apoyo ya existe.");
        }

        // Asigna la siguiente prioridad disponible para este adulto mayor.
        int prioridad = redApoyoRepository
                .findMaxPrioridadByAdultoMayorId(idAdulto) + 1;

        RedApoyo relacion = new RedApoyo();
        relacion.setAdultoMayor(solicitud.getAdultoMayor());
        relacion.setIntegrante(solicitud.getAsistente());
        relacion.setPrioridad(prioridad);
        relacion.setEstado(true);
        relacion.setFechaAsociacion(LocalDateTime.now());
        redApoyoRepository.save(relacion); // Crea la vinculación efectiva.

        solicitud.setEstado("APROBADA");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudRepository.save(solicitud); // Registra la aprobación.
    }

    @Transactional
    public void rechazarSolicitud(Integer idSolicitud, Authentication authentication) {
        Integer idAdulto = obtenerIdAutenticado(authentication);
        SolicitudVinculacion solicitud = obtenerSolicitudPendiente(idSolicitud, idAdulto);

        solicitud.setEstado("RECHAZADA");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        solicitudRepository.save(solicitud); // No crea ninguna relación.
    }

    private SolicitudVinculacion obtenerSolicitudPendiente(Integer idSolicitud, Integer idAdulto) {
        SolicitudVinculacion solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada."));

        // Solo el adulto mayor destinatario puede responder.
        if (!solicitud.getAdultoMayor().getIdUsuario().equals(idAdulto)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes responder esta solicitud.");
        }
        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue respondida.");
        }
        if (!Boolean.TRUE.equals(solicitud.getAdultoMayor().getEstado())
                || !"ADULTO_MAYOR".equals(solicitud.getAdultoMayor().getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Adulto mayor no autorizado.");
        }
        return solicitud;
    }

    private Integer obtenerIdAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }
        try {
            return Integer.valueOf(authentication.getName()); // JWT subject = ID del usuario.
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }
    }

}
