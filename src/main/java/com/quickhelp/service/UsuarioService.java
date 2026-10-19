package com.quickhelp.service;

import com.quickhelp.dto.UsuarioRequestDTO;
import com.quickhelp.entity.Rol;
import com.quickhelp.entity.Usuario;
import com.quickhelp.repository.RolRepository;
import com.quickhelp.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import com.quickhelp.dto.UsuarioUpdateDTO; // DTO con los datos permitidos para modificar un usuario.

@Service // Indica que esta clase contiene la lógica de negocio de los usuarios.
public class UsuarioService {

    private final UsuarioRepository usuarioRepository; // Permite realizar operaciones CRUD sobre usuario.
    private final RolRepository rolRepository; // Permite buscar el rol que se asignará al usuario.
    private final PasswordEncoder passwordEncoder; // Permite convertir la contraseña en un hash BCrypt.

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository; // Spring inyecta UsuarioRepository.
        this.rolRepository = rolRepository; // Spring inyecta RolRepository.
        this.passwordEncoder = passwordEncoder; // Spring inyecta el BCrypt configurado en PasswordConfig.
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll(); // Obtiene todos los usuarios registrados.
    }

    public Optional<Usuario> buscarPorId(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario); // Busca un usuario por su ID.
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo); // Busca un usuario por su correo.
    }

    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreo(correo); // Devuelve true si el correo ya está registrado.
    }

    public Usuario crearUsuario(UsuarioRequestDTO dto) { // Recibe los datos enviados desde la app móvil o panel web.
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado"); // Evita correos duplicados.
        }

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new RuntimeException("El rol no existe")); // Comprueba que id_rol exista.

        Usuario usuario = new Usuario(); // Crea una nueva entidad Usuario.
        usuario.setRol(rol); // Asigna el rol seleccionado.
        usuario.setNombreApellido(dto.getNombreApellido()); // Asigna nombre y apellido.
        usuario.setCorreo(dto.getCorreo()); // Asigna correo.
        usuario.setTelefono(dto.getTelefono()); // Asigna teléfono.
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword())); // Convierte la contraseña a hash BCrypt.
        usuario.setTokenFcm(dto.getTokenFcm()); // Guarda token para futuras notificaciones push.
        usuario.setFraseActivacion(dto.getFraseActivacion()); // Guarda frase de activación por voz.
        usuario.setVozActiva(dto.getVozActiva() != null ? dto.getVozActiva() : false); // False si no se especifica.
        usuario.setEstado(dto.getEstado() != null ? dto.getEstado() : true); // Activo por defecto.

        return usuarioRepository.save(usuario); // INSERT del nuevo usuario en PostgreSQL.
    }

    public Usuario actualizarUsuario(Integer idUsuario, UsuarioUpdateDTO dto) { // Actualiza un usuario existente.
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")); // Comprueba que el usuario exista.

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new RuntimeException("El rol no existe")); // Comprueba que el nuevo rol exista.

        usuarioRepository.findByCorreo(dto.getCorreo()).ifPresent(usuarioCorreo -> { // Busca si el correo ya pertenece
                                                                                     // a alguien.
            if (!usuarioCorreo.getIdUsuario().equals(idUsuario)) {
                throw new RuntimeException("El correo ya está registrado por otro usuario"); // Impide correos
                                                                                             // duplicados.
            }
        });

        usuario.setRol(rol); // Actualiza el rol.
        usuario.setNombreApellido(dto.getNombreApellido()); // Actualiza nombre y apellido.
        usuario.setCorreo(dto.getCorreo()); // Actualiza correo.
        usuario.setTelefono(dto.getTelefono()); // Actualiza teléfono.
        usuario.setTokenFcm(dto.getTokenFcm()); // Actualiza token de notificaciones.
        usuario.setFraseActivacion(dto.getFraseActivacion()); // Actualiza frase de activación.

        if (dto.getVozActiva() != null)
            usuario.setVozActiva(dto.getVozActiva()); // Solo cambia vozActiva si fue enviada.
        if (dto.getEstado() != null)
            usuario.setEstado(dto.getEstado()); // Solo cambia estado si fue enviado.

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword())); // Solo cambia la contraseña si se envió
                                                                                // una nueva.
        }

        return usuarioRepository.save(usuario); // JpaRepository hace el UPDATE en PostgreSQL.
    }

    public void eliminarUsuario(Integer idUsuario) { // Elimina un usuario existente por su ID.
        if (!usuarioRepository.existsById(idUsuario)) { // Comprueba primero que el usuario exista.
            throw new RuntimeException("Usuario no encontrado"); // Evita intentar eliminar un ID inexistente.
        }

        usuarioRepository.deleteById(idUsuario); // DELETE físico del usuario en PostgreSQL.
    }
}