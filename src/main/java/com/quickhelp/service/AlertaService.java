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
        EstadoAlerta estadoResuelta = estadoAlertaRepository
                .findByCodigo("RESUELTA")
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

        auditoriaRepository.save(auditoria);
    }
}
