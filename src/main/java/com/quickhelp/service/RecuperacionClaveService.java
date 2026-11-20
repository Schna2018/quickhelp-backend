package com.quickhelp.service;

import com.quickhelp.entity.RecuperacionClave;
import com.quickhelp.entity.Usuario;
import com.quickhelp.repository.RecuperacionClaveRepository;
import com.quickhelp.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Recuperar la contraseña con un código de 6 dígitos enviado por correo.
@Service
public class RecuperacionClaveService {

    private static final int MINUTOS_VIGENCIA = 15;
    private static final int MAXIMO_INTENTOS = 5;
    private static final int SEGUNDOS_ENTRE_ENVIOS = 60;
    private static final String CODIGO_INVALIDO = "El código es incorrecto o ya venció. Pide uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final RecuperacionClaveRepository recuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CorreoService correoService;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionClaveService(
            UsuarioRepository usuarioRepository,
            RecuperacionClaveRepository recuperacionRepository,
            PasswordEncoder passwordEncoder,
            CorreoService correoService) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoService = correoService;
    }

    // Envía un código nuevo. Si el correo no existe no hace nada, para no revelar qué cuentas existen.
    @Transactional
    public void solicitarCodigo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.strip()).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getEstado())) return;

        LocalDateTime ahora = LocalDateTime.now();

        // Evita enviar muchos correos seguidos a la misma cuenta.
        var anterior = recuperacionRepository
                .findFirstByUsuario_IdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario());
        if (anterior.isPresent()
                && anterior.get().getFechaCreacion().plusSeconds(SEGUNDOS_ENTRE_ENVIOS).isAfter(ahora)) {
            return;
        }

        recuperacionRepository.invalidarPendientes(usuario.getIdUsuario());

        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        RecuperacionClave recuperacion = new RecuperacionClave();
        recuperacion.setUsuario(usuario);
        recuperacion.setCodigoHash(passwordEncoder.encode(codigo));
        recuperacion.setFechaCreacion(ahora);
        recuperacion.setFechaExpiracion(ahora.plusMinutes(MINUTOS_VIGENCIA));
        recuperacion.setIntentos(0);
        recuperacion.setUsado(false);
        recuperacionRepository.save(recuperacion);

        if (!correoService.enviarCodigoRecuperacion(
                usuario.getCorreo(), usuario.getNombreApellido(), codigo)) {
            // Se deshace el código guardado: el usuario puede intentarlo de nuevo enseguida.
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No pudimos enviar el correo. Inténtalo de nuevo en unos minutos.");
        }
    }

    // Cambia la contraseña si el código es correcto. Los intentos fallidos sí se guardan.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void cambiarClave(String correo, String codigo, String nuevaClave) {
        if (nuevaClave == null || nuevaClave.strip().isEmpty() || nuevaClave.length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres.");
        }

        Usuario usuario = usuarioRepository.findByCorreo(correo.strip())
                .filter(cuenta -> Boolean.TRUE.equals(cuenta.getEstado()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CODIGO_INVALIDO));

        RecuperacionClave recuperacion = recuperacionRepository
                .findFirstByUsuario_IdUsuarioAndUsadoFalseOrderByFechaCreacionDesc(usuario.getIdUsuario())
                .filter(r -> r.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .filter(r -> r.getIntentos() < MAXIMO_INTENTOS)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, CODIGO_INVALIDO));

        if (!passwordEncoder.matches(codigo, recuperacion.getCodigoHash())) {
            int intentos = recuperacion.getIntentos() + 1;
            recuperacion.setIntentos(intentos);
            recuperacionRepository.save(recuperacion);
            int quedan = MAXIMO_INTENTOS - intentos;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, quedan > 0
                    ? "El código es incorrecto. Te quedan " + quedan + (quedan == 1 ? " intento." : " intentos.")
                    : CODIGO_INVALIDO);
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);
        recuperacion.setUsado(true);
        recuperacionRepository.save(recuperacion);
    }
}
