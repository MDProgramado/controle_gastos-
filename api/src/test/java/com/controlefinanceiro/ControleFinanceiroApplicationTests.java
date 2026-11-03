package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Teste trivial sem contexto Spring: o contexto passa a ser exercitado a partir da Task 2 via
 * Testcontainers (HealthIntegrationTest), pois ainda não há banco nesta etapa.
 */
class ControleFinanceiroApplicationTests {

  @Test
  void classePrincipalFazParteDoPacoteRaiz() {
    assertThat(ControleFinanceiroApplication.class.getPackageName())
        .isEqualTo("com.controlefinanceiro");
  }
}
