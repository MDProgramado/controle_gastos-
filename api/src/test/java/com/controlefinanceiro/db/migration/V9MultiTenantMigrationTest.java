package com.controlefinanceiro.db.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class V9MultiTenantMigrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void v9CriaColunasIndicesETabelaConviteEmBancoLimpo() {
    assertEstruturaMultiTenant("public");
    assertUnicidades("public");
  }

  @Test
  void v9AplicaSobreSchemaLegadoComDados() {
    jdbc.execute("CREATE SCHEMA legado");
    migrar("legado", "8");

    Long empresaId =
        jdbc.queryForObject(
            "INSERT INTO legado.empresa (nome) VALUES ('Legado') RETURNING id", Long.class);
    jdbc.update(
        "INSERT INTO legado.usuario"
            + " (empresa_id, email, senha_hash, nome, role, ativo)"
            + " VALUES (?, ?, ?, ?, ?, true)",
        empresaId,
        "legado@teste.com",
        "$2a$12$fixtureHashSomenteTestes12345678901234567890123456789012",
        "Usuario Legado",
        "ADMIN");

    migrar("legado", "9");

    assertEstruturaMultiTenant("legado");
    Integer preservados =
        jdbc.queryForObject(
            "SELECT count(*) FROM legado.usuario WHERE email = 'legado@teste.com'", Integer.class);
    assertThat(preservados).isEqualTo(1);
  }

  private void migrar(String schema, String target) {
    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .schemas(schema)
        .locations("classpath:db/migration", "classpath:com/controlefinanceiro/db/migration")
        .target(target)
        .load()
        .migrate();
  }

  private void assertEstruturaMultiTenant(String schema) {
    List<String> colunasUsuario =
        jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns"
                + " WHERE table_schema = ? AND table_name = 'usuario'",
            String.class,
            schema);
    assertThat(colunasUsuario)
        .contains("google_sub", "email_verificado", "token_version", "senha_hash");

    List<String> colunasConvite =
        jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns"
                + " WHERE table_schema = ? AND table_name = 'usuario_convite'",
            String.class,
            schema);
    assertThat(colunasConvite)
        .containsExactlyInAnyOrder(
            "id",
            "empresa_id",
            "email",
            "role",
            "token_hash",
            "expira_em",
            "usado_em",
            "criado_por",
            "criado_em");

    List<String> indices =
        jdbc.queryForList(
            "SELECT indexname FROM pg_indexes WHERE schemaname = ?", String.class, schema);
    assertThat(indices)
        .contains(
            "idx_lanc_empresa_usuario_data",
            "idx_importacao_empresa_usuario",
            "idx_usuario_google_sub_unico",
            "idx_usuario_convite_token_hash");

    String definicaoIndiceGoogle =
        jdbc.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE schemaname = ? AND indexname ="
                + " 'idx_usuario_google_sub_unico'",
            String.class,
            schema);
    assertThat(definicaoIndiceGoogle)
        .contains("UNIQUE")
        .contains("WHERE")
        .contains("google_sub IS NOT NULL");

    String nullableEmailVerificado =
        jdbc.queryForObject(
            "SELECT is_nullable FROM information_schema.columns"
                + " WHERE table_schema = ? AND table_name = 'usuario'"
                + " AND column_name = 'email_verificado'",
            String.class,
            schema);
    assertThat(nullableEmailVerificado).isEqualTo("NO");
  }

  private void assertUnicidades(String schema) {
    Long empresa1 =
        jdbc.queryForObject(
            "INSERT INTO " + schema + ".empresa (nome) VALUES ('Empresa Um') RETURNING id",
            Long.class);
    Long empresa2 =
        jdbc.queryForObject(
            "INSERT INTO " + schema + ".empresa (nome) VALUES ('Empresa Dois') RETURNING id",
            Long.class);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO "
                        + schema
                        + ".usuario (empresa_id, email, senha_hash, nome, role, ativo)"
                        + " VALUES (?, ?, ?, ?, ?, true)",
                    empresa2,
                    "admin@controle.com",
                    "hash",
                    "Duplicado",
                    "USER"))
        .isInstanceOf(DataIntegrityViolationException.class);

    jdbc.update(
        "UPDATE " + schema + ".usuario SET google_sub = ? WHERE email = ?",
        "google-sub-unico",
        "admin@controle.com");
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".usuario (empresa_id, email, senha_hash, nome, role, ativo)"
            + " VALUES (?, ?, ?, ?, ?, true)",
        empresa1,
        "outro@teste.com",
        "hash",
        "Outro",
        "USER");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE " + schema + ".usuario SET google_sub = ? WHERE email = ?",
                    "google-sub-unico",
                    "outro@teste.com"))
        .isInstanceOf(DataIntegrityViolationException.class);

    jdbc.update(
        "INSERT INTO "
            + schema
            + ".usuario (empresa_id, email, senha_hash, nome, role, ativo)"
            + " VALUES (?, ?, ?, ?, ?, true)",
        empresa2,
        "sem-google-um@teste.com",
        "hash",
        "Sem Google Um",
        "USER");
    jdbc.update(
        "INSERT INTO "
            + schema
            + ".usuario (empresa_id, email, senha_hash, nome, role, ativo)"
            + " VALUES (?, ?, ?, ?, ?, true)",
        empresa2,
        "sem-google-dois@teste.com",
        "hash",
        "Sem Google Dois",
        "USER");
  }
}
