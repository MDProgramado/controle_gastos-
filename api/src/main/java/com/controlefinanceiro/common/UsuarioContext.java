package com.controlefinanceiro.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class UsuarioContext {

  private UsuarioContext() {}

  public static Long getUsuarioId() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao != null
        && autenticacao.isAuthenticated()
        && autenticacao.getPrincipal() instanceof UsuarioAuth usuario) {
      return usuario.usuarioId();
    }
    throw new NaoAutorizadoException();
  }
}
