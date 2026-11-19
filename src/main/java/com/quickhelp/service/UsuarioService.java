
package com.quickhelp.service;

import com.quickhelp.dto.UsuarioRequestDTO; // Datos para crear usuarios.
import com.quickhelp.dto.UsuarioUpdateDTO; // Datos para actualizar usuarios.
import com.quickhelp.entity.Rol; // Entidad de roles.
import com.quickhelp.entity.Usuario; // Entidad de usuarios.
import com.quickhelp.repository.AlertaRepository; // Consulta emergencias relacionadas.
import com.quickhelp.repository.HistorialAlertaRepository; // Consulta acciones históricas.
import com.quickhelp.repository.RolRepository; // Acceso a los roles.
import com.quickhelp.repository.UsuarioRepository; // Acceso a los usuarios.
import java.util.List; // Listas de usuarios.
import java.util.Optional; // Resultados opcionales.
import org.springframework.dao.DataIntegrityViolationException; // Errores de integridad referencial.
import org.springframework.http.HttpStatus; // Estados HTTP.
import org.springframework.security.crypto.password.PasswordEncoder; // Cifrado de contraseñas.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@Service // Registra la clase como servicio.
public class UsuarioService {

    private final UsuarioRepository usuarioRepository; // Repositorio de usuarios.
    private final RolRepository rolRepository; // Repositorio de roles.
    private final AlertaRepository alertaRepository; // Repositorio de emergencias.
    private final HistorialAlertaRepository historialAlertaRepository; // Repositorio del historial.
    private final PasswordEncoder passwordEncoder; // Codificador BCrypt.

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          AlertaRepository alertaRepository,
                          HistorialAlertaRepository historialAlertaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.alertaRepository = alertaRepository;
        this.historialAlertaRepository = historialAlertaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true) // Consulta sin modificar registros.
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll(); // Recupera los usuarios.
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario); // Busca por identificador.
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo); // Busca por correo.
    }

    @Transactional(readOnly = true)
    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreo(correo); // Comprueba duplicados.
    }

    @Transactional // Crea un usuario dentro de una transacción.
    public Usuario crearUsuario(UsuarioRequestDTO dto) {

        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El correo ya está registrado.");
        }

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El rol no existe."));

        if (!Boolean.TRUE.equals(rol.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede asignar un rol desactivado.");
        }

        Usuario usuario = new Usuario(); // Construye el usuario.
        usuario.setRol(rol);
        usuario.setNombreApellido(dto.getNombreApellido());
        usuario.setCorreo(dto.getCorreo());
        usuario.setTelefono(dto.getTelefono());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword())); // Cifra contraseña.
        usuario.setTokenFcm(dto.getTokenFcm());
        usuario.setFraseActivacion(dto.getFraseActivacion());
        usuario.setVozActiva(dto.getVozActiva() != null ? dto.getVozActiva() : false);
        usuario.setEstado(dto.getEstado() != null ? dto.getEstado() : true);

        return usuarioRepository.save(usuario); // Guarda en PostgreSQL.
    }

    @Transactional // Actualiza un usuario dentro de una transacción.
    public Usuario actualizarUsuario(Integer idUsuario, UsuarioUpdateDTO dto) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El rol no existe."));

        if (!Boolean.TRUE.equals(rol.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede asignar un rol desactivado.");
        }

        // Evita modificar el rol de una cuenta administradora.
        if (usuario.getRol() != null
                && "ADMINISTRADOR".equals(usuario.getRol().getCodigo())
                && !"ADMINISTRADOR".equals(rol.getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No se permite cambiar el rol de una cuenta administradora.");
        }

        // Comprueba que el correo no pertenezca a otro usuario.
        usuarioRepository.findByCorreo(dto.getCorreo()).ifPresent(usuarioCorreo -> {
            if (!usuarioCorreo.getIdUsuario().equals(idUsuario)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El correo ya está registrado por otro usuario.");
            }
        });

        usuario.setRol(rol); // Actualiza el rol.
        usuario.setNombreApellido(dto.getNombreApellido());
        usuario.setCorreo(dto.getCorreo());
        usuario.setTelefono(dto.getTelefono());

        // Evita borrar el token FCM cuando el campo no se envía.
        if (dto.getTokenFcm() != null) {
            usuario.setTokenFcm(dto.getTokenFcm());
        }

        // Conserva la frase de activación cuando no se envía otra.
        if (dto.getFraseActivacion() != null) {
            usuario.setFraseActivacion(dto.getFraseActivacion());
        }

        if (dto.getVozActiva() != null) {
            usuario.setVozActiva(dto.getVozActiva()); // Actualiza activación por voz.
        }

        if (dto.getEstado() != null) {
            usuario.setEstado(dto.getEstado()); // Actualiza estado de la cuenta.
        }

        // Actualiza la contraseña solo cuando se proporciona una nueva.
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return usuarioRepository.save(usuario); // Guarda los cambios.
    }

    @Transactional // Conserva el usuario y sus relaciones históricas.
    public void eliminarUsuario(Integer idUsuario) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            return; // Ya está desactivado.
        }

        usuario.setEstado(false); // Desactivación lógica.
        usuarioRepository.save(usuario); // Conserva el historial.
    }

    /**
     * ELIMINACIÓN DEFINITIVA DE USUARIOS.
     * Comprueba las relaciones históricas antes de ejecutar el DELETE.
     * Protege las emergencias y los registros de atención.
     * PostgreSQL mantiene sus restricciones de integridad referencial.
     */
    @Transactional
    public void eliminarUsuarioDefinitivamente(Integer idUsuario) {

        // Comprueba que el usuario exista en PostgreSQL.
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        // Impide eliminar usuarios relacionados con alguna emergencia.
        boolean tieneEmergencias =
                alertaRepository.existeAlertaRelacionadaConUsuario(idUsuario);

        if (tieneEmergencias) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar definitivamente este usuario porque " +
                    "tiene emergencias relacionadas. Puedes desactivar su cuenta " +
                    "para conservar el historial.");
        }

        // Impide eliminar usuarios que realizaron acciones históricas.
        boolean tieneHistorial =
                historialAlertaRepository.existeHistorialRelacionadoConUsuario(idUsuario);

        if (tieneHistorial) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar definitivamente este usuario porque " +
                    "tiene acciones registradas en el historial de emergencias. " +
                    "Puedes desactivar su cuenta.");
        }

        // Elimina físicamente únicamente cuando no existen relaciones detectadas.
        try {
            usuarioRepository.delete(usuario);

            // Ejecuta el DELETE y comprueba las restricciones de PostgreSQL.
            usuarioRepository.flush();

        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar definitivamente este usuario porque " +
                    "tiene otros registros relacionados.",
                    e);
        }
    }
}
