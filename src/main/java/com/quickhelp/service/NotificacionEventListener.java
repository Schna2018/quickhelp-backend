
package com.quickhelp.service;

//import org.springframework.context.event.EventListener; // Escucha eventos de Spring.
//import org.springframework.scheduling.annotation.Async; // Permite ejecución asíncrona.
import org.springframework.stereotype.Component; // Registra el componente.
import org.springframework.transaction.event.TransactionPhase; // Fases de la transacción.
import org.springframework.transaction.event.TransactionalEventListener; // Escucha después del commit.
import com.quickhelp.event.AlertaCreadaEvent; // Evento de alerta creada.

@Component // Registra el listener en Spring Boot.
public class NotificacionEventListener {

    private final ProcesamientoNotificacionService procesamientoNotificacionService;

    public NotificacionEventListener(ProcesamientoNotificacionService procesamientoNotificacionService) {
        this.procesamientoNotificacionService = procesamientoNotificacionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Ejecuta tras confirmar la transacción.
    public void procesarNotificaciones(AlertaCreadaEvent evento) {
        procesamientoNotificacionService.procesarPendientes(evento.idAlerta()); // Procesa únicamente esta alerta.
    }
}
