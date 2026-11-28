package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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

/**
 * Item 3.1/3.3: tabela ai_uso_diario (upsert acumulativo), endpoint de uso isolado por empresa e
 * actuator protegido.
 */
@Testcontainers
// Testes do Spring Boot desligam os exportadores de métricas por padrão; o Prometheus precisa
// deles.
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "metrics.token=fixture-metrics-token-somente-testes-it")
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AiUsoIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  private static final String SENHA = "fixture-somente-testes-it";
  private static final String SUFIXO = UUID.randomUUID().toString().substring(0, 8);
  private static final String METRICS_TOKEN = "fixture-metrics-token-somente-testes-it";
  private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

  @Autowired private TestRestTemplate rest;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AiUsoService usoService;

  private Long empresaA;
  private Long empresaB;
  private Long adminA;
  private Long userA;
  private Long adminB;
  private String tokenAdminA;
  private String tokenUserA;
  private String tokenAdminB;

  @BeforeAll
  void semear() throws Exception {
    String hash = passwordEncoder.encode(SENHA);
    empresaA = criarEmpresa("Uso IA A " + SUFIXO);
    empresaB = criarEmpresa("Uso IA B " + SUFIXO);
    adminA = criarUsuario(empresaA, "admin-uso-a-" + SUFIXO + "@teste.com", hash, "ADMIN");
    userA = criarUsuario(empresaA, "user-uso-a-" + SUFIXO + "@teste.com", hash, "USER");
    adminB = criarUsuario(empresaB, "admin-uso-b-" + SUFIXO + "@teste.com", hash, "ADMIN");
    tokenAdminA = login("admin-uso-a-" + SUFIXO + "@teste.com");
    tokenUserA = login("user-uso-a-" + SUFIXO + "@teste.com");
    tokenAdminB = login("admin-uso-b-" + SUFIXO + "@teste.com");
  }

  @AfterAll
  void limpar() {
    if (empresaA == null || empresaB == null) {
      return;
    }
    jdbc.update("delete from ai_uso_diario where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from usuario where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from empresa where id in (?, ?)", empresaA, empresaB);
  }

  @Test
  @DisplayName("upsert acumula tokens, custo e chamadas na mesma linha do dia/operação/modelo")
  void upsertAcumula() {
    usoService.registrar(
        empresaA, userA, List.of(new UsoIa("chat", "m/acc", 100, 10, new BigDecimal("0.001"), 1)));
    usoService.registrar(
        empresaA,
        userA,
        List.of(
            new UsoIa("chat", "m/acc", 50, 5, new BigDecimal("0.0005"), 1),
            new UsoIa("chat", "m/acc", 25, 1, new BigDecimal("0.000123"), 2)));

    Map<String, Object> linha =
        jdbc.queryForMap(
            "select tokens_entrada, tokens_saida, custo_usd, chamadas from ai_uso_diario"
                + " where empresa_id = ? and usuario_id = ? and data = ? and operacao = 'chat'"
                + " and modelo = 'm/acc'",
            empresaA,
            userA,
            java.sql.Date.valueOf(HOJE));
    assertThat(((Number) linha.get("tokens_entrada")).longValue()).isEqualTo(175);
    assertThat(((Number) linha.get("tokens_saida")).longValue()).isEqualTo(16);
    assertThat((BigDecimal) linha.get("custo_usd")).isEqualByComparingTo("0.001623");
    assertThat(((Number) linha.get("chamadas")).intValue()).isEqualTo(4);
  }

  @Test
  @DisplayName("admin vê só o uso da própria empresa; USER recebe 403")
  void endpointIsoladoPorEmpresa() throws Exception {
    usoService.registrar(
        empresaA,
        adminA,
        List.of(new UsoIa("extracao", "m/iso", 1000, 200, new BigDecimal("0.05"), 1)));
    usoService.registrar(
        empresaB, adminB, List.of(new UsoIa("chat", "m/iso", 7, 7, new BigDecimal("0.7"), 1)));

    String url = "/api/admin/ia/uso?de=" + HOJE.minusDays(1) + "&ate=" + HOJE;

    JsonNode a = json(get(url, tokenAdminA));
    for (JsonNode linha : a.get("linhas")) {
      assertThat(linha.get("usuarioId").asLong()).isIn(adminA, userA);
    }
    assertThat(a.get("total").get("tokensEntrada").asLong()).isGreaterThanOrEqualTo(1000);

    JsonNode b = json(get(url, tokenAdminB));
    assertThat(b.get("linhas")).hasSize(1);
    assertThat(b.get("linhas").get(0).get("usuarioId").asLong()).isEqualTo(adminB);
    assertThat(b.get("total").get("custoUsd").decimalValue()).isEqualByComparingTo("0.7");

    assertThat(get(url, tokenUserA).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            get("/api/admin/ia/uso?de=" + HOJE + "&ate=" + HOJE.minusDays(2), tokenAdminA)
                .getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("actuator: health público; prometheus exige ADMIN ou X-Metrics-Token")
  void actuatorProtegido() {
    assertThat(rest.getForEntity("/actuator/health", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(rest.getForEntity("/api/health", String.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);

    assertThat(rest.getForEntity("/actuator/prometheus", String.class).getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(get("/actuator/prometheus", tokenUserA).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(comToken("/actuator/prometheus", "token-errado").getStatusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED);

    ResponseEntity<String> admin = get("/actuator/prometheus", tokenAdminA);
    assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
    ResponseEntity<String> token = comToken("/actuator/prometheus", METRICS_TOKEN);
    assertThat(token.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(token.getBody())
        .contains("jvm_")
        .doesNotContain("empresa_id")
        .doesNotContain("usuario_id");

    // Demais endpoints do actuator não ficam expostos.
    assertThat(get("/actuator/env", tokenAdminA).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(rest.getForEntity("/actuator/env", String.class).getStatusCode())
        .isNotEqualTo(HttpStatus.OK);
  }

  private ResponseEntity<String> comToken(String path, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Metrics-Token", token);
    return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  private ResponseEntity<String> get(String path, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
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
}
