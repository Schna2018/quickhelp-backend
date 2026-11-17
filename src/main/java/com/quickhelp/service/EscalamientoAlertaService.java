
package com.quickhelp.service;

import com.quickhelp.entity.Alerta; // Entidad de emergencias.
import com.quickhelp.repository.AlertaRepository; // Consulta las alertas.
import org.springframework.scheduling.annotation.Scheduled; // Ejecuta tareas programadas.
import org.springframework.stereotype.Service; // Servicio Spring Boot.
import java.time.Duration; // Calcula el tiempo transcurrido.
import java.time.LocalDateTime; // Fecha y hora actual.
import java.util.List; // Maneja las alertas encontradas.

@Service
public class EscalamientoAlertaService {

    private static final long INTERVALO_MINUTOS = 2;

    private final AlertaRepository alertaRepository;

    // Inyecta el repositorio de alertas.
    public EscalamientoAlertaService(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    // Revisa las alertas activas cada 30 segundos.
    @Scheduled(fixedDelay = 30000)
    public void revisarAlertasActivas() {

        List<Alerta> alertas = alertaRepository
                .findByEstadoActual_CodigoOrderByFechaCreacionDesc("ACTIVA");

        LocalDateTime ahora = LocalDateTime.now();

        for (Alerta alerta : alertas) {

            // Evita procesar alertas sin fecha de creación.
            if (alerta.getFechaCreacion() == null) {
                continue;
            }

            // Calcula cuánto tiempo lleva activa la emergencia.
            long minutos = Duration.between(
                    alerta.getFechaCreacion(), ahora).toMinutes();

            // Identifica alertas que alcanzaron el primer intervalo.
            if (minutos >= INTERVALO_MINUTOS) {
                // Pendiente: registrar y enviar el escalamiento.
                // No se generan notificaciones hasta integrar
                // el control de prioridad y duplicados.
            }
        }
    }
}
