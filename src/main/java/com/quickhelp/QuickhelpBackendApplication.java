
package com.quickhelp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // Habilita las tareas automáticas.
import java.util.TimeZone; // Zona horaria de las fechas del sistema.

@SpringBootApplication
@EnableScheduling // Permite ejecutar el escalamiento automático de alertas.
public class QuickhelpBackendApplication {

    public static void main(String[] args) {
        // Todas las fechas del sistema se registran en hora de Chile (incluye horario de verano).
        TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
        SpringApplication.run(QuickhelpBackendApplication.class, args);
    }

}
