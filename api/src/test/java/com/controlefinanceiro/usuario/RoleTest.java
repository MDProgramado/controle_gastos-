package com.controlefinanceiro.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.controlefinanceiro.common.Role;
import com.controlefinanceiro.common.RoleInvalidoException;
import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void aceitaApenasAdminEUserEmMaiusculas() {
    assertThat(Role.from("ADMIN")).isEqualTo(Role.ADMIN);
    assertThat(Role.from("USER")).isEqualTo(Role.USER);
  }

  @Test
  void rejeitaValoresInvalidos() {
    assertThatThrownBy(() -> Role.from("admin"))
        .isInstanceOf(RoleInvalidoException.class)
        .hasMessageContaining("admin");
    assertThatThrownBy(() -> Role.from("ROOT")).isInstanceOf(RoleInvalidoException.class);
    assertThatThrownBy(() -> Role.from("")).isInstanceOf(RoleInvalidoException.class);
    assertThatThrownBy(() -> Role.from(null)).isInstanceOf(RoleInvalidoException.class);
  }
}
