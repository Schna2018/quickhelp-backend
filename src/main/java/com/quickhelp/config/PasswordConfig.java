package com.quickhelp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration // Indica que esta clase contiene configuraciones que Spring debe cargar al iniciar.
public class PasswordConfig {

    @Bean // Registra PasswordEncoder como un objeto administrado por Spring para poder inyectarlo donde lo necesitemos.
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt transforma la contraseña en un hash seguro antes de guardarla.
    }
}