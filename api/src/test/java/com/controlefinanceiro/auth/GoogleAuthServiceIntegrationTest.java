package com.controlefinanceiro.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlefinanceiro.usuario.ConviteService;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** D-06/D-07: cadastro, login e convite com ID token Google — PostgreSQL real e Flyway completo. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class GoogleAuthServiceIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  private static final String NOME = "Nome Google";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtService jwtService;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TenantOnboardingService onboarding;
  @Autowired private ConviteService conviteService;

  @MockBean private GoogleTokenValidator googleTokenValidator;

  @Test
  void cadastroComEmailNovoCriaEmpresaUsuarioComGoogleSubESenhaAleatoria() throws Exception {
    String email = "google-cadastro@teste.com";
    long empresasAntes = totalEmpresas();

    String token = autenticar("CADASTRO", null, email, "sub-cadastro");

    assertThat(token).isNotBlank();
    assertThat(totalEmpresas()).isEqualTo(empresasAntes + 1);
    assertThat(coluna("select role from usuario where email = ?", email)).isEqualTo("USER");
    assertThat(coluna("select google_sub from usuario where email = ?", email))
        .isEqualTo("sub-cadastro");
    assertThat(coluna("select email_verificado from usuario where email = ?", email))
        .isEqualTo(true);
    assertThat((String) coluna("select senha_hash from usuario where email = ?", email))
        .startsWith("$2");
  }

  @Test
  void loginComEmailExistenteVinculaGoogleSubNaEmpresaExistente() throws Exception {
    String email = "google-login@teste.com";
    onboarding.onboarding("Empresa Login", "Admin", email, "senha123");
    long empresasAntes = totalEmpresas();
    long empresaId = empresaDoUsuario(email);

    String token = autenticar("LOGIN", null, email, "sub-login");

    assertThat(token).isNotBlank();
    assertThat(totalEmpresas()).isEqualTo(empresasAntes);
    assertThat(coluna("select google_sub from usuario where email = ?", email))
        .isEqualTo("sub-login");
    assertThat(coluna("select email_verificado from usuario where email = ?", email))
        .isEqualTo(true);
    assertThat(empresaDoUsuario(email)).isEqualTo(empresaId);
    assertThat(empresaDaClaim(token)).isEqualTo(empresaId);
  }

  @Test
  void conviteValidoCriaUsuarioNaEmpresaDoConvite() throws Exception {
    String admin = "google-admin-convite@teste.com";
    String convidado = "google-convidado@teste.com";
    onboarding.onboarding("Empresa Convite", "Admin", admin, "senha123");
    long empresaId = empresaDoUsuario(admin);
    String conviteToken = conviteService.criar(empresaId, convidado, "USER", admin).token();
    long empresasAntes = totalEmpresas();

    String token = autenticar("CONVITE", conviteToken, convidado, "sub-convite");

    assertThat(token).isNotBlank();
    assertThat(totalEmpresas()).isEqualTo(empresasAntes);
    assertThat(empresaDoUsuario(convidado)).isEqualTo(empresaId);
    assertThat(coluna("select role from usuario where email = ?", convidado)).isEqualTo("USER");
    assertThat(coluna("select google_sub from usuario where email = ?", convidado))
        .isEqualTo("sub-convite");
    assertThat(coluna("select email_verificado from usuario where email = ?", convidado))
        .isEqualTo(true);
  }

  @Test
  void conviteComEmailDivergenteERejeitado() throws Exception {
    String admin = "google-admin-divergente@teste.com";
    String convidado = "google-convidado-divergente@teste.com";
    onboarding.onboarding("Empresa Divergente", "Admin", admin, "senha123");
    String conviteToken =
        conviteService.criar(empresaDoUsuario(admin), convidado, "USER", admin).token();

    when(googleTokenValidator.validar(anyString()))
        .thenReturn(
            new GoogleTokenValidator.DadosGoogle("sub-divergente", "outro@teste.com", NOME));

    mvc.perform(
            post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new GoogleRequest("id-token-fixture", "CONVITE", conviteToken))))
        .andExpect(status().isBadRequest());

    assertThat(
            jdbc.queryForObject(
                "select count(*) from usuario where email = ?", Long.class, "outro@teste.com"))
        .isZero();
  }

  @Test
  void emailExistenteDeOutraEmpresaEntraNaEmpresaExistente() throws Exception {
    String email = "google-outra-empresa@teste.com";
    onboarding.onboarding("Empresa Existente", "Admin", email, "senha123");
    long empresaId = empresaDoUsuario(email);
    long empresasAntes = totalEmpresas();

    String token = autenticar("LOGIN", null, email, "sub-outra-empresa");

    assertThat(token).isNotBlank();
    assertThat(totalEmpresas()).isEqualTo(empresasAntes);
    assertThat(empresaDaClaim(token)).isEqualTo(empresaId);
  }

  private String autenticar(String intencao, String conviteToken, String email, String sub)
      throws Exception {
    when(googleTokenValidator.validar(anyString()))
        .thenReturn(new GoogleTokenValidator.DadosGoogle(sub, email, NOME));

    String corpo =
        objectMapper.writeValueAsString(
            new GoogleRequest("id-token-fixture", intencao, conviteToken));
    String resposta =
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.idToken").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(resposta).get("token").asText();
  }

  private long totalEmpresas() {
    return jdbc.queryForObject("select count(*) from empresa", Long.class);
  }

  private Object coluna(String sql, String email) {
    return jdbc.queryForObject(sql, Object.class, email);
  }

  private long empresaDoUsuario(String email) {
    return jdbc.queryForObject("select empresa_id from usuario where email = ?", Long.class, email);
  }

  private long empresaDaClaim(String token) {
    Object claim = jwtService.jwtDecoder().decode(token).getClaim("empresa_id");
    assertThat(claim).isInstanceOf(Number.class);
    return ((Number) claim).longValue();
  }
}
