package com.controlefinanceiro.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlefinanceiro.usuario.ConviteResponse;
import com.controlefinanceiro.usuario.ConviteService;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthPublicoIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TenantOnboardingService onboarding;
  @Autowired private ConviteService conviteService;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void registroPublicoRetornaToken() throws Exception {
    mvc.perform(
            post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"empresaNome\":\"Publica\",\"nome\":\"Admin\","
                        + "\"email\":\"publico@teste.com\",\"senha\":\"senha123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void registroComEmailDuplicadoRetorna400Generico() throws Exception {
    String corpo =
        "{\"empresaNome\":\"Duplicada\",\"nome\":\"Admin\","
            + "\"email\":\"duplicado@teste.com\",\"senha\":\"senha123\"}";
    mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isOk());

    mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(corpo))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Não foi possível concluir o cadastro"));
  }

  @Test
  void conviteAceitoRetornaToken() throws Exception {
    onboarding.onboarding("Convites", "Admin", "adminconvite@teste.com", "senha123");
    Long empresaId =
        jdbc.queryForObject(
            "SELECT empresa_id FROM usuario WHERE email = 'adminconvite@teste.com'", Long.class);
    String adminToken =
        objectMapper
            .readTree(
                mvc.perform(
                        post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                "{\"email\":\"adminconvite@teste.com\","
                                    + "\"senha\":\"senha123\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("token")
            .asText();

    ConviteResponse convite =
        conviteService.criar(empresaId, "convidado@teste.com", "USER", "adminconvite@teste.com");
    String token =
        objectMapper
            .readTree(
                mvc.perform(
                        post("/api/auth/convites/aceitar")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                "{\"token\":\""
                                    + convite.token()
                                    + "\",\"nome\":\"Convidado\",\"senha\":\"senha123\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("token")
            .asText();

    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("convidado@teste.com"));
  }

  @Test
  void usuariosSemTokenRetorna401() throws Exception {
    mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
  }

  @Test
  void usuariosComTokenUserRetorna403() throws Exception {
    onboarding.onboarding("Permissoes", "Admin", "adminperm@teste.com", "senha123");
    Long empresaId =
        jdbc.queryForObject(
            "SELECT empresa_id FROM usuario WHERE email = 'adminperm@teste.com'", Long.class);
    ConviteResponse convite =
        conviteService.criar(empresaId, "userperm@teste.com", "USER", "adminperm@teste.com");
    String token =
        objectMapper
            .readTree(
                mvc.perform(
                        post("/api/auth/convites/aceitar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                "{\"token\":\""
                                    + convite.token()
                                    + "\",\"nome\":\"User\",\"senha\":\"senha123\"}"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("token")
            .asText();

    mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
