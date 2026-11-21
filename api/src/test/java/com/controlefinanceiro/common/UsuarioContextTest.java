package com.controlefinanceiro.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class UsuarioContextTest {

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void leUsuarioIdETenantDoPrincipal() {
    autenticar(new UsuarioAuth("user@teste.com", 42L, 7L, 3, "USER"));

    assertThat(UsuarioContext.getUsuarioId()).isEqualTo(7L);
    assertThat(TenantContext.getEmpresaId()).isEqualTo(42L);
  }

  @Test
  void semPrincipalLancaNaoAutorizado() {
    assertThatThrownBy(UsuarioContext::getUsuarioId).isInstanceOf(NaoAutorizadoException.class);
  }

  private void autenticar(UsuarioAuth usuario) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                usuario, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.role()))));
  }
}
