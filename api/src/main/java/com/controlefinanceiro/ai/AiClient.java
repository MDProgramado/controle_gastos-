package com.controlefinanceiro.ai;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP para a OpenRouter (modelo Gemini via OpenRouter API). Compatível com a API de chat
 * completions da OpenAI.
 */
@Component
public class AiClient {

  private final RestClient restClient;
  private final String modelo;
  private final boolean configurado;

  public AiClient(
      @Value("${ai.openrouter.api-key:}") String apiKey,
      @Value("${ai.openrouter.modelo:google/gemini-2.0-flash-001}") String modelo) {
    this.modelo = modelo;
    this.configurado = apiKey != null && !apiKey.isBlank();
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
   * Envia uma lista de mensagens para a OpenRouter e retorna o conteúdo da resposta. Lança
   * IllegalStateException se a chave da API não estiver configurada.
   */
  public String completar(List<Map<String, Object>> mensagens) {
    if (!configurado) {
      throw new IllegalStateException(
          "ai.openrouter.api-key não configurada. Defina a env var OPENROUTER_API_KEY.");
    }

    Map<String, Object> corpo =
        Map.of("model", modelo, "messages", mensagens, "temperature", 0.4, "max_tokens", 1200);

    String texto =
        restClient
            .post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(corpo)
            .retrieve()
            .body(String.class);

    if (texto == null || texto.isBlank()) {
      throw new IllegalStateException("OpenRouter retornou resposta vazia.");
    }

    return extrairConteudo(texto);
  }

  /**
   * Extrai o campo choices[0].message.content da resposta JSON da OpenRouter sem depender de
   * biblioteca JSON externa (usa parse simples).
   */
  private String extrairConteudo(String json) {
    int inicio = json.indexOf("\"content\"");
    if (inicio < 0) {
      throw new IllegalStateException("Resposta da OpenRouter não contém 'content': " + json);
    }
    int doisPontos = json.indexOf(":", inicio);
    if (doisPontos < 0) {
      throw new IllegalStateException("Formato inesperado na resposta da OpenRouter: " + json);
    }
    int valorInicio = json.indexOf('"', doisPontos + 1);
    if (valorInicio < 0) {
      throw new IllegalStateException("Valor de content não encontrado na resposta: " + json);
    }
    int valorFim = json.indexOf('"', valorInicio + 1);
    if (valorFim < 0) {
      throw new IllegalStateException("Fim do valor de content não encontrado: " + json);
    }
    return json.substring(valorInicio + 1, valorFim);
  }
}
