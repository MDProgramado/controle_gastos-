package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.ai.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
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
import org.springframework.boot.test.web.server.LocalServerPort;
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
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  /** Fixture óbvio de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  /** Resposta JSON simulada da IA (uma despesa paga de cartão; categoria seed existe). */
  private static final String JSON_IA =
      "{\"itens\":[{\"data\":\"2026-11-03\",\"descricao\":\"Almoco no restaurante\","
          + "\"valor\":\"45.00\",\"tipo\":\"DESPESA_PAGA\",\"categoria\":\"Alimentacao\","
          + "\"origem\":\"CARTAO\"}]}";

  /** Resposta para o teste de confirmação: 3 tipos de item + cabeçalho de fatura. */
  private static final String JSON_IA_CONFIRMACAO =
      "{\"itens\":["
          + "{\"data\":\"2026-11-03\",\"descricao\":\"Salario\",\"valor\":\"1000.00\","
          + "\"tipo\":\"RECEITA\",\"categoria\":\"Rendimentos e reembolsos\",\"origem\":\"EXTRATO\"},"
          + "{\"data\":\"2026-11-03\",\"descricao\":\"Almoco\",\"valor\":\"45.00\","
          + "\"tipo\":\"DESPESA_PAGA\",\"categoria\":\"Alimentacao\",\"origem\":\"EXTRATO\"},"
          + "{\"data\":\"2026-11-05\",\"descricao\":\"Notebook em 3x\",\"valor\":\"500.00\","
          + "\"tipo\":\"A_PAGAR\",\"categoria\":\"Alimentacao\",\"origem\":\"CARTAO\","
          + "\"parcelaAtual\":1,\"parcelaTotal\":3}"
          + "],"
          + "\"fatura\":{\"total\":\"500.00\",\"vencimento\":\"2026-12-10\","
          + "\"mesReferencia\":\"2026-11-01\"}}";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  @Autowired private PasswordEncoder passwordEncoder;

  @MockBean private AiClient aiClient;

  @LocalServerPort private int localServerPort;

  private String token;

  @BeforeEach
  void setup() throws Exception {
    token = login("admin@controle.com", SENHA_FIXTURE);
    garantirUsuarioUser();
  }

  @AfterEach
  void limparStaging() {
    // Ordem por FK (dependentes primeiro, pai por último):
    // conta_a_pagar.fatura_id -> fatura_cartao.id
    // lancamento.documento_id, fatura_cartao.documento_id,
    // importacao_fatura.documento_id, importacao_item.documento_id
    // -> importacao_documento.id
    // (todas introduzidas na fase 5, V3, commit 589fcb6).
    jdbc.update("DELETE FROM conta_a_pagar");
    jdbc.update("DELETE FROM lancamento WHERE documento_id IS NOT NULL");
    jdbc.update("DELETE FROM fatura_cartao");
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
    assertThat(resposta.getBody())
        .contains("\"modeloUsado\"")
        .contains("\"degradado\"")
        .contains("\"resumo\"");

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
  @DisplayName("arquivo acima de 8 MB → 413")
  void arquivoAcimaDoLimiteRetorna413() throws Exception {
    byte[] arquivoGrande = new byte[9 * 1024 * 1024]; // 9 MB > limite de 8 MB
    new java.util.Random(42).nextBytes(arquivoGrande);

    // Constrói corpo multipart manualmente para definir Content-Length exato
    String boundary = "----Boundary_" + System.currentTimeMillis();
    ByteArrayOutputStream corpo = new ByteArrayOutputStream();
    corpo.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
    corpo.write(
        ("Content-Disposition: form-data; name=\"arquivos\"; filename=\"grande.pdf\"\r\n")
            .getBytes(StandardCharsets.UTF_8));
    corpo.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
    corpo.write(arquivoGrande);
    corpo.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    byte[] corpoBytes = corpo.toByteArray();

    HttpClient client = HttpClient.newBuilder().build();
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + localServerPort + "/api/importacoes/pdf"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(BodyPublishers.ofByteArray(corpoBytes))
            .build();
    HttpResponse<String> resposta = client.send(request, BodyHandlers.ofString());

    assertThat(resposta.statusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
    assertThat(resposta.body()).contains("excede");
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
  @DisplayName("token ausente → 401 em todos os endpoints de /api/importacoes (R3)")
  void semTokenRetorna401() {
    HttpHeaders vazios = new HttpHeaders();

    assertThat(rest.getForEntity("/api/importacoes", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(rest.getForEntity("/api/importacoes/1", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            rest.postForEntity("/api/importacoes/pdf", new HttpEntity<>(vazios), String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            rest.postForEntity(
                    "/api/importacoes/1/confirmar",
                    new HttpEntity<>("{\"itens\":[]}", vazios),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            rest.exchange(
                    "/api/importacoes/1", HttpMethod.DELETE, new HttpEntity<>(vazios), Void.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName(
      "item inválido na resposta da IA vai para rejeitados e não derruba o documento (D-21)")
  void itemInvalidoVaiParaRejeitados() throws Exception {
    String json =
        "{\"itens\":["
            + "{\"data\":\"2026-11-03\",\"descricao\":\"Almoco\",\"valor\":\"45.00\","
            + "\"tipo\":\"DESPESA_PAGA\",\"categoria\":\"Alimentacao\",\"origem\":\"CARTAO\"},"
            + "{\"data\":\"2026-11-04\",\"descricao\":\"Estorno\",\"valor\":\"-5.00\","
            + "\"tipo\":\"DESPESA_PAGA\",\"categoria\":\"Alimentacao\",\"origem\":\"CARTAO\"}"
            + "]}";
    stubIa(json);

    ResponseEntity<String> resposta =
        upload("fatura.pdf", pdfComTexto("fatura do cartao vencimento limite parcela fechamento"));

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resposta.getBody()).contains("VALOR_INVALIDO");
    // item válido foi gravado; o inválido virou rejeitado (nada do item ruim em staging)
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM importacao_item", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM importacao_documento", Long.class))
        .isEqualTo(1);
  }

  @Test
  @DisplayName("isolamento: empresa A não vê nem acessa importação da empresa B (R3)")
  void isolamentoEntreEmpresas() throws Exception {
    stubIa(JSON_IA);
    Long empresaB = garantirUsuarioEmpresaB();
    String tokenB = tokenFixado("outra@controle.com", empresaB, "USER");

    // usuário B importa um documento → staging fica na empresa B (nunca na 1 do seed)
    HttpHeaders headersB = new HttpHeaders();
    headersB.setBearerAuth(tokenB);
    headersB.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> corpoB = new LinkedMultiValueMap<>();
    corpoB.add(
        "arquivos",
        recurso(
            "fatura-b.pdf", pdfComTexto("fatura do cartao vencimento limite parcela fechamento")));
    ResponseEntity<String> uploadB =
        rest.postForEntity(
            "/api/importacoes/pdf", new HttpEntity<>(corpoB, headersB), String.class);
    assertThat(uploadB.getStatusCode()).isEqualTo(HttpStatus.OK);

    Long empresaDoDocumento =
        jdbc.queryForObject("SELECT empresa_id FROM importacao_documento LIMIT 1", Long.class);
    assertThat(empresaDoDocumento).isEqualTo(empresaB).isNotEqualTo(1L);
    Long idB = jdbc.queryForObject("SELECT id FROM importacao_documento LIMIT 1", Long.class);

    // empresa A (admin do seed): histórico não traz o documento de B; detalhe e descarte → 404
    HttpHeaders headersA = new HttpHeaders();
    headersA.setBearerAuth(token);
    ResponseEntity<String> historico =
        rest.exchange("/api/importacoes", HttpMethod.GET, new HttpEntity<>(headersA), String.class);
    assertThat(historico.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(historico.getBody()).doesNotContain("fatura-b.pdf");

    assertThat(
            rest.exchange(
                    "/api/importacoes/" + idB,
                    HttpMethod.GET,
                    new HttpEntity<>(headersA),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(
            rest.exchange(
                    "/api/importacoes/" + idB,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headersA),
                    Void.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    // e a confirmação cruzada também é 404 (confirmar é endpoint separado, 05-05)
    HttpHeaders jsonA = new HttpHeaders();
    jsonA.setBearerAuth(token);
    jsonA.setContentType(MediaType.APPLICATION_JSON);
    assertThat(
            rest.postForEntity(
                    "/api/importacoes/" + idB + "/confirmar",
                    new HttpEntity<>("{\"itens\":[]}", jsonA),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
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

  @Test
  @DisplayName("confirmação grava lancamento (ENTRADA/SAIDA) + conta_a_pagar; 2ª vez idempotente")
  void confirmacaoTudoOuNadaEIdempotente() throws Exception {
    stubIa(JSON_IA_CONFIRMACAO);
    upload("fatura.pdf", pdfComTexto("fatura do cartao vencimento limite parcela fechamento"));
    List<Long> itemIds =
        jdbc.queryForList("SELECT id FROM importacao_item ORDER BY id", Long.class);
    assertThat(itemIds).hasSize(3);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo =
        "{\"itens\":[%s]}"
            .formatted(
                itemIds.stream()
                    .map(id -> "{\"itemId\":%d,\"incluir\":true}".formatted(id))
                    .reduce((a, b) -> a + "," + b)
                    .orElse(""));

    ResponseEntity<String> primeira =
        rest.postForEntity(
            "/api/importacoes/{id}/confirmar",
            new HttpEntity<>(corpo, headers),
            String.class,
            jdbc.queryForObject("SELECT id FROM importacao_documento LIMIT 1", Long.class));
    assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(primeira.getBody()).contains("\"importados\":3");

    // definitivos: 2 lancamentos (ENTRADA/SAIDA) com rastreio completo + 1 conta a pagar
    assertThat(contar("lancamento")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM lancamento WHERE tipo = 'ENTRADA'", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM lancamento WHERE tipo = 'SAIDA'", Long.class))
        .isEqualTo(1);
    // D-08/D-05: chave_dedupe, origem e documento_id preenchidos; NENHUM tipo A_PAGAR
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM lancamento WHERE chave_dedupe IS NOT NULL", Long.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM lancamento WHERE origem IS NULL OR documento_id IS NULL",
                Long.class))
        .isZero();
    assertThat(contar("conta_a_pagar")).isEqualTo(1);
    assertThat(contar("fatura_cartao")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM conta_a_pagar WHERE fatura_id IS NOT NULL", Long.class))
        .isEqualTo(1);

    // idempotência: segunda confirmação não duplica nada (D-07)
    ResponseEntity<String> segunda =
        rest.postForEntity(
            "/api/importacoes/{id}/confirmar",
            new HttpEntity<>(corpo, headers),
            String.class,
            jdbc.queryForObject("SELECT id FROM importacao_documento LIMIT 1", Long.class));
    assertThat(segunda.getBody()).contains("\"importados\":0").contains("\"duplicados\":3");
    assertThat(contar("lancamento")).isEqualTo(2);
    assertThat(contar("conta_a_pagar")).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT status FROM importacao_documento", String.class))
        .isEqualTo("CONFIRMADO");
  }

  @Test
  @DisplayName("confirmação de documento inexistente → 404 (R3)")
  void confirmarInexistenteRetorna404() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> resposta =
        rest.postForEntity(
            "/api/importacoes/999999/confirmar",
            new HttpEntity<>("{\"itens\":[]}", headers),
            String.class);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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

  /**
   * Empresa B + usuário B (outra@controle.com) para o teste de isolamento — idempotente entre os
   * testes da classe (o container PostgreSQL vive para a classe inteira).
   */
  private Long garantirUsuarioEmpresaB() {
    long jaExiste =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = 'outra@controle.com'", Long.class);
    if (jaExiste == 0) {
      jdbc.update("INSERT INTO empresa (nome) VALUES ('Empresa B Isolamento')");
      Long empresaB =
          jdbc.queryForObject(
              "SELECT id FROM empresa WHERE nome = 'Empresa B Isolamento'", Long.class);
      jdbc.update(
          "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) VALUES (?, ?, ?, ?, ?, true)",
          empresaB,
          "outra@controle.com",
          "senha-nao-usada-jwt-direto",
          "Usuario Empresa B",
          "USER");
    }
    return jdbc.queryForObject(
        "SELECT empresa_id FROM usuario WHERE email = 'outra@controle.com'", Long.class);
  }

  /** Assina um JWT fixture HS256 válido (mesmo padrão de AuthTenantIntegrationTest). */
  private String tokenFixado(String email, long empresaId, String role) throws Exception {
    String segredo = System.getenv("JWT_SECRET");
    Long usuarioId =
        jdbc.queryForObject(
            "SELECT id FROM usuario WHERE email = ? AND empresa_id = ?",
            Long.class,
            email,
            empresaId);
    Long tokenVersion =
        jdbc.queryForObject(
            "SELECT token_version FROM usuario WHERE email = ? AND empresa_id = ?",
            Long.class,
            email,
            empresaId);
    Date agora = new Date();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(email)
            .claim("empresa_id", empresaId)
            .claim("usuario_id", usuarioId)
            .claim("token_version", tokenVersion)
            .claim("role", role)
            .issueTime(agora)
            .expirationTime(new Date(agora.getTime() + 3600_000L))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(segredo.getBytes(StandardCharsets.UTF_8)));
    return jwt.serialize();
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
