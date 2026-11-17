package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração do modelo de telefone e fluxo de verificação do WhatsApp.
 *
 * <p>Cobre os 6 cenários obrigatórios: 1. ADMIN de outra empresa tenta vincular: 403. 2. USER tenta
 * vincular: 403. 3. Código errado 3 vezes: bloqueio na 4ª dentro da hora. 4. Código expirado:
 * rejeitado. 5. Outro usuário confirma código alheio: rejeitado. 6. Troca de número: anterior
 * inativo.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsuarioTelefoneIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Autowired private JdbcTemplate jdbc;

  private Long empresaAdminId;
  private Long empresaOutraId;
  private String empresaOutraNome;
  private String tokenAdmin;
  private String tokenAdminOutraEmpresa;
  private String tokenUser;

  private String sufixoEmpresa() {
    return String.valueOf(System.currentTimeMillis());
  }

  @BeforeEach
  void setup() throws Exception {
    // Nome único por execução: evita depender de UNIQUE constraint em empresa.nome
    String empresaAdminNome = "Controle Financeiro Teste " + sufixoEmpresa();
    empresaAdminId =
        jdbc.queryForObject(
            "INSERT INTO empresa (nome) VALUES (?) RETURNING id", Long.class, empresaAdminNome);

    // Cria o usuário ADMIN da empresa principal com senha conhecida
    jdbc.update("DELETE FROM usuario WHERE email = 'admin@controle.com'");
    jdbc.update(
        "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) "
            + "VALUES (?, ?, ?, ?, ?, true)",
        empresaAdminId,
        "admin@controle.com",
        new BCryptPasswordEncoder(12).encode(SENHA_FIXTURE),
        "Administrador",
        "ADMIN");
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);

    // Cria uma segunda empresa para teste de isolamento
    empresaOutraNome = "Empresa Outra Teste " + sufixoEmpresa();
    empresaOutraId =
        jdbc.queryForObject(
            "INSERT INTO empresa (nome) VALUES (?) RETURNING id", Long.class, empresaOutraNome);

    // Cria o usuário ADMIN da empresa principal com senha conhecida
    jdbc.update("DELETE FROM usuario WHERE email = 'admin@controle.com'");
    jdbc.update(
        "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) "
            + "VALUES (?, ?, ?, ?, ?, true)",
        empresaAdminId,
        "admin@controle.com",
        new BCryptPasswordEncoder(12).encode(SENHA_FIXTURE),
        "Administrador",
        "ADMIN");
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);

    // Cria uma segunda empresa para teste de isolamento
    empresaOutraNome = "Empresa Outra Teste " + sufixoEmpresa();
    empresaOutraId =
        jdbc.queryForObject(
            "INSERT INTO empresa (nome) VALUES (?) RETURNING id", Long.class, empresaOutraNome);

    // Cria um ADMIN na outra empresa com senha conhecida
    jdbc.update("DELETE FROM usuario WHERE email = 'admin@outra.com'");
    jdbc.update(
        "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) "
            + "VALUES (?, ?, ?, ?, ?, true)",
        empresaOutraId,
        "admin@outra.com",
        new BCryptPasswordEncoder(12).encode(SENHA_FIXTURE),
        "Admin Outra",
        "ADMIN");
    tokenAdminOutraEmpresa = login("admin@outra.com", SENHA_FIXTURE);

    // Cria um USER na empresa admin com senha conhecida
    jdbc.update("DELETE FROM usuario WHERE email = 'user@controle.com'");
    jdbc.update(
        "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) "
            + "VALUES (?, ?, ?, ?, ?, true)",
        empresaAdminId,
        "user@controle.com",
        new BCryptPasswordEncoder(12).encode(SENHA_FIXTURE),
        "Usuário Comum",
        "USER");
    tokenUser = login("user@controle.com", SENHA_FIXTURE);
  }

  @AfterEach
  void limparDadosTeste() {
    jdbc.update("DELETE FROM auditoria_telefone");
    jdbc.update("DELETE FROM whatsapp_codigo_verificacao");
    jdbc.update("DELETE FROM usuario_telefone");
    jdbc.update("DELETE FROM usuario WHERE email IN ('user@controle.com', 'admin@outra.com', 'admin@controle.com')");
    jdbc.update("DELETE FROM empresa WHERE nome = ?", empresaOutraNome);
    jdbc.update("DELETE FROM empresa WHERE nome LIKE 'Controle Financeiro Teste %'");
  }

  /* ========================================================================
   * Cenário 1: ADMIN de outra empresa tenta vincular: 403
   * ======================================================================== */

  @Test
  void adminDeOutraEmpresaNaoPodeVincularTelefone() throws Exception {
    Long usuarioIdAdmin = usuarioIdDoEmail("admin@controle.com");

    String corpo = "{\"numeroE164\":\"+5571999999999\"}";
    HttpHeaders headers = authHeaders(tokenAdminOutraEmpresa);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/usuarios/%d/telefone".formatted(usuarioIdAdmin),
            HttpMethod.POST,
            new HttpEntity<>(corpo, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  /* ========================================================================
   * Cenário 2: USER tenta vincular: 403
   * ======================================================================== */

  @Test
  void usuarioNaoAdminNaoPodeVincularTelefone() throws Exception {
    Long usuarioIdUser = usuarioIdDoEmail("user@controle.com");

    String corpo = "{\"numeroE164\":\"+5571999999999\"}";
    HttpHeaders headers = authHeaders(tokenUser);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/usuarios/%d/telefone".formatted(usuarioIdUser),
            HttpMethod.POST,
            new HttpEntity<>(corpo, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  /* ========================================================================
   * Cenário 3: Código errado 3 vezes: bloqueio na 4ª dentro da hora
   * ======================================================================== */

  @Test
  void codigoErradoTrêsVezesBloqueiaNaQuarta() throws Exception {
    Long usuarioIdAdmin = usuarioIdDoEmail("admin@controle.com");

    // ADMIN vincula um telefone para o próprio usuário
    vincularTelefone(tokenAdmin, usuarioIdAdmin, "+5571999999999");

    // O próprio admin tenta verificar com código errado 3 vezes
    for (int i = 1; i <= 3; i++) {
      ResponseEntity<String> response = verificar(tokenAdmin, "+5571999999999", "000000");
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // 4ª tentativa deve ser bloqueada pelo limite
    ResponseEntity<String> response = verificar(tokenAdmin, "+5571999999999", "000000");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode body = mapper.readTree(response.getBody());
    assertThat(body.get("detail").asText()).contains("tentativas");
  }

  /* ========================================================================
   * Cenário 4: Código expirado: rejeitado
   * ======================================================================== */

  @Test
  void codigoExpiradoERejeitado() throws Exception {
    Long usuarioIdAdmin = usuarioIdDoEmail("admin@controle.com");

    // ADMIN vincula um telefone para o próprio usuário
    vincularTelefone(tokenAdmin, usuarioIdAdmin, "+5571999999998");

    // Força expiração do código diretamente no banco
    jdbc.update(
        "UPDATE whatsapp_codigo_verificacao SET expira_em = now() - interval '1 hour' "
            + "WHERE telefone_id = (SELECT id FROM usuario_telefone WHERE numero_e164 = '+5571999999998')");

    ResponseEntity<String> response = verificar(tokenAdmin, "+5571999999998", "123456");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /* ========================================================================
   * Cenário 5: Outro usuário confirma código alheio: rejeitado
   * ======================================================================== */

  @Test
  void outroUsuarioNaoPodeConfirmarCodigoAlheio() throws Exception {
    Long usuarioIdAdmin = usuarioIdDoEmail("admin@controle.com");
    Long usuarioIdUser = usuarioIdDoEmail("user@controle.com");

    // ADMIN vincula um telefone para o usuário admin
    vincularTelefone(tokenAdmin, usuarioIdAdmin, "+5571999999997");

    // O USER tenta confirmar o código do telefone do admin (não é dono)
    ResponseEntity<String> response = verificar(tokenUser, "+5571999999997", "123456");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  /* ========================================================================
   * Cenário 6: Troca de número: anterior inativo
   * ======================================================================== */

  @Test
  void trocaDeNumeroDesativaAnterior() throws Exception {
    Long usuarioIdAdmin = usuarioIdDoEmail("admin@controle.com");

    // Vincula primeiro número
    vincularTelefone(tokenAdmin, usuarioIdAdmin, "+5571999999996");
    assertThat(telefoneAtivo(usuarioIdAdmin, "+5571999999996")).isTrue();

    // Vincula segundo número para o mesmo usuário
    vincularTelefone(tokenAdmin, usuarioIdAdmin, "+5571999999995");
    assertThat(telefoneAtivo(usuarioIdAdmin, "+5571999999995")).isTrue();

    // Primeiro número deve estar inativo
    assertThat(telefoneAtivo(usuarioIdAdmin, "+5571999999996")).isFalse();
    // Segundo número deve estar ativo
    assertThat(telefoneAtivo(usuarioIdAdmin, "+5571999999995")).isTrue();

    // Verifica auditoria: deve ter 2 registros (VINCULAR e TROCAR)
    int auditorias =
        jdbc.queryForObject(
            "SELECT count(*) FROM auditoria_telefone WHERE usuario_id = ?",
            Integer.class,
            usuarioIdAdmin);
    assertThat(auditorias).isEqualTo(2);
  }

  /* ========================================================================
   * Helpers
   * ======================================================================== */

  private void vincularTelefone(String token, Long usuarioId, String numeroE164) throws Exception {
    String corpo = "{\"numeroE164\":\"" + numeroE164 + "\"}";
    HttpHeaders headers = authHeaders(token);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/usuarios/%d/telefone".formatted(usuarioId),
            HttpMethod.POST,
            new HttpEntity<>(corpo, headers),
            String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private ResponseEntity<String> verificar(String token, String numeroE164, String codigo)
      throws Exception {
    String corpo = "{\"numeroE164\":\"" + numeroE164 + "\",\"codigo\":\"" + codigo + "\"}";
    HttpHeaders headers = authHeaders(token);
    return rest.exchange(
        "/api/telefones/verificar",
        HttpMethod.POST,
        new HttpEntity<>(corpo, headers),
        String.class);
  }

  private boolean telefoneAtivo(Long usuarioId, String numeroE164) {
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM usuario_telefone WHERE usuario_id = ? AND numero_e164 = ? AND ativo = true",
            Integer.class,
            usuarioId,
            numeroE164);
    return count != null && count > 0;
  }

  private Long empresaIdDoEmail(String email) {
    return jdbc.queryForObject("SELECT empresa_id FROM usuario WHERE email = ?", Long.class, email);
  }

  private Long usuarioIdDoEmail(String email) {
    return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
  }

  private HttpHeaders authHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    return headers;
  }

  private String login(String email, String senha) throws Exception {
    String corpo = "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> response =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return mapper.readTree(response.getBody()).get("token").asText();
  }
}
