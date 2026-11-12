package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP para a OpenRouter (modelo Gemini via OpenRouter API). Compatível com a API de chat
 * completions da OpenAI.
 *
 * <p>O parse da resposta usa Jackson ({@link ObjectMapper}) em vez de manipulação de string: o
 * campo {@code choices[0].message.content} pode conter aspas, quebras de linha e caracteres de
 * escape, o que quebraria um parse ingênuo por {@code indexOf}.
 */
@Component
public class AiClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final long PAUSA_ENTRE_TENTATIVAS_MS = 1500L;

  private final RestClient restClient;
  private final String modelo;
  private final List<String> modelos;
  private final boolean configurado;

  public AiClient(
      @Value("${ai.openrouter.api-key:}") String apiKey,
      @Value("${ai.openrouter.modelo:google/gemini-2.5-flash-lite}") String modelo,
      @Value("${ai.openrouter.modelos:}") String modelosConfig) {
    this.modelo = modelo;
    this.configurado = apiKey != null && !apiKey.isBlank();

    // Lista de fallback: quando o provedor do primeiro modelo está sobrecarregado (429), a
    // OpenRouter tenta o próximo da lista (route=fallback).
    List<String> lista =
        java.util.Arrays.stream(modelosConfig.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();
    this.modelos = lista.isEmpty() ? List.of(modelo) : lista;

    this.restClient =
        configurado
            ? RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("HTTP-Referer", "https://controlefinanceiro.app")
                .defaultHeader("X-Title", "Controle Financeiro IA")
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build()
            : null;
  }

  /**
   * Envia uma lista de mensagens para a OpenRouter e retorna o conteúdo da resposta.
   *
   * <p>Em 429 (provedor sobrecarregado) tenta uma segunda vez após uma pausa curta. Qualquer outra
   * falha vira {@link ServicoIaIndisponivelException} com mensagem legível.
   */
  public String completar(List<Map<String, Object>> mensagens) {
    if (!configurado) {
      throw new IllegalStateException(
          "ai.openrouter.api-key não configurada. Defina a env var OPENROUTER_API_KEY.");
    }

    Map<String, Object> corpo = montarCorpo(mensagens);

    try {
      return extrairConteudo(chamar(corpo));
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 429) {
        // Provedor temporariamente sobrecarregado: uma nova tentativa costuma passar.
        try {
          Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
          return extrairConteudo(chamar(corpo));
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new ServicoIaIndisponivelException(
              "O serviço de IA está temporariamente indisponível. Tente novamente em instantes.",
              interrupted);
        } catch (RestClientResponseException segunda) {
          throw new ServicoIaIndisponivelException(
              "O serviço de IA está sobrecarregado agora. Tente novamente em alguns instantes.",
              segunda);
        }
      }
      throw new ServicoIaIndisponivelException(
          "Não foi possível falar com o serviço de IA agora. Tente novamente em instantes.", e);
    }
  }

  /**
   * Monta o corpo da requisição. Com mais de um modelo, usa {@code models} + {@code route=fallback}
   * para que a OpenRouter tente o próximo quando o provedor do atual falhar.
   */
  private Map<String, Object> montarCorpo(List<Map<String, Object>> mensagens) {
    Map<String, Object> corpo = new java.util.LinkedHashMap<>();
    if (modelos.size() > 1) {
      corpo.put("models", modelos);
      corpo.put("route", "fallback");
    } else {
      corpo.put("model", modelo);
    }
    corpo.put("messages", mensagens);
    corpo.put("temperature", 0.4);
    corpo.put("max_tokens", 800);
    return corpo;
  }

  /** Executa a chamada HTTP devolvendo o corpo bruto da OpenRouter. */
  private String chamar(Map<String, Object> corpo) {
    return restClient
        .post()
        .uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .body(corpo)
        .retrieve()
        .body(String.class);
  }

  /**
   * Extrai o campo {@code choices[0].message.content} da resposta JSON da OpenRouter. Se o corpo
   * trouxer um objeto {@code error}, lança IllegalStateException com a mensagem do provedor.
   */
  private String extrairConteudo(String json) {
    if (json == null || json.isBlank()) {
      throw new ServicoIaIndisponivelException("O serviço de IA devolveu uma resposta vazia.");
    }

    JsonNode raiz;
    try {
      raiz = MAPPER.readTree(json);
    } catch (Exception e) {
      throw new ServicoIaIndisponivelException(
          "Não foi possível interpretar a resposta do serviço de IA.", e);
    }

    JsonNode erro = raiz.get("error");
    if (erro != null && !erro.isNull()) {
      String msg = erro.path("message").asText("erro desconhecido");
      throw new ServicoIaIndisponivelException("O serviço de IA retornou erro: " + msg);
    }

    JsonNode content = raiz.path("choices").path(0).path("message").path("content");
    if (content.isMissingNode() || content.isNull()) {
      throw new ServicoIaIndisponivelException("O serviço de IA devolveu uma resposta inesperada.");
    }
    return content.asText("");
  }
}
