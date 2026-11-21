
package com.quickhelp.config;

import com.quickhelp.entity.Usuario; // Entidad de usuarios.
import com.quickhelp.repository.UsuarioRepository; // Consulta el estado actual del usuario.
import java.util.List; // Define los valores permitidos para CORS.
import org.springframework.context.annotation.Bean; // Define componentes Spring.
import org.springframework.context.annotation.Configuration; // Configuración de seguridad.
import org.springframework.http.HttpMethod; // Métodos HTTP.
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // Seguridad HTTP.
import org.springframework.security.config.http.SessionCreationPolicy; // Sesiones sin estado.
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter; // Convierte JWT.
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter; // Extrae roles.
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; // Autenticación JWT.
import org.springframework.security.web.SecurityFilterChain; // Filtros de seguridad.
import org.springframework.security.web.access.intercept.AuthorizationFilter; // Filtro de autorización.
import org.springframework.web.filter.OncePerRequestFilter; // Ejecuta el control una vez por petición.
import org.springframework.security.core.Authentication; // Usuario autenticado.
import org.springframework.security.core.context.SecurityContextHolder; // Contexto de seguridad.
import org.springframework.web.cors.CorsConfiguration; // Configura los permisos CORS.
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Aplica CORS a las rutas.
import jakarta.servlet.FilterChain; // Cadena de filtros.
import jakarta.servlet.ServletException; // Errores del filtro.
import jakarta.servlet.http.HttpServletRequest; // Petición HTTP.
import jakarta.servlet.http.HttpServletResponse; // Respuesta HTTP.
import java.io.IOException; // Errores de entrada y salida.

@Configuration // Configuración de seguridad del backend.
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UsuarioRepository usuarioRepository) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Permite conexiones autorizadas.
                .csrf(csrf -> csrf.disable()) // API sin sesiones basadas en cookies.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login", // Inicio de sesión público.
                                "/api/auth/registro/adulto", // Registro de adultos mayores.
                                "/api/auth/registro/asistente", // Registro de personas de apoyo.
                                "/api/auth/recuperar-clave/solicitar", // Pedir código por correo.
                                "/api/auth/recuperar-clave/cambiar", // Cambiar contraseña con el código.
                                "/error" // Muestra el error real en vez de un 401.
                        ).permitAll()

                        // Solo administradores pueden consultar configuraciones.
                        .requestMatchers(HttpMethod.GET, "/api/parametros", "/api/parametros/**")
                        .hasRole("ADMIN")

                        // Solo administradores pueden modificar configuraciones.
                        .requestMatchers(HttpMethod.PATCH, "/api/parametros/**")
                        .hasRole("ADMIN")

                        // Bloquea otros métodos no autorizados en este módulo.
                        .requestMatchers("/api/parametros", "/api/parametros/**")
                        .denyAll()

                        // Solo administradores pueden consultar todas las redes de apoyo.
                        .requestMatchers(HttpMethod.GET, "/api/administracion/red-apoyo")
                        .hasRole("ADMIN")

                        // Bloquea otras operaciones sobre este módulo administrativo.
                        .requestMatchers(
                                "/api/administracion/red-apoyo",
                                "/api/administracion/red-apoyo/**"
                        ).denyAll()

                        // Los demás endpoints requieren JWT.
                        .anyRequest().authenticated()
                )

                // Valida los JWT y convierte sus roles en autoridades Spring.
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))

                // Comprueba el estado actual antes de autorizar operaciones.
                .addFilterBefore(
                        filtroUsuariosActivos(usuarioRepository),
                        AuthorizationFilter.class
                );

        return http.build();
    }

    // Bloquea tokens pertenecientes a usuarios eliminados o desactivados.
    private OncePerRequestFilter filtroUsuariosActivos(UsuarioRepository usuarioRepository) {
        return new OncePerRequestFilter() {

            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {

                Authentication authentication =
                        SecurityContextHolder.getContext().getAuthentication();

                // Solo comprueba las peticiones autenticadas mediante JWT.
                if (authentication instanceof JwtAuthenticationToken jwtAuth) {

                    Integer idUsuario;

                    try {
                        idUsuario = Integer.valueOf(jwtAuth.getName());
                    } catch (NumberFormatException | NullPointerException e) {
                        SecurityContextHolder.clearContext();
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Token de usuario inválido."
                        );
                        return;
                    }

                    Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);

                    // Rechaza cuentas inexistentes, inactivas o con roles desactivados.
                    if (usuario == null
                            || !Boolean.TRUE.equals(usuario.getEstado())
                            || usuario.getRol() == null
                            || !Boolean.TRUE.equals(usuario.getRol().getEstado())) {

                        SecurityContextHolder.clearContext();
                        response.sendError(
                                HttpServletResponse.SC_FORBIDDEN,
                                "La cuenta está desactivada o no existe."
                        );
                        return;
                    }

                    // Evita conservar privilegios de un rol anterior en el JWT.
                    String rolActual = usuario.getRol().getCodigo();
                    boolean rolCoincide = jwtAuth.getAuthorities().stream()
                            .anyMatch(authority ->
                                    authority.getAuthority().equals("ROLE_" + rolActual));

                    if (!rolCoincide) {
                        SecurityContextHolder.clearContext();
                        response.sendError(
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Los permisos de la cuenta cambiaron. Inicia sesión nuevamente."
                        );
                        return;
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles"); // Lee los roles incluidos en el JWT.
        authorities.setAuthorityPrefix("ROLE_"); // Convierte los roles en autoridades Spring.

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Permite los orígenes web y los orígenes predeterminados de Capacitor.
        config.setAllowedOrigins(List.of(
                "http://localhost:8100", // Ionic en el navegador durante el desarrollo.
                "http://localhost:4200", // Portal web durante el desarrollo.
                "https://quick-help-web.vercel.app", // Portal web publicado en Vercel.
                "https://localhost", // Aplicación Android con Capacitor.
                "capacitor://localhost" // Aplicación iOS con Capacitor.
        ));

        // Las reglas de autorización anteriores siguen aplicándose.
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Permite enviar datos JSON y el token Bearer.
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        config.setAllowCredentials(false); // La API utiliza tokens, no cookies de sesión.
        config.setMaxAge(3600L); // Guarda la comprobación CORS durante una hora.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
