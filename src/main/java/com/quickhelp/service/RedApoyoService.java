
package com.quickhelp.service;

import com.quickhelp.dto.RedApoyoResponseDTO; // DTO de respuesta.
import com.quickhelp.entity.RedApoyo; // Entidad de las relaciones de apoyo.
import com.quickhelp.entity.Usuario; // Entidad del usuario.
import com.quickhelp.repository.RedApoyoRepository; // Acceso a la red de apoyo.
import com.quickhelp.repository.UsuarioRepository; // Acceso a usuarios.
import java.util.List; // Lista de integrantes.
import org.springframework.http.HttpStatus; // Códigos HTTP.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import org.springframework.web.server.ResponseStatusException; // Errores HTTP.

@Service
public class RedApoyoService {

    private final RedApoyoRepository redApoyoRepository;
    private final UsuarioRepository usuarioRepository;

    // Inyecta los repositorios necesarios.
    public RedApoyoService(RedApoyoRepository redApoyoRepository,
            UsuarioRepository usuarioRepository) {
        this.redApoyoRepository = redApoyoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true) // Consulta sin modificar registros.
    public List<RedApoyoResponseDTO> listarRedApoyo(Authentication authentication) {

        // Obtiene el identificador del usuario desde el JWT.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        Integer idAdulto;
        try {
            idAdulto = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }

        // Comprueba que el usuario sea un adulto mayor activo.
        Usuario adulto = usuarioRepository.findById(idAdulto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!Boolean.TRUE.equals(adulto.getEstado())
                || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Solo el adulto mayor puede consultar su red de apoyo.");
        }

        // Devuelve exclusivamente las relaciones del adulto autenticado.
        return redApoyoRepository.findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(idAdulto)
                .stream()
                .map(relacion -> new RedApoyoResponseDTO(
                        relacion.getIdRedApoyo(), // ID de la relación.
                        relacion.getIntegrante().getIdUsuario(), // ID del asistente.
                        relacion.getIntegrante().getNombreApellido(), // Nombre del asistente.
                        relacion.getPrioridad(), // Orden de contacto.
                        relacion.getEstado(), // Estado de la relación.
                        relacion.getFechaAsociacion() // Fecha de vinculación.
                ))
                .toList();
    }
    
    @Transactional // Actualiza la relación sin eliminar su historial.
    public void desactivarIntegrante(Integer idRedApoyo, Authentication authentication) {
        // Verifica que exista una sesión autenticada.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        // Obtiene el identificador del usuario desde el JWT.
        Integer idAdulto;
        try {
            idAdulto = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }

        // Comprueba que el usuario sea un adulto mayor activo.
        Usuario adulto = usuarioRepository.findById(idAdulto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!Boolean.TRUE.equals(adulto.getEstado())
                || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operación no autorizada.");
        }

        // Busca la vinculación que se desea desactivar.
        RedApoyo relacion = redApoyoRepository.findById(idRedApoyo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vinculación no encontrada."));

        // Impide modificar una relación perteneciente a otro adulto mayor.
        if (!relacion.getAdultoMayor().getIdUsuario().equals(idAdulto)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta vinculación no te pertenece.");
        }

        // Evita repetir la desactivación de una relación inactiva.
        if (!Boolean.TRUE.equals(relacion.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La vinculación ya está inactiva.");
        }

        relacion.setEstado(false); // Desactiva el acceso asociado a la relación.
        redApoyoRepository.save(relacion); // Conserva el registro en PostgreSQL.
    }

    
    @Transactional // Reactiva una vinculación existente.
    public void reactivarIntegrante(Integer idRedApoyo, Authentication authentication) {
        // Verifica que el usuario haya iniciado sesión.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        // Obtiene el identificador del adulto mayor desde el JWT.
        Integer idAdulto;
        try {
            idAdulto = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }

        // Comprueba que el usuario sea un adulto mayor activo.
        Usuario adulto = usuarioRepository.findById(idAdulto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!Boolean.TRUE.equals(adulto.getEstado())
                || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operación no autorizada.");
        }

        // Recupera la vinculación sin crear registros duplicados.
        RedApoyo relacion = redApoyoRepository.findById(idRedApoyo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vinculación no encontrada."));

        // Impide modificar vinculaciones de otros adultos mayores.
        if (!relacion.getAdultoMayor().getIdUsuario().equals(idAdulto)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta vinculación no te pertenece.");
        }

        // Evita reactivar una relación que ya está habilitada.
        if (Boolean.TRUE.equals(relacion.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La vinculación ya está activa.");
        }

        // Comprueba que el asistente todavía esté habilitado.
        Usuario asistente = relacion.getIntegrante();
        if (!Boolean.TRUE.equals(asistente.getEstado())
                || asistente.getRol() == null
                || !Boolean.TRUE.equals(asistente.getRol().getEstado())
                || !"ASISTENTE".equals(asistente.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El asistente no está disponible.");
        }

        relacion.setEstado(true); // Recupera la vinculación y su prioridad original.
        redApoyoRepository.save(relacion); // Guarda el cambio en PostgreSQL.
    }

    
    @Transactional // Cambia la prioridad manteniendo un orden único.
    public void cambiarPrioridad(Integer idRedApoyo, Integer nuevaPrioridad,
            Authentication authentication) {

        // Comprueba que exista una sesión autenticada.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión.");
        }

        // Obtiene el identificador del adulto mayor desde el JWT.
        Integer idAdulto;
        try {
            idAdulto = Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }

        // Verifica que el usuario sea un adulto mayor activo.
        Usuario adulto = usuarioRepository.findById(idAdulto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        if (!Boolean.TRUE.equals(adulto.getEstado())
                || adulto.getRol() == null
                || !Boolean.TRUE.equals(adulto.getRol().getEstado())
                || !"ADULTO_MAYOR".equals(adulto.getRol().getCodigo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operación no autorizada.");
        }

        // Busca la relación y comprueba que pertenezca al adulto autenticado.
        RedApoyo relacion = redApoyoRepository.findById(idRedApoyo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vinculación no encontrada."));

        if (!relacion.getAdultoMayor().getIdUsuario().equals(idAdulto)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta vinculación no te pertenece.");
        }

        // Obtiene las relaciones ordenadas para reorganizar las prioridades.
        List<RedApoyo> relaciones = redApoyoRepository
                .findByAdultoMayor_IdUsuarioOrderByPrioridadAsc(idAdulto);

        if (nuevaPrioridad == null || nuevaPrioridad < 1
                || nuevaPrioridad > relaciones.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prioridad fuera de rango.");
        }

        // Retira temporalmente la relación de su posición anterior.
        relaciones.removeIf(item -> item.getIdRedApoyo().equals(idRedApoyo));

        // Inserta la relación en la posición solicitada.
        relaciones.add(nuevaPrioridad - 1, relacion);

        // Reasigna prioridades consecutivas a toda la red.
        for (int i = 0; i < relaciones.size(); i++) {
            relaciones.get(i).setPrioridad(i + 1);
        }

        redApoyoRepository.saveAll(relaciones); // Guarda el nuevo orden.
    }



}
