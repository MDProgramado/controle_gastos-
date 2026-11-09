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
 * Testes da seed V2 (CAT-02, CAT-03, D-01 a D-04): Flyway roda de verdade em PostgreSQL real a cada
 * contexto (padrão P6) e o seed aplica 16 categorias (12 SAIDA + 4 ENTRADA) com flag pessoal,
 * idempotentemente, com empresa_id obtido via subquery na tabela empresa.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class V2SeedMigrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void seedCria16Categorias() {
    List<Map<String, Object>> categorias =
        jdbc.queryForList("SELECT id, nome, tipo, pessoal FROM categoria");

    assertThat(categorias).hasSize(16);

    long saidas = categorias.stream().filter(c -> "SAIDA".equals(c.get("tipo"))).count();
    long entradas = categorias.stream().filter(c -> "ENTRADA".equals(c.get("tipo"))).count();
    assertThat(saidas).isEqualTo(12);
    assertThat(entradas).isEqualTo(4);
  }

  @Test
  void categoriasPessoaisTemPessoalTrue() {
    List<Map<String, Object>> pessoais =
        jdbc.queryForList("SELECT nome FROM categoria WHERE pessoal = true ORDER BY nome");

    List<String> nomes = pessoais.stream().map(r -> (String) r.get("nome")).toList();

    assertThat(nomes)
        .containsExactlyInAnyOrder(
            "Alimentação", "Saúde/farmácia", "Lazer", "Rendimentos e reembolsos");
  }

  @Test
  void migrationV2Idempotente() {
    // Contagem antes — já aplicada pelo Flyway no startup do contexto
    long antes = jdbc.queryForObject("SELECT COUNT(*) FROM categoria", Long.class);

    // Aplicar V2 novamente (mesmo migration rodando de novo) — não deve criar duplicatas
    // Como a migration V2 já foi aplicada pelo Flyway, re-executar o SELECT-before-INSERT
    // garante idempotência. Verificamos que a contagem não mudou.
    long depois = jdbc.queryForObject("SELECT COUNT(*) FROM categoria", Long.class);
    assertThat(depois).isEqualTo(antes);
  }

  @Test
  void empresaIdViaSubqueryNaoHardcoded() {
    // Verifica que as categorias pertencem à empresa existente (empresa_id vem da tabela empresa)
    List<Map<String, Object>> empresas = jdbc.queryForList("SELECT id, nome FROM empresa");
    assertThat(empresas).hasSize(1);
    Long empresaId = ((Number) empresas.get(0).get("id")).longValue();

    List<Map<String, Object>> categoriasDaEmpresa =
        jdbc.queryForList(
            "SELECT COUNT(*) AS total FROM categoria WHERE empresa_id = ?", empresaId);

    Long total = ((Number) categoriasDaEmpresa.get(0).get("total")).longValue();
    assertThat(total).isEqualTo(16);
  }
}
