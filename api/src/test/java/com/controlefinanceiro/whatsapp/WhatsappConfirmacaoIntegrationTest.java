package com.controlefinanceiro.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WhatsappConfirmacaoIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final String NUMERO = "+5571999888777";
  private static final String EMAIL = "whatsapp-fixture@controle.com";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private UsuarioTelefoneRepository telefoneRepository;
  @Autowired private WhatsappConfirmacaoPendenteRepository confirmacaoRepository;
  @Autowired private WhatsappWebhookService webhookService;

  @MockBean private WhatsappEnvioService envioService;

  private Long empresaId;
  private Long usuarioId;
  private Long categoriaId;
  private Long telefoneId;

  @BeforeEach
  void setup() {
    empresaId =
        jdbc.queryForObject(
            "INSERT INTO empresa (nome) VALUES (?) RETURNING id",
            Long.class,
            "Empresa WhatsApp Teste " + System.nanoTime());
    usuarioId =
        jdbc.queryForObject(
            "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) "
                + "VALUES (?, ?, ?, ?, 'USER', true) RETURNING id",
            Long.class,
            empresaId,
            EMAIL,
            "hash-fixture",
            "Usuario WhatsApp");
    categoriaId =
        jdbc.queryForObject(
            "INSERT INTO categoria (empresa_id, nome, tipo) VALUES (?, ?, 'SAIDA') RETURNING id",
            Long.class,
            empresaId,
            "Alimentacao");
    telefoneId =
        jdbc.queryForObject(
            "INSERT INTO usuario_telefone "
                + "(empresa_id, usuario_id, numero_e164, verificado_em, ativo) "
                + "VALUES (?, ?, ?, now(), true) RETURNING id",
            Long.class,
            empresaId,
            usuarioId,
            NUMERO);
  }

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM lancamento WHERE empresa_id = ?", empresaId);
    jdbc.update("DELETE FROM whatsapp_confirmacao_pendente");
    jdbc.update("DELETE FROM whatsapp_mensagem_processada");
    jdbc.update("DELETE FROM whatsapp_codigo_verificacao");
    jdbc.update("DELETE FROM auditoria_telefone");
    jdbc.update("DELETE FROM usuario_telefone");
    jdbc.update("DELETE FROM categoria WHERE empresa_id = ?", empresaId);
    jdbc.update("DELETE FROM usuario WHERE email = ?", EMAIL);
    jdbc.update("DELETE FROM empresa WHERE id = ?", empresaId);
  }

  @Test
  void confirmacaoPorTextoSimGravaLancamentoComEmpresaEUsuarioDoTelefone() {
    Long pendenciaId = criarPendencia();

    webhookService.processarConfirmacaoTexto("wamid-sim-1", NUMERO, "sim");

    assertThat(status(pendenciaId)).isEqualTo("CONFIRMADA");
    Integer lancamentos =
        jdbc.queryForObject(
            "SELECT count(*) FROM lancamento WHERE empresa_id = ? AND usuario_id = ? "
                + "AND message_id_origem = ? AND canal = 'WHATSAPP'",
            Integer.class,
            empresaId,
            usuarioId,
            "wamid-sim-1");
    assertThat(lancamentos).isEqualTo(1);
    verify(envioService).enviarTexto(eq(NUMERO), contains("gravado"), eq("wamid-sim-1"));
  }

  @Test
  void segundaConfirmacaoNaoGravaLancamentoDuplicado() {
    Long pendenciaId = criarPendencia();

    webhookService.processarConfirmacaoTexto("wamid-sim-2", NUMERO, "sim");
    webhookService.processarConfirmacaoTexto("wamid-sim-3", NUMERO, "sim");

    assertThat(status(pendenciaId)).isEqualTo("CONFIRMADA");
    Integer lancamentos =
        jdbc.queryForObject(
            "SELECT count(*) FROM lancamento WHERE empresa_id = ? AND message_id_origem LIKE 'wamid-sim-%'",
            Integer.class, empresaId);
    assertThat(lancamentos).isEqualTo(1);
  }

  @Test
  void pendenciaExpiradaNaoGravaLancamento() {
    Long pendenciaId = criarPendencia();
    jdbc.update(
        "UPDATE whatsapp_confirmacao_pendente SET expira_em = now() - interval '1 minute' WHERE id = ?",
        pendenciaId);

    webhookService.processarConfirmacaoTexto("wamid-sim-4", NUMERO, "sim");

    assertThat(status(pendenciaId)).isEqualTo("EXPIRADA");
    Integer lancamentos =
        jdbc.queryForObject(
            "SELECT count(*) FROM lancamento WHERE empresa_id = ?", Integer.class, empresaId);
    assertThat(lancamentos).isZero();
  }

  @Test
  void multiplasPendenciasSemIdPedeIdentificacao() {
    criarPendencia();
    Long segunda = criarPendencia();

    webhookService.processarConfirmacaoTexto("wamid-sim-5", NUMERO, "sim");

    assertThat(status(segunda)).isEqualTo("PENDENTE");
    Integer lancamentos =
        jdbc.queryForObject(
            "SELECT count(*) FROM lancamento WHERE empresa_id = ?", Integer.class, empresaId);
    assertThat(lancamentos).isZero();
    verify(envioService).enviarTexto(eq(NUMERO), contains("sim <id>"), eq("wamid-sim-5"));
  }

  @Test
  void confirmacaoComIdExplicativoConfirmaApenasAPendenciaIndicada() {
    criarPendencia();
    Long segunda = criarPendencia();

    webhookService.processarConfirmacaoTexto("wamid-sim-6", NUMERO, "sim " + segunda);

    assertThat(status(segunda)).isEqualTo("CONFIRMADA");
    Integer lancamentos =
        jdbc.queryForObject(
            "SELECT count(*) FROM lancamento WHERE empresa_id = ? AND message_id_origem = ?",
            Integer.class,
            empresaId,
            "wamid-sim-6");
    assertThat(lancamentos).isEqualTo(1);
  }

  @Test
  void cancelamentoPorTextoNaoGravaLancamento() {
    Long pendenciaId = criarPendencia();

    webhookService.processarConfirmacaoTexto("wamid-nao-1", NUMERO, "nao " + pendenciaId);

    assertThat(status(pendenciaId)).isEqualTo("CANCELADA");
    Integer lancamentos =
        jdbc.queryForObject(
            "SELECT count(*) FROM lancamento WHERE empresa_id = ?", Integer.class, empresaId);
    assertThat(lancamentos).isZero();
    verify(envioService).enviarTexto(eq(NUMERO), contains("cancelado"), eq("wamid-nao-1"));
  }

  private Long criarPendencia() {
    WhatsappConfirmacaoPendente pendencia = new WhatsappConfirmacaoPendente();
    pendencia.setUsuarioTelefone(telefoneRepository.findById(telefoneId).orElseThrow());
    pendencia.setProposta(
        "{\"tipo\":\"SAIDA\",\"valor\":\"10.00\",\"data\":\"2026-11-19\","
            + "\"descricao\":\"teste\",\"categoriaSugerida\":\"Alimentacao\"}");
    pendencia.setExpiraEm(OffsetDateTime.now().plus(1, ChronoUnit.HOURS));
    pendencia.setStatus("PENDENTE");
    return confirmacaoRepository.saveAndFlush(pendencia).getId();
  }

  private String status(Long pendenciaId) {
    return jdbc.queryForObject(
        "SELECT status FROM whatsapp_confirmacao_pendente WHERE id = ?", String.class, pendenciaId);
  }
}
