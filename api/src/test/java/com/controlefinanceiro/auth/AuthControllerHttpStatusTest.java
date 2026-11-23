package com.controlefinanceiro.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.controlefinanceiro.config.ProblemDetailExceptionHandler;
import com.controlefinanceiro.usuario.ConviteService;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerHttpStatusTest {

  private MockMvc mvc;
  private ObjectMapper objectMapper;
  private GoogleAuthService googleAuthService;

  @BeforeEach
  void setup() {
    googleAuthService = mock(GoogleAuthService.class);
    AuthController controller =
        new AuthController(
            mock(AuthService.class),
            mock(TenantOnboardingService.class),
            mock(ConviteService.class),
            googleAuthService);
    mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ProblemDetailExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void tokenGoogleInvalidoRetorna401() throws Exception {
    when(googleAuthService.autenticarComToken(any())).thenThrow(new GoogleTokenInvalidoException());

    mvc.perform(
            post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("id-token-fixture")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("Token Google inválido"));
  }

  @Test
  void controllerDelegaUmaUnicaVezAoServico() throws Exception {
    when(googleAuthService.autenticarComToken(any())).thenReturn(new TokenResponse("jwt-proprio"));

    mvc.perform(
            post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("id-token-fixture")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-proprio"));

    verify(googleAuthService)
        .autenticarComToken(new GoogleRequest("id-token-fixture", "LOGIN", null));
  }

  @Test
  void verificacaoGoogleIndisponivelRetorna503() throws Exception {
    when(googleAuthService.autenticarComToken(any()))
        .thenThrow(new GoogleVerificacaoIndisponivelException());

    mvc.perform(
            post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("id-token-fixture")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.detail").value("Verificação do token Google indisponível"));
  }

  @Test
  void idTokenAcimaDe4096CaracteresRetorna400() throws Exception {
    String idToken = "a".repeat(4097);

    mvc.perform(
            post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(idToken)))
        .andExpect(status().isBadRequest());
  }

  private String corpo(String idToken) throws Exception {
    return objectMapper.writeValueAsString(new GoogleRequest(idToken, "LOGIN", null));
  }
}
