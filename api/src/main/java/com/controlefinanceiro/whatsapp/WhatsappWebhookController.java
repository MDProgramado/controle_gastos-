package com.controlefinanceiro.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook público da Meta Cloud API (WhatsApp).
 *
 * <p>GET /webhooks/whatsapp — responde o desafio de verificação da Meta (hub.verify_token). POST
 * /webhooks/whatsapp — valida X-Hub-Signature-256 com o app secret, processa as mensagens e
 * responde 200 à Meta em até 5 segundos (processamento pesado em fila assíncrona).
 *
 * <p>Este controller NÃO contém lógica de negócio — apenas delega ao {@link
 * WhatsappWebhookService}, que chama os mesmos services do site.
 */
@RestController
public class WhatsappWebhookController {

  private final WhatsappWebhookService webhookService;

  @Value("${whatsapp.verify.token:}")
  private String verifyToken;

  @Value("${whatsapp.app.secret:}")
  private String appSecret;

  public WhatsappWebhookController(WhatsappWebhookService webhookService) {
    this.webhookService = webhookService;
  }

  public void setAppSecret(String appSecret) {
    this.appSecret = appSecret;
  }

  public void setVerifyToken(String verifyToken) {
    this.verifyToken = verifyToken;
  }

  /**
   * Desafio de verificação da Meta. Retorna hub.challenge se hub.verify_token coincidir com o token
   * configurado na env var WHATSAPP_VERIFY_TOKEN.
   */
  @GetMapping("/webhooks/whatsapp")
  public ResponseEntity<?> verificar(
      @RequestParam(name = "hub.mode", required = false) String mode,
      @RequestParam(name = "hub.verify_token", required = false) String token,
      @RequestParam(name = "hub.challenge", required = false) String challenge) {

    if ("subscribe".equals(mode) && verifyToken != null && verifyToken.equals(token)) {
      return ResponseEntity.ok(challenge);
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token de verificação inválido");
  }

  /**
   * Recebe mensagens da Meta. Valida a assinatura HMAC-SHA256 antes de processar.
   *
   * @param signature valor do header X-Hub-Signature-256 (formato sha256=<hex>)
   * @param payload corpo bruto da requisição (JSON da Meta)
   * @return 200 sempre que a assinatura for válida (mesmo em caso de erro interno, para a Meta não
   *     reenviar); 401 se a assinatura for inválida
   */
  @PostMapping("/webhooks/whatsapp")
  public ResponseEntity<?> receber(
      @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
      @RequestBody String payload) {

    if (!validarAssinatura(signature, payload)) {
      // Assinatura inválida: 401, nada processado
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Assinatura inválida");
    }

    // Processamento assíncrono: responde 200 imediatamente à Meta
    webhookService.processarWebhook(payload, signature);
    return ResponseEntity.ok().build();
  }

  /**
   * Valida a assinatura HMAC-SHA256 da Meta. Calcula o hash do corpo com o app secret e compara com
   * o header X-Hub-Signature-256.
   */
  private boolean validarAssinatura(String signature, String payload) {
    if (signature == null || appSecret == null || appSecret.isBlank()) {
      return false;
    }

    String expected = "sha256=";
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key =
          new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(key);
      byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      expected += bytesToHex(hmacBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      return false;
    }

    // Comparação em tempo constante para evitar timing attack
    if (signature.length() != expected.length()) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < signature.length(); i++) {
      result |= signature.charAt(i) ^ expected.charAt(i);
    }
    return result == 0;
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
