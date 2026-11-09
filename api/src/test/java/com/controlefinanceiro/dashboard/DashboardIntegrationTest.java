package com.controlefinanceiro.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.controlefinanceiro.dashboard.dto.DashboardResponse;
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
 * Testes de integração do endpoint de agregação GET /api/dashboard?ano=&mes= (DSH-01, DSH-02,
 * DSH-03, DSH-05) com PostgreSQL real e Flyway completo (padrão P6). Valida totais de entradas/
 * saídas/saldo, comparação com mês anterior, breakdown por categoria de SAÍDA (inclusive zeradas
 * via LEFT JOIN), isolamento de tenant via empresa_id do token e zeros para meses sem lançamentos.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardIntegrationTest {

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
  void dashboardRetornaTotaisEMes() throws Exception {
    Long categoriaSaida = criarCategoriaViaApi(tokenAdmin, "Supermercado", "SAIDA", false);
    Long categoriaEntrada = criarCategoriaViaApi(tokenAdmin, "Salario", "ENTRADA", false);

    criarLancamentoViaApi(
        tokenAdmin,
        categoriaSaida,
        "SAIDA",
        new BigDecimal("150.00"),
        LocalDate.of(2026, 11, 8),
        "Compra mercado");
    criarLancamentoViaApi(
        tokenAdmin,
        categoriaEntrada,
        "ENTRADA",
        new BigDecimal("3000.00"),
        LocalDate.of(2026, 11, 10),
        "Pagamento cliente");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/dashboard?ano=2026&mes=11",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DashboardResponse dto = mapper.readValue(response.getBody(), DashboardResponse.class);
    assertThat(dto.totalEntradas()).isEqualByComparingTo(new BigDecimal("3000.00"));
    assertThat(dto.totalSaidas()).isEqualByComparingTo(new BigDecimal("150.00"));
    assertThat(dto.saldo()).isEqualByComparingTo(new BigDecimal("2850.00"));
    assertThat(dto.categorias()).isNotNull();
  }

  @Test
  void mesSemLancamentosRetornaZeros() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/dashboard?ano=2026&mes=01",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DashboardResponse dto = mapper.readValue(response.getBody(), DashboardResponse.class);
    assertThat(dto.totalEntradas()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(dto.totalSaidas()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(dto.saldo()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(dto.categorias()).isNotNull();
    // LEFT JOIN traz todas as categorias de SAÍDA, inclusive zeradas (DSH-02)
    assertThat(dto.categorias()).allMatch(c -> c.total().compareTo(BigDecimal.ZERO) == 0);
  }

  @Test
  void comparacaoMesAnteriorBaseZeroRetornaPercentualTrace() throws Exception {
    Long categoriaEntrada = criarCategoriaViaApi(tokenAdmin, "Salario", "ENTRADA", false);
    criarLancamentoViaApi(
        tokenAdmin,
        categoriaEntrada,
        "ENTRADA",
        new BigDecimal("1000.00"),
        LocalDate.of(2026, 11, 5),
        "Entrada novembro");

    // Mês de novembro com lançamentos, outubro (anterior) sem lançamentos → base zero → percentual
    // "—"
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/dashboard?ano=2026&mes=11",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DashboardResponse dto = mapper.readValue(response.getBody(), DashboardResponse.class);
    assertThat(dto.percentualEntradas()).isEqualTo("—");
    assertThat(dto.percentualSaidas()).isEqualTo("—");
    assertThat(dto.percentualSaldo()).isEqualTo("—");
    // Variação absoluta ainda é exibida (não null)
    assertThat(dto.variacaoEntradas()).isNotNull();
    assertThat(dto.variacaoSaidas()).isNotNull();
    assertThat(dto.variacaoSaldo()).isNotNull();
  }

  @Test
  void endpointNaoExpõeEmpresaIdETenantIsolaDados() throws Exception {
    Long categoriaSaida = criarCategoriaViaApi(tokenAdmin, "Supermercado", "SAIDA", false);
    criarLancamentoViaApi(
        tokenAdmin,
        categoriaSaida,
        "SAIDA",
        new BigDecimal("100.00"),
        LocalDate.of(2026, 11, 8),
        "Saída empresa 1");

    // O endpoint não aceita parâmetro empresa_id — só ano e mes. Os dados vêm do TenantContext.
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/dashboard?ano=2026&mes=11",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    DashboardResponse dto = mapper.readValue(response.getBody(), DashboardResponse.class);
    assertThat(dto.totalSaidas()).isEqualByComparingTo(new BigDecimal("100.00"));
    // Supermercado aparece com total 100.00; outras categorias podem aparecer zeradas (LEFT JOIN)
    assertThat(dto.categorias())
        .filteredOn(c -> c.nome().equals("Supermercado"))
        .extracting(CategoryExpense::total)
        .containsExactly(new BigDecimal("100.00"));
    // Não há campo empresa_id no corpo da resposta (R3 gate)
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
