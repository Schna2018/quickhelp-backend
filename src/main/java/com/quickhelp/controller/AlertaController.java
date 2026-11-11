
package com.quickhelp.controller;

import com.quickhelp.dto.AlertaResponseDTO; // Respuesta de alertas.
import com.quickhelp.dto.CrearAlertaRequestDTO; // Coordenadas GPS opcionales.
import com.quickhelp.dto.ResolverAlertaRequestDTO; // Observación de cierre.
import com.quickhelp.service.AlertaService; // Servicio de emergencias.
import java.util.List; // Listado de alertas.
import java.util.Map; // Respuestas JSON.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.GetMapping; // Consultas GET.
import org.springframework.web.bind.annotation.PatchMapping; // Actualizaciones PATCH.
import org.springframework.web.bind.annotation.PathVariable; // ID desde la URL.
import org.springframework.web.bind.annotation.PostMapping; // Creación POST.
import org.springframework.web.bind.annotation.RequestBody; // Datos JSON.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta principal.
import org.springframework.web.bind.annotation.ResponseStatus; // Estado HTTP.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.

@RestController // Expone las operaciones de emergencias.
@RequestMapping("/api/alertas") // Ruta principal del módulo.
public class AlertaController {

    private final AlertaService alertaService; // Servicio existente.

    // Inyecta el servicio de alertas.
    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    // Crea una emergencia con coordenadas GPS opcionales.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> crearAlerta(
            @RequestBody(required = false) CrearAlertaRequestDTO request,
            Authentication authentication) {

        Integer idAlerta = alertaService.crearAlerta(authentication, request);
        return Map.of("idAlerta", idAlerta);
    }

    // Consulta las alertas del adulto mayor autenticado.
    @GetMapping
    public List<AlertaResponseDTO> listarAlertas(Authentication authentication) {
        return alertaService.listarAlertas(authentication);
    }

    // El asistente consulta su red sin enviar identificadores desde la app.
    @GetMapping("/mi-red")
    public List<AlertaResponseDTO> listarAlertasDeMiRed(Authentication authentication) {
        return alertaService.listarAlertasDeMiRed(authentication);
    }

    // Consulta las alertas de un adulto mayor vinculado.
    @GetMapping("/adulto-mayor/{idAdultoMayor}")
    public List<AlertaResponseDTO> listarAlertasAdultoMayor(
            @PathVariable Integer idAdultoMayor,
            Authentication authentication) {

        return alertaService.listarAlertasAdultoMayor(
                idAdultoMayor, authentication);
    }

    // Consulta la ubicación GPS de una alerta autorizada.
    @GetMapping("/{idAlerta}/ubicacion")
    public Map<String, Object> consultarUbicacion(
            @PathVariable Integer idAlerta,
            Authentication authentication) {

        return alertaService.consultarUbicacion(idAlerta, authentication);
    }

    // Permite que un asistente vinculado inicie la atención.
    @PatchMapping("/{idAlerta}/iniciar-atencion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciarAtencion(
            @PathVariable Integer idAlerta,
            Authentication authentication) {

        alertaService.iniciarAtencion(idAlerta, authentication);
    }

    // Permite al asistente responsable resolver la emergencia.
    @PatchMapping("/{idAlerta}/resolver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolverAlerta(
            @PathVariable Integer idAlerta,
            @RequestBody ResolverAlertaRequestDTO request,
            Authentication authentication) {

        alertaService.resolverAlerta(
                idAlerta,
                request.observacion(),
                authentication);
    }
}
