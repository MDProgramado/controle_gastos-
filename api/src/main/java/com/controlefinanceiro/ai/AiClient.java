package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
 * <p><strong>Extração de PDFs (fase 5):</strong> {@link #completarComCascata(List,
 * OpcoesCompletar)} generaliza a chamada com per-modelo iteração, backoff exponencial (1,5 s → 3 s
 * → 6 s), honra o header {@code Retry-After}, avança de modelo quando o conteúdo não passa no
 * validador (ex.: JSON inválido) e expõe {@code modeloUsado} + {@code degradado}. O caminho do chat
 * ({@link #completar(List)}) mantém o comportamento original da fase 4, intacto.
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

  /**
   * Opções para {@link #completarComCascata(List, OpcoesCompletar)}.
   *
   * @param modelos lista de modelos em ordem de prioridade (gratuitos no caminho da extração)
   * @param temperatura temperatura do modelo ({@code null} = default 0.4 do chat)
   * @param maxTokens limite de tokens de saída ({@code null} = default 800 do chat)
   * @param tentativasPorModelo tentativas por modelo antes de avançar na cascata
   * @param esperaInicialMs pausa inicial entre tentativas (backoff exponencial 1×, 2×, 4×)
   * @param conteudoValido validador opcional do conteúdo; se reprovar, avança para o próximo modelo
   */
  public record OpcoesCompletar(
      List<String> modelos,
      Double temperatura,
      Integer maxTokens,
      int tentativasPorModelo,
      long esperaInicialMs,
      Predicate<String> conteudoValido) {}

  /**
   * Resultado de {@link #completarComCascata}: conteúdo, modelo que respondeu e se houve fallback.
   */
  public record RespostaCompletar(
      String conteudo, String modeloUsado, boolean degradado, int chamadas) {}

  @Autowired
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

  /** Construtor para testes de unidade: injeta o {@link RestClient} mockado (sem rede). */
  AiClient(List<String> modelos, List<String> modelosReserva, RestClient restClient) {
    this.modelo = modelos.isEmpty() ? "google/gemma-4-31b-it:free" : modelos.get(0);
    this.modelos = modelos;
    this.modelosReserva = modelosReserva;
    this.restClient = restClient;
    this.configurado = true;
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
        dormir(PAUSA_ENTRE_TENTATIVAS_MS);
        return extrairConteudo(chamar(corpo), etapa);
      }
      throw e;
    }
  }

  /**
   * Envia mensagens com iteração por modelo, backoff exponencial e validador de conteúdo (fase 5).
   *
   * <p>Por modelo da lista, até {@link OpcoesCompletar#tentativasPorModelo()} tentativas com
   * backoff {@code esperaInicialMs * 2^(tentativa-1)} (1,5 s → 3 s → 6 s), honrando o header {@code
   * Retry-After}. Gatilhos de avanço para o próximo modelo: HTTP 402 (cobrança, retry não resolve),
   * 429/5xx esgotadas as tentativas, e conteúdo que não passe no {@link
   * OpcoesCompletar#conteudoValido()}. Esgotada a lista, tenta {@code modelos-reserva} (último
   * recurso, igual ao chat) antes de lançar {@link ServicoIaIndisponivelException}.
   *
   * @return {@link RespostaCompletar} com o conteúdo, o modelo que de fato respondeu ({@code
   *     modeloUsado}, campo {@code model} da OpenRouter) e se veio de fallback ({@code degradado})
   */
  public RespostaCompletar completarComCascata(
      List<Map<String, Object>> mensagens, OpcoesCompletar opcoes) {
    if (!configurado) {
      throw new IllegalStateException(
          "ai.openrouter.api-key não configurada. Defina a env var OPENROUTER_API_KEY.");
    }

    List<String> principal =
        (opcoes.modelos() == null || opcoes.modelos().isEmpty()) ? modelos : opcoes.modelos();
    RespostaCompletar resposta = tentarEmLista(mensagens, opcoes, principal, false);
    if (resposta != null) {
      return resposta;
    }

    // Último recurso — reserva paga, igual ao chat: o limite de :free é por conta/dia e
    // derruba todos os modelos gratuitos de uma vez.
    if (!modelosReserva.isEmpty() && !modelosReserva.equals(principal)) {
      LOG.warn(
          "Cascata de modelos gratuitos falhou. Indo para o último recurso: {}", modelosReserva);
      resposta = tentarEmLista(mensagens, opcoes, modelosReserva, true);
      if (resposta != null) {
        return resposta;
      }
    }

    throw new ServicoIaIndisponivelException(
        "Serviço de IA indisponível no momento. Tente novamente em instantes.");
  }

  /**
   * Tenta cada modelo de uma lista em sequência. Devolve {@code null} quando todos falham (para o
   * chamador decidir se há uma próxima etapa, como a reserva paga).
   */
  private RespostaCompletar tentarEmLista(
      List<Map<String, Object>> mensagens,
      OpcoesCompletar opcoes,
      List<String> lista,
      boolean etapaReserva) {
    int chamadas = 0;
    for (int i = 0; i < lista.size(); i++) {
      String modeloAtual = lista.get(i);
      boolean degradado = etapaReserva || i > 0;

      for (int tentativa = 1; tentativa <= opcoes.tentativasPorModelo(); tentativa++) {
        chamadas++;
        try {
          String json =
              chamar(
                  montarCorpo(
                      mensagens, List.of(modeloAtual), opcoes.temperatura(), opcoes.maxTokens()));
          String conteudo = extrairConteudo(json, etapaReserva ? "reserva" : "cascata");
          String modeloUsado = extrairModeloUsado(json);

          if (opcoes.conteudoValido() != null && !opcoes.conteudoValido().test(conteudo)) {
            LOG.warn(
                "Modelo {} devolveu conteúdo fora do esperado (tentativa {}). Avançando de modelo.",
                modeloAtual,
                tentativa);
            // Com temperature 0.0 o mesmo prompt tende a repetir o erro: trocar de modelo resolve.
            break;
          }

          LOG.info("Resposta da IA servida pelo modelo {} (chamada {})", modeloUsado, chamadas);
          return new RespostaCompletar(conteudo, modeloUsado, degradado, chamadas);
        } catch (RestClientResponseException e) {
          int status = e.getStatusCode().value();
          boolean recuperavel = status == 429 || status >= 500;
          if (recuperavel && tentativa < opcoes.tentativasPorModelo()) {
            dormir(pausaComRetryAfter(e, opcoes.esperaInicialMs(), tentativa));
            continue;
          }
          LOG.warn("Modelo {} falhou (HTTP {}). Avançando de modelo.", modeloAtual, status);
          break;
        } catch (ServicoIaIndisponivelException e) {
          // Resposta vazia, objeto error ou formato inesperado: falha do modelo, avança.
          LOG.warn(
              "Modelo {} devolveu resposta inválida ({}). Avançando de modelo.",
              modeloAtual,
              e.getMessage());
          break;
        }
      }
    }
    return null;
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

  /** Pausa entre tentativas; interrupção do thread aborta a operação com 503 legível. */
  private void dormir(long pausa) {
    try {
      Thread.sleep(pausa);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServicoIaIndisponivelException(
          "O serviço de IA está temporariamente indisponível. Tente novamente em instantes.", e);
    }
  }

  /**
   * Pausa da próxima tentativa: {@code Retry-After} (segundos) quando a OpenRouter informar, senão
   * backoff exponencial {@code esperaInicialMs * 2^(tentativa-1)} (1,5 s → 3 s → 6 s).
   */
  private long pausaComRetryAfter(
      RestClientResponseException e, long esperaInicialMs, int tentativa) {
    var headers = e.getResponseHeaders();
    String retryAfter = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
    if (retryAfter != null) {
      try {
        return Long.parseLong(retryAfter.trim()) * 1000L;
      } catch (NumberFormatException ignored) {
        // valor estranho no header: usa o backoff exponencial
      }
    }
    return esperaInicialMs * (long) Math.pow(2, tentativa - 1);
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
            .map(s -> s.trim())
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
    return montarCorpo(mensagens, lista, 0.4, 800);
  }

  /**
   * Monta o corpo com temperature/max_tokens parametrizados (fase 5); {@code null} mantém os
   * defaults do chat (0.4 / 800). Com um único modelo, envia {@code model}; com vários, {@code
   * models} + {@code route=fallback}.
   */
  private Map<String, Object> montarCorpo(
      List<Map<String, Object>> mensagens,
      List<String> lista,
      Double temperatura,
      Integer maxTokens) {
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
    corpo.put("temperature", temperatura != null ? temperatura : 0.4);
    corpo.put("max_tokens", maxTokens != null ? maxTokens : 800);
    return corpo;
  }

  /** Executa a chamada HTTP devolvendo o corpo bruto da OpenRouter. */
  private String chamar(Map<String, Object> corpo) {
    return restClient
        .post()
        .uri("/chat/completions")
        .contentType(new MediaType("application", "json"))
        .body(java.util.Objects.requireNonNull(corpo))
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

  /** Extrai o campo {@code model} da resposta da OpenRouter (modelo que de fato respondeu). */
  private String extrairModeloUsado(String json) {
    try {
      JsonNode raiz = MAPPER.readTree(json);
      return raiz.path("model").asText("desconhecido");
    } catch (Exception e) {
      return "desconhecido";
    }
  }
}
