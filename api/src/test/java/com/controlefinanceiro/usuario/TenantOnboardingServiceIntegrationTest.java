package com.controlefinanceiro.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.auth.TokenResponse;
import com.controlefinanceiro.common.CadastroInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantOnboardingServiceIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private TenantOnboardingService service;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void onboardingCriaEmpresaUsuarioECategoriasPadrao() {
    TokenResponse resposta =
        service.onboarding("Empresa Nova", "Admin Nova", "adminnova@teste.com", "senha123");

    assertThat(resposta.token()).isNotBlank();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM empresa WHERE nome = 'Empresa Nova'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM usuario WHERE email = 'adminnova@teste.com' AND role = 'USER'"
                    + " AND ativo = true",
                Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM categoria c JOIN empresa e ON e.id = c.empresa_id"
                    + " WHERE e.nome = 'Empresa Nova'",
                Integer.class))
        .isEqualTo(16);
    assertThat(
            jdbc.queryForObject(
                "SELECT senha_hash FROM usuario WHERE email = 'adminnova@teste.com'", String.class))
        .startsWith("$2a$12$");
  }

  @Test
  void onboardingComEmailExistenteNaoCriaEmpresa() {
    service.onboarding("Primeira", "Admin Um", "unico@teste.com", "senha123");
    Integer antes =
        jdbc.queryForObject("SELECT count(*) FROM empresa WHERE nome = 'Segunda'", Integer.class);

    try {
      service.onboarding("Segunda", "Admin Dois", "unico@teste.com", "senha123");
      throw new AssertionError("esperava CadastroInvalidoException");
    } catch (CadastroInvalidoException esperado) {
      assertThat(esperado).hasMessage("Não foi possível concluir o cadastro");
    }

    Integer depois =
        jdbc.queryForObject("SELECT count(*) FROM empresa WHERE nome = 'Segunda'", Integer.class);
    assertThat(depois).isEqualTo(antes);
  }
}
