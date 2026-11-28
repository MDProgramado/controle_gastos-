package com.controlefinanceiro.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autenticação do scraper de métricas: em {@code /actuator/prometheus}, o header {@code
 * X-Metrics-Token} igual à env var {@code METRICS_TOKEN} concede {@code ROLE_METRICS}. Sem token
 * configurado o header é ignorado (só ADMIN via JWT acessa). Token errado: 401.
 */
public class MetricsTokenFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Metrics-Token";
  static final String CAMINHO = "/actuator/prometheus";

  private final byte[] token;

  public MetricsTokenFilter(String token) {
    this.token =
        token == null || token.isBlank() ? null : token.trim().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !CAMINHO.equals(request.getRequestURI()) || request.getHeader(HEADER) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    byte[] recebido = request.getHeader(HEADER).trim().getBytes(StandardCharsets.UTF_8);
    if (token == null || !MessageDigest.isEqual(token, recebido)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "metrics-scraper", null, List.of(new SimpleGrantedAuthority("ROLE_METRICS"))));
    chain.doFilter(request, response);
  }
}
