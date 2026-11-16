package com.quickhelp.controller;

import com.quickhelp.dto.MensajeAlertaRequestDTO; // Mensaje recibido.
import com.quickhelp.dto.MensajeAlertaResponseDTO; // Mensaje de salida.
import com.quickhelp.service.MensajeAlertaService; // Lógica y permisos.
import jakarta.validation.Valid; // Valida los datos recibidos.
import org.springframework.http.ResponseEntity; // Respuestas HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.*; // Endpoints REST.
import java.util.List; // Lista de mensajes.

@RestController
@RequestMapping("/api/alertas")
public class MensajeAlertaController {

    private final MensajeAlertaService mensajeService;

    public MensajeAlertaController(MensajeAlertaService mensajeService) {
        this.mensajeService = mensajeService;
    }

    // Permite consultar mensajes al administrador y a participantes autorizados.
    @GetMapping("/{idAlerta}/mensajes")
    public ResponseEntity<List<MensajeAlertaResponseDTO>> listar(
            @PathVariable Integer idAlerta,
            Authentication authentication) {
        return ResponseEntity.ok(
                mensajeService.listar(idAlerta, authentication));
    }

    // Guarda el mensaje o devuelve el existente si es un reintento.
    @PostMapping("/{idAlerta}/mensajes")
    public ResponseEntity<MensajeAlertaResponseDTO> enviar(
            @PathVariable Integer idAlerta,
            @Valid @RequestBody MensajeAlertaRequestDTO solicitud,
            Authentication authentication) {
        return ResponseEntity.ok(
                mensajeService.enviar(idAlerta, solicitud, authentication));
    }
}