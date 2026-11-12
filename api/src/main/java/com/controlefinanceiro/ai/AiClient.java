package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP para a OpenRouter (modelo Gemma 4 via OpenRouter API). Compatível com a API de chat
 * completions da OpenAI.
 *
 * <p><strong>Estratégia de modelos (fallback em cascata):</strong>
 *
 * <ol>
 *   <li><b>Cascata de modelos gratuitos</b> — primário, secundário e terciário, na ordem
 *       configurada em {@code ai.openrouter.modelos}. A OpenRouter recebe a lista com {@code
 *       route=fallback}: se o modelo atual falhar (provedor sobrecarregado, modelo fora do ar), ela
 *       tenta o próximo da cascata sem nova chamada da aplicação.
 *   <li><b>Último recurso</b> — {@code ai.openrouter.modelos-reserva}. Entra em campo quando a
 *       cascata inteira falha. Existe porque o limite de modelos :free é <b>por conta e por dia</b>
 *       (50 req/dia na conta free): quando ele estoura, todos os modelos gratuitos caem juntos e
 *       nenhuma cascata de modelos gratuitos resolve. Sem a reserva, o chat cairia todo dia após as
 *       50 primeiras mensagens. Para desativar, defina a variável de ambiente {@code
 *       AI_OPENROUTER_MODELOS_RESERVA} com o valor {@code none}.
 * </ol>
 *
 * <p>O parse da resposta usa Jackson ({@link ObjectMapper}) em vez de manipulação de string: o
 * campo {@code choices[0].message.content} pode conter aspas, quebras de linha e caracteres de
 * escape, o que quebraria um parse ingênuo por {@code indexOf}.
 */
@Component
public class AiClient {

  private static final Logger LOG = LoggerFactory.getLogger(AiClient.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final long PAUSA_ENTRE_TENTATIVAS_MS = 1500L;

  /** Valores que desligam uma lista de modelos (último recurso). */
  private static final java.util.Set<String> DESATIVA =
      java.util.Set.of("none", "off", "null", "-", "nenhum");

  private final RestClient restClient;
  private final String modelo;
  private final List<String> modelos;
  private final List<String> modelosReserva;
  private final boolean configurado;

  public AiClient(
      @Value("${ai.openrouter.api-key:}") String apiKey,
      @Value("${ai.openrouter.modelo:google/gemma-4-31b-it:free}") String modelo,
      @Value("${ai.openrouter.modelos:}") String modelosConfig,
      @Value("${ai.openrouter.modelos-reserva:}") String modelosReservaConfig) {
    this.modelo = modelo;
    this.configurado = apiKey != null && !apiKey.isBlank();

    // Cascata de modelos gratuitos (primário -> secundário -> terciário).
    this.modelos = listaDeModelos(modelosConfig, modelo);
    // Último recurso: entra quando a cascata inteira falha.
    this.modelosReserva = listaDeModelos(modelosReservaConfig, null);

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
   * <p>Etapa 1: cascata de modelos gratuitos (a OpenRouter tenta cada um da lista). Etapa 2: último
   * recurso, só quando a cascata inteira falha.
   */
  public String completar(List<Map<String, Object>> mensagens) {
    if (!configurado) {
      throw new IllegalStateException(
          "ai.openrouter.api-key não configurada. Defina a env var OPENROUTER_API_KEY.");
    }

    // Etapa 1 — cascata de modelos gratuitos.
    try {
      return executar(mensagens, modelos, "cascata", true);
    } catch (RestClientResponseException e) {
      if (modelosReserva.isEmpty()) {
        throw indisponivel(e);
      }
      LOG.warn(
          "Cascata de modelos gratuitos falhou (HTTP {}). Indo para o último recurso: {}",
          e.getStatusCode().value(),
          modelosReserva);
      // Etapa 2 — último recurso (modelo pago): o limite de :free é por conta/dia e derruba
      // todos os modelos gratuitos de uma vez.
      try {
        return executar(mensagens, modelosReserva, "reserva", false);
      } catch (RestClientResponseException segunda) {
        throw indisponivel(segunda);
      }
    }
  }

  /**
   * Executa a chamada em uma lista de modelos. Com mais de um modelo, usa {@code models} + {@code
   * route=fallback} para que a OpenRouter tente o próximo quando o atual falhar.
   *
   * @param repetirEm429 quando true, faz uma segunda tentativa após uma pausa curta (429 do
   *     provedor costuma ser passageiro)
   */
  private String executar(
      List<Map<String, Object>> mensagens, List<String> lista, String etapa, boolean repetirEm429) {
    Map<String, Object> corpo = montarCorpo(mensagens, lista);

    try {
      return extrairConteudo(chamar(corpo), etapa);
    } catch (RestClientResponseException e) {
      if (repetirEm429 && e.getStatusCode().value() == 429) {
        try {
          Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
          return extrairConteudo(chamar(corpo), etapa);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw new ServicoIaIndisponivelException(
              "O serviço de IA está temporariamente indisponível. Tente novamente em instantes.",
              interrupted);
        }
      }
      throw e;
    }
  }

  /** Converte a falha da OpenRouter em erro com mensagem que o usuário entende. */
  private ServicoIaIndisponivelException indisponivel(RestClientResponseException e) {
    if (e.getStatusCode().value() == 429) {
      return new ServicoIaIndisponivelException(
          "O serviço de IA está sobrecarregado agora (limite de requisições). Tente novamente em"
              + " alguns instantes.",
          e);
    }
    return new ServicoIaIndisponivelException(
        "Não foi possível falar com o serviço de IA agora. Tente novamente em instantes.", e);
  }

  /**
   * Converte a configuração (lista separada por vírgula) em lista de modelos.
   *
   * <p>Valores sentinela ({@code none}, {@code off}, {@code null}, {@code -}) e string vazia
   * desativam a lista — assim dá para desligar o último recurso só com variável de ambiente (em
   * Windows, uma env var vazia é removida pelo sistema, então o sentinela é o caminho confiável).
   */
  private List<String> listaDeModelos(String config, String padrao) {
    List<String> lista =
        java.util.Arrays.stream(config.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .filter(s -> !DESATIVA.contains(s.toLowerCase(java.util.Locale.ROOT)))
            .distinct()
            .toList();
    if (!lista.isEmpty()) {
      return lista;
    }
    return padrao == null ? List.of() : List.of(padrao);
  }

  /**
   * Monta o corpo da requisição. Com mais de um modelo, usa {@code models} + {@code route=fallback}
   * para que a OpenRouter tente o próximo quando o provedor do atual falhar.
   */
  private Map<String, Object> montarCorpo(List<Map<String, Object>> mensagens, List<String> lista) {
    Map<String, Object> corpo = new java.util.LinkedHashMap<>();
    if (lista.size() > 1) {
      corpo.put("models", lista);
      corpo.put("route", "fallback");
    } else if (lista.size() == 1) {
      corpo.put("model", lista.get(0));
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
   * trouxer um objeto {@code error}, lança {@link ServicoIaIndisponivelException} com a mensagem do
   * provedor.
   */
  private String extrairConteudo(String json, String etapa) {
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

    // Log do modelo que atendeu: com fallback em cascata é essencial saber qual foi usado
    // (o :free esgota o quota diário da conta e a OpenRouter cai para o próximo da lista).
    LOG.info(
        "Resposta da IA servida pelo modelo {} (etapa: {})",
        raiz.path("model").asText("desconhecido"),
        etapa);

    return content.asText("");
  }
}
