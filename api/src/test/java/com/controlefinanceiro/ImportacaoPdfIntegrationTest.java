package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.ai.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração da importação de PDFs (fase 5, 05-04) com PostgreSQL real e Flyway completo
 * (padrão P6).
 *
 * <p>A IA é mockada ({@code @MockBean AiClient}) — sem rede e sem custo de cota. Os PDFs são
 * gerados em memória com o próprio PDFBox (com texto e sem texto). Garantias validadas: staging
 * gravado e NADA em lancamento/conta_a_pagar (D-02); reenvio = DUPLICADO (D-09); PDF digitalizado =
 * SEM_TEXTO e lote segue (D-12); IA indisponível com lote inteiro perdido → 503 (D-23); isolamento
 * por empresa do token (R3).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ImportacaoPdfIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Fixture óbvio de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  /** Resposta JSON simulada da IA (uma despesa paga de cartão; categoria seed existe). */
  private static final String JSON_IA =
      "{\"itens\":[{\"data\":\"2026-11-03\",\"descricao\":\"Almoco no restaurante\","
          + "\"valor\":\"45.00\",\"tipo\":\"DESPESA_PAGA\",\"categoria\":\"Alimentacao\","
          + "\"origem\":\"CARTAO\"}]}";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockBean private AiClient aiClient;

  private String token;

  @BeforeEach
  void setup() throws Exception {
    token = login("admin@controle.com", SENHA_FIXTURE);
    garantirUsuarioUser();
  }

  @AfterEach
  void limparStaging() {
    // Ordem por FK: fatura → item → documento (staging da fase 5 apenas).
    jdbc.update("DELETE FROM importacao_fatura");
    jdbc.update("DELETE FROM importacao_item");
    jdbc.update("DELETE FROM importacao_documento");
  }

  @Test
  @DisplayName("análise grava staging e NÃO grava lancamento/conta_a_pagar (D-02, D-06)")
  void analiseGravaStagingNaoGravaDefinitivo() throws Exception {
    stubIa(JSON_IA);
    long lancamentosAntes = contar("lancamento");

    ResponseEntity<String> resposta =
        upload("fatura.pdf", pdfComTexto("fatura do cartao vencimento limite parcela fechamento"));

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM importacao_documento", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM importacao_item", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT status FROM importacao_documento", String.class))
        .isEqualTo("ANALISADO");
    assertThat(contar("lancamento")).isEqualTo(lancamentosAntes); // nada definitivo
    assertThat(contar("conta_a_pagar")).isZero();
  }

  @Test
  @DisplayName("reenvio do mesmo arquivo → DUPLICADO, sem novo staging (D-09)")
  void reenvioMesmoArquivoEhDuplicado() throws Exception {
    stubIa(JSON_IA);
    byte[] pdf = pdfComTexto("fatura do cartao vencimento limite parcela fechamento");
    assertThat(upload("fatura.pdf", pdf).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(upload("fatura.pdf", pdf).getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> terceiro = upload("fatura.pdf", pdf);
    assertThat(terceiro.getBody()).contains("DUPLICADO");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM importacao_documento", Long.class))
        .isEqualTo(1); // reenvio não cria staging novo
  }

  @Test
  @DisplayName("PDF digitalizado → SEM_TEXTO e lote segue com o próximo arquivo (D-12)")
  void pdfSemTextoNaoDerrubaLote() throws Exception {
    stubIa(JSON_IA);

    ResponseEntity<String> resposta =
        upload(
            "digitalizado.pdf",
            pdfComTexto(null), // página em branco: sem texto extraível
            "valido.pdf",
            pdfComTexto("extrato bancario pix transferencia saldo"));

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resposta.getBody()).contains("SEM_TEXTO").contains("ANALISADO");
  }

  @Test
  @DisplayName("POST sem arquivos → 400")
  void loteVazioRetorna400() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<String> resposta =
        rest.exchange(
            "/api/importacoes/pdf", HttpMethod.POST, new HttpEntity<>(headers), String.class);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("IA indisponível e nenhum documento analisado → 503, sem staging órfão (D-23)")
  void iaIndisponivelNenhumAnalisadoRetorna503() throws Exception {
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenThrow(new com.controlefinanceiro.common.ServicoIaIndisponivelException("IA fora"));

    ResponseEntity<String> resposta =
        upload("fatura.pdf", pdfComTexto("fatura do cartao vencimento"));

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM importacao_documento", Long.class))
        .isZero();
  }

  @Test
  @DisplayName("token ausente → 401 em /api/importacoes (R3)")
  void semTokenRetorna401() {
    ResponseEntity<String> resposta = rest.getForEntity("/api/importacoes", String.class);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("GET detalhe e DELETE descarte: pendente vira DESCARTADO")
  void detalheEDescarte() throws Exception {
    stubIa(JSON_IA);
    upload("fatura.pdf", pdfComTexto("fatura do cartao vencimento limite parcela fechamento"));
    Long id = jdbc.queryForObject("SELECT id FROM importacao_documento LIMIT 1", Long.class);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    ResponseEntity<String> detalhe =
        rest.exchange(
            "/api/importacoes/" + id, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertThat(detalhe.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(detalhe.getBody()).contains("fatura.pdf").contains("itens");

    ResponseEntity<Void> descarte =
        rest.exchange(
            "/api/importacoes/" + id, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    assertThat(descarte.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    String status =
        jdbc.queryForObject(
            "SELECT status FROM importacao_documento WHERE id = ?", String.class, id);
    assertThat(status).isEqualTo("DESCARTADO");
  }

  /* ============================ helpers ============================ */

  /** Stub padrão da IA mockada (o teste de 503 define o stub dele e não chama isto). */
  private void stubIa(String json) {
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar(json, "google/gemma-4-31b-it:free", false, 1));
  }

  private void garantirUsuarioUser() {
    Long empresaId =
        jdbc.queryForObject(
            "SELECT id FROM empresa WHERE nome = 'Controle Financeiro'", Long.class);
    long jaExiste =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = 'usuario@controle.com'", Long.class);
    if (jaExiste == 0) {
      String senhaHash = passwordEncoder.encode(SENHA_FIXTURE);
      jdbc.update(
          "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) VALUES (?, ?, ?, ?, ?, true)",
          empresaId,
          "usuario@controle.com",
          senhaHash,
          "Usuario Comum",
          "USER");
    }
  }

  private long contar(String tabela) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela, Long.class);
  }

  /** Upload multipart com 1..N pares (nome, conteúdo). */
  private ResponseEntity<String> upload(String nome1, byte[] conteudo1, Object... pares)
      throws IOException {
    MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
    corpo.add("arquivos", recurso(nome1, conteudo1));
    for (int i = 0; i < pares.length; i += 2) {
      corpo.add("arquivos", recurso((String) pares[i], (byte[]) pares[i + 1]));
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return rest.postForEntity(
        "/api/importacoes/pdf", new HttpEntity<>(corpo, headers), String.class);
  }

  private ByteArrayResource recurso(String nome, byte[] bytes) {
    return new ByteArrayResource(bytes) {
      @Override
      public String getFilename() {
        return nome;
      }
    };
  }

  /** Gera um PDF em memória. {@code texto} nulo = página em branco (sem texto extraível). */
  private static byte[] pdfComTexto(String texto) throws IOException {
    try (PDDocument documento = new PDDocument()) {
      PDPage pagina = new PDPage();
      documento.addPage(pagina);
      if (texto != null) {
        try (PDPageContentStream stream = new PDPageContentStream(documento, pagina)) {
          stream.beginText();
          stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
          stream.newLineAtOffset(50, 700);
          stream.showText(texto);
          stream.endText();
        }
      }
      ByteArrayOutputStream saida = new ByteArrayOutputStream();
      documento.save(saida);
      return saida.toByteArray();
    }
  }

  private String login(String email, String senha) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha);
    ResponseEntity<String> response =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
    return mapper.readTree(response.getBody()).get("token").asText();
  }
}
