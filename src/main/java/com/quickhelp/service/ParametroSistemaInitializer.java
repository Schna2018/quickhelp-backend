
package com.quickhelp.service;

import com.quickhelp.entity.ParametroSistema; // Entidad de configuración.
import com.quickhelp.repository.ParametroSistemaRepository; // Acceso a parámetros.
import org.springframework.boot.ApplicationArguments; // Argumentos de inicio.
import org.springframework.boot.ApplicationRunner; // Inicialización de Spring.
import org.springframework.stereotype.Component; // Componente administrado por Spring.
import org.springframework.transaction.annotation.Transactional; // Transacción de BD.

@Component
public class ParametroSistemaInitializer implements ApplicationRunner {

    private final ParametroSistemaRepository parametroRepository;

    // Reutiliza el repositorio existente.
    public ParametroSistemaInitializer(ParametroSistemaRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    // Registra únicamente parámetros inexistentes.
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearSiNoExiste("NOMBRE_SISTEMA", "Quick Help",
                "STRING", "Nombre visible del sistema.");

        crearSiNoExiste("NOMBRE_INSTITUCION", "Por definir",
                "STRING", "Institución responsable.");

        crearSiNoExiste("CORREO_SOPORTE", "Por definir",
                "STRING", "Correo institucional de soporte pendiente.");

        crearSiNoExiste("TELEFONO_EMERGENCIA", "Por definir",
                "STRING", "Teléfono institucional de emergencia pendiente.");

        // Nueva preferencia: notificaciones por correo.
        crearSiNoExiste("NOTIFICACIONES_CORREO_HABILITADAS", "false",
                "BOOLEAN", "Permite habilitar las notificaciones por correo.");
    }

    // Conserva cualquier valor previamente registrado.
    private void crearSiNoExiste(String clave, String valor,
                                 String tipoDato, String descripcion) {
        if (parametroRepository.findByClave(clave).isPresent()) {
            return;
        }

        ParametroSistema parametro = new ParametroSistema();
        parametro.setClave(clave);
        parametro.setValor(valor);
        parametro.setTipoDato(tipoDato);
        parametro.setDescripcion(descripcion);

        parametroRepository.save(parametro);
    }
}
