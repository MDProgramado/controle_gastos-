package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.categoria.CategoriaRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração de categorias (CAT-01 a CAT-06) com PostgreSQL real e Flyway completo
 * (padrão P6): GET /api/categorias retorna 16 categorias seedadas; write endpoints
 * (POST/PUT/DELETE) restritos a ADMIN; USER recebe 403; integridade categoria×tipo (CAT-04).
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

  @Autowired private JdbcTemplate jdbc;

  @Autowired private PasswordEncoder passwordEncoder;

  private String tokenAdmin;

  private String tokenUser;

  @BeforeEach
  void setup() throws Exception {
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);
    garantirUsuarioUser();
    tokenUser = login("usuario@controle.com", SENHA_FIXTURE);
  }

  private void garantirUsuarioUser() {
    Long empresaId =
        jdbc.queryForObject(
            "SELECT id FROM empresa WHERE nome = 'Controle Financeiro'", Long.class);
    long jaExiste =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE email = 'usuario@controle.com'", Long.class);
    if (jaExiste == 0) {
      String senhaHash = passwordEncoder.encode(SENHA_FIXTURE);
      jdbc.update(
          "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo) VALUES (?, ?, ?, ?, ?, true)",
          empresaId,
          "usuario@controle.com",
          senhaHash,
          "Usuario Comum",
          "USER");
    }
  }

  @AfterEach
  void apagarCategoriasCriadasNosTestes() {
    // Isola testes de escrita: remove categorias criadas via API (id > 16 = seed da V2)
    // para que listarCategoriasRetorna16CategoriasSeedadas não seja afetado por ordem de execução.
    jdbc.update("DELETE FROM categoria WHERE id > 16");
  }

  @Test
  void categoriaRequestNaoExisteAindaRed() {
    // Referencia CategoriaRequest que ainda não existe — compilação falha (RED).
    // Esta classe será criada na Task 2 (GREEN).
    CategoriaRequest request = new CategoriaRequest("Teste", "SAIDA", false);
    assertThat(request).isNotNull();
  }

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

  @Test
  void criarCategoriaComSucessoRetorna201() throws Exception {
    String corpo = "{\"nome\":\"Supermercado\",\"tipo\":\"SAIDA\",\"pessoal\":true}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/categorias", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    JsonNode body = mapper.readTree(response.getBody());
    assertThat(body.get("nome").asText()).isEqualTo("Supermercado");
    assertThat(body.get("tipo").asText()).isEqualTo("SAIDA");
    assertThat(body.get("pessoal").asBoolean()).isTrue();
    assertThat(body.has("empresa_id")).as("R3: empresa_id não deve aparecer na resposta").isFalse();
  }

  @Test
  void usuarioNaoPodeCriarCategoriaRetorna403() throws Exception {
    String corpo = "{\"nome\":\"Supermercado\",\"tipo\":\"SAIDA\",\"pessoal\":true}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenUser);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/categorias", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void editarCategoriaComSucessoRetorna200() throws Exception {
    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Antigo Nome", "SAIDA", false);
    String corpo = "{\"nome\":\"Novo Nome\",\"tipo\":\"SAIDA\",\"pessoal\":false}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/categorias/" + categoriaId,
            HttpMethod.PUT,
            new HttpEntity<>(corpo, headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = mapper.readTree(response.getBody());
    assertThat(body.get("nome").asText()).isEqualTo("Novo Nome");
    assertThat(body.get("id").asLong()).isEqualTo(categoriaId);
  }

  @Test
  void desativarCategoriaRetorna204() throws Exception {
    Long categoriaId = criarCategoriaViaApi(tokenAdmin, "Para Desativar", "ENTRADA", false);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/categorias/" + categoriaId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void categoriaTipoIncompativelRetorna400() throws Exception {
    String corpo = "{\"nome\":\"Teste\",\"tipo\":\"INVALIDO\",\"pessoal\":false}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    ResponseEntity<String> response =
        rest.exchange(
            "/api/categorias", HttpMethod.POST, new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private Long criarCategoriaViaApi(String token, String nome, String tipo, boolean pessoal)
      throws Exception {
    String corpo =
        "{\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo + "\",\"pessoal\":" + pessoal + "}";
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
    String corpo = "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    ResponseEntity<String> response =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
    return mapper.readTree(response.getBody()).get("token").asText();
  }
}
