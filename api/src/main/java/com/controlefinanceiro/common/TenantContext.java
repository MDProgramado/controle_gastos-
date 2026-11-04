package com.controlefinanceiro.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * ÚNICO ponto de acesso ao tenant no projeto (R3/AUTH-03, PLAN §3.7.3) — vale para TODAS as fases.
 * Lê {@code empresa_id} SOMENTE do {@code SecurityContext} (claim do JWT → principal {@link
 * UsuarioAuth}), nunca de body/header/query.
 */
public final class TenantContext {

  private TenantContext() {}

  /**
   * @return o {@code empresaId} do token autenticado atual.
   */
  public static Long getEmpresaId() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao != null
        && autenticacao.isAuthenticated()
        && autenticacao.getPrincipal() instanceof UsuarioAuth usuario) {
      return usuario.empresaId();
    }
    throw new NaoAutorizadoException();
  }
}
