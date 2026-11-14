package com.quickhelp.controller;

import com.quickhelp.dto.PersonaVinculadaDTO; // Persona adulta vinculada al asistente.
import com.quickhelp.dto.RedApoyoResponseDTO; // Integrante de la red del adulto mayor.
import com.quickhelp.service.RedApoyoService; // Servicio de red de apoyo.
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/red-apoyo")
public class RedApoyoController {

    private final RedApoyoService redApoyoService;

    public RedApoyoController(RedApoyoService redApoyoService) {
        this.redApoyoService = redApoyoService;
    }

    // GET /api/red-apoyo: el adulto mayor consulta a los integrantes de su red.
    @GetMapping
    public List<RedApoyoResponseDTO> listarRedApoyo(Authentication authentication) {
        return redApoyoService.listarRedApoyo(authentication);
    }

    // GET /api/red-apoyo/personas: el asistente consulta a las personas que apoya.
    @GetMapping("/personas")
    public List<PersonaVinculadaDTO> listarPersonasVinculadas(Authentication authentication) {
        return redApoyoService.listarPersonasVinculadas(authentication);
    }

    // Desactiva una vinculación del adulto mayor autenticado.
    @PatchMapping("/{idRedApoyo}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarIntegrante(@PathVariable Integer idRedApoyo,
            Authentication authentication) {
        redApoyoService.desactivarIntegrante(idRedApoyo, authentication);
    }

    // Reactiva una vinculación existente.
    @PatchMapping("/{idRedApoyo}/reactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivarIntegrante(@PathVariable Integer idRedApoyo,
            Authentication authentication) {
        redApoyoService.reactivarIntegrante(idRedApoyo, authentication);
    }

    // Cambia el orden de prioridad de un integrante.
    @PatchMapping("/{idRedApoyo}/prioridad")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPrioridad(@PathVariable Integer idRedApoyo,
            @RequestParam Integer nuevaPrioridad,
            Authentication authentication) {
        redApoyoService.cambiarPrioridad(idRedApoyo, nuevaPrioridad, authentication);
    }
}