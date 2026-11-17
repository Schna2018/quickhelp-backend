
package com.quickhelp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // Habilita las tareas automáticas.

@SpringBootApplication
@EnableScheduling // Permite ejecutar el escalamiento automático de alertas.
public class QuickhelpBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickhelpBackendApplication.class, args);
    }

}
