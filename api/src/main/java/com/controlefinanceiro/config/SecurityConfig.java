package com.controlefinanceiro.config;

import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Padrão P3 completo (transição explícita da cadeia mínima do plano 01): stateless, regras por rota
 * e Bearer via {@code oauth2ResourceServer} (Nimbus) — NUNCA um segundo SecurityConfig e NUNCA um
 * filtro JWT manual (anti-pattern 2: filtros hand-rolled erram exp/alg/clock skew).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // API stateless com Bearer — CSRF não se aplica (o cookie httpOnly fica no BFF do web)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/login", "/api/health")
                    .permitAll()
                    // GET liberado para ADMIN e USER; escrita restrita a ADMIN (T-2-05, CAT-05)
                    .requestMatchers(HttpMethod.GET, "/api/categorias")
                    .hasAnyRole("ADMIN", "USER")
                    .requestMatchers("/api/categorias/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  /**
   * Key-link do plano: claim {@code empresa_id} do JWT → principal {@link UsuarioAuth} → {@code
   * SecurityContext} → {@code TenantContext.getEmpresaId()} (R3/AUTH-03). Authorities {@code
   * ROLE_ADMIN}/{@code ROLE_USER} a partir do claim {@code role} (AUTH-05).
   */
  @Bean
  Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return jwt -> {
      Object empresaClaim = jwt.getClaim("empresa_id");
      String role = jwt.getClaim("role");
      if (!(empresaClaim instanceof Number numero) || role == null || role.isBlank()) {
        throw new InvalidBearerTokenException("Token sem as claims de sessão (empresa_id/role)");
      }
      UsuarioAuth principal = new UsuarioAuth(jwt.getSubject(), numero.longValue(), role);
      var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
      return new UsuarioAuthAuthenticationToken(principal, authorities);
    };
  }

  /** BCrypt custo 12 (R6/AUTH-02) — mesmo custo do seed V1_1. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
