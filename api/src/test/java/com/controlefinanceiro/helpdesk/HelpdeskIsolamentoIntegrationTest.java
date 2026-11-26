package com.controlefinanceiro.helpdesk;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
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
class HelpdeskIsolamentoIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  private static final String SENHA = "fixture-somente-testes-it";
  private static final String SUFOXO = UUID.randomUUID().toString().substring(0, 8);

  @Autowired private TestRestTemplate rest;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PasswordEncoder passwordEncoder;

  private Long empresaA;
  private Long empresaB;
  private Long adminA;
  private Long userA;
  private Long userB;
  private Long ticketAUser;
  private Long ticketAAdmin;
  private Long ticketBUser;
  private String tokenAdminA;
  private String tokenUserA;
  private String tokenUserB;

  @BeforeAll
  void semear() throws Exception {
    String hash = passwordEncoder.encode(SENHA);
    empresaA = criarEmpresa("Helpdesk Iso A " + SUFOXO);
    empresaB = criarEmpresa("Helpdesk Iso B " + SUFOXO);
    adminA = criarUsuario(empresaA, "admin-iso-a-" + SUFOXO + "@teste.com", hash, "ADMIN");
    userA = criarUsuario(empresaA, "user-iso-a-" + SUFOXO + "@teste.com", hash, "USER");
    userB = criarUsuario(empresaB, "user-iso-b-" + SUFOXO + "@teste.com", hash, "USER");

    ticketAUser = criarTicket(empresaA, userA, "Ticket usuario A", "2026-11-24 09:00:00");
    ticketAAdmin = criarTicket(empresaA, adminA, "Ticket admin A", "2026-11-25 09:00:00");
    ticketBUser = criarTicket(empresaB, userB, "Ticket usuario B", "2026-11-25 10:00:00");

    criarMensagem(ticketAUser, empresaA, userA, "USER", "Mensagem antiga A", "2026-11-24 09:05:00");
    criarMensagem(ticketAUser, empresaA, adminA, "ADMIN", "Resposta A", "2026-11-24 10:05:00");
    criarMensagem(ticketBUser, empresaB, userB, "USER", "Mensagem B", "2026-11-25 10:05:00");

    tokenAdminA = login("admin-iso-a-" + SUFOXO + "@teste.com");
    tokenUserA = login("user-iso-a-" + SUFOXO + "@teste.com");
    tokenUserB = login("user-iso-b-" + SUFOXO + "@teste.com");
  }

  @AfterAll
  void limpar() {
    if (empresaA == null || empresaB == null) {
      return;
    }
    jdbc.update("delete from helpdesk_mensagem where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from helpdesk_ticket where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from usuario where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from empresa where id in (?, ?)", empresaA, empresaB);
  }

  @Test
  @DisplayName("usuario ve somente seus tickets, do mais novo para o mais antigo")
  void usuarioVeSomenteSeusTickets() throws Exception {
    JsonNode page = json(get("/api/helpdesk/tickets", tokenUserA));
    assertThat(page.get("tickets")).hasSize(1);
    assertThat(page.get("tickets").get(0).get("id").asLong()).isEqualTo(ticketAUser);

    assertThat(get("/api/helpdesk/tickets/" + ticketAAdmin, tokenUserA).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(get("/api/helpdesk/tickets/" + ticketBUser, tokenUserA).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(
            post(
                    "/api/helpdesk/tickets/" + ticketAAdmin + "/mensagens",
                    "{\"conteudo\":\"vazamento\"}",
                    tokenUserA)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("admin ve a fila da empresa sem filtrar usuario")
  void adminVeFilaDaEmpresa() throws Exception {
    JsonNode page = json(get("/api/admin/helpdesk/tickets", tokenAdminA));
    assertThat(page.get("tickets")).hasSize(2);
    assertThat(page.get("tickets").get(0).get("id").asLong()).isEqualTo(ticketAAdmin);
    assertThat(page.get("tickets").get(1).get("id").asLong()).isEqualTo(ticketAUser);

    assertThat(
            json(get("/api/admin/helpdesk/tickets/" + ticketAUser, tokenAdminA)).get("id").asLong())
        .isEqualTo(ticketAUser);
    assertThat(get("/api/admin/helpdesk/tickets/" + ticketBUser, tokenAdminA).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(
            post(
                    "/api/admin/helpdesk/tickets/" + ticketBUser + "/mensagens",
                    "{\"conteudo\":\"vazamento\"}",
                    tokenAdminA)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("usuario de outra empresa nao ve ticket da empresa A")
  void usuarioDeOutraEmpresaNaoVeTicketDaEmpresaA() throws Exception {
    assertThat(get("/api/helpdesk/tickets/" + ticketAUser, tokenUserB).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    JsonNode page = json(get("/api/helpdesk/tickets", tokenUserB));
    assertThat(page.get("tickets")).hasSize(1);
    assertThat(page.get("tickets").get(0).get("id").asLong()).isEqualTo(ticketBUser);
  }

  @Test
  @DisplayName("mensagens aparecem da mais antiga para a mais nova no detalhe")
  void mensagensOrdenadasDaMaisAntigaParaAMaisNova() throws Exception {
    JsonNode detalhe = json(get("/api/helpdesk/tickets/" + ticketAUser, tokenUserA));
    assertThat(detalhe.get("mensagens")).hasSize(2);
    assertThat(detalhe.get("mensagens").get(0).get("conteudo").asText())
        .isEqualTo("Mensagem antiga A");
    assertThat(detalhe.get("mensagens").get(1).get("conteudo").asText()).isEqualTo("Resposta A");
    assertThat(detalhe.get("mensagens").get(1).get("origem").asText()).isEqualTo("ADMIN");

    JsonNode detalheAdmin = json(get("/api/admin/helpdesk/tickets/" + ticketAUser, tokenAdminA));
    assertThat(detalheAdmin.get("mensagens")).hasSize(2);
  }

  private ResponseEntity<String> get(String path, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private ResponseEntity<String> post(String path, String corpo, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    return rest.postForEntity(path, new HttpEntity<>(corpo, headers), String.class);
  }

  private JsonNode json(ResponseEntity<String> response) throws Exception {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
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

  private Long criarUsuario(Long empresaId, String email, String hash, String role) {
    jdbc.update(
        "insert into usuario(empresa_id, email, senha_hash, nome, role, ativo, email_verificado,"
            + " token_version) values (?, ?, ?, ?, ?, true, true, 0)",
        empresaId,
        email,
        hash,
        email,
        role);
    return jdbc.queryForObject("select id from usuario where email = ?", Long.class, email);
  }

  private Long criarTicket(Long empresaId, Long usuarioId, String assunto, String criadoEm) {
    jdbc.update(
        "insert into helpdesk_ticket(empresa_id, usuario_id, assunto, descricao, status, criado_em)"
            + " values (?, ?, ?, ?, 'ABERTO', ?)",
        empresaId,
        usuarioId,
        assunto,
        "Descricao " + assunto,
        Timestamp.valueOf(criadoEm));
    return jdbc.queryForObject(
        "select max(id) from helpdesk_ticket where empresa_id = ? and usuario_id = ?",
        Long.class,
        empresaId,
        usuarioId);
  }

  private void criarMensagem(
      Long ticketId,
      Long empresaId,
      Long usuarioId,
      String origem,
      String conteudo,
      String criadoEm) {
    jdbc.update(
        "insert into helpdesk_mensagem(ticket_id, empresa_id, usuario_id, origem, conteudo,"
            + " criado_em) values (?, ?, ?, ?, ?, ?)",
        ticketId,
        empresaId,
        usuarioId,
        origem,
        conteudo,
        Timestamp.valueOf(criadoEm));
  }
}
