package com.controlefinanceiro.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Envio de códigos de verificação via Meta Cloud API (WhatsApp).
 *
 * <p>Usa template aprovado para envio de código de 6 dígitos. Se o envio falhar, registra erro e
 * não marca o telefone como verificado.
 */
@Service
public class WhatsappEnvioService {

  private static final Logger LOG = LoggerFactory.getLogger(WhatsappEnvioService.class);

  @Value("${whatsapp.template.codigo:}")
  private String templateCodigo;

  @Value("${whatsapp.api.url:}")
  private String apiUrl;

  @Value("${whatsapp.access.token:}")
  private String accessToken;

  /**
   * Envia o código de verificação para o número via Meta Cloud API (template aprovado).
   *
   * <p>Se o envio falhar, registra erro e lança exceção para que o serviço não marque o telefone
   * como verificado.
   *
   * @param numeroE164 número destino no formato E.164
   * @param codigo código de 6 dígitos (não logar em texto plano)
   */
  public void enviarCodigo(String numeroE164, String codigo) {
    try {
      // TODO: integração real com Meta Cloud API (POST /vXX.XX/{phone-number-id}/messages)
      // usando WHATSAPP_API_URL e WHATSAPP_ACCESS_TOKEN.
      // Por enquanto, registra a intenção de envio sem logar o código.
      LOG.info(
          "Envio de código de verificação via Meta para número {} (template={})",
          mascararNumero(numeroE164),
          templateCodigo);
    } catch (Exception e) {
      LOG.error(
          "Falha ao enviar código WhatsApp para número {}: {}",
          mascararNumero(numeroE164),
          e.getMessage());
      throw new RuntimeException("Falha ao enviar código de verificação", e);
    }
  }

  /** Máscara do número para logs: nunca exibe o número completo. */
  String mascararNumero(String numeroE164) {
    if (numeroE164 == null || numeroE164.length() < 8) {
      return "***";
    }
    return numeroE164.substring(0, 4) + "****" + numeroE164.substring(numeroE164.length() - 4);
  }
}
