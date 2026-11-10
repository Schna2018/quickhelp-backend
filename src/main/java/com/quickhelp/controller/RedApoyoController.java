
package com.quickhelp.controller;

import com.quickhelp.dto.RedApoyoResponseDTO; // DTO de respuesta.
import com.quickhelp.service.RedApoyoService; // Servicio de red de apoyo.
import java.util.List; // Lista de integrantes.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.GetMapping; // Endpoint GET.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta base.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.
import org.springframework.http.HttpStatus; // Estados de respuesta HTTP.
import org.springframework.web.bind.annotation.PatchMapping; // Permite actualizar parcialmente una vinculación.
import org.springframework.web.bind.annotation.PathVariable; // Obtiene el ID desde la URL.
import org.springframework.web.bind.annotation.ResponseStatus; // Define el código HTTP de respuesta.
import org.springframework.web.bind.annotation.RequestParam; // Recibe la nueva prioridad.


@RestController
@RequestMapping("/api/red-apoyo") // Ruta principal de la red de apoyo.
public class RedApoyoController {

    private final RedApoyoService redApoyoService;

    // Inyecta el servicio mediante el constructor.
    public RedApoyoController(RedApoyoService redApoyoService) {
        this.redApoyoService = redApoyoService;
    }

    @GetMapping // GET /api/red-apoyo.
    public List<RedApoyoResponseDTO> listarRedApoyo(Authentication authentication) {
        return redApoyoService.listarRedApoyo(authentication); // Consulta la red del adulto autenticado.
    }
    
    // Desactiva una vinculación perteneciente al adulto mayor autenticado.
    @PatchMapping("/{idRedApoyo}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Responde HTTP 204.
    public void desactivarIntegrante(@PathVariable Integer idRedApoyo,
            Authentication authentication) {
        redApoyoService.desactivarIntegrante(idRedApoyo, authentication);
    }

    // Reactiva una vinculación existente del adulto mayor autenticado.
    @PatchMapping("/{idRedApoyo}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Responde HTTP 204.
    public void reactivarIntegrante(@PathVariable Integer idRedApoyo,
            Authentication authentication) {
        redApoyoService.reactivarIntegrante(idRedApoyo, authentication);
    }

    
    // Cambia el orden de prioridad de un integrante de la red de apoyo.
    @PatchMapping("/{idRedApoyo}/prioridad")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Devuelve HTTP 204.
    public void cambiarPrioridad(@PathVariable Integer idRedApoyo,
            @RequestParam Integer nuevaPrioridad,
            Authentication authentication) {
        redApoyoService.cambiarPrioridad(idRedApoyo, nuevaPrioridad, authentication);
    }

}
