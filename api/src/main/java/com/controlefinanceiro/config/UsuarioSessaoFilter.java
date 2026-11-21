package com.controlefinanceiro.config;

import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class UsuarioSessaoFilter extends OncePerRequestFilter {

  public static final String USUARIO_ATUAL = "usuarioAutenticado";

  private final UsuarioRepository usuarioRepository;

  public UsuarioSessaoFilter(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao != null
        && autenticacao.isAuthenticated()
        && autenticacao.getPrincipal() instanceof UsuarioAuth usuario) {
      Optional<Usuario> encontrado =
          usuarioRepository.findByIdAndEmpresaId(usuario.usuarioId(), usuario.empresaId());
      if (encontrado.isEmpty()
          || !encontrado.get().isAtivo()
          || encontrado.get().getTokenVersion() != usuario.tokenVersion()) {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      request.setAttribute(USUARIO_ATUAL, encontrado.get());
    }
    filterChain.doFilter(request, response);
  }
}
