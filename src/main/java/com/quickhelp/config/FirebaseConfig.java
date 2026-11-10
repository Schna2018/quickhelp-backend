
package com.quickhelp.config;

import com.google.auth.oauth2.GoogleCredentials; // Credenciales de Firebase.
import com.google.firebase.FirebaseApp; // Aplicación Firebase.
import com.google.firebase.FirebaseOptions; // Opciones de configuración.
import jakarta.annotation.PostConstruct; // Ejecuta la inicialización al arrancar.
import org.springframework.context.annotation.Configuration; // Configuración Spring.
import java.io.FileInputStream; // Lectura del archivo de credenciales.
import java.io.IOException; // Manejo de errores de lectura.

@Configuration // Registra la configuración en Spring Boot.
public class FirebaseConfig {

    @PostConstruct // Inicializa Firebase al arrancar el backend.
    public void inicializarFirebase() throws IOException {

        // Obtiene la ruta de credenciales desde una variable de entorno.
        String rutaCredenciales = System.getenv("FIREBASE_CREDENTIALS");

        // Evita iniciar Firebase sin una ruta configurada.
        if (rutaCredenciales == null || rutaCredenciales.isBlank()) {
            throw new IllegalStateException(
                    "No está configurada la variable FIREBASE_CREDENTIALS.");
        }

        // Evita inicializar Firebase más de una vez.
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        // Lee las credenciales sin incluirlas en el código fuente.
        try (FileInputStream archivo = new FileInputStream(rutaCredenciales)) {

            GoogleCredentials credenciales =
                    GoogleCredentials.fromStream(archivo);

            // Construye la configuración de Firebase Admin.
            FirebaseOptions opciones = FirebaseOptions.builder()
                    .setCredentials(credenciales)
                    .build();

            // Inicializa Firebase para enviar notificaciones FCM.
            FirebaseApp.initializeApp(opciones);
        }
    }
}
