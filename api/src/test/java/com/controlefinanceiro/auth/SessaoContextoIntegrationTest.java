package com.controlefinanceiro.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import com.controlefinanceiro.usuario.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SessaoContextoIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TenantOnboardingService onboarding;
  @Autowired private JwtService jwtService;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void tokenExpõeEmpresaEUsuario() throws Exception {
    String token = login("sessao@teste.com");

    mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.empresa_id").isNumber())
        .andExpect(jsonPath("$.usuario_id").isNumber())
        .andExpect(jsonPath("$.email").value("sessao@teste.com"));
  }

  @Test
  void roleForaDoEnumRetorna401() throws Exception {
    onboarding.onboarding("Sessao Role", "Admin", "sessaorole@teste.com", "senha123");
    Long empresaId = id("SELECT empresa_id FROM usuario WHERE email = ?", "sessaorole@teste.com");
    Long usuarioId = id("SELECT id FROM usuario WHERE email = ?", "sessaorole@teste.com");

    Empresa empresa = new Empresa();
    empresa.setId(empresaId);
    Usuario usuario = new Usuario();
    usuario.setId(usuarioId);
    usuario.setEmail("sessaorole@teste.com");
    usuario.setEmpresa(empresa);
    usuario.setRole("ROOT");
    usuario.setTokenVersion(0);
    String token = jwtService.gerarToken(usuario);

    mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void usuarioDesativadoOuComVersaoDivergenteRetorna401() throws Exception {
    String email = "sessaodesativado@teste.com";
    String token = login(email);

    jdbc.update("UPDATE usuario SET ativo = false WHERE email = ?", email);

    mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());

    jdbc.update("UPDATE usuario SET ativo = true, token_version = 1 WHERE email = ?", email);

    mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  private String login(String email) throws Exception {
    onboarding.onboarding("Sessao", "Admin", email, "senha123");
    String resposta =
        mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"senha\":\"senha123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(resposta).get("token").asText();
  }

  private Long id(String sql, String email) {
    Long valor = jdbc.queryForObject(sql, Long.class, email);
    assertThat(valor).isNotNull();
    return valor;
  }
}
