package com.quickhelp.repository;

import com.quickhelp.entity.MensajeAlerta; // Entidad del mensaje.
import org.springframework.data.jpa.repository.EntityGraph; // Carga el autor al consultar.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones de persistencia.
import java.util.List; // Mensajes de la conversación.
import java.util.Optional; // Resultado que puede no existir.
import java.util.UUID; // Clave de envío.

public interface MensajeAlertaRepository
        extends JpaRepository<MensajeAlerta, Integer> {

    // Devuelve los mensajes desde el más antiguo al más reciente.
    @EntityGraph(attributePaths = "autor")
    List<MensajeAlerta> findByAlerta_IdAlertaOrderByFechaHoraAscIdMensajeAsc(
            Integer idAlerta);

    // Detecta mensajes ya guardados para evitar duplicarlos al reintentar.
    Optional<MensajeAlerta> findByAlerta_IdAlertaAndAutor_IdUsuarioAndClave(
            Integer idAlerta,
            Integer idAutor,
            UUID clave);
}