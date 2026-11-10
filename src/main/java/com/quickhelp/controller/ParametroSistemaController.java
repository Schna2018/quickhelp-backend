
package com.quickhelp.controller;

import com.quickhelp.dto.ActualizarParametroRequestDTO; // Datos de actualización.
import com.quickhelp.dto.ParametroSistemaResponseDTO; // Respuesta segura.
import com.quickhelp.entity.ParametroSistema; // Entidad de configuración.
import com.quickhelp.service.ParametroSistemaService; // Lógica de parámetros.
import java.util.List; // Listado de configuraciones.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.GetMapping; // Consultas HTTP.
import org.springframework.web.bind.annotation.PatchMapping; // Actualizaciones HTTP.
import org.springframework.web.bind.annotation.PathVariable; // Identificador de parámetro.
import org.springframework.web.bind.annotation.RequestBody; // Datos JSON.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta base.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@RestController // Expone la configuración mediante una API REST.
@RequestMapping("/api/parametros")
public class ParametroSistemaController {

    private final ParametroSistemaService parametroSistemaService;

    // Inyecta el servicio de configuración existente.
    public ParametroSistemaController(ParametroSistemaService parametroSistemaService) {
        this.parametroSistemaService = parametroSistemaService;
    }

    // Lista los parámetros; el servicio exige un administrador activo.
    @GetMapping
    public List<ParametroSistemaResponseDTO> listar(Authentication authentication) {
        return parametroSistemaService.listar(authentication)
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    // Modifica únicamente el valor de un parámetro existente.
    @PatchMapping("/{idParametro}")
    public ParametroSistemaResponseDTO actualizar(
            @PathVariable Integer idParametro,
            @RequestBody ActualizarParametroRequestDTO request,
            Authentication authentication) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Debes enviar el nuevo valor.");
        }

        ParametroSistema actualizado = parametroSistemaService.actualizar(
                idParametro, request.valor(), authentication);

        return convertirDTO(actualizado);
    }

    // Evita exponer directamente entidades JPA en las respuestas.
    private ParametroSistemaResponseDTO convertirDTO(ParametroSistema parametro) {
        Integer idUsuarioActualiza = parametro.getUsuarioActualiza() == null
                ? null
                : parametro.getUsuarioActualiza().getIdUsuario();

        return new ParametroSistemaResponseDTO(
                parametro.getIdParametro(),
                parametro.getClave(),
                parametro.getValor(),
                parametro.getTipoDato(),
                parametro.getDescripcion(),
                idUsuarioActualiza,
                parametro.getFechaActualizacion()
        );
    }
}
