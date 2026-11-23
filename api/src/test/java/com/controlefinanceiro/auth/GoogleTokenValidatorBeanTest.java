package com.controlefinanceiro.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GoogleTokenValidatorBeanTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(GoogleTokenValidator.class)
          .withPropertyValues("google.client-id=cliente-teste");

  @Test
  void criaBeanComConstrutorDeConfiguracaoQuandoHaMultiplosConstructores() {
    contextRunner.run(context -> assertThat(context).hasSingleBean(GoogleTokenValidator.class));
  }
}
