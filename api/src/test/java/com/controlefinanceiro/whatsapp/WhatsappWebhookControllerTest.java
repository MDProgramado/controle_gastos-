package com.controlefinanceiro.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

  private WhatsappWebhookService mockService;
  private WhatsappWebhookController controller;

  @BeforeEach
  void setUp() {
    mockService = mock(WhatsappWebhookService.class);
    controller = new WhatsappWebhookController(mockService);
  }

  private String calcularAssinatura(String payload, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(key);
    byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    for (byte b : hmacBytes) {
      sb.append(String.format("%02x", b));
    }
    return "sha256=" + sb.toString();
  }

  @Test
  void webhookSemAssinaturaRetorna401() throws Exception {
    controller.setAppSecret("test-app-secret-1234567890123456");

    String payload = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
    ResponseEntity<?> resposta = controller.receber(null, payload);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void webhookComAssinaturaInvalidaRetorna401() throws Exception {
    controller.setAppSecret("test-app-secret-1234567890123456");

    String payload = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
    ResponseEntity<?> resposta = controller.receber("sha256=assinaturainvalida", payload);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void webhookComAssinaturaValidaRetorna200() throws Exception {
    controller.setAppSecret("test-app-secret-1234567890123456");

    String payload = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
    String assinatura = calcularAssinatura(payload, "test-app-secret-1234567890123456");
    ResponseEntity<?> resposta = controller.receber(assinatura, payload);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void webhookComSegredoVazioRetorna503MesmoComAssinaturaValida() throws Exception {
    controller.setAppSecret("");

    String payload = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";
    ResponseEntity<?> resposta = controller.receber("sha256=qualquercoisa", payload);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    verifyNoInteractions(mockService);
  }

  @Test
  void verificacaoSemTokenConfiguradoRetorna503() {
    controller.setVerifyToken("");

    ResponseEntity<?> resposta = controller.verificar("subscribe", "", "challenge-123");

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void verificacaoComTokenInvalidoRetorna403() {
    controller.setVerifyToken("token-configurado");

    ResponseEntity<?> resposta = controller.verificar("subscribe", "token-errado", "challenge-123");

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void verificacaoComTokenCorretoRetornaChallenge() {
    controller.setVerifyToken("token-configurado");

    ResponseEntity<?> resposta =
        controller.verificar("subscribe", "token-configurado", "challenge-123");

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resposta.getBody()).isEqualTo("challenge-123");
  }
}
