package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.auth.LoginRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Gate estrutural R3 (Task 3, teste 4): o DTO de login NUNCA pode ter campo ou getter com "empresa"
 * no nome — o tenant é exclusivo do token. Vale para TODAS as fases.
 */
class LoginRequestEstruturaTest {

  @Test
  void nenhumCampoComEmpresaNoNome() {
    Field[] campos = LoginRequest.class.getDeclaredFields();

    assertThat(campos).isNotEmpty();
    for (Field campo : campos) {
      assertThat(campo.getName().toLowerCase())
          .as("campo %s do LoginRequest não pode mencionar empresa (R3)", campo.getName())
          .doesNotContain("empresa");
    }
  }

  @Test
  void nenhumMetodoAcessorComEmpresaNoNome() {
    for (Method metodo : LoginRequest.class.getDeclaredMethods()) {
      assertThat(metodo.getName().toLowerCase())
          .as("método %s do LoginRequest não pode mencionar empresa (R3)", metodo.getName())
          .doesNotContain("empresa");
    }
  }
}
