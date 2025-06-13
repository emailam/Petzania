package com.example.adoption_and_breeding_module.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Allow every request
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Disable CSRF if you're only exposing a stateless REST API
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}

