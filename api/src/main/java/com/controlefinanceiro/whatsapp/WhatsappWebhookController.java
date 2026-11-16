package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.common.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
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
import org.springframework.web.server.ResponseStatusException;

/**
 * Webhook público da Meta Cloud API (WhatsApp).
 *
 * <p>GET /webhooks/whatsapp — responde o desafio de verificação da Meta (hub.verify_token).
 * <p>POST /webhooks/whatsapp — valida X-Hub-Signature-256 com o app secret. Se o segredo estiver
 * ausente ou a assinatura for inválida, responde 401 e nada processa.
 *
 * <p>Este controller NÃO contém lógica de negócio — apenas valida a assinatura e delega ao
 * {@link WhatsappWebhookService}.
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

  /**
   * Desafio de verificação da Meta. Retorna hub.challenge se hub.verify_token coincidir com o
   * token configurado na env var WHATSAPP_VERIFY_TOKEN.
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
   * <p>Se WHATSAPP_APP_SECRET estiver vazio, toda requisição recebe 401. Se a assinatura for
   * inválida, também 401. Nada é processado sem assinatura válida.
   */
  @PostMapping("/webhooks/whatsapp")
  public ResponseEntity<?> receber(
      @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
      @RequestBody String payload) {

    if (appSecret == null || appSecret.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WHATSAPP_APP_SECRET não configurado");
    }

    if (!validarAssinatura(signature, payload)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura inválida");
    }

    webhookService.processarWebhook(payload, signature);
    return ResponseEntity.ok().build();
  }

  /**
   * Valida a assinatura HMAC-SHA256 da Meta. Calcula o hash do corpo com o app secret e compara
   * com o header X-Hub-Signature-256 usando comparação de tempo constante.
   */
  private boolean validarAssinatura(String signature, String payload) {
    if (signature == null || !signature.startsWith("sha256=")) {
      return false;
    }

    String expectedHex;
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec key = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(key);
      byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hmacBytes) {
        sb.append(String.format("%02x", b));
      }
      expectedHex = sb.toString();
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      return false;
    }

    String fornecido = signature.substring("sha256=".length());
    if (fornecido.length() != expectedHex.length()) {
      return false;
    }

    int result = 0;
    for (int i = 0; i < fornecido.length(); i++) {
      result |= fornecido.charAt(i) ^ expectedHex.charAt(i);
    }
    return result == 0;
  }
}
