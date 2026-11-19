
package com.quickhelp.config;

import com.google.auth.oauth2.GoogleCredentials; // Credenciales de Firebase.
import com.google.firebase.FirebaseApp; // Aplicación Firebase.
import com.google.firebase.FirebaseOptions; // Opciones de configuración.
import jakarta.annotation.PostConstruct; // Inicialización al arrancar.
import org.springframework.context.annotation.Configuration; // Configuración Spring.

import java.io.ByteArrayInputStream; // Lectura de credenciales en memoria.
import java.io.FileInputStream; // Lectura de archivos JSON.
import java.io.IOException; // Manejo de errores de lectura.
import java.io.InputStream; // Flujo de entrada.
import java.nio.charset.StandardCharsets; // Codificación UTF-8.

@Configuration // Registra la configuración en Spring Boot.
public class FirebaseConfig {

    @PostConstruct // Inicializa Firebase al arrancar el backend.
    public void inicializarFirebase() throws IOException {

        // Evita inicializar Firebase más de una vez.
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        // Obtiene las credenciales desde Render o el entorno local.
        String configuracion = System.getenv("FIREBASE_CREDENTIALS");

        // Verifica que la variable esté configurada.
        if (configuracion == null || configuracion.isBlank()) {
            throw new IllegalStateException(
                    "No está configurada la variable FIREBASE_CREDENTIALS.");
        }

        // Determina si contiene JSON o una ruta de archivo.
        boolean esJson = configuracion.stripLeading().startsWith("{");

        // Abre las credenciales según el formato configurado.
        try (InputStream archivo = esJson
                ? new ByteArrayInputStream(
                        configuracion.getBytes(StandardCharsets.UTF_8))
                : new FileInputStream(configuracion)) {

            // Obtiene las credenciales de la cuenta de servicio.
            GoogleCredentials credenciales =
                    GoogleCredentials.fromStream(archivo);

            // Construye las opciones de Firebase Admin.
            FirebaseOptions opciones = FirebaseOptions.builder()
                    .setCredentials(credenciales)
                    .build();

            // Inicializa Firebase para enviar notificaciones PUSH.
            FirebaseApp.initializeApp(opciones);
        }
    }
}
