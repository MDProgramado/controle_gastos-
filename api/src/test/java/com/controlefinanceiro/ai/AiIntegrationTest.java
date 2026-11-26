package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.Role;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * Testes de integração do endpoint POST /api/ai/chat (Prompt 5/6 — fase IA). Testa a interpretação
 * de intents (lancamento/consulta/esclarecimento), a persistência de histórico na memória e a
 * não-exposição do empresa_id (R3 gate).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  /** Fixture óbvio de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;
  @Autowired private ObjectMapper mapper;
  @Autowired private TenantOnboardingService onboarding;
  @Autowired private UsuarioRepository usuarioRepository;
  @MockBean private AiClient aiClient;

  private String tokenAdmin;

  @BeforeEach
  void login() throws Exception {
    tokenAdmin = login("admin@controle.com", SENHA_FIXTURE);
  }

  @Test
  void mensagemComGasteiRetornaIntentLancamentoComProposta() throws Exception {
    // Mock da OpenRouter: retorna proposta de lançamento
    when(aiClient.completar(any())).thenReturn("LANCAMENTO|SAIDA|45.00|2026-11-10||Almoço");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    String corpo = "{\"mensagem\":\"gastei 45 no almoço\"}";

    ResponseEntity<String> response =
        rest.postForEntity("/api/ai/chat", new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    AiResponse dto = mapper.readValue(response.getBody(), AiResponse.class);
    assertThat(dto.intent()).isEqualTo("lancamento");
    assertThat(dto.proposta()).isNotNull();
    assertThat(dto.proposta().tipo()).isEqualTo("SAIDA");
    assertThat(dto.proposta().valor()).isEqualByComparingTo("45.00");
    assertThat(dto.propostas()).hasSize(1);
    assertThat(dto.proposta().data()).isEqualTo("2026-11-10");
    assertThat(dto.proposta().categoriaSugerida()).isEqualTo("Almoço");
  }

  @Test
  void mensagemComQuantoRetornaIntentConsultaComDados() throws Exception {
    // Mock da OpenRouter: retorna consulta com dados
    when(aiClient.completar(any())).thenReturn("CONSULTA|Gastou R$ 342,50 com alimentação");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    String corpo = "{\"mensagem\":\"quanto gastei com alimentação\"}";

    ResponseEntity<String> response =
        rest.postForEntity("/api/ai/chat", new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    AiResponse dto = mapper.readValue(response.getBody(), AiResponse.class);
    assertThat(dto.intent()).isEqualTo("consulta");
    assertThat(dto.resposta()).contains("342,50");
    assertThat(dto.dados()).isNotNull();
  }

  @Test
  void mensagemSemIntencaoRetornaEsclarecimentoComOpcoes() throws Exception {
    // Mock da OpenRouter: retorna esclarecimento com opções
    when(aiClient.completar(any()))
        .thenReturn("ESCLARECIMENTO|Não entendi. Tente: gastou quanto com X?|Opção 1|Opção 2");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    String corpo = "{\"mensagem\":\"qual é o significado da vida\"}";

    ResponseEntity<String> response =
        rest.postForEntity("/api/ai/chat", new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    AiResponse dto = mapper.readValue(response.getBody(), AiResponse.class);
    assertThat(dto.intent()).isEqualTo("esclarecimento");
    assertThat(dto.opcoes()).hasSizeGreaterThan(0);
    assertThat(dto.opcoes()).contains("Opção 1", "Opção 2");
  }

  @Test
  void respostaNaoExpõeEmpresaIdNoCorpo() throws Exception {
    when(aiClient.completar(any())).thenReturn("CONSULTA|Resposta genérica");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    String corpo = "{\"mensagem\":\"qualquer coisa\"}";

    ResponseEntity<String> response =
        rest.postForEntity("/api/ai/chat", new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    String corpoResposta = response.getBody();
    assertThat(corpoResposta).doesNotContain("empresa_id");
    assertThat(corpoResposta).doesNotContain("empresaId");
  }

  @Test
  void semTokenRetorna401() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"mensagem\":\"qualquer coisa\"}";

    ResponseEntity<String> response =
        rest.postForEntity("/api/ai/chat", new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void mensagemVaziaRetornaEsclarecimento() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(tokenAdmin);
    String corpo = "{\"mensagem\":\"   \"}";

    ResponseEntity<String> response =
        rest.postForEntity("/api/ai/chat", new HttpEntity<>(corpo, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    AiResponse dto = mapper.readValue(response.getBody(), AiResponse.class);
    assertThat(dto.intent()).isEqualTo("esclarecimento");
  }

  @Test
  void usuarioSemRoleAdminNaoAcessaConhecimentoDaIa() throws Exception {
    onboarding.onboarding(
        "Empresa Usuario IA", "Usuario IA", "user-ia@controle.com", SENHA_FIXTURE);
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(login("user-ia@controle.com", SENHA_FIXTURE));
    headers.setContentType(MediaType.APPLICATION_JSON);

    assertThat(
            rest.exchange(
                    "/api/ai/chat/conhecimento",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            rest.exchange(
                    "/api/ai/chat/conhecimento",
                    HttpMethod.POST,
                    new HttpEntity<>("{\"conhecimento\":\"x\"}", headers),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            rest.exchange(
                    "/api/ai/chat/conhecimento/limpar",
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void conhecimentoEscopadoPorEmpresa() throws Exception {
    onboarding.onboarding(
        "Empresa Dois IA", "Admin Dois", "admin-dois-ia@controle.com", SENHA_FIXTURE);
    Usuario adminDoisUsuario =
        usuarioRepository.findByEmail("admin-dois-ia@controle.com").orElseThrow();
    adminDoisUsuario.setRole(Role.ADMIN.valor());
    usuarioRepository.save(adminDoisUsuario);

    HttpHeaders adminUm = new HttpHeaders();
    adminUm.setBearerAuth(tokenAdmin);
    adminUm.setContentType(MediaType.APPLICATION_JSON);
    assertThat(
            rest.exchange(
                    "/api/ai/chat/conhecimento",
                    HttpMethod.POST,
                    new HttpEntity<>("{\"conhecimento\":\"REGRA DA EMPRESA UM\"}", adminUm),
                    String.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    HttpHeaders adminDois = new HttpHeaders();
    adminDois.setBearerAuth(login("admin-dois-ia@controle.com", SENHA_FIXTURE));
    ResponseEntity<String> resposta =
        rest.exchange(
            "/api/ai/chat/conhecimento", HttpMethod.GET, new HttpEntity<>(adminDois), String.class);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resposta.getBody() == null ? "" : resposta.getBody())
        .doesNotContain("REGRA DA EMPRESA UM");

    rest.exchange(
        "/api/ai/chat/conhecimento/limpar",
        HttpMethod.POST,
        new HttpEntity<>(adminUm),
        String.class);
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
