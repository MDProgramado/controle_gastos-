package com.controlefinanceiro;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * R3/AUTH-03 (coração da regra de tenant): o {@code empresa_id} vem SÓ do token — um client hostil
 * mandando {@code empresa_id} no body é ignorado (provado por eco no probe de teste). Cobre também
 * /api/auth/me (sessão para o BFF) e expiração do token (D-01: 8h sem refresh).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
class AuthTenantIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  /** Fixture ÓBVIO de teste — a senha real vive só no env var do serviço (D-04). */
  private static final String SENHA_FIXTURE = "fixture-somente-testes-it";

  @Autowired private TestRestTemplate rest;

  @Autowired private ObjectMapper mapper;

  @Test
  void corpoHostilComEmpresaIdNaoDefineTenant_TokenManda() throws Exception {
    String token = login();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);
    // client malicioso tentando trocar o tenant para 999
    String corpoHostil = "{\"empresa_id\": 999}";

    ResponseEntity<String> response =
        rest.postForEntity(
            "/api/test/tenant", new HttpEntity<>(corpoHostil, headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    // tenant usado é o do TOKEN (empresa 1 do seed), nunca 999 do body (AUTH-03)
    JsonNode eco = mapper.readTree(response.getBody());
    assertThat(eco.get("empresa_id").asLong()).isEqualTo(1L);
  }

  @Test
  void meComTokenValidoDevolveSessaoCorreta() throws Exception {
    String token = login();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<String> response =
        rest.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    JsonNode sessao = mapper.readTree(response.getBody());
    assertThat(sessao.get("email").asText()).isEqualTo("admin@controle.com");
    assertThat(sessao.get("empresa_id").asLong()).isEqualTo(1L);
    assertThat(sessao.get("role").asText()).isEqualTo("ADMIN");
  }

  @Test
  void tokenExpiradoRetorna401() throws Exception {
    // D-01: expiração de 8h sem refresh — token com exp no passado é rejeitado (401)
    String tokenExpirado = tokenExpirado("admin@controle.com", 1L, "ADMIN");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenExpirado);
    ResponseEntity<String> response =
        rest.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private String login() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String corpo = "{\"email\":\"admin@controle.com\",\"senha\":\"" + SENHA_FIXTURE + "\"}";
    ResponseEntity<String> response =
        rest.postForEntity("/api/auth/login", new HttpEntity<>(corpo, headers), String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    try {
      return mapper.readTree(response.getBody()).get("token").asText();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** Assina um token HS256 já expirado com o MESMO fixture de segredo do surefire (D-04). */
  private String tokenExpirado(String email, long empresaId, String role) throws Exception {
    String segredo = System.getenv("JWT_SECRET");
    Date agora = new Date();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(email)
            .claim("empresa_id", empresaId)
            .claim("role", role)
            .issueTime(new Date(agora.getTime() - 10 * 3600_000L))
            .expirationTime(new Date(agora.getTime() - 2 * 3600_000L))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(segredo.getBytes(StandardCharsets.UTF_8)));
    return jwt.serialize();
  }
}
