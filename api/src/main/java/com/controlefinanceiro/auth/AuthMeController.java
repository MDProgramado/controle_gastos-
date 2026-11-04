package com.controlefinanceiro.auth;

import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioAuth;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/auth/me — devolve a sessão do token atual. É a rota que o BFF do web usará para validar
 * sessão (D-02/next: plano 03). Os dados vêm todos do principal no SecurityContext (R3).
 */
@RestController
public class AuthMeController {

  @GetMapping("/api/auth/me")
  public MeResponse me() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAuth usuario)) {
      throw new NaoAutorizadoException();
    }
    return new MeResponse(usuario.subject(), TenantContext.getEmpresaId(), usuario.role());
  }

  /** {@code empresa_id} serializado com o nome do claim (snake_case), como o resto da API. */
  public record MeResponse(String email, @JsonProperty("empresa_id") Long empresaId, String role) {}
}
