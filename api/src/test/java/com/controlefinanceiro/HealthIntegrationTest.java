package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração com PostgreSQL real (padrão P6): Flyway V1 sobe do zero a cada contexto e os
 * gates R1/R3 são assertados contra o information_schema.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate"})
class HealthIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private TestRestTemplate rest;

  @Autowired private JdbcTemplate jdbc;

  @Test
  void healthRetornaOkQuandoBancoAcessivel() {
    ResponseEntity<String> response = rest.getForEntity("/api/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"OK\"");
  }

  @Test
  void flywayV1AplicadaDoZeroComSucesso() {
    Map<String, Object> row =
        jdbc.queryForMap("SELECT success FROM flyway_schema_history WHERE version = '1'");

    assertThat(row.get("success")).isEqualTo(true);
  }

  @Test
  void gateR1ValorEhNumeric142ComCheckEGuessNulo() {
    // R1: lancamento.valor NUMERIC(14,2)
    Map<String, Object> valor =
        jdbc.queryForMap(
            "SELECT data_type, numeric_precision, numeric_scale"
                + " FROM information_schema.columns"
                + " WHERE table_name = 'lancamento' AND column_name = 'valor'");
    assertThat(valor.get("data_type")).isEqualTo("numeric");
    assertThat(((Number) valor.get("numeric_precision")).intValue()).isEqualTo(14);
    assertThat(((Number) valor.get("numeric_scale")).intValue()).isEqualTo(2);

    // R1: CHECK (valor > 0) presente
    String checkDef =
        jdbc.queryForObject(
            "SELECT pg_get_constraintdef(oid)"
                + " FROM pg_constraint"
                + " WHERE conrelid = 'lancamento'::regclass"
                + " AND contype = 'c'"
                + " AND pg_get_constraintdef(oid) LIKE '%valor%'",
            String.class);
    // Postgres normaliza para `((valor > (0)::numeric))` — remove casts/parênteses antes
    String checkNormalizado = checkDef.replace("::numeric", "").replace("(", "").replace(")", "");
    assertThat(checkNormalizado).contains("valor > 0");

    // R3: empresa_id e demais FKs NOT NULL
    int notNullFks =
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.columns"
                + " WHERE is_nullable = 'NO'"
                + " AND (table_name, column_name) IN ("
                + "  ('usuario', 'empresa_id'),"
                + "  ('categoria', 'empresa_id'),"
                + "  ('lancamento', 'empresa_id'),"
                + "  ('lancamento', 'usuario_id'),"
                + "  ('lancamento', 'categoria_id'))",
            Integer.class);
    assertThat(notNullFks).isEqualTo(5);
  }
}
