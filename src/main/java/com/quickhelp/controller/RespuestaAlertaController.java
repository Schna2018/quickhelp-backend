
package com.quickhelp.controller;

import com.quickhelp.dto.RespuestaAlertaRequestDTO; // Datos recibidos desde la app móvil.
import com.quickhelp.service.RespuestaAlertaService; // Lógica de respuestas.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.http.ResponseEntity; // Respuestas REST.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.*; // Anotaciones REST.
import java.util.Map; // Respuesta con identificador generado.
import com.quickhelp.dto.RespuestaAlertaResponseDTO; // DTO de consulta.
import java.util.List; // Lista de respuestas.


@RestController // Expone los endpoints REST.
@RequestMapping("/api/alertas") // Ruta principal de emergencias.
public class RespuestaAlertaController {

    private final RespuestaAlertaService respuestaAlertaService;

    // Inyección del servicio mediante constructor.
    public RespuestaAlertaController(
            RespuestaAlertaService respuestaAlertaService) {
        this.respuestaAlertaService = respuestaAlertaService;
    }

    // Registra la respuesta de un asistente ante una emergencia.
    @PostMapping("/{idAlerta}/respuestas")
    public ResponseEntity<Map<String, Integer>> registrarRespuesta(
            @PathVariable Integer idAlerta,
            @RequestBody RespuestaAlertaRequestDTO solicitud,
            Authentication authentication) {

        // Valida permisos, estado de la alerta y tipo de respuesta.
        Integer idRespuesta = respuestaAlertaService.registrarRespuesta(
                idAlerta, solicitud, authentication);

        // Devuelve 201 Created y el identificador registrado.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("idRespuesta", idRespuesta));
    }

    // Consulta las respuestas de una emergencia.
    @GetMapping("/{idAlerta}/respuestas")
    public ResponseEntity<List<RespuestaAlertaResponseDTO>> listarRespuestas(
            @PathVariable Integer idAlerta,
            Authentication authentication) {

        // El servicio comprueba que el usuario tenga acceso a la alerta.
        return ResponseEntity.ok(
                respuestaAlertaService.listarRespuestas(
                        idAlerta, authentication));
    }
    
    // Actualiza la respuesta del asistente autenticado.
    @PutMapping("/{idAlerta}/respuestas")
    public ResponseEntity<Void> actualizarRespuesta(
            @PathVariable Integer idAlerta,
            @RequestBody RespuestaAlertaRequestDTO solicitud,
            Authentication authentication) {

        // Actualiza el registro existente y valida los permisos.
        respuestaAlertaService.actualizarRespuesta(
                idAlerta, solicitud, authentication);

        return ResponseEntity.noContent().build(); // Devuelve HTTP 204.
    }


}
