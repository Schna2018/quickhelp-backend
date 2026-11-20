package com.quickhelp.service;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;

// Envía correos con la API de Brevo por HTTPS (Render gratis bloquea SMTP, no HTTPS).
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final String apiKey;
    private final String remitente;
    private final RestClient brevo;

    public CorreoService(
            @Value("${BREVO_API_KEY:}") String apiKey,
            @Value("${QUICKHELP_CORREO_REMITENTE:}") String remitente) {
        this.apiKey = apiKey.strip();
        this.remitente = remitente.strip();

        SimpleClientHttpRequestFactory tiempos = new SimpleClientHttpRequestFactory();
        tiempos.setConnectTimeout(10_000);
        tiempos.setReadTimeout(15_000);
        this.brevo = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .requestFactory(tiempos)
                .build();
    }

    // Devuelve false si no se pudo enviar (sin clave, sin remitente o error de Brevo).
    public boolean enviarCodigoRecuperacion(String correo, String nombre, String codigo) {
        if (apiKey.isEmpty() || remitente.isEmpty()) {
            log.error("Correo no configurado: faltan BREVO_API_KEY o QUICKHELP_CORREO_REMITENTE.");
            return false;
        }

        String nombreSeguro = HtmlUtils.htmlEscape(nombre == null ? "" : nombre.strip());
        String html = """
                <div style="font-family: Arial, sans-serif; background: #eff2f7; padding: 24px; color: #202634">
                  <div style="max-width: 480px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden">
                    <div style="background: #e51c35; color: #ffffff; padding: 20px 26px; font-size: 24px">Quick<b>Help</b></div>
                    <div style="padding: 26px; font-size: 15px; line-height: 1.55">
                      <p style="margin: 0 0 12px">Hola %s,</p>
                      <p style="margin: 0 0 12px">Recibimos una solicitud para cambiar la contraseña de tu cuenta de Quick Help. Tu código es:</p>
                      <p style="margin: 18px 0; text-align: center; font-size: 34px; font-weight: 700; letter-spacing: 10px; color: #c91d39">%s</p>
                      <p style="margin: 0 0 12px">Vence en <b>15 minutos</b> y se puede usar una sola vez.</p>
                      <p style="margin: 0; color: #4a5566">Si no lo pediste, ignora este correo: tu contraseña no cambia.</p>
                    </div>
                  </div>
                </div>
                """.formatted(nombreSeguro, codigo);
        String texto = "Hola " + (nombre == null ? "" : nombre.strip())
                + ",\n\nTu código de Quick Help para cambiar la contraseña es: " + codigo
                + "\nVence en 15 minutos y se puede usar una sola vez."
                + "\n\nSi no lo pediste, ignora este correo: tu contraseña no cambia.";

        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", "Quick Help", "email", remitente),
                "to", List.of(Map.of("email", correo)),
                "subject", "Tu código de Quick Help: " + codigo,
                "htmlContent", html,
                "textContent", texto);

        try {
            brevo.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.error("Brevo no pudo enviar el código de recuperación: {}", ex.getMessage());
            return false;
        }
    }
}
