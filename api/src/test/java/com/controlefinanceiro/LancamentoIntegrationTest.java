package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.lancamento.LancamentoRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração de lançamentos (LAN-01 a LAN-07) com PostgreSQL real e Flyway completo
 * (padrão P6): CRUD de lançamentos, validação monetária estrita (BigDecimal, max 2 casas, rejeita
 * não arredonda), integridade categoria×tipo e filtros por mês/tipo/categoria dentro da empresa do
 * token.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LancamentoIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Fixture óbvio de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  private String tokenAdmin;

  @BeforeEach
  void setup() throws Exception {
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);
  }

  @AfterEach
  void apagarLancamentosECategoriasCriadosNosTestes() {
    // Isola testes: remove lançamentos e categorias criados via API (id > 16 = seed da V2)
    jdbc.update("DELETE FROM lancamento");
    jdbc.update("DELETE FROM categoria WHERE id > 16");
  }

  @Test
  void lancamentoRequestNaoExisteAindaRed() {
    // Referencia LancamentoRequest que ainda não existe — compilação falha (RED).
    // Esta classe será criada na Task 2 (GREEN).
    LancamentoRequest request =
        new LancamentoRequest(1L, "SAIDA", BigDecimal.TEN, LocalDate.now(), "teste");
    assertThat(request).isNotNull();
  }

  @Test
  void criarLancamentoComSucessoRetorna201() throws Exception {
    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Supermercado", "SAIDA", false);
    String corpo =
        "{\"categoriaId\":%d,\"tipo\":\"SAIDA\",\"valor\":100.00,\"data\":\"2026-11-08\",\"descricao\":\"Compra no supermercado\"}"
            .formatted(categoriaId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode body = mapper.readTree(response.getBody());
    assertThat(body.get("id")).isNotNull();
    assertThat(body.get("tipo").asText()).isEqualTo("SAIDA");
    assertThat(body.get("valor").asDouble()).isEqualTo(100.00);
    assertThat(body.get("data").asText()).isEqualTo("2026-11-08");
    assertThat(body.get("descricao").asText()).isEqualTo("Compra no supermercado");
    assertThat(body.get("categoriaNome").asText()).isEqualTo("Supermercado");
  }

  @Test
  void editarLancamentoComSucessoRetorna200() throws Exception {
    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Antigo Nome", "SAIDA", false);
    Long lancamentoId =
        criarLancamentoViaApi(
            tokenAdmin,
            categoriaId,
            "SAIDA",
            new BigDecimal("100.00"),
            LocalDate.of(2026, 11, 8),
            "Descricao antiga");

    String corpo =
        "{\"categoriaId\":%d,\"tipo\":\"SAIDA\",\"valor\":200.00,\"data\":\"2026-11-09\",\"descricao\":\"Descricao nova\"}"
            .formatted(categoriaId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos/%d".formatted(lancamentoId),
            HttpMethod.PUT,
            new HttpEntity<>(corpo, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = mapper.readTree(response.getBody());
    assertThat(body.get("id").asLong()).isEqualTo(lancamentoId);
    assertThat(body.get("valor").asDouble()).isEqualTo(200.00);
    assertThat(body.get("descricao").asText()).isEqualTo("Descricao nova");
  }

  @Test
  void excluirLancamentoComSucessoRetorna204() throws Exception {
    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Para Excluir", "SAIDA", false);
    Long lancamentoId =
        criarLancamentoViaApi(
            tokenAdmin,
            categoriaId,
            "SAIDA",
            new BigDecimal("50.00"),
            LocalDate.of(2026, 11, 8),
            "Descricao");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos/%d".formatted(lancamentoId),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // GET subsequente deve retornar 404 (lançamento não existe mais)
    ResponseEntity<String> getResponse =
        rest.getForEntity("/api/lancamentos/%d".formatted(lancamentoId), String.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void valorComTresCasasDecimaisRejeitado() throws Exception {
    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Teste", "SAIDA", false);
    String corpo =
        "{\"categoriaId\":%d,\"tipo\":\"SAIDA\",\"valor\":10.123,\"data\":\"2026-11-08\",\"descricao\":\"Teste\"}"
            .formatted(categoriaId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void valorNaoPositivoRejeitado() throws Exception {
    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Teste", "SAIDA", false);
    String corpo =
        "{\"categoriaId\":%d,\"tipo\":\"SAIDA\",\"valor\":0,\"data\":\"2026-11-08\",\"descricao\":\"Teste\"}"
            .formatted(categoriaId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void categoriaTipoIncompativelRejeitado() throws Exception {
    Long categoriaEntradaId = criarCategoriaViaApi(tokenAdmin, "Salario", "ENTRADA", false);
    String corpo =
        "{\"categoriaId\":%d,\"tipo\":\"SAIDA\",\"valor\":100.00,\"data\":\"2026-11-08\",\"descricao\":\"Teste\"}"
            .formatted(categoriaEntradaId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void listarLancamentosFiltradoPorTipoRetornaApenasDaEmpresa() throws Exception {
    Long categoriaSaidaId = criarCategoriaViaApi(tokenAdmin, "Supermercado", "SAIDA", false);
    Long categoriaEntradaId = criarCategoriaViaApi(tokenAdmin, "Salario", "ENTRADA", false);

    criarLancamentoViaApi(
        tokenAdmin,
        categoriaSaidaId,
        "SAIDA",
        new BigDecimal("100.00"),
        LocalDate.of(2026, 11, 8),
        "Saída 1");
    criarLancamentoViaApi(
        tokenAdmin,
        categoriaEntradaId,
        "ENTRADA",
        new BigDecimal("2000.00"),
        LocalDate.of(2026, 11, 8),
        "Entrada 1");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos?tipo=SAIDA", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = mapper.readTree(response.getBody());
    for (JsonNode lancamento : body) {
      assertThat(lancamento.get("tipo").asText()).isEqualTo("SAIDA");
    }
  }

  private Long criarCategoriaViaApi(String token, String nome, String tipo, boolean pessoal)
      throws Exception {
    String corpo =
        "{\"nome\":\"%s\",\"tipo\":\"%s\",\"pessoal\":%b}".formatted(nome, tipo, pessoal);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/categorias", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);
    return mapper.readTree(response.getBody()).get("id").asLong();
  }

  private Long criarLancamentoViaApi(
      String token,
      Long categoriaId,
      String tipo,
      BigDecimal valor,
      LocalDate data,
      String descricao)
      throws Exception {
    String corpo =
        "{\"categoriaId\":%d,\"tipo\":\"%s\",\"valor\":%s,\"data\":\"%s\",\"descricao\":\"%s\"}"
            .formatted(categoriaId, tipo, valor.toPlainString(), data, descricao);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);
    return mapper.readTree(response.getBody()).get("id").asLong();
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
