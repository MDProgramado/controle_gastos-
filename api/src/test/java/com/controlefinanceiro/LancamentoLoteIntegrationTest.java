package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.lancamento.LancamentoLoteRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
 * Testes de integração de lote tudo-ou-nada (LOT-01, LOT-02) com PostgreSQL real e Flyway completo
 * (padrão P6): lote válido cria todos, linha inválida faz rollback total com índice da linha, lote
 * acima de 500 linhas rejeitado com 400.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LancamentoLoteIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Fixture óbvio de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  private String tokenAdmin;

  @AfterEach
  void apagarLancamentosECategoriasCriadosNosTestes() {
    jdbc.update("DELETE FROM lancamento");
    jdbc.update("DELETE FROM categoria WHERE id > 16");
  }

  @Test
  void lancamentoLoteRequestNaoExisteAindaRed() {
    // Referencia LancamentoLoteRequest que ainda não existe — compilação falha (RED).
    // Esta classe será criada na Task 2 (GREEN).
    LancamentoLoteRequest request = new LancamentoLoteRequest(List.of());
    assertThat(request).isNotNull();
  }

  @Test
  void processarLoteValidoCriaTodos() throws Exception {
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);

    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Supermercado", "SAIDA", false);

    String corpo =
        """
        {
          "linhas": [
            {"categoriaId":%d,"tipo":"SAIDA","valor":100.00,"data":"2026-11-08","descricao":"Linha 1"},
            {"categoriaId":%d,"tipo":"SAIDA","valor":50.00,"data":"2026-11-08","descricao":"Linha 2"},
            {"categoriaId":%d,"tipo":"SAIDA","valor":25.50,"data":"2026-11-08","descricao":"Linha 3"}
          ]
        }
        """
            .formatted(categoriaId, categoriaId, categoriaId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos/lote",
            HttpMethod.POST,
            new HttpEntity<>(corpo, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode body = mapper.readTree(response.getBody());
    assertThat(body).isNotNull();

    long count = jdbc.queryForObject("SELECT COUNT(*) FROM lancamento", Long.class);
    assertThat(count).isEqualTo(3);
  }

  @Test
  void processarLoteComLinhaInvalidaFazRollback() throws Exception {
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);

    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Supermercado", "SAIDA", false);

    // Linha 2 tem valor com 3 casas decimais — inválido
    String corpo =
        """
        {
          "linhas": [
            {"categoriaId":%d,"tipo":"SAIDA","valor":100.00,"data":"2026-11-08","descricao":"Linha 1"},
            {"categoriaId":%d,"tipo":"SAIDA","valor":10.123,"data":"2026-11-08","descricao":"Linha 2 invalida"},
            {"categoriaId":%d,"tipo":"SAIDA","valor":25.50,"data":"2026-11-08","descricao":"Linha 3"}
          ]
        }
        """
            .formatted(categoriaId, categoriaId, categoriaId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos/lote",
            HttpMethod.POST,
            new HttpEntity<>(corpo, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    JsonNode body = mapper.readTree(response.getBody());
    String detalhe = body.get("detail").asText();
    assertThat(detalhe).contains("1"); // índice da linha inválida (base 0)

    // Rollback total: nenhuma linha persistida
    long count = jdbc.queryForObject("SELECT COUNT(*) FROM lancamento", Long.class);
    assertThat(count).isEqualTo(0);
  }

  @Test
  void loteExcedeLimite500Rejeitado() throws Exception {
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);

    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Supermercado", "SAIDA", false);

    // Monta 501 linhas
    StringBuilder linhas = new StringBuilder();
    for (int i = 0; i < 501; i++) {
      if (i > 0) {
        linhas.append(",");
      }
      linhas.append(
          "{\"categoriaId\":%d,\"tipo\":\"SAIDA\",\"valor\":10.00,\"data\":\"2026-11-08\",\"descricao\":\"Linha %d\"}"
              .formatted(categoriaId, i));
    }

    String corpo = "{\"linhas\":[%s]}".formatted(linhas);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/lancamentos/lote",
            HttpMethod.POST,
            new HttpEntity<>(corpo, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

  private String login(String email, String senha) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha);
    ResponseEntity<String> response =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
    return mapper.readTree(response.getBody()).get("token").asText();
  }
}
