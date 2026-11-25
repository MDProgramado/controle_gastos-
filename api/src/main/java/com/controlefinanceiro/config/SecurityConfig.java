package com.controlefinanceiro.config;

import com.controlefinanceiro.common.Role;
import com.controlefinanceiro.common.RoleInvalidoException;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import com.controlefinanceiro.usuario.UsuarioRepository;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Padrão P3 completo (transição explícita da cadeia mínima do plano 01): stateless, regras por rota
 * e Bearer via {@code oauth2ResourceServer} (Nimbus) — NUNCA um segundo SecurityConfig e NUNCA um
 * filtro JWT manual (anti-pattern 2: filtros hand-rolled erram exp/alg/clock skew).
 */
@Configuration
@EnableWebSecurity
@EnableAsync
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarioRepository)
      throws Exception {
    http
        // API stateless com Bearer — CSRF não se aplica (o cookie httpOnly fica no BFF do web)
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.disable())
        .headers(
            headers ->
                headers
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(
                        referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.maxAgeInSeconds(63_072_000L).includeSubDomains(true))
                    .cacheControl(cache -> cache.disable()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/login",
                        "/api/auth/registro",
                        "/api/auth/convites/aceitar",
                        "/api/auth/google",
                        "/api/health")
                    .permitAll()
                    // Webhook público da Meta (validação via X-Hub-Signature-256, não JWT)
                    .requestMatchers(HttpMethod.GET, "/webhooks/whatsapp")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/webhooks/whatsapp")
                    .permitAll()
                    // GET liberado para ADMIN e USER; escrita restrita a ADMIN (T-2-05, CAT-05)
                    .requestMatchers(HttpMethod.GET, "/api/categorias")
                    .hasAnyRole("ADMIN", "USER")
                    .requestMatchers("/api/categorias/**")
                    .hasRole("ADMIN")
                    // Gestão de contas e vinculação de telefone: somente ADMIN (D-04, F1)
                    .requestMatchers("/api/usuarios/**")
                    .hasRole("ADMIN")
                    // Conhecimento da IA é administrativo e escopado pela empresa do JWT (F-002)
                    .requestMatchers("/api/ai/chat/conhecimento", "/api/ai/chat/conhecimento/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/ai/rag/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/helpdesk/**")
                    .hasRole("USER")
                    .requestMatchers("/api/admin/helpdesk/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .addFilterAfter(
            new UsuarioSessaoFilter(usuarioRepository), BearerTokenAuthenticationFilter.class);
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
      Object usuarioClaim = jwt.getClaim("usuario_id");
      Object versaoClaim = jwt.getClaim("token_version");
      String roleClaim = jwt.getClaim("role");
      if (!(empresaClaim instanceof Number empresa)
          || !(usuarioClaim instanceof Number usuario)
          || !(versaoClaim instanceof Number versao)
          || roleClaim == null
          || roleClaim.isBlank()) {
        throw new InvalidBearerTokenException(
            "Token sem as claims de sessão (empresa_id/usuario_id/token_version/role)");
      }
      Role role;
      try {
        role = Role.from(roleClaim);
      } catch (RoleInvalidoException ex) {
        throw new InvalidBearerTokenException("Role inválida no token", ex);
      }
      UsuarioAuth principal =
          new UsuarioAuth(
              jwt.getSubject(),
              empresa.longValue(),
              usuario.longValue(),
              versao.intValue(),
              role.valor());
      var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.valor()));
      return new UsuarioAuthAuthenticationToken(principal, authorities);
    };
  }

  /** BCrypt custo 12 (R6/AUTH-02) — mesmo custo do seed V1_1. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
