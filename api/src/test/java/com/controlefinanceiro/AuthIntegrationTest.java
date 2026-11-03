package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
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
 * Testes de integração de autenticação (AUTH-01/02/05) com PostgreSQL real e Flyway completo
 * (padrão P6): login emite JWT de 8h (D-01), erros são genéricos (D-05 anti-enumeração), corpo com
 * campo desconhecido é rejeitado e rotas protegidas devolvem 401 sem/inválido.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Fixture ÓBVIO de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Test
  void loginComCredenciaisValidasRetornaTokenComClaimsDe8Horas() throws Exception {
    ResponseEntity<String> response = login("admin@controle.com", SENHA_FIXTURE);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode token = mapper.readTree(response.getBody()).get("token");
    assertThat(token).isNotNull();

    JsonNode payload = decodificarPayload(token.asText());

    assertThat(payload.get("sub").asText()).isEqualTo("admin@controle.com");
    assertThat(payload.get("empresa_id").asLong()).isEqualTo(1L);
    assertThat(payload.get("role").asText()).isEqualTo("ADMIN");

    // D-01: validade de 8h = 28800s (tolerância de 60s)
    long iat = payload.get("iat").asLong();
    long exp = payload.get("exp").asLong();
    assertThat(exp - iat).isBetween(28800L - 60L, 28800L + 60L);
  }

  @Test
  void senhaErradaEEmailInexistenteDevolvemAMesmaMensagemGenerica() throws Exception {
    ResponseEntity<String> senhaErrada = login("admin@controle.com", "outra-senha-errada");
    ResponseEntity<String> emailInexistente = login("nao-existe@controle.com", SENHA_FIXTURE);

    // D-05 anti-enumeração: os dois casos são indistinguíveis para o client
    assertThat(senhaErrada.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(emailInexistente.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(detalhe(senhaErrada)).isEqualTo("E-mail ou senha inválidos");
    assertThat(detalhe(emailInexistente)).isEqualTo(detalhe(senhaErrada));
  }

  @Test
  void corpoComCampoDesconhecidoRetorna400() {
    // spring.jackson.deserialization.fail-on-unknown-properties: true rejeita o campo extra
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"email\":\"admin@controle.com\",\"senha\":\"x\",\"empresaId\":999}";

    ResponseEntity<String> response =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rotaProtegidaSemTokenOuComTokenAleatorioRetorna401() {
    ResponseEntity<String> semToken = rest.getForEntity("/api/auth/me", String.class);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("token-aleatorio-invalido");
    ResponseEntity<String> tokenAleatorio =
        rest.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(semToken.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(tokenAleatorio.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private ResponseEntity<String> login(String email, String senha) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    return rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
  }

  private String detalhe(ResponseEntity<String> response) throws Exception {
    return mapper.readTree(response.getBody()).get("detail").asText();
  }

  private JsonNode decodificarPayload(String token) throws Exception {
    String payload = token.split("\\.")[1];
    payload += "=".repeat((4 - payload.length() % 4) % 4);
    return mapper.readTree(Base64.getUrlDecoder().decode(payload));
  }
}
