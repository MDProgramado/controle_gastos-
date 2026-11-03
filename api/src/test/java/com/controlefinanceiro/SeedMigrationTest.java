package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes da seed V1_1 (AUTH-04/D-03/D-04): Flyway roda de verdade em PostgreSQL real a cada
 * contexto (padrão P6) e o seed precisa existir exatamente uma vez, com hash BCrypt.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeedMigrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void seedCriaExatamenteUmaEmpresaEUmUsuarioAdmin() {
    List<Map<String, Object>> empresas = jdbc.queryForList("SELECT id, nome FROM empresa");

    assertThat(empresas).hasSize(1);
    assertThat(empresas.get(0).get("nome")).isEqualTo("Controle Financeiro");

    List<Map<String, Object>> usuarios =
        jdbc.queryForList("SELECT email, role, ativo FROM usuario");

    assertThat(usuarios).hasSize(1);
    assertThat(usuarios.get(0).get("email")).isEqualTo("admin@controle.com");
    assertThat(usuarios.get(0).get("role")).isEqualTo("ADMIN");
    assertThat(usuarios.get(0).get("ativo")).isEqualTo(true);
  }

  @Test
  void senhaDoSeedEhHashBcryptENuncaTextoPuro() {
    String hash =
        jdbc.queryForObject(
            "SELECT senha_hash FROM usuario WHERE email = 'admin@controle.com'", String.class);

    // AUTH-02: prefixo $2 (BCrypt) e nunca a senha em claro (D-04)
    assertThat(hash).startsWith("$2");
    assertThat(hash).hasSize(60);
    assertThat(hash).isNotEqualTo(System.getenv("SEED_ADMIN_PASSWORD"));
  }
}
