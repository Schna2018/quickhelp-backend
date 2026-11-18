package com.quickhelp.service;

import com.quickhelp.dto.MensajeAlertaRequestDTO; // Datos recibidos.
import com.quickhelp.dto.MensajeAlertaResponseDTO; // Datos del chat.
import com.quickhelp.entity.Alerta; // Emergencia asociada.
import com.quickhelp.entity.MensajeAlerta; // Mensaje persistido.
import com.quickhelp.entity.Usuario; // Cuenta autenticada.
import com.quickhelp.event.MensajeAlertaCreadoEvent; // Evento que activa el aviso del chat.
import com.quickhelp.repository.AlertaRepository; // Consulta y bloqueo de alertas.
import com.quickhelp.repository.MensajeAlertaRepository; // Persistencia de mensajes.
import com.quickhelp.repository.UsuarioRepository; // Consulta de cuentas.
import org.springframework.context.ApplicationEventPublisher; // Publicación de eventos.
import org.springframework.http.HttpStatus; // Códigos HTTP.
import org.springframework.security.core.Authentication; // Identidad del JWT.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.
import java.util.List; // Lista de mensajes.

@Service
public class MensajeAlertaService {

    private final MensajeAlertaRepository mensajeRepository;
    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RedApoyoAutorizacionService autorizacionService;
    private final ApplicationEventPublisher eventos;

    public MensajeAlertaService(
            MensajeAlertaRepository mensajeRepository,
            AlertaRepository alertaRepository,
            UsuarioRepository usuarioRepository,
            RedApoyoAutorizacionService autorizacionService,
            ApplicationEventPublisher eventos) {
        this.mensajeRepository = mensajeRepository;
        this.alertaRepository = alertaRepository;
        this.usuarioRepository = usuarioRepository;
        this.autorizacionService = autorizacionService;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<MensajeAlertaResponseDTO> listar(
            Integer idAlerta,
            Authentication authentication) {

        Usuario usuario = obtenerUsuarioActivo(authentication);
        Alerta alerta = obtenerAlerta(idAlerta, false);

        // Autoriza al administrador, al propietario o al asistente vinculado.
        validarLectura(alerta, usuario, authentication);

        // Permite consultar el historial de emergencias cerradas.
        return mensajeRepository
                .findByAlerta_IdAlertaOrderByFechaHoraAscIdMensajeAsc(idAlerta)
                .stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional
    public MensajeAlertaResponseDTO enviar(
            Integer idAlerta,
            MensajeAlertaRequestDTO solicitud,
            Authentication authentication) {

        Usuario usuario = obtenerUsuarioActivo(authentication);

        // Valida también aquí para proteger llamadas internas al servicio.
        if (solicitud == null || solicitud.clave() == null
                || solicitud.texto() == null
                || solicitud.texto().isBlank()
                || solicitud.texto().length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mensaje no válido.");
        }

        // Coordina los envíos simultáneos mediante el bloqueo de la alerta.
        Alerta alerta = obtenerAlerta(idAlerta, true);
        validarLectura(alerta, usuario, authentication);

        // El administrador puede consultar los mensajes.
        if (esAdministrador(usuario)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El administrador tiene acceso de lectura al chat.");
        }

        String texto = solicitud.texto().strip();

        // Devuelve el mensaje existente cuando se reintenta el mismo envío.
        var existente = mensajeRepository
                .findByAlerta_IdAlertaAndAutor_IdUsuarioAndClave(
                        idAlerta, usuario.getIdUsuario(), solicitud.clave());

        if (existente.isPresent()) {
            MensajeAlerta mensaje = existente.get();

            if (!mensaje.getTexto().equals(texto)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Esta clave ya pertenece a otro mensaje.");
            }

            return convertir(mensaje);
        }

        String estado = alerta.getEstadoActual().getCodigo();

        // Solo las emergencias abiertas admiten mensajes nuevos.
        if (!"ACTIVA".equals(estado)
                && !"EN_ATENCION".equals(estado)
                && !"PENDIENTE".equals(estado)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La emergencia está cerrada. El chat es de solo lectura.");
        }

        MensajeAlerta mensaje = new MensajeAlerta();
        mensaje.setAlerta(alerta);
        mensaje.setAutor(usuario); // Obtiene el autor desde la sesión.
        mensaje.setTexto(texto);
        mensaje.setClave(solicitud.clave());

        MensajeAlerta guardado = mensajeRepository.saveAndFlush(mensaje);

        // Solo los mensajes nuevos generan aviso; el listener espera la confirmación en BD.
        eventos.publishEvent(new MensajeAlertaCreadoEvent(idAlerta, guardado.getIdMensaje()));
        return convertir(guardado);
    }

    private Usuario obtenerUsuarioActivo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token no válido.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Cuenta no disponible."));

        // Comprueba que la cuenta y su rol continúen activos.
        if (!Boolean.TRUE.equals(usuario.getEstado())
                || usuario.getRol() == null
                || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cuenta o rol inactivo.");
        }

        return usuario;
    }

    private Alerta obtenerAlerta(Integer idAlerta, boolean bloquear) {
        if (idAlerta == null || idAlerta <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identificador de alerta no válido.");
        }

        var resultado = bloquear
                ? alertaRepository.buscarPorIdConBloqueo(idAlerta)
                : alertaRepository.findById(idAlerta);

        return resultado.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Alerta no encontrada."));
    }

    private void validarLectura(
            Alerta alerta,
            Usuario usuario,
            Authentication authentication) {

        // El administrador puede consultar cualquier conversación.
        if (esAdministrador(usuario)) {
            return;
        }

        Integer idAdultoMayor = alerta.getAdultoMayor().getIdUsuario();
        String rol = usuario.getRol().getCodigo();

        // El adulto mayor solo accede a sus propias emergencias.
        if ("ADULTO_MAYOR".equals(rol)
                && usuario.getIdUsuario().equals(idAdultoMayor)) {
            return;
        }

        // Reutiliza la validación existente de cuentas y vínculos activos.
        if ("ASISTENTE".equals(rol)) {
            autorizacionService.validarAccesoAsistente(
                    idAdultoMayor, authentication);
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes acceso a esta conversación.");
    }

    private boolean esAdministrador(Usuario usuario) {
        return "ADMIN".equals(usuario.getRol().getCodigo());
    }

    private MensajeAlertaResponseDTO convertir(MensajeAlerta mensaje) {
        return new MensajeAlertaResponseDTO(
                mensaje.getIdMensaje(),
                mensaje.getAutor().getIdUsuario(),
                mensaje.getAutor().getNombreApellido(),
                mensaje.getTexto(),
                mensaje.getFechaHora());
    }
}
