package com.controlefinanceiro.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  private static final String NUMERO = "+5571999888777";
  private static final String OUTRO_NUMERO = "+5571888777666";
  private static final String NUMERO_NAO_VERIFICADO = "+5571777666555";
  private static final String EMAIL = "whatsapp-fixture@controle.com";
  private static final String OUTRO_EMAIL = "whatsapp-outra-fixture@controle.com";
  private static final int ESPERA_ASSINCRONA_SEGUNDOS = 20;
  private static final long ESPERA_ASSINCRONA_MS = 20_000L;

  @Autowired private JdbcTemplate jdbc;
  @Autowired private UsuarioTelefoneRepository telefoneRepository;
  @Autowired private WhatsappConfirmacaoPendenteRepository confirmacaoRepository;
  @Autowired private WhatsappWebhookService webhookService;

  @MockBean private WhatsappEnvioService envioService;
  @MockBean private com.controlefinanceiro.ai.AiService aiService;

  @org.springframework.boot.test.mock.mockito.SpyBean
  private com.controlefinanceiro.lancamento.LancamentoService lancamentoService;

  private Long empresaId;
  private Long usuarioId;
  private Long categoriaId;
  private Long telefoneId;
  private Long empresaOutraId;
  private Long usuarioOutraId;
  private Long categoriaOutraId;
  private Long telefoneOutraId;
  private Long telefoneNaoVerificadoId;

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

    empresaOutraId =
        jdbc.queryForObject(
            "INSERT INTO empresa (nome) VALUES (?) RETURNING id",
            Long.class,
            "Empresa WhatsApp Outra " + System.nanoTime());
    usuarioOutraId =
        jdbc.queryForObject(
            "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) "
                + "VALUES (?, ?, ?, ?, 'USER', true) RETURNING id",
            Long.class,
            empresaOutraId,
            OUTRO_EMAIL,
            "hash-fixture",
            "Usuario Outra Empresa");
    categoriaOutraId =
        jdbc.queryForObject(
            "INSERT INTO categoria (empresa_id, nome, tipo) VALUES (?, ?, 'SAIDA') RETURNING id",
            Long.class,
            empresaOutraId,
            "Alimentacao");
    telefoneOutraId =
        jdbc.queryForObject(
            "INSERT INTO usuario_telefone "
                + "(empresa_id, usuario_id, numero_e164, verificado_em, ativo) "
                + "VALUES (?, ?, ?, now(), true) RETURNING id",
            Long.class,
            empresaOutraId,
            usuarioOutraId,
            OUTRO_NUMERO);
    telefoneNaoVerificadoId =
        jdbc.queryForObject(
            "INSERT INTO usuario_telefone (empresa_id, usuario_id, numero_e164, ativo) "
                + "VALUES (?, ?, ?, true) RETURNING id",
            Long.class,
            empresaId,
            usuarioId,
            NUMERO_NAO_VERIFICADO);
  }

  @AfterEach
  void limpar() {
    jdbc.update("DELETE FROM lancamento WHERE empresa_id IN (?, ?)", empresaId, empresaOutraId);
    jdbc.update("DELETE FROM whatsapp_confirmacao_pendente");
    jdbc.update("DELETE FROM whatsapp_mensagem_processada");
    jdbc.update("DELETE FROM whatsapp_codigo_verificacao");
    jdbc.update("DELETE FROM auditoria_telefone");
    jdbc.update("DELETE FROM usuario_telefone");
    jdbc.update("DELETE FROM categoria WHERE empresa_id IN (?, ?)", empresaId, empresaOutraId);
    jdbc.update("DELETE FROM usuario WHERE email IN (?, ?)", EMAIL, OUTRO_EMAIL);
    jdbc.update("DELETE FROM empresa WHERE id IN (?, ?)", empresaId, empresaOutraId);
  }

  @Test
  void confirmacaoPorTextoSimGravaLancamentoComEmpresaEUsuarioDoTelefone() {
    Long pendenciaId = criarPendencia();
    org.mockito.ArgumentCaptor<com.controlefinanceiro.lancamento.LancamentoRequest> reqCaptor =
        org.mockito.ArgumentCaptor.forClass(
            com.controlefinanceiro.lancamento.LancamentoRequest.class);

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
    verify(lancamentoService)
        .criarComCanal(
            eq(empresaId), eq(usuarioId), reqCaptor.capture(), eq("WHATSAPP"), eq("wamid-sim-1"));
    verify(envioService).enviarTexto(eq(NUMERO), contains("gravado"), eq("wamid-sim-1"));
    org.mockito.InOrder ordem = org.mockito.Mockito.inOrder(lancamentoService, envioService);
    ordem
        .verify(lancamentoService)
        .criarComCanal(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    ordem.verify(envioService).enviarTexto(eq(NUMERO), contains("gravado"), eq("wamid-sim-1"));
  }

  @Test
  void falhaAoCriarLancamentoNaoConfirmaPendenciaENaoEnviaSucesso() {
    Long pendenciaId = criarPendencia();
    org.mockito.Mockito.doThrow(new RuntimeException("falha simulada criarComCanal"))
        .when(lancamentoService)
        .criarComCanal(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                webhookService.processarConfirmacaoTexto(
                    "wamid-falha-1", NUMERO, "sim " + pendenciaId))
        .isInstanceOf(RuntimeException.class);

    assertThat(status(pendenciaId)).isEqualTo("PENDENTE");
    assertThat(contarLancamentos(empresaId)).isZero();
    verify(envioService, org.mockito.Mockito.never())
        .enviarTexto(
            org.mockito.ArgumentMatchers.any(),
            contains("gravado"),
            org.mockito.ArgumentMatchers.any());
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

  @Test
  void mensagemSimDeNumeroNaoVerificadoNaoConfirmaPendencia() throws Exception {
    Long pendenciaId = criarPendencia(telefoneNaoVerificadoId);
    String messageId = "wamid-nao-verificado-1";

    webhookService.processarWebhook(payloadTexto(messageId, NUMERO_NAO_VERIFICADO, "sim"), "sig");

    aguardarResultado(messageId, "NAO_VERIFICADO");
    assertThat(status(pendenciaId)).isEqualTo("PENDENTE");
    assertThat(contarLancamentos(empresaId)).isZero();
    verify(envioService, timeout(ESPERA_ASSINCRONA_MS))
        .enviarInstrucaoCadastro(eq(NUMERO_NAO_VERIFICADO), eq(messageId));
  }

  @Test
  void duplicataConcorrenteComDoisWamidDiferentesGravaUmUnicoLancamento() throws Exception {
    for (int tentativa = 1; tentativa <= 5; tentativa++) {
      reset(envioService);
      Long pendenciaId = criarPendencia();
      String primeiro = "wamid-race-" + tentativa + "-a";
      String segundo = "wamid-race-" + tentativa + "-b";
      CyclicBarrier barreira = new CyclicBarrier(2);
      List<Throwable> erros = Collections.synchronizedList(new ArrayList<>());

      Thread primeiraThread = threadWebhook(primeiro, barreira, erros);
      Thread segundaThread = threadWebhook(segundo, barreira, erros);
      primeiraThread.start();
      segundaThread.start();
      primeiraThread.join();
      segundaThread.join();

      assertThat(erros).isEmpty();
      aguardarResultados(primeiro, segundo, erros);

      assertThat(status(pendenciaId)).isEqualTo("CONFIRMADA");
      Integer lancamentos =
          jdbc.queryForObject(
              "SELECT count(*) FROM lancamento WHERE empresa_id = ? AND message_id_origem IN (?, ?)",
              Integer.class,
              empresaId,
              primeiro,
              segundo);
      assertThat(lancamentos).isEqualTo(1);

      ArgumentCaptor<String> textos = ArgumentCaptor.forClass(String.class);
      verify(envioService, timeout(ESPERA_ASSINCRONA_MS).times(2))
          .enviarTexto(eq(NUMERO), textos.capture(), anyString());
      assertThat(textos.getAllValues())
          .filteredOn(texto -> texto.contains("já foi processado"))
          .hasSize(1);
      assertThat(textos.getAllValues()).filteredOn(texto -> texto.contains("gravado")).hasSize(1);
    }
  }


  @Test
  void mensagemTextoDeNumeroVerificadoRegistraEEnviaResposta() throws Exception {
    org.mockito.Mockito.when(
            aiService.processar(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(
            new com.controlefinanceiro.ai.AiResponse(
                "consulta",
                "Seu saldo atual é R$ 100,00",
                java.util.List.of(),
                null,
                java.util.List.of(),
                java.util.List.of()));

    String messageId = "wamid-texto-comum-1";
    webhookService.processarWebhook(payloadTexto(messageId, NUMERO, "qual meu saldo?"), "sig");

    aguardarResultado(messageId, "CONSULTA");
    verify(envioService, timeout(ESPERA_ASSINCRONA_MS))
        .enviarTexto(eq(NUMERO), contains("saldo"), eq(messageId));
  }

  @Test
  void confirmacaoComTelefoneVerificadoDeOutraEmpresaNaoGravaNaEmpresaDaPendencia()
      throws Exception {
    Long pendenciaId = criarPendencia();
    String messageId = "wamid-cross-tenant-1";

    webhookService.processarWebhook(payloadTexto(messageId, OUTRO_NUMERO, "sim"), "sig");

    aguardarResultado(messageId, "DUPLICADA");
    assertThat(status(pendenciaId)).isEqualTo("PENDENTE");
    assertThat(contarLancamentos(empresaId)).isZero();
    assertThat(contarLancamentos(empresaOutraId)).isZero();
    verify(envioService, timeout(ESPERA_ASSINCRONA_MS))
        .enviarTexto(eq(OUTRO_NUMERO), contains("já foi processado"), eq(messageId));
  }

  private Thread threadWebhook(String messageId, CyclicBarrier barreira, List<Throwable> erros) {
    return new Thread(
        () -> {
          try {
            barreira.await();
            webhookService.processarWebhook(payloadTexto(messageId, NUMERO, "sim"), "sig");
          } catch (Exception e) {
            erros.add(e);
          }
        });
  }

  private String payloadTexto(String messageId, String from, String texto) {
    return ("{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"entry-1\","
            + "\"changes\":[{\"value\":{\"messages\":[{\"id\":\"%s\",\"from\":\"%s\","
            + "\"text\":{\"body\":\"%s\"}}]}}]}]}")
        .formatted(messageId, from, texto);
  }

  private void aguardarResultado(String messageId, String resultadoEsperado)
      throws InterruptedException {
    long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(ESPERA_ASSINCRONA_SEGUNDOS);
    String ultimo = null;
    while (System.nanoTime() < limite) {
      ultimo =
          jdbc
              .query(
                  "SELECT resultado FROM whatsapp_mensagem_processada WHERE message_id = ?",
                  (rs, rowNum) -> rs.getString(1),
                  messageId)
              .stream()
              .findFirst()
              .orElse(null);
      if (resultadoEsperado.equals(ultimo)) {
        return;
      }
      Thread.sleep(20);
    }
    assertThat(ultimo).as("resultado da mensagem %s", messageId).isEqualTo(resultadoEsperado);
  }

  private void aguardarResultados(String primeiro, String segundo, List<Throwable> erros)
      throws InterruptedException {
    long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(ESPERA_ASSINCRONA_SEGUNDOS);
    while (System.nanoTime() < limite) {
      Integer processadas =
          jdbc.queryForObject(
              "SELECT count(*) FROM whatsapp_mensagem_processada WHERE message_id IN (?, ?)",
              Integer.class,
              primeiro,
              segundo);
      if (processadas != null && processadas == 2) {
        return;
      }
      Thread.sleep(20);
    }
    Integer processadas =
        jdbc.queryForObject(
            "SELECT count(*) FROM whatsapp_mensagem_processada WHERE message_id IN (?, ?)",
            Integer.class,
            primeiro,
            segundo);
    List<String> resultados =
        jdbc.query(
            "SELECT message_id || '=' || resultado FROM whatsapp_mensagem_processada "
                + "WHERE message_id IN (?, ?)",
            (rs, rowNum) -> rs.getString(1),
            primeiro,
            segundo);
    assertThat(processadas)
        .as(
            "mensagens processadas %s e %s; resultados=%s; erros=%s",
            primeiro, segundo, resultados, erros)
        .isEqualTo(2);
  }

  private int contarLancamentos(Long idEmpresa) {
    Integer total =
        jdbc.queryForObject(
            "SELECT count(*) FROM lancamento WHERE empresa_id = ?", Integer.class, idEmpresa);
    return total == null ? 0 : total;
  }

  private Long criarPendencia() {
    return criarPendencia(telefoneId);
  }

  private Long criarPendencia(Long idTelefone) {
    WhatsappConfirmacaoPendente pendencia = new WhatsappConfirmacaoPendente();
    pendencia.setUsuarioTelefone(telefoneRepository.findById(idTelefone).orElseThrow());
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
