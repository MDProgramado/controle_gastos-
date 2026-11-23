package com.controlefinanceiro.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.conta.ContaAPagarRepository;
import com.controlefinanceiro.conta.FaturaCartaoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Date;
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

/** Isolamento financeiro entre usuários da mesma empresa e entre empresas (06-04-t2). */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsolamentoUsuarioIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final String SENHA = "fixture-somente-testes-it";
  private static final String SUFOXO = UUID.randomUUID().toString().substring(0, 8);

  @Autowired private TestRestTemplate rest;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ContaAPagarRepository contaAPagarRepository;
  @Autowired private FaturaCartaoRepository faturaCartaoRepository;

  private Long empresaA;
  private Long empresaB;
  private Long adminA;
  private Long userA;
  private Long userB;
  private Long categoriaEntradaA;
  private Long categoriaSaidaA;
  private Long lancAdminA;
  private Long lancUserA;
  private Long docAdminA;
  private Long docUserA;
  private Long contaUserA;
  private Long faturaUserA;
  private String tokenAdminA;
  private String tokenUserA;
  private String tokenUserB;

  @BeforeAll
  void semear() throws Exception {
    String hash = passwordEncoder.encode(SENHA);
    empresaA = criarEmpresa("Isolamento A " + SUFOXO);
    empresaB = criarEmpresa("Isolamento B " + SUFOXO);
    adminA = criarUsuario(empresaA, "admin-a-" + SUFOXO + "@teste.com", hash, "ADMIN");
    userA = criarUsuario(empresaA, "user-a-" + SUFOXO + "@teste.com", hash, "USER");
    userB = criarUsuario(empresaB, "user-b-" + SUFOXO + "@teste.com", hash, "USER");
    categoriaEntradaA = criarCategoria(empresaA, "Entrada " + SUFOXO, "ENTRADA");
    categoriaSaidaA = criarCategoria(empresaA, "Saida " + SUFOXO, "SAIDA");

    lancAdminA =
        criarLancamento(empresaA, adminA, categoriaEntradaA, "ENTRADA", new BigDecimal("1000.00"));
    lancUserA =
        criarLancamento(empresaA, userA, categoriaEntradaA, "ENTRADA", new BigDecimal("100.00"));
    criarLancamento(
        empresaB,
        userB,
        criarCategoria(empresaB, "Entrada B " + SUFOXO, "ENTRADA"),
        "ENTRADA",
        new BigDecimal("5000.00"));

    docAdminA = criarDocumento(empresaA, adminA, "admin.pdf");
    docUserA = criarDocumento(empresaA, userA, "user.pdf");
    faturaUserA = criarFaturaCartao(empresaA, userA, docUserA);
    contaUserA =
        criarContaAPagar(empresaA, userA, faturaUserA, categoriaSaidaA, new BigDecimal("42.00"));

    tokenAdminA = login("admin-a-" + SUFOXO + "@teste.com");
    tokenUserA = login("user-a-" + SUFOXO + "@teste.com");
    tokenUserB = login("user-b-" + SUFOXO + "@teste.com");
  }

  @AfterAll
  void limpar() {
    jdbc.update("delete from conta_a_pagar where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from fatura_cartao where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from importacao_fatura where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from importacao_item where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from importacao_documento where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from lancamento where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from categoria where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from usuario where empresa_id in (?, ?)", empresaA, empresaB);
    jdbc.update("delete from empresa where id in (?, ?)", empresaA, empresaB);
  }

  @Test
  @DisplayName("userA nao ve lancamentos, importacoes nem contas do adminA")
  void userANaoVeDadosDoAdminA() throws Exception {
    JsonNode lancamentos = json(get("/api/lancamentos", tokenUserA));
    assertThat(lancamentos.get("lancamentos")).hasSize(1);
    assertThat(lancamentos.get("lancamentos").get(0).get("id").asLong()).isEqualTo(lancUserA);

    JsonNode importacoes = json(get("/api/importacoes", tokenUserA));
    assertThat(importacoes.get("content")).hasSize(1);
    assertThat(importacoes.get("content").get(0).get("id").asLong()).isEqualTo(docUserA);

    assertThat(get("/api/lancamentos/" + lancAdminA, tokenUserA).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(get("/api/importacoes/" + docAdminA, tokenUserA).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(contaAPagarRepository.findByEmpresaIdAndUsuarioIdOrderByVencimento(empresaA, userA))
        .extracting("id")
        .containsExactly(contaUserA);
    assertThat(faturaCartaoRepository.findByEmpresaIdAndUsuarioId(empresaA, userA))
        .extracting("id")
        .containsExactly(faturaUserA);
  }

  @Test
  @DisplayName("ADMIN nao tem acesso aos dados financeiros de outro usuario")
  void adminNaoAcessaDadosDeOutroUsuario() {
    assertThat(get("/api/lancamentos/" + lancUserA, tokenAdminA).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(get("/api/importacoes/" + docUserA, tokenAdminA).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("usuario da empresa B nao ve recursos da empresa A")
  void usuarioDaEmpresaBNaoVeRecursosDaEmpresaA() throws Exception {
    assertThat(get("/api/lancamentos/" + lancUserA, tokenUserB).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(get("/api/importacoes/" + docUserA, tokenUserB).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(json(get("/api/lancamentos", tokenUserB)).get("lancamentos")).hasSize(1);
  }

  @Test
  @DisplayName("dashboard de cada usuario bate com SQL isolado por usuario")
  void dashboardIsoladoPorUsuarioBateComSql() throws Exception {
    JsonNode dashboardUserA = json(get("/api/dashboard?ano=2026&mes=11", tokenUserA));
    JsonNode dashboardAdminA = json(get("/api/dashboard?ano=2026&mes=11", tokenAdminA));

    BigDecimal esperadoUserA = totalEntradas(empresaA, userA);
    BigDecimal esperadoAdminA = totalEntradas(empresaA, adminA);

    assertThat(dashboardUserA.get("totalEntradas").decimalValue())
        .isEqualByComparingTo(esperadoUserA);
    assertThat(dashboardAdminA.get("totalEntradas").decimalValue())
        .isEqualByComparingTo(esperadoAdminA);
    assertThat(esperadoUserA).isNotEqualByComparingTo(esperadoAdminA);
  }

  private BigDecimal totalEntradas(Long empresaId, Long usuarioId) {
    return jdbc.queryForObject(
        "select coalesce(sum(valor), 0) from lancamento where empresa_id = ? and usuario_id = ?"
            + " and tipo = 'ENTRADA' and data between '2026-11-01' and '2026-11-30'",
        BigDecimal.class,
        empresaId,
        usuarioId);
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
        "insert into usuario(empresa_id, email, senha_hash, nome, role) values (?, ?, ?, ?, ?)",
        empresaId,
        email,
        hash,
        email,
        role);
    return jdbc.queryForObject("select id from usuario where email = ?", Long.class, email);
  }

  private Long criarCategoria(Long empresaId, String nome, String tipo) {
    jdbc.update(
        "insert into categoria(empresa_id, nome, tipo) values (?, ?, ?)", empresaId, nome, tipo);
    return jdbc.queryForObject(
        "select id from categoria where empresa_id = ? and nome = ? and tipo = ?",
        Long.class,
        empresaId,
        nome,
        tipo);
  }

  private Long criarLancamento(
      Long empresaId, Long usuarioId, Long categoriaId, String tipo, BigDecimal valor) {
    jdbc.update(
        "insert into lancamento(empresa_id, usuario_id, categoria_id, tipo, valor, data) "
            + "values (?, ?, ?, ?, ?, ?)",
        empresaId,
        usuarioId,
        categoriaId,
        tipo,
        valor,
        Date.valueOf("2026-11-10"));
    return jdbc.queryForObject(
        "select max(id) from lancamento where empresa_id = ? and usuario_id = ?",
        Long.class,
        empresaId,
        usuarioId);
  }

  private Long criarDocumento(Long empresaId, Long usuarioId, String nome) {
    jdbc.update(
        "insert into importacao_documento(empresa_id, usuario_id, nome_arquivo, tipo_documento,"
            + " hash_sha256, tamanho_bytes, status) values (?, ?, ?, 'EXTRATO', ?, 10, 'ANALISADO')",
        empresaId,
        usuarioId,
        nome + SUFOXO,
        UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", ""));
    return jdbc.queryForObject(
        "select max(id) from importacao_documento where empresa_id = ? and usuario_id = ?",
        Long.class,
        empresaId,
        usuarioId);
  }

  private Long criarFaturaCartao(Long empresaId, Long usuarioId, Long documentoId) {
    jdbc.update(
        "insert into fatura_cartao(empresa_id, usuario_id, documento_id, data_vencimento, total)"
            + " values (?, ?, ?, ?, 42.00)",
        empresaId,
        usuarioId,
        documentoId,
        Date.valueOf("2026-11-20"));
    return jdbc.queryForObject(
        "select max(id) from fatura_cartao where empresa_id = ? and usuario_id = ?",
        Long.class,
        empresaId,
        usuarioId);
  }

  private Long criarContaAPagar(
      Long empresaId, Long usuarioId, Long faturaId, Long categoriaId, BigDecimal valor) {
    jdbc.update(
        "insert into conta_a_pagar(empresa_id, usuario_id, fatura_id, categoria_id, descricao,"
            + " valor, data, vencimento, origem) values (?, ?, ?, ?, 'conta teste', ?, ?, ?, 'CARTAO')",
        empresaId,
        usuarioId,
        faturaId,
        categoriaId,
        valor,
        Date.valueOf("2026-11-10"),
        Date.valueOf("2026-11-20"));
    return jdbc.queryForObject(
        "select max(id) from conta_a_pagar where empresa_id = ? and usuario_id = ?",
        Long.class,
        empresaId,
        usuarioId);
  }
}
