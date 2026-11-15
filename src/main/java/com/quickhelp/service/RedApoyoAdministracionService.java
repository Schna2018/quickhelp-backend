
package com.quickhelp.service;

import com.quickhelp.dto.RedApoyoAdministracionDTO; // Respuesta administrativa.
import com.quickhelp.entity.RedApoyo; // Entidad de vinculación.
import com.quickhelp.repository.RedApoyoRepository; // Acceso a PostgreSQL.
import java.util.Comparator; // Ordenamiento de vinculaciones.
import java.util.List; // Lista de resultados.
import org.springframework.stereotype.Service; // Servicio Spring.
import org.springframework.transaction.annotation.Transactional; // Transacción de lectura.

@Service
public class RedApoyoAdministracionService {

    private final RedApoyoRepository redApoyoRepository;

    public RedApoyoAdministracionService(RedApoyoRepository redApoyoRepository) {
        this.redApoyoRepository = redApoyoRepository;
    }

    // Consulta todas las vinculaciones sin modificar registros.
    @Transactional(readOnly = true)
    public List<RedApoyoAdministracionDTO> listarVinculaciones() {
        return redApoyoRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((RedApoyo red) -> red.getAdultoMayor().getIdUsuario())
                        .thenComparing(RedApoyo::getPrioridad)
                        .thenComparing(RedApoyo::getIdRedApoyo))
                .map(red -> new RedApoyoAdministracionDTO(
                        red.getIdRedApoyo(),
                        red.getAdultoMayor().getIdUsuario(),
                        red.getAdultoMayor().getNombreApellido(),
                        red.getIntegrante().getIdUsuario(),
                        red.getIntegrante().getNombreApellido(),
                        red.getPrioridad(),
                        red.getEstado(),
                        red.getFechaAsociacion()
                ))
                .toList();
    }
}
