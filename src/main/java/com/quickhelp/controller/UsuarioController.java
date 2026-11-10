
package com.quickhelp.controller;

import com.quickhelp.dto.UsuarioRequestDTO; // DTO para crear usuarios.
import com.quickhelp.dto.UsuarioResponseDTO; // DTO para devolver datos seguros.
import com.quickhelp.dto.UsuarioUpdateDTO; // DTO para actualizar usuarios.
import com.quickhelp.entity.Usuario; // Entidad Usuario.
import com.quickhelp.service.PermisoService; // Servicio de autorización.
import com.quickhelp.service.UsuarioService; // Servicio de usuarios.
import jakarta.validation.Valid; // Validaciones de los DTO.
import java.util.List; // Lista de usuarios.
import org.springframework.http.HttpStatus; // Códigos de respuesta HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.web.bind.annotation.*; // Anotaciones REST.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@RestController // Controlador REST para administrar usuarios.
@RequestMapping("/api/usuarios") // Ruta principal del módulo.
public class UsuarioController {

    private final UsuarioService usuarioService; // Lógica de negocio.
    private final PermisoService permisoService; // Validación de permisos.

    public UsuarioController(UsuarioService usuarioService, PermisoService permisoService) {
        this.usuarioService = usuarioService;
        this.permisoService = permisoService;
    }

    @GetMapping // GET /api/usuarios
    public List<UsuarioResponseDTO> listarUsuarios(Authentication authentication) {
        permisoService.exigirPermiso(authentication, "USUARIO_GESTIONAR"); // Verifica autorización.
        return usuarioService.listarUsuarios().stream()
                .map(this::convertirADTO) // Convierte entidades a DTO.
                .toList();
    }

    @GetMapping("/{id}") // GET /api/usuarios/{id}
    public UsuarioResponseDTO buscarPorId(@PathVariable Integer id,
            Authentication authentication) {
        permisoService.exigirPermiso(authentication, "USUARIO_GESTIONAR"); // Verifica autorización.

        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return convertirADTO(usuario);
    }

    @PostMapping // POST /api/usuarios
    @ResponseStatus(HttpStatus.CREATED) // Devuelve HTTP 201.
    public UsuarioResponseDTO crearUsuario(@Valid @RequestBody UsuarioRequestDTO dto,
            Authentication authentication) {
        permisoService.exigirPermiso(authentication, "USUARIO_GESTIONAR"); // Verifica autorización.
        Usuario usuarioCreado = usuarioService.crearUsuario(dto);
        return convertirADTO(usuarioCreado);
    }

    @PutMapping("/{id}") // PUT /api/usuarios/{id}
    public UsuarioResponseDTO actualizarUsuario(@PathVariable Integer id,
            @Valid @RequestBody UsuarioUpdateDTO dto,
            Authentication authentication) {
        permisoService.exigirPermiso(authentication, "USUARIO_GESTIONAR"); // Verifica autorización.
        Usuario usuarioActualizado = usuarioService.actualizarUsuario(id, dto);
        return convertirADTO(usuarioActualizado);
    }

    @DeleteMapping("/{id}") // DELETE /api/usuarios/{id}.
    @ResponseStatus(HttpStatus.NO_CONTENT) // Devuelve HTTP 204.
    public void eliminarUsuario(@PathVariable Integer id,
            Authentication authentication) {
        permisoService.exigirPermiso(authentication, "USUARIO_GESTIONAR"); // Valida autorización.

        Integer idAutenticado;
        try {
            idAutenticado = Integer.valueOf(authentication.getName()); // Obtiene ID del JWT.
        } catch (NumberFormatException | NullPointerException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Token inválido.");
        }

        if (id.equals(idAutenticado)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puedes desactivar tu propia cuenta."); // Evita perder el acceso.
        }

        usuarioService.eliminarUsuario(id); // Desactiva sin borrar el historial.
    }

    private UsuarioResponseDTO convertirADTO(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();

        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setIdRol(usuario.getRol().getIdRol());
        dto.setNombreApellido(usuario.getNombreApellido());
        dto.setCorreo(usuario.getCorreo());
        dto.setTelefono(usuario.getTelefono());
        dto.setTokenFcm(null); // No expone el identificador del dispositivo.
        dto.setFraseActivacion(null); // No expone información de activación por voz.
        dto.setVozActiva(usuario.getVozActiva());
        dto.setEstado(usuario.getEstado());

        return dto; // Nunca devuelve passwordHash.
    }
}
