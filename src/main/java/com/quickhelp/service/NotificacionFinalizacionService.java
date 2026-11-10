
package com.quickhelp.service;

import com.quickhelp.repository.NotificacionRepository; // Repositorio existente.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import org.springframework.transaction.annotation.Transactional; // Transacciones.
import java.time.LocalDateTime; // Fecha y hora del envío.
import org.springframework.transaction.annotation.Propagation; // Define una transacción nueva.

@Service // Registra el servicio en Spring.
public class NotificacionFinalizacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionFinalizacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // Confirma el resultado en una transacción independiente.
    public boolean finalizar(Integer idNotificacion, String estado,
            LocalDateTime fechaEnvio, String resultado) {

        // Solo permite estados finales válidos.
        if (!"ENVIADA".equals(estado) && !"ERROR".equals(estado)) {
            throw new IllegalArgumentException("Estado de envío inválido.");
        }

        // Actualiza el resultado sin modificar el número de intentos.
        int actualizados = notificacionRepository.finalizarEnvio(
                idNotificacion, estado, fechaEnvio, resultado);

        return actualizados == 1; // Confirma si se actualizó el registro.
    }
}
