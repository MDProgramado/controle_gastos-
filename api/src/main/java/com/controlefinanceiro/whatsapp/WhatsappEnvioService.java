package com.controlefinanceiro.whatsapp;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Envio de mensagens via Meta Cloud API (WhatsApp). Toda falha de envio registra erro e não reenvia
 * lançamento. O processamento pesado do webhook é executado assincronamente (@Async) para responder
 * 200 à Meta em até 5 segundos.
 *
 * <p>Configuração por variável de ambiente: WHATSAPP_ACCESS_TOKEN, WHATSAPP_PHONE_NUMBER_ID,
 * WHATSAPP_API_URL (padrão v21.0) e WHATSAPP_TEMPLATE_CODIGO. Sem token e phone number id, o envio
 * é pulado com aviso em log (útil em teste e em dev).
 */
@EnableAsync
@Service
public class WhatsappEnvioService {

  private static final Logger LOG = LoggerFactory.getLogger(WhatsappEnvioService.class);

  private final WhatsappMensagemProcessadaRepository mensagemRepository;
  private final RestClient restClient;
  private final String phoneNumberId;
  private final String templateCodigoPadrao;
  private final boolean configurado;

  public WhatsappEnvioService(
      WhatsappMensagemProcessadaRepository mensagemRepository,
      RestClient.Builder restClientBuilder,
      @Value("${WHATSAPP_API_URL:https://graph.facebook.com/v21.0}") String apiUrl,
      @Value("${WHATSAPP_PHONE_NUMBER_ID:}") String phoneNumberId,
      @Value("${WHATSAPP_ACCESS_TOKEN:}") String accessToken,
      @Value("${WHATSAPP_TEMPLATE_CODIGO:codigo_verificacao_gastos}") String templateCodigo) {
    this(
        mensagemRepository,
        restClientBuilder,
        apiUrl,
        phoneNumberId,
        accessToken,
        templateCodigo,
        3000,
        15000);
  }

  @Autowired
  public WhatsappEnvioService(
      WhatsappMensagemProcessadaRepository mensagemRepository,
      RestClient.Builder restClientBuilder,
      @Value("${WHATSAPP_API_URL:https://graph.facebook.com/v21.0}") String apiUrl,
      @Value("${WHATSAPP_PHONE_NUMBER_ID:}") String phoneNumberId,
      @Value("${WHATSAPP_ACCESS_TOKEN:}") String accessToken,
      @Value("${WHATSAPP_TEMPLATE_CODIGO:codigo_verificacao_gastos}") String templateCodigo,
      @Value("${whatsapp.connect-timeout-ms:3000}") int connectTimeoutMs,
      @Value("${whatsapp.read-timeout-ms:15000}") int readTimeoutMs) {
    this.mensagemRepository = mensagemRepository;
    this.phoneNumberId = phoneNumberId;
    this.templateCodigoPadrao = templateCodigo;
    this.configurado = !phoneNumberId.isBlank() && !accessToken.isBlank();
    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    fabrica.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    this.restClient =
        restClientBuilder
            .requestFactory(fabrica)
            .baseUrl(apiUrl)
            .defaultHeader("Authorization", "Bearer " + accessToken)
            .build();
    if (!configurado) {
      LOG.warn(
          "WhatsApp não configurado (WHATSAPP_ACCESS_TOKEN/WHATSAPP_PHONE_NUMBER_ID ausentes)");
    }
  }

  /**
   * Envia uma mensagem de texto simples para um número E.164.
   *
   * @param numeroE164 número no formato +5571999999999
   * @param texto corpo da mensagem (não logar este conteúdo em produção)
   * @param messageId id da mensagem vindo da Meta (para idempotência, opcional no envio)
   */
  @Async(WhatsappAsyncConfig.EXECUTOR_NAME)
  public void enviarTexto(String numeroE164, String texto, String messageId) {
    if (!configurado) {
      registrarSemConfiguracao(numeroE164, messageId);
      return;
    }
    Map<String, Object> corpo =
        Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", digitos(numeroE164),
            "type", "text",
            "text", Map.of("body", texto == null ? "" : texto));
    boolean ok = enviarPayload(numeroE164, corpo, "texto");
    if (messageId != null) {
      registrarMensagemProcessada(messageId, ok ? "ENVIADO" : "FALHA_ENVIO");
    }
  }

  /**
   * Envia mensagem de confirmação de lançamento. Por enquanto, o texto traz as instruções de
   * resposta (sim/não + id). Botões interativos ficam para uma etapa posterior.
   *
   * @param numeroE164 número do usuário
   * @param texto resumo do lançamento proposto
   * @param callbackConfirmar payload do botão Confirmar (não usado ainda)
   * @param callbackCancelar payload do botão Cancelar (não usado ainda)
   * @param messageId id da mensagem para idempotência
   */
  @Async(WhatsappAsyncConfig.EXECUTOR_NAME)
  public void enviarConfirmacao(
      String numeroE164,
      String texto,
      String callbackConfirmar,
      String callbackCancelar,
      String messageId) {
    if (!configurado) {
      registrarSemConfiguracao(numeroE164, messageId);
      return;
    }
    Map<String, Object> corpo =
        Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", digitos(numeroE164),
            "type", "text",
            "text", Map.of("body", texto == null ? "" : texto));
    boolean ok = enviarPayload(numeroE164, corpo, "confirmacao");
    if (messageId != null) {
      registrarMensagemProcessada(messageId, ok ? "CONFIRMACAO_PENDENTE" : "FALHA_ENVIO");
    }
  }

  @Async(WhatsappAsyncConfig.EXECUTOR_NAME)
  public void enviarCodigo(String numeroE164, String codigo) {
    enviarCodigo(numeroE164, codigo, templateCodigoPadrao);
  }

  /**
   * Envia o código de verificação de 6 dígitos via template de autenticação aprovado da Meta.
   *
   * @param numeroE164 número destino
   * @param codigo código de 6 dígitos (não logar em texto plano)
   * @param templateNome nome do template aprovado (env var WHATSAPP_TEMPLATE_CODIGO)
   */
  @Async(WhatsappAsyncConfig.EXECUTOR_NAME)
  public void enviarCodigo(String numeroE164, String codigo, String templateNome) {
    if (!configurado) {
      LOG.info(
          "Código não enviado: WhatsApp não configurado para número {}",
          mascararNumero(numeroE164));
      return;
    }
    // Template de autenticação com corpo (código) e botão "Copiar código" (mesmo valor).
    List<Map<String, Object>> componentes =
        List.of(
            Map.of("type", "body", "parameters", List.of(Map.of("type", "text", "text", codigo))),
            Map.of(
                "type", "button",
                "sub_type", "url",
                "index", "0",
                "parameters", List.of(Map.of("type", "text", "text", codigo))));
    Map<String, Object> corpo =
        Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", digitos(numeroE164),
            "type", "template",
            "template",
                Map.of(
                    "name", templateNome,
                    "language", Map.of("code", "pt_BR"),
                    "components", componentes));
    enviarPayload(numeroE164, corpo, "codigo");
  }

  /** Responde uma única vez com instrução de cadastro para número não vinculado. */
  @Async(WhatsappAsyncConfig.EXECUTOR_NAME)
  public void enviarInstrucaoCadastro(String numeroE164, String messageId) {
    String texto =
        "Este número não está cadastrado no Controle de Gastos. Acesse o app e cadastre seu telefone"
            + " para começar a registrar seus gastos por aqui.";
    if (!configurado) {
      registrarSemConfiguracao(numeroE164, messageId);
      return;
    }
    Map<String, Object> corpo =
        Map.of(
            "messaging_product", "whatsapp",
            "recipient_type", "individual",
            "to", digitos(numeroE164),
            "type", "text",
            "text", Map.of("body", texto));
    boolean ok = enviarPayload(numeroE164, corpo, "instrucao_cadastro");
    if (messageId != null) {
      registrarMensagemProcessada(messageId, ok ? "INSTRUCAO_CADASTRO" : "FALHA_ENVIO");
    }
  }

  /**
   * Faz a chamada POST /{phone-number-id}/messages. Retorna true em sucesso. Nunca loga o corpo da
   * requisição nem a resposta da Meta (podem conter número ou código).
   */
  private boolean enviarPayload(String numeroE164, Map<String, Object> corpo, String tipo) {
    try {
      restClient
          .post()
          .uri("/{id}/messages", phoneNumberId)
          .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
          .body(corpo)
          .retrieve()
          .toBodilessEntity();
      LOG.info("Envio de {} para número {} concluído", tipo, mascararNumero(numeroE164));
      return true;
    } catch (RestClientResponseException e) {
      LOG.error(
          "Meta recusou envio de {} para número {}: HTTP {}",
          tipo,
          mascararNumero(numeroE164),
          e.getStatusCode().value());
      return false;
    } catch (RestClientException e) {
      LOG.error(
          "Falha de rede ao enviar {} para número {}: {}",
          tipo,
          mascararNumero(numeroE164),
          e.getClass().getSimpleName());
      return false;
    }
  }

  private void registrarSemConfiguracao(String numeroE164, String messageId) {
    LOG.warn("Envio pulado (WhatsApp não configurado) para número {}", mascararNumero(numeroE164));
    if (messageId != null) {
      registrarMensagemProcessada(messageId, "NAO_CONFIGURADO");
    }
  }

  /** Remove o '+' do E.164: a Cloud API espera só dígitos em "to". */
  String digitos(String numeroE164) {
    return numeroE164 == null ? "" : numeroE164.replaceFirst("^\\+", "");
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
    LOG.info("Expiração de confirmações pendentes para telefone {}", usuarioTelefoneId);
  }
}
