package com.controlefinanceiro.common;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Token de recurso com principal {@link UsuarioAuth} — o que o key-link do plano exige (claim
 * {@code empresa_id} do JWT → principal → SecurityContext → {@link TenantContext}).
 *
 * <p>Por que não {@code JwtAuthenticationToken}: no Spring Security 6.5 os construtores públicos
 * são {@code (Jwt)}, {@code (Jwt, Collection)} e {@code (Jwt, Collection, String)} — o principal é
 * SEMPRE o próprio {@code Jwt}; não existe variante com principal customizável (o exemplo do plano/
 * RESEARCH era de versões anteriores). Esta classe preserva o design planejado (principal = {@code
 * UsuarioAuth}) com a API real da versão travada.
 */
public class UsuarioAuthAuthenticationToken extends AbstractAuthenticationToken {

  private final transient UsuarioAuth usuario;

  public UsuarioAuthAuthenticationToken(
      UsuarioAuth usuario, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.usuario = usuario;
    setAuthenticated(true);
  }

  @Override
  public Object getPrincipal() {
    return usuario;
  }

  @Override
  public Object getCredentials() {
    return "";
  }

  @Override
  public String getName() {
    return usuario.subject();
  }
}
