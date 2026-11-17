package com.controlefinanceiro.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Processamento assíncrono do webhook do WhatsApp.
 *
 * <p>Responde 200 à Meta imediatamente e processa o payload em segundo plano, para não exceder o
 * tempo limite de resposta da Meta (5 segundos).
 */
@Service
public class WhatsappWebhookService {

  private static final Logger LOG = LoggerFactory.getLogger(WhatsappWebhookService.class);

  /**
   * Processa o corpo do webhook da Meta de forma assíncrona.
   *
   * <p>A validação de assinatura já foi feita pelo controller. Este método apenas processa o
   * payload em background, para que o controller possa responder 200 rapidamente à Meta.
   */
  @Async
  public void processarWebhook(String payload, String signature) {
    try {
      LOG.info(
          "Processando webhook WhatsApp (payload de {} bytes)",
          payload == null ? 0 : payload.length());
      // TODO: interpretação da mensagem, busca de telefone vinculado, criação de pendência,
      //       confirmação de lançamento, consulta de dashboard.
    } catch (Exception e) {
      LOG.error("Erro ao processar webhook WhatsApp: {}", e.getMessage());
    }
  }
}
