
package com.quickhelp.service;

import com.quickhelp.repository.NotificacionRepository; // Repositorio de notificaciones.
import java.time.LocalDateTime; // Fecha y hora de envío o reintento.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import org.springframework.transaction.annotation.Propagation; // Transacciones independientes.
import org.springframework.transaction.annotation.Transactional; // Control transaccional.

@Service // Registra el servicio en Spring.
public class NotificacionFinalizacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionFinalizacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    // Confirma el resultado en una transacción independiente.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finalizar(Integer idNotificacion, String estado,
            LocalDateTime fechaEnvio, String resultado) {

        // Solo permite estados finales válidos.
        if (!"ENVIADA".equals(estado) && !"ERROR".equals(estado)) {
            throw new IllegalArgumentException("Estado de envío inválido.");
        }

        // Actualiza el resultado sin modificar el número de intentos.
        int actualizados = notificacionRepository.finalizarEnvio(
                idNotificacion, estado, fechaEnvio, resultado);

        return actualizados == 1;
    }

    // Reprograma un envío fallido en una transacción independiente.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reprogramar(Integer idNotificacion,
            LocalDateTime fechaReintento, String resultado) {

        // Valida los datos necesarios para el próximo intento.
        if (idNotificacion == null) {
            throw new IllegalArgumentException("El ID de notificación es obligatorio.");
        }

        if (fechaReintento == null) {
            throw new IllegalArgumentException("La fecha de reintento es obligatoria.");
        }

        // Conserva el número de intentos y registra el motivo del fallo.
        int actualizados = notificacionRepository.reprogramarNotificacion(
                idNotificacion, fechaReintento, resultado);

        return actualizados == 1;
    }
}
