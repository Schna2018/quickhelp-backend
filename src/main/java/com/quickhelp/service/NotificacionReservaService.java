
package com.quickhelp.service;

import com.quickhelp.repository.NotificacionRepository; // Repositorio existente.
import org.springframework.stereotype.Service; // Servicio de Spring Boot.
import org.springframework.transaction.annotation.Transactional; // Control de transacciones.
import org.springframework.transaction.annotation.Propagation; // Define una transacción nueva.

@Service // Registra el servicio en Spring Boot.
public class NotificacionReservaService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionReservaService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) // Reserva en una transacción independiente.
    public boolean reservar(Integer idNotificacion) {

        // Actualiza el estado únicamente si continúa PENDIENTE.
        int registrosActualizados =
                notificacionRepository.reservarNotificacion(idNotificacion);

        // True: reserva exitosa. False: ya fue reservada o procesada.
        return registrosActualizados == 1;
    }
}
