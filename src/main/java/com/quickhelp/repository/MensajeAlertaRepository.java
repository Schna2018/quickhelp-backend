package com.quickhelp.repository;

import com.quickhelp.entity.MensajeAlerta; // Entidad del mensaje.
import org.springframework.data.jpa.repository.EntityGraph; // Carga el autor al consultar.
import org.springframework.data.jpa.repository.JpaRepository; // Operaciones de persistencia.
import org.springframework.data.jpa.repository.Query; // Selección de destinatarios.
import org.springframework.data.repository.query.Param; // Parámetro de consulta.
import org.springframework.transaction.annotation.Transactional; // Transacción de lectura.
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

    // Consulta dispositivos de los participantes activos, salvo el autor y los tokens compartidos.
    // Los asistentes solo reciben avisos del chat si ya respondieron la emergencia.
    @Transactional(readOnly = true)
    @Query("""
            SELECT DISTINCT TRIM(destinatario.tokenFcm)
            FROM MensajeAlerta mensaje
            JOIN mensaje.alerta alerta
            JOIN alerta.adultoMayor adulto
            JOIN adulto.rol rolAdulto,
            Usuario destinatario
            JOIN destinatario.rol rol
            WHERE mensaje.idMensaje = :idMensaje
              AND adulto.estado = true
              AND rolAdulto.estado = true
              AND rolAdulto.codigo = 'ADULTO_MAYOR'
              AND destinatario.idUsuario <> mensaje.autor.idUsuario
              AND destinatario.estado = true
              AND rol.estado = true
              AND destinatario.tokenFcm IS NOT NULL
              AND TRIM(destinatario.tokenFcm) <> ''
              AND NOT EXISTS (
                  SELECT 1 FROM Usuario otraCuenta
                  WHERE TRIM(otraCuenta.tokenFcm) = TRIM(destinatario.tokenFcm)
                    AND otraCuenta.idUsuario <> destinatario.idUsuario
              )
              AND (
                  (destinatario.idUsuario = adulto.idUsuario AND rol.codigo = 'ADULTO_MAYOR')
                  OR (rol.codigo = 'ASISTENTE' AND EXISTS (
                      SELECT 1 FROM RedApoyo vinculo
                      WHERE vinculo.adultoMayor.idUsuario = adulto.idUsuario
                        AND vinculo.integrante.idUsuario = destinatario.idUsuario
                        AND vinculo.estado = true
                  )
                  AND EXISTS (
                      SELECT 1 FROM RespuestaAlerta respuesta
                      WHERE respuesta.alerta.idAlerta = alerta.idAlerta
                        AND respuesta.integrante.idUsuario = destinatario.idUsuario
                  ))
              )
            """)
    List<String> buscarTokensParaAviso(@Param("idMensaje") Integer idMensaje);

    // Datos que muestra la notificación de un mensaje nuevo.
    interface DatosAvisoMensaje {
        String getNombreAutor();
        String getTexto();
    }

    // Consulta solo el autor y el texto del mensaje.
    @Query("""
            SELECT mensaje.autor.nombreApellido AS nombreAutor,
                   mensaje.texto AS texto
            FROM MensajeAlerta mensaje
            WHERE mensaje.idMensaje = :idMensaje
            """)
    Optional<DatosAvisoMensaje> buscarDatosAviso(@Param("idMensaje") Integer idMensaje);
}
