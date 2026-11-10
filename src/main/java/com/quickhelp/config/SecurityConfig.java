
package com.quickhelp.config;

import org.springframework.context.annotation.Bean; // Define componentes Spring.
import org.springframework.context.annotation.Configuration; // Configuración de seguridad.
import org.springframework.http.HttpMethod; // Métodos HTTP.
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // Seguridad HTTP.
import org.springframework.security.config.http.SessionCreationPolicy; // Sesiones sin estado.
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter; // Convierte JWT.
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter; // Extrae roles.
import org.springframework.security.web.SecurityFilterChain; // Filtros de seguridad.

@Configuration // Configuración de seguridad del backend.
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API sin sesiones basadas en cookies.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login", // Inicio de sesión público.
                                "/api/auth/registro/adulto", // Registro de adultos mayores.
                                "/api/auth/registro/asistente" // Registro de personas de apoyo.
                        ).permitAll()

                        // Solo administradores pueden consultar configuraciones.
                        .requestMatchers(HttpMethod.GET, "/api/parametros", "/api/parametros/**")
                        .hasRole("ADMINISTRADOR")

                        // Solo administradores pueden modificar configuraciones.
                        .requestMatchers(HttpMethod.PATCH, "/api/parametros/**")
                        .hasRole("ADMINISTRADOR")

                        // Bloquea otros métodos no autorizados en este módulo.
                        .requestMatchers("/api/parametros", "/api/parametros/**")
                        .denyAll()

                        // Los demás endpoints requieren JWT.
                        .anyRequest().authenticated()
                )

                // Valida los JWT y convierte sus roles en autoridades Spring.
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
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
}
