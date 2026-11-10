
package com.quickhelp.repository;

import com.quickhelp.entity.RespuestaAlerta; // Entidad de respuestas a emergencias.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones CRUD.
import java.util.List; // Permite devolver varias respuestas.
import java.util.Optional; // Permite buscar una respuesta que puede no existir.


public interface RespuestaAlertaRepository
        extends JpaRepository<RespuestaAlerta, Integer> {

    // Obtiene las respuestas de una alerta, desde la más reciente.
    List<RespuestaAlerta> findByAlerta_IdAlertaOrderByFechaHoraDesc(
            Integer idAlerta);

    // Obtiene las respuestas enviadas por un asistente.
    List<RespuestaAlerta> findByIntegrante_IdUsuarioOrderByFechaHoraDesc(
            Integer idIntegrante);

    // Comprueba si un asistente ya respondió una alerta.
    boolean existsByAlerta_IdAlertaAndIntegrante_IdUsuario(
            Integer idAlerta, Integer idIntegrante);

    // Busca la respuesta registrada por un asistente en una alerta.
    Optional<RespuestaAlerta> findByAlerta_IdAlertaAndIntegrante_IdUsuario(
            Integer idAlerta, Integer idIntegrante);
       
}
