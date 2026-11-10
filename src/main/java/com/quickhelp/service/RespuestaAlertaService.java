
package com.quickhelp.service;

import com.quickhelp.dto.RespuestaAlertaRequestDTO; // Datos enviados desde la app móvil.
import com.quickhelp.entity.Alerta; // Emergencia que recibe la respuesta.
import com.quickhelp.entity.RespuestaAlerta; // Registro de respuesta.
import com.quickhelp.entity.Usuario; // Asistente autenticado.
import com.quickhelp.repository.AlertaRepository; // Consulta y bloqueo de alertas.
import com.quickhelp.repository.RespuestaAlertaRepository; // Persistencia de respuestas.
import com.quickhelp.repository.UsuarioRepository; // Consulta de usuarios.
import org.springframework.http.HttpStatus; // Estados de respuesta HTTP.
import org.springframework.security.core.Authentication; // Identidad JWT.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.
import java.time.LocalDateTime; // Fecha y hora de respuesta.
import java.util.Set; // Conjunto de respuestas permitidas.
import com.quickhelp.dto.RespuestaAlertaResponseDTO; // Datos de salida.
import java.util.List; // Lista de respuestas.


@Service
public class RespuestaAlertaService {

    // Tipos de respuesta aceptados por el backend.
    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "VOY_EN_CAMINO",
            "NO_PUEDO_ASISTIR",
            "VOY_A_DEMORAR",
            "PERSONALIZADA"
    );

    private final AlertaRepository alertaRepository;
    private final RespuestaAlertaRepository respuestaAlertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RedApoyoAutorizacionService autorizacionService;

    // Inyección de dependencias mediante constructor.
    public RespuestaAlertaService(
            AlertaRepository alertaRepository,
            RespuestaAlertaRepository respuestaAlertaRepository,
            UsuarioRepository usuarioRepository,
            RedApoyoAutorizacionService autorizacionService) {
        this.alertaRepository = alertaRepository;
        this.respuestaAlertaRepository = respuestaAlertaRepository;
        this.usuarioRepository = usuarioRepository;
        this.autorizacionService = autorizacionService;
    }

    @Transactional // Registra la respuesta de forma transaccional.
    public Integer registrarRespuesta(
            Integer idAlerta,
            RespuestaAlertaRequestDTO solicitud,
            Authentication authentication) {

        // Verifica que exista una sesión autenticada.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        // Comprueba que el cuerpo de la solicitud sea válido.
        if (solicitud == null || solicitud.tipoRespuesta() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe indicar un tipo de respuesta.");
        }

        String tipo = solicitud.tipoRespuesta().trim();

        // Solo acepta las cuatro respuestas definidas.
        if (!TIPOS_PERMITIDOS.contains(tipo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de respuesta no permitido.");
        }

        String observacion = solicitud.observacion() == null
                ? null : solicitud.observacion().trim();

        // La respuesta personalizada necesita un mensaje.
        if ("PERSONALIZADA".equals(tipo)
                && (observacion == null || observacion.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe escribir un mensaje personalizado.");
        }

        // Obtiene el identificador del asistente desde el JWT.
        Integer idAsistente;
        try {
            idAsistente = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de usuario no válido.");
        }

        // Bloquea la alerta para coordinar respuestas concurrentes.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        // Verifica que el asistente esté activo y vinculado al adulto mayor.
        autorizacionService.validarAccesoAsistente(
                alerta.getAdultoMayor().getIdUsuario(), authentication);

        // No permite responder emergencias resueltas o canceladas.
        String estado = alerta.getEstadoActual().getCodigo();
        if (!"ACTIVA".equals(estado) && !"EN_ATENCION".equals(estado)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La alerta ya no admite respuestas.");
        }

        // Evita más de una respuesta por asistente y alerta.
        if (respuestaAlertaRepository
                .existsByAlerta_IdAlertaAndIntegrante_IdUsuario(
                        idAlerta, idAsistente)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Ya registró una respuesta para esta alerta.");
        }

        // Recupera al asistente autenticado.
        Usuario asistente = usuarioRepository.findById(idAsistente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Asistente no encontrado."));

        // Construye el registro de respuesta.
        RespuestaAlerta respuesta = new RespuestaAlerta();
        respuesta.setAlerta(alerta);
        respuesta.setIntegrante(asistente);
        respuesta.setTipoRespuesta(tipo);
        respuesta.setObservacion(observacion);
        respuesta.setFechaHora(LocalDateTime.now());

        // Guarda y devuelve el identificador generado.
        return respuestaAlertaRepository.save(respuesta).getIdRespuesta();
    }

    
    @Transactional(readOnly = true) // Consulta sin modificar registros.
    public List<RespuestaAlertaResponseDTO> listarRespuestas(
            Integer idAlerta, Authentication authentication) {

        // Comprueba que el usuario esté autenticado.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        // Recupera la emergencia solicitada.
        Alerta alerta = alertaRepository.findById(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        // Obtiene el identificador del usuario autenticado.
        Integer idUsuario;
        try {
            idUsuario = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de usuario no válido.");
        }

        // Identifica al propietario de la emergencia.
        Integer idAdultoMayor = alerta.getAdultoMayor().getIdUsuario();

        if (idUsuario.equals(idAdultoMayor)) {
            // Verifica que el propietario siga activo.
            Usuario adultoMayor = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Usuario no encontrado."));

            if (!Boolean.TRUE.equals(adultoMayor.getEstado())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Usuario inactivo.");
            }
        } else {
            // Para otros usuarios exige vinculación activa como asistente.
            autorizacionService.validarAccesoAsistente(
                    idAdultoMayor, authentication);
        }

        // Convierte los registros de PostgreSQL en DTO de respuesta.
        return respuestaAlertaRepository
                .findByAlerta_IdAlertaOrderByFechaHoraDesc(idAlerta)
                .stream()
                .map(respuesta -> new RespuestaAlertaResponseDTO(
                        respuesta.getIdRespuesta(),
                        respuesta.getAlerta().getIdAlerta(),
                        respuesta.getIntegrante().getIdUsuario(),
                        respuesta.getTipoRespuesta(),
                        respuesta.getObservacion(),
                        respuesta.getFechaHora()
                ))
                .toList();
    }
    
    @Transactional // Actualiza la respuesta dentro de una transacción.
    public void actualizarRespuesta(
            Integer idAlerta,
            RespuestaAlertaRequestDTO solicitud,
            Authentication authentication) {

        // Verifica la autenticación.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
        }

        // Valida el tipo de respuesta recibido.
        if (solicitud == null || solicitud.tipoRespuesta() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe indicar una respuesta.");
        }

        String tipo = solicitud.tipoRespuesta().trim();

        if (!TIPOS_PERMITIDOS.contains(tipo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de respuesta no permitido.");
        }

        // Normaliza el mensaje adicional.
        String observacion = solicitud.observacion() == null
                ? null : solicitud.observacion().trim();

        // Exige un mensaje cuando la respuesta es personalizada.
        if ("PERSONALIZADA".equals(tipo)
                && (observacion == null || observacion.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debe escribir un mensaje.");
        }

        // Obtiene la identidad del asistente desde JWT.
        Integer idAsistente;
        try {
            idAsistente = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token de usuario no válido.");
        }

        // Bloquea la emergencia durante la actualización.
        Alerta alerta = alertaRepository.buscarPorIdConBloqueo(idAlerta)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Alerta no encontrada."));

        // Comprueba que exista una vinculación activa.
        autorizacionService.validarAccesoAsistente(
                alerta.getAdultoMayor().getIdUsuario(), authentication);

        // Solo permite modificar respuestas de alertas abiertas.
        String estado = alerta.getEstadoActual().getCodigo();

        if (!"ACTIVA".equals(estado) && !"EN_ATENCION".equals(estado)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La alerta ya está cerrada.");
        }

        // Recupera exclusivamente la respuesta del asistente autenticado.
        RespuestaAlerta respuesta = respuestaAlertaRepository
                .findByAlerta_IdAlertaAndIntegrante_IdUsuario(
                        idAlerta, idAsistente)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe una respuesta previa."));

        // Actualiza los datos sin crear otro registro.
        respuesta.setTipoRespuesta(tipo);
        respuesta.setObservacion(observacion);
        respuesta.setFechaHora(LocalDateTime.now());

        respuestaAlertaRepository.save(respuesta); // Persiste la actualización.
    }


}
