package com.controlefinanceiro.db.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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
class V12HelpdeskMigrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void v12CriaEstruturaDoHelpdesk() {
    assertThat(colunas("helpdesk_ticket"))
        .containsExactlyInAnyOrder(
            "id",
            "empresa_id",
            "usuario_id",
            "assunto",
            "descricao",
            "status",
            "criado_em",
            "atualizado_em");
    assertThat(colunas("helpdesk_mensagem"))
        .containsExactlyInAnyOrder(
            "id", "ticket_id", "empresa_id", "usuario_id", "origem", "conteudo", "criado_em");

    assertThat(nullabilidade("helpdesk_ticket", "empresa_id")).isEqualTo("NO");
    assertThat(nullabilidade("helpdesk_ticket", "usuario_id")).isEqualTo("NO");
    assertThat(nullabilidade("helpdesk_ticket", "status")).isEqualTo("NO");
    assertThat(nullabilidade("helpdesk_ticket", "atualizado_em")).isEqualTo("YES");
    assertThat(nullabilidade("helpdesk_mensagem", "ticket_id")).isEqualTo("NO");
    assertThat(nullabilidade("helpdesk_mensagem", "empresa_id")).isEqualTo("NO");
    assertThat(nullabilidade("helpdesk_mensagem", "usuario_id")).isEqualTo("NO");
    assertThat(nullabilidade("helpdesk_mensagem", "origem")).isEqualTo("NO");

    assertThat(indices()).contains("idx_helpdesk_ticket_empresa_usuario_status");
    String definicaoIndice =
        jdbc.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public' AND indexname ="
                + " 'idx_helpdesk_ticket_empresa_usuario_status'",
            String.class);
    assertThat(definicaoIndice).contains("(empresa_id, usuario_id, status)");

    assertThat(restricoes("helpdesk_ticket", "f"))
        .contains(
            "FOREIGN KEY (empresa_id) REFERENCES empresa(id)",
            "FOREIGN KEY (usuario_id) REFERENCES usuario(id)");
    assertThat(restricoes("helpdesk_mensagem", "f"))
        .contains(
            "FOREIGN KEY (ticket_id) REFERENCES helpdesk_ticket(id) ON DELETE CASCADE",
            "FOREIGN KEY (empresa_id) REFERENCES empresa(id)",
            "FOREIGN KEY (usuario_id) REFERENCES usuario(id)");
  }

  @Test
  void v12ValidaStatusEOrigemERemoveMensagensEmCascade() {
    Long empresaId =
        jdbc.queryForObject(
            "INSERT INTO empresa (nome) VALUES ('Empresa Helpdesk') RETURNING id", Long.class);
    Long usuarioId =
        jdbc.queryForObject(
            "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo)"
                + " VALUES (?, ?, ?, ?, 'USER', true) RETURNING id",
            Long.class,
            empresaId,
            "helpdesk@teste.com",
            "hash",
            "Usuario Helpdesk");

    Long ticketPadrao =
        jdbc.queryForObject(
            "INSERT INTO helpdesk_ticket (empresa_id, usuario_id, assunto, descricao)"
                + " VALUES (?, ?, ?, ?) RETURNING id",
            Long.class,
            empresaId,
            usuarioId,
            "Sem status",
            "Status padrao");
    assertThat(statusDoTicket(ticketPadrao)).isEqualTo("ABERTO");

    for (String status : List.of("ABERTO", "EM_ANDAMENTO", "RESOLVIDO", "FECHADO")) {
      Long ticketId = criarTicket(empresaId, usuarioId, "Assunto " + status, status);
      Long mensagemId = criarMensagem(ticketId, empresaId, usuarioId, "USER", "Resposta" + status);
      String origem =
          jdbc.queryForObject(
              "SELECT origem FROM helpdesk_mensagem WHERE id = ?", String.class, mensagemId);
      assertThat(origem).isEqualTo("USER");
    }

    assertThatThrownBy(() -> criarTicket(empresaId, usuarioId, "Status invalido", "CANCELADO"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> criarMensagem(ticketPadrao, empresaId, usuarioId, "SISTEMA", "Origem invalida"))
        .isInstanceOf(DataIntegrityViolationException.class);

    jdbc.update("DELETE FROM helpdesk_ticket WHERE empresa_id = ?", empresaId);
    Integer mensagensRestantes =
        jdbc.queryForObject(
            "SELECT count(*) FROM helpdesk_mensagem WHERE empresa_id = ?",
            Integer.class,
            empresaId);
    assertThat(mensagensRestantes).isZero();
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

  private String statusDoTicket(Long ticketId) {
    return jdbc.queryForObject(
        "SELECT status FROM helpdesk_ticket WHERE id = ?", String.class, ticketId);
  }

  private Long criarTicket(Long empresaId, Long usuarioId, String assunto, String status) {
    return jdbc.queryForObject(
        "INSERT INTO helpdesk_ticket (empresa_id, usuario_id, assunto, descricao, status)"
            + " VALUES (?, ?, ?, ?, ?) RETURNING id",
        Long.class,
        empresaId,
        usuarioId,
        assunto,
        "Descricao " + assunto,
        status);
  }

  private Long criarMensagem(
      Long ticketId, Long empresaId, Long usuarioId, String origem, String conteudo) {
    return jdbc.queryForObject(
        "INSERT INTO helpdesk_mensagem (ticket_id, empresa_id, usuario_id, origem, conteudo)"
            + " VALUES (?, ?, ?, ?, ?) RETURNING id",
        Long.class,
        ticketId,
        empresaId,
        usuarioId,
        origem,
        conteudo);
  }
}
