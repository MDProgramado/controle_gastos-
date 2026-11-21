package com.controlefinanceiro.whatsapp;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envio de mensagens via Meta Cloud API (WhatsApp). Toda falha de envio registra erro e não reenvia
 * lançamento. O processamento pesado do webhook é executado assincronamente (@Async) para responder
 * 200 à Meta em até 5 segundos.
 */
@EnableAsync
@Service
public class WhatsappEnvioService {

  private static final Logger LOG = LoggerFactory.getLogger(WhatsappEnvioService.class);

  private final WhatsappMensagemProcessadaRepository mensagemRepository;

  public WhatsappEnvioService(WhatsappMensagemProcessadaRepository mensagemRepository) {
    this.mensagemRepository = mensagemRepository;
  }

  /**
   * Envia uma mensagem de texto simples para um número E.164.
   *
   * @param numeroE164 número no formato +5571999999999
   * @param texto corpo da mensagem (não logar este conteúdo em produção)
   * @param messageId id da mensagem vindo da Meta (para idempotência, opcional no envio)
   */
  @Async
  public void enviarTexto(String numeroE164, String texto, String messageId) {
    try {
      // TODO: integração real com Meta Cloud API (POST /vXX.XX/{phone-number-id}/messages)
      // usando env vars WHATSAPP_API_URL e WHATSAPP_ACCESS_TOKEN.
      // Por enquanto, registra a intenção de envio sem logar o corpo financeiro.
      LOG.info(
          "Envio de texto para número {} (messageId={}, tamanho={})",
          mascararNumero(numeroE164),
          messageId,
          texto == null ? 0 : texto.length());
      if (messageId != null) {
        registrarMensagemProcessada(messageId, "ENVIADO");
      }
    } catch (Exception e) {
      LOG.error(
          "Falha ao enviar mensagem WhatsApp para número {}: {}",
          mascararNumero(numeroE164),
          e.getMessage());
      // Não reenvia lançamento. Apenas registra o erro.
    }
  }

  /**
   * Envia mensagem com botões interativos "Confirmar" e "Cancelar" para confirmação de lançamento.
   *
   * @param numeroE164 número do usuário
   * @param texto resumo do lançamento proposto
   * @param callbackConfirmar payload do botão Confirmar
   * @param callbackCancelar payload do botão Cancelar
   * @param messageId id da mensagem para idempotência
   */
  @Async
  public void enviarConfirmacao(
      String numeroE164,
      String texto,
      String callbackConfirmar,
      String callbackCancelar,
      String messageId) {
    try {
      // TODO: integração real com Meta Cloud API (mensagem interativa com buttons)
      LOG.info(
          "Envio de confirmação para número {} (messageId={}, textoTamanho={})",
          mascararNumero(numeroE164),
          messageId,
          texto == null ? 0 : texto.length());
      if (messageId != null) {
        registrarMensagemProcessada(messageId, "CONFIRMACAO_PENDENTE");
      }
    } catch (Exception e) {
      LOG.error(
          "Falha ao enviar confirmação WhatsApp para número {}: {}",
          mascararNumero(numeroE164),
          e.getMessage());
    }
  }

  @Async
  public void enviarCodigo(String numeroE164, String codigo) {
    enviarCodigo(numeroE164, codigo, "template_padrao");
  }

  /**
   * Envia o código de verificação de 6 dígitos via template aprovado da Meta.
   *
   * @param numeroE164 número destino
   * @param codigo código de 6 dígitos (não logar em texto plano)
   * @param templateNome nome do template aprovado (env var WHATSAPP_TEMPLATE_CODIGO)
   */
  @Async
  public void enviarCodigo(String numeroE164, String codigo, String templateNome) {
    try {
      // TODO: integração real com Meta Cloud API (template de autenticação)
      LOG.info(
          "Envio de código de verificação para número {} (template={})",
          mascararNumero(numeroE164),
          templateNome);
    } catch (Exception e) {
      LOG.error(
          "Falha ao enviar código WhatsApp para número {}: {}",
          mascararNumero(numeroE164),
          e.getMessage());
    }
  }

  /** Responde uma única vez com instrução de cadastro para número não vinculado. */
  @Async
  public void enviarInstrucaoCadastro(String numeroE164, String messageId) {
    try {
      // TODO: envio real via Meta
      LOG.info(
          "Envio de instrução de cadastro para número {} (messageId={})",
          mascararNumero(numeroE164),
          messageId);
      if (messageId != null) {
        registrarMensagemProcessada(messageId, "INSTRUCAO_CADASTRO");
      }
    } catch (Exception e) {
      LOG.error(
          "Falha ao enviar instrução de cadastro para número {}: {}",
          mascararNumero(numeroE164),
          e.getMessage());
    }
  }

  @Transactional
  void registrarMensagemProcessada(String messageId, String resultado) {
    if (mensagemRepository.existsByMessageId(messageId)) {
      return;
    }
    WhatsappMensagemProcessada m = new WhatsappMensagemProcessada();
    m.setMessageId(messageId);
    m.setResultado(resultado);
    mensagemRepository.save(m);
  }

  /** Máscara do número para logs: nunca exibe o número completo. */
  String mascararNumero(String numeroE164) {
    if (numeroE164 == null || numeroE164.length() < 8) {
      return "***";
    }
    return numeroE164.substring(0, 4) + "****" + numeroE164.substring(numeroE164.length() - 4);
  }

  /** Limpa confirmações expiradas para um telefone (marca como EXPIRADA). */
  @Transactional
  public void expirarConfirmacoes(Long usuarioTelefoneId) {
    // TODO: implementar limpeza via repositório quando necessário
    OffsetDateTime agora = OffsetDateTime.now();
    LOG.info(
        "Expiração de confirmações pendentes para telefone {} em {}", usuarioTelefoneId, agora);
  }
}
