package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração de categorias (CAT-01, CAT-02, CAT-03, CAT-06) com PostgreSQL real e Flyway
 * completo (padrão P6): GET /api/categorias retorna 16 categorias seedadas (12 SAÍDA + 4 ENTRADA, 5
 * pessoais [P]), filtradas pela empresa_id do token (tenant isolation).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoriaIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Fixture óbvio de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Test
  void listarCategoriasRetorna16CategoriasSeedadas() throws Exception {
    String token = login("admin@controle.com", SENHA_FIXTURE);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<String> response =
        rest.exchange("/api/categorias", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = mapper.readTree(response.getBody());
    assertThat(body.isArray()).isTrue();
    assertThat(body).hasSize(16);

    long saidas = 0;
    long entradas = 0;
    for (JsonNode categoria : body) {
      String tipo = categoria.get("tipo").asText();
      if ("SAIDA".equals(tipo)) {
        saidas++;
      } else if ("ENTRADA".equals(tipo)) {
        entradas++;
      }
    }
    assertThat(saidas).isEqualTo(12);
    assertThat(entradas).isEqualTo(4);

    List<String> nomesPessoais = new ArrayList<>();
    for (JsonNode categoria : body) {
      if (categoria.get("pessoal").asBoolean()) {
        nomesPessoais.add(categoria.get("nome").asText());
      }
    }
    assertThat(nomesPessoais)
        .containsExactlyInAnyOrder(
            "Alimentação", "Saúde/farmácia", "Lazer", "Rendimentos e reembolsos");
  }

  @Test
  void categoriasNaoExpoemEmpresaIdNoBodyR3() throws Exception {
    String token = login("admin@controle.com", SENHA_FIXTURE);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<String> response =
        rest.exchange("/api/categorias", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = mapper.readTree(response.getBody());
    body.forEach(
        categoria ->
            assertThat(categoria.has("empresa_id"))
                .as("R3: empresa_id nunca deve aparecer no corpo da resposta")
                .isFalse());
  }

  private String login(String email, String senha) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    ResponseEntity<String> response =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
    return mapper.readTree(response.getBody()).get("token").asText();
  }
}
