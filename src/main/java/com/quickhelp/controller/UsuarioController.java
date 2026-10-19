package com.quickhelp.controller;

import com.quickhelp.dto.UsuarioRequestDTO;
import com.quickhelp.dto.UsuarioResponseDTO;
import com.quickhelp.entity.Usuario;
import com.quickhelp.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.quickhelp.dto.UsuarioUpdateDTO; // DTO utilizado para recibir los datos que se modificarán.

@RestController // Indica que esta clase recibe peticiones HTTP y devuelve respuestas JSON.
@RequestMapping("/api/usuarios") // Define la ruta base de esta API: /api/usuarios.
public class UsuarioController {

    private final UsuarioService usuarioService; // Servicio que contiene la lógica de negocio de usuarios.

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService; // Spring inyecta automáticamente UsuarioService.
    }

    @GetMapping // Atiende GET /api/usuarios y obtiene todos los usuarios.
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioService.listarUsuarios()
                .stream() // Recorre la lista de entidades Usuario.
                .map(this::convertirADTO) // Convierte cada Usuario a UsuarioResponseDTO.
                .toList(); // Devuelve la lista final sin exponer passwordHash.
    }

    @GetMapping("/{id}") // Atiende GET /api/usuarios/{id} y busca un usuario específico.
    public UsuarioResponseDTO buscarPorId(@PathVariable Integer id) { // @PathVariable obtiene el ID desde la URL.
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")); // Error si el ID no existe.

        return convertirADTO(usuario); // Devuelve los datos seguros del usuario.
    }

    @PostMapping // Atiende POST /api/usuarios y permite crear un nuevo usuario.
    @ResponseStatus(HttpStatus.CREATED) // Devuelve código HTTP 201 cuando el usuario se crea correctamente.
    public UsuarioResponseDTO crearUsuario(@Valid @RequestBody UsuarioRequestDTO dto) {
        // @RequestBody convierte el JSON recibido en UsuarioRequestDTO.
        // @Valid ejecuta las validaciones @NotNull, @NotBlank, @Email, @Size, etc.

        Usuario usuarioCreado = usuarioService.crearUsuario(dto); // Envía los datos al Service para guardarlos.
        return convertirADTO(usuarioCreado); // Devuelve el usuario creado sin mostrar passwordHash.
    }

    @PutMapping("/{id}") // Atiende PUT /api/usuarios/{id} para modificar un usuario existente.
    public UsuarioResponseDTO actualizarUsuario(
            @PathVariable Integer id, // Obtiene el id_usuario directamente desde la URL.
            @Valid @RequestBody UsuarioUpdateDTO dto) { // Recibe y valida los nuevos datos enviados en JSON.

        Usuario usuarioActualizado = usuarioService.actualizarUsuario(id, dto); // Envía los cambios al Service.
        return convertirADTO(usuarioActualizado); // Devuelve el usuario actualizado sin exponer passwordHash.
    }

    @DeleteMapping("/{id}") // Atiende DELETE /api/usuarios/{id} para eliminar un usuario por su ID.
    @ResponseStatus(HttpStatus.NO_CONTENT) // Devuelve HTTP 204 cuando la eliminación fue correcta.
    public void eliminarUsuario(@PathVariable Integer id) { // @PathVariable obtiene el ID desde la URL.
        usuarioService.eliminarUsuario(id); // Envía el ID al Service para eliminar el usuario.
    }

    private UsuarioResponseDTO convertirADTO(Usuario usuario) { // Convierte la entidad Usuario en una respuesta segura.
        UsuarioResponseDTO dto = new UsuarioResponseDTO();

        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setIdRol(usuario.getRol().getIdRol());
        dto.setNombreApellido(usuario.getNombreApellido());
        dto.setCorreo(usuario.getCorreo());
        dto.setTelefono(usuario.getTelefono());
        dto.setTokenFcm(usuario.getTokenFcm());
        dto.setFraseActivacion(usuario.getFraseActivacion());
        dto.setVozActiva(usuario.getVozActiva());
        dto.setEstado(usuario.getEstado());

        return dto;
    }
}