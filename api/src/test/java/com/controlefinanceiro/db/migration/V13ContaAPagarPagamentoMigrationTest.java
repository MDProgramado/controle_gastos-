package com.controlefinanceiro.db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
class V13ContaAPagarPagamentoMigrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void v13AdicionaCamposEIndicesDeBaixa() {
    assertThat(colunas("conta_a_pagar")).contains("pago", "pago_em", "lancamento_id", "versao");

    assertThat(nullabilidade("conta_a_pagar", "pago")).isEqualTo("NO");
    assertThat(valorDefault("conta_a_pagar", "pago")).isEqualTo("false");
    assertThat(tipo("conta_a_pagar", "pago")).isEqualTo("boolean");

    assertThat(nullabilidade("conta_a_pagar", "pago_em")).isEqualTo("YES");
    assertThat(tipo("conta_a_pagar", "pago_em")).isEqualTo("timestamp with time zone");

    assertThat(nullabilidade("conta_a_pagar", "lancamento_id")).isEqualTo("YES");
    assertThat(tipo("conta_a_pagar", "lancamento_id")).isEqualTo("bigint");

    assertThat(nullabilidade("conta_a_pagar", "versao")).isEqualTo("NO");
    assertThat(valorDefault("conta_a_pagar", "versao")).isEqualTo("0");
    assertThat(tipo("conta_a_pagar", "versao")).isEqualTo("integer");

    assertThat(indices()).contains("idx_conta_a_pagar_empresa_usuario_pago");
    assertThat(restricoes("conta_a_pagar", "f"))
        .anyMatch(def -> def.contains("FOREIGN KEY (lancamento_id) REFERENCES lancamento(id)"));
    assertThat(restricoes("conta_a_pagar", "u"))
        .anyMatch(def -> def.contains("UNIQUE (lancamento_id)"));
  }

  private List<String> colunas(String tabela) {
    return jdbc.queryForList(
        "SELECT column_name FROM information_schema.columns"
            + " WHERE table_schema = 'public' AND table_name = ?",
        String.class,
        tabela);
  }

  private String nullabilidade(String tabela, String coluna) {
    return jdbc.queryForObject(
        "SELECT is_nullable FROM information_schema.columns"
            + " WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
        String.class,
        tabela,
        coluna);
  }

  private String tipo(String tabela, String coluna) {
    return jdbc.queryForObject(
        "SELECT data_type FROM information_schema.columns"
            + " WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
        String.class,
        tabela,
        coluna);
  }

  private String valorDefault(String tabela, String coluna) {
    return jdbc.queryForObject(
        "SELECT column_default FROM information_schema.columns"
            + " WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
        String.class,
        tabela,
        coluna);
  }

  private List<String> indices() {
    return jdbc.queryForList(
        "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class);
  }

  private List<String> restricoes(String tabela, String tipo) {
    return jdbc.queryForList(
        "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c"
            + " JOIN pg_class t ON t.oid = c.conrelid"
            + " WHERE t.relname = ? AND c.contype = CAST(? AS \"char\")",
        String.class,
        tabela,
        tipo);
  }
}
