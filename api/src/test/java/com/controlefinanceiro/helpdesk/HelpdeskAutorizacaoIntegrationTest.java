package com.controlefinanceiro.helpdesk;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HelpdeskAutorizacaoIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final String SENHA = "fixture-somente-testes-it";
  private static final String SUFOXO = UUID.randomUUID().toString().substring(0, 8);

  @Autowired private TestRestTemplate rest;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PasswordEncoder passwordEncoder;

  private Long empresaId;
  private String tokenAdmin;
  private String tokenUser;

  @BeforeAll
  void semear() throws Exception {
    String hash = passwordEncoder.encode(SENHA);
    empresaId = criarEmpresa("Helpdesk Auth " + SUFOXO);
    criarUsuario(empresaId, "admin-auth-" + SUFOXO + "@teste.com", hash, "ADMIN");
    criarUsuario(empresaId, "user-auth-" + SUFOXO + "@teste.com", hash, "USER");
    tokenAdmin = login("admin-auth-" + SUFOXO + "@teste.com");
    tokenUser = login("user-auth-" + SUFOXO + "@teste.com");
  }

  @AfterAll
  void limpar() {
    if (empresaId == null) {
      return;
    }
    jdbc.update("delete from helpdesk_mensagem where empresa_id = ?", empresaId);
    jdbc.update("delete from helpdesk_ticket where empresa_id = ?", empresaId);
    jdbc.update("delete from usuario where empresa_id = ?", empresaId);
    jdbc.update("delete from empresa where id = ?", empresaId);
  }

  @Test
  @DisplayName("rotas do helpdesk sem token retornam 401")
  void rotasSemTokenRetornam401() {
    assertThat(get("/api/helpdesk/tickets", null).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            post("/api/helpdesk/tickets", "{\"assunto\":\"a\",\"descricao\":\"b\"}", null)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(get("/api/admin/helpdesk/tickets", null).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(
            patch("/api/admin/helpdesk/tickets/1/status", "{\"status\":\"FECHADO\"}", null)
                .getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("USER nao acessa rotas administrativas do helpdesk")
  void userNaoAcessaRotasAdministrativas() {
    assertThat(get("/api/admin/helpdesk/tickets", tokenUser).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(get("/api/admin/helpdesk/tickets/1", tokenUser).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            post("/api/admin/helpdesk/tickets/1/mensagens", "{\"conteudo\":\"oi\"}", tokenUser)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            patch("/api/admin/helpdesk/tickets/1/status", "{\"status\":\"FECHADO\"}", tokenUser)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("ADMIN nao acessa rotas de usuario do helpdesk")
  void adminNaoAcessaRotasDeUsuario() {
    assertThat(get("/api/helpdesk/tickets", tokenAdmin).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(get("/api/helpdesk/tickets/1", tokenAdmin).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  @DisplayName("usuario autenticado cria e responde proprio ticket")
  void usuarioCriaERespondeProprioTicket() throws Exception {
    JsonNode criado =
        json(
            post(
                "/api/helpdesk/tickets",
                "{\"assunto\":\"Ajuda\",\"descricao\":\"Erro\"}",
                tokenUser));
    long ticketId = criado.get("id").asLong();

    assertThat(criado.get("status").asText()).isEqualTo("ABERTO");
    assertThat(json(get("/api/helpdesk/tickets", tokenUser)).get("tickets")).hasSize(1);
    assertThat(json(get("/api/helpdesk/tickets/" + ticketId, tokenUser)).get("id").asLong())
        .isEqualTo(ticketId);

    JsonNode resposta =
        json(
            post(
                "/api/helpdesk/tickets/" + ticketId + "/mensagens",
                "{\"conteudo\":\"Mais detalhes\"}",
                tokenUser));
    assertThat(resposta.get("origem").asText()).isEqualTo("USER");
    assertThat(resposta.get("conteudo").asText()).isEqualTo("Mais detalhes");
  }

  @Test
  @DisplayName("admin autenticado responde e altera status na fila")
  void adminRespondeEAlteraStatus() throws Exception {
    JsonNode criado =
        json(
            post(
                "/api/helpdesk/tickets",
                "{\"assunto\":\"Fila\",\"descricao\":\"Aberto\"}",
                tokenUser));
    long ticketId = criado.get("id").asLong();

    JsonNode resposta =
        json(
            post(
                "/api/admin/helpdesk/tickets/" + ticketId + "/mensagens",
                "{\"conteudo\":\"Estamos analisando\"}",
                tokenAdmin));
    assertThat(resposta.get("origem").asText()).isEqualTo("ADMIN");

    JsonNode alterado =
        json(
            patch(
                "/api/admin/helpdesk/tickets/" + ticketId + "/status",
                "{\"status\":\"EM_ANDAMENTO\"}",
                tokenAdmin));
    assertThat(alterado.get("status").asText()).isEqualTo("EM_ANDAMENTO");
  }

  private ResponseEntity<String> get(String path, String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private ResponseEntity<String> post(String path, String corpo, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return rest.postForEntity(path, new HttpEntity<>(corpo, headers), String.class);
  }

  private ResponseEntity<String> patch(String path, String corpo, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return rest.exchange(path, HttpMethod.PATCH, new HttpEntity<>(corpo, headers), String.class);
  }

  private JsonNode json(ResponseEntity<String> response) throws Exception {
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    return mapper.readTree(response.getBody());
  }

  private String login(String email) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}";
    ResponseEntity<String> resposta =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    return mapper.readTree(resposta.getBody()).get("token").asText();
  }

  private Long criarEmpresa(String nome) {
    jdbc.update("insert into empresa(nome) values (?)", nome);
    return jdbc.queryForObject("select id from empresa where nome = ?", Long.class, nome);
  }

  private void criarUsuario(Long empresaId, String email, String hash, String role) {
    jdbc.update(
        "insert into usuario(empresa_id, email, senha_hash, nome, role, ativo, email_verificado,"
            + " token_version) values (?, ?, ?, ?, ?, true, true, 0)",
        empresaId,
        email,
        hash,
        email,
        role);
  }
}
