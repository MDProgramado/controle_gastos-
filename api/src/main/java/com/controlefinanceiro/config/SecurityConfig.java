package com.controlefinanceiro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security mínima desta onda (padrão P3 simplificado): sem o starter-security configurado, o
 * default do Spring Security devolve 401 em /api/health e o teste de saúde falha.
 *
 * <p>O plano 02 (Task 2) ESTENDE este mesmo arquivo para o padrão P3 completo — regras por rota,
 * Bearer Nimbus (oauth2ResourceServer), claims — nunca criar um segundo SecurityConfig.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // API stateless com Bearer (fase 2+) — CSRF não se aplica
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers("/api/health").permitAll().anyRequest().authenticated());
    return http.build();
  }
}
