
package com.quickhelp.controller;

import com.quickhelp.dto.RedApoyoAdministracionDTO; // Datos de las vinculaciones.
import com.quickhelp.service.RedApoyoAdministracionService; // Servicio administrativo.
import java.util.List; // Lista de resultados.
import org.springframework.web.bind.annotation.GetMapping; // Endpoint GET.
import org.springframework.web.bind.annotation.RequestMapping; // Ruta principal.
import org.springframework.web.bind.annotation.RestController; // Controlador REST.

@RestController
@RequestMapping("/api/administracion/red-apoyo")
public class RedApoyoAdministracionController {

    private final RedApoyoAdministracionService redApoyoService;

    public RedApoyoAdministracionController(
            RedApoyoAdministracionService redApoyoService) {
        this.redApoyoService = redApoyoService;
    }

    // Consulta administrativa de todas las vinculaciones.
    // El acceso se restringirá al rol ADMINISTRADOR en SecurityConfig.
    @GetMapping
    public List<RedApoyoAdministracionDTO> listarVinculaciones() {
        return redApoyoService.listarVinculaciones();
    }
}
