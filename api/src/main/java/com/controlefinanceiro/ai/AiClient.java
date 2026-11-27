package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP para a OpenRouter (API compatível com chat completions).
 *
 * <p>O chat usa cascata por modelo controlada pelo cliente, com orçamento total de tempo. A
 * extração de PDF usa {@link #completarComCascata(List, OpcoesCompletar)} com validação de conteúdo
 * e backoff por modelo. Falhas recuperáveis (429/5xx/402, timeout/conexão, conteúdo vazio/length)
 * avançam para o próximo modelo quando houver orçamento.
 */
@Component
public class AiClient {

  private static final Logger LOG = LoggerFactory.getLogger(AiClient.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final long MARGEM_TENTATIVA_MS = 8000L;

  /** Valores que desligam uma lista de modelos (último recurso). */
  private static final java.util.Set<String> DESATIVA =
      java.util.Set.of("none", "off", "null", "-", "nenhum");

  private final RestClient restClient;
  private final String modelo;
  private final List<String> modelos;
  private final List<String> modelosReserva;
  private final int maxTokensChat;
  private final long chatDeadlineMs;
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
      @Value("${ai.openrouter.modelos-reserva:}") String modelosReservaConfig,
      @Value("${ai.openrouter.connect-timeout-ms:3000}") int connectTimeoutMs,
      @Value("${ai.openrouter.read-timeout-ms:25000}") int readTimeoutMs,
      @Value("${ai.openrouter.max-tokens-chat:2000}") int maxTokensChat,
      @Value("${ai.openrouter.chat-deadline-ms:55000}") long chatDeadlineMs) {
    this.modelo = modelo;
    this.configurado = apiKey != null && !apiKey.isBlank();
    this.modelos = listaDeModelos(modelosConfig, modelo);
    this.modelosReserva = listaDeModelos(modelosReservaConfig, null);
    this.maxTokensChat = maxTokensChat;
    this.chatDeadlineMs = chatDeadlineMs;

    SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
    fabrica.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    fabrica.setReadTimeout(Duration.ofMillis(readTimeoutMs));

    this.restClient =
        configurado
            ? RestClient.builder()
                .requestFactory(fabrica)
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
    this(modelos, modelosReserva, restClient, 2000, 55000L);
  }

  AiClient(
      List<String> modelos,
      List<String> modelosReserva,
      RestClient restClient,
      int maxTokensChat,
      long chatDeadlineMs) {
    this.modelo = modelos.isEmpty() ? "google/gemma-4-31b-it:free" : modelos.get(0);
    this.modelos = modelos;
    this.modelosReserva = modelosReserva;
    this.restClient = restClient;
    this.maxTokensChat = maxTokensChat;
    this.chatDeadlineMs = chatDeadlineMs;
    this.configurado = true;
  }

  /**
   * Envia mensagens para a OpenRouter com cascata por modelo e orçamento total de tempo.
   *
   * <p>Falhas recuperáveis avançam para o próximo modelo: 429/5xx/402, timeout/conexão, resposta
   * vazia, content nulo/vazio e finish_reason=length. O pior caso respeita {@code chatDeadlineMs}.
   */
  public String completar(List<Map<String, Object>> mensagens) {
    return completar(mensagens, System.nanoTime() + Duration.ofMillis(chatDeadlineMs).toNanos());
  }

  /**
   * Variante com deadline absoluto (System.nanoTime) compartilhado por toda a mensagem, permitindo
   * que retries/fallbacks consumam o mesmo orçamento.
   */
  public String completar(List<Map<String, Object>> mensagens, long deadlineNanos) {
    if (!configurado) {
      throw new IllegalStateException(
          "ai.openrouter.api-key n?o configurada. Defina a env var OPENROUTER_API_KEY.");
    }

    List<String> candidatos = listaCandidatosChat();
    ServicoIaIndisponivelException ultimaFalha = null;

    for (int i = 0; i < candidatos.size(); i++) {
      String modeloAtual = candidatos.get(i);
      long restante = restanteMsAte(deadlineNanos);
      if (restante < MARGEM_TENTATIVA_MS) {
        LOG.warn(
            "Or?amento do chat esgotado antes do modelo {} (restante={}ms)", modeloAtual, restante);
        break;
      }

      try {
        Map<String, Object> corpo =
            montarCorpo(mensagens, List.of(modeloAtual), 0.4, maxTokensChat);
        String json = chamar(corpo);
        return extrairConteudo(json, "chat", modeloAtual);
      } catch (ServicoIaIndisponivelException e) {
        ultimaFalha = e;
        LOG.warn(
            "Modelo {} falhou no chat (recuper?vel). Avan?ando. motivo={}",
            modeloAtual,
            e.getMessage());
      } catch (RestClientResponseException e) {
        int status = e.getStatusCode().value();
        boolean recuperavel = recuperavel(status) || ehErroReasoning(status, e);
        ultimaFalha = indisponivel(e);
        LOG.warn(
            "Modelo {} falhou no chat (HTTP {}, recuper?vel={}). {}",
            modeloAtual,
            status,
            recuperavel,
            recuperavel ? "Avan?ando." : "Encerrando.");
        if (!recuperavel) {
          throw ultimaFalha;
        }
      } catch (ResourceAccessException e) {
        ultimaFalha =
            new ServicoIaIndisponivelException(
                "O servi?o de IA demorou para responder. Tente novamente em instantes.", e);
        LOG.warn("Modelo {} falhou por timeout/conex?o no chat. Avan?ando.", modeloAtual, e);
      }
    }

    if (ultimaFalha != null) {
      throw ultimaFalha;
    }
    throw new ServicoIaIndisponivelException(
        "N?o foi poss?vel falar com o servi?o de IA agora. Tente novamente em instantes.");
  }

  private List<String> listaCandidatosChat() {
    java.util.LinkedHashSet<String> candidatos = new java.util.LinkedHashSet<>(modelos);
    candidatos.addAll(modelosReserva);
    if (candidatos.isEmpty()) {
      candidatos.add(modelo);
    }
    return List.copyOf(candidatos);
  }

  private long restanteMsAte(long deadlineNanos) {
    return Math.max(0, Duration.ofNanos(deadlineNanos - System.nanoTime()).toMillis());
  }

  private long restanteMs(long inicioNanos) {
    long decorrido = Duration.ofNanos(System.nanoTime() - inicioNanos).toMillis();
    return Math.max(0, chatDeadlineMs - decorrido);
  }

  private boolean ehErroReasoning(int status, RestClientResponseException e) {
    if (status != 400) return false;
    try {
      String body = e.getResponseBodyAsString();
      return body != null && body.toLowerCase(java.util.Locale.ROOT).contains("reasoning");
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private boolean recuperavel(int status) {
    return status == 402 || status == 429 || status >= 500;
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
          boolean recuperavel = status == 402 || status == 429 || status >= 500;
          if (recuperavel && tentativa < opcoes.tentativasPorModelo()) {
            dormir(pausaComRetryAfter(e, opcoes.esperaInicialMs(), tentativa));
            continue;
          }
          LOG.warn("Modelo {} falhou (HTTP {}). Avançando de modelo.", modeloAtual, status);
          break;
        } catch (ResourceAccessException e) {
          if (tentativa < opcoes.tentativasPorModelo()) {
            dormir(opcoes.esperaInicialMs() * (long) Math.pow(2, tentativa - 1));
            continue;
          }
          LOG.warn("Modelo {} falhou por timeout/conexão. Avançando de modelo.", modeloAtual, e);
          break;
        } catch (ServicoIaIndisponivelException e) {
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

  private Map<String, Object> montarCorpo(List<Map<String, Object>> mensagens, List<String> lista) {
    return montarCorpo(mensagens, lista, 0.4, 800);
  }

  /**
   * Monta corpo de chat completion. Usa sempre {@code model} único (sem {@code route:fallback}),
   * pois a cascata é controlada pelo cliente. Inclui {@code reasoning.effort=none} quando suportado
   * pelo provedor.
   */
  private Map<String, Object> montarCorpo(
      List<Map<String, Object>> mensagens,
      List<String> lista,
      Double temperatura,
      Integer maxTokens) {
    Map<String, Object> corpo = new java.util.LinkedHashMap<>();
    String modeloAlvo = lista.size() == 1 ? lista.get(0) : modelo;
    corpo.put("model", modeloAlvo);
    corpo.put("messages", mensagens);
    corpo.put("temperature", temperatura != null ? temperatura : 0.4);
    corpo.put("max_tokens", maxTokens != null ? maxTokens : 800);
    corpo.put("reasoning", Map.of("effort", "low", "exclude", true));
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

  private String extrairConteudo(String json, String etapa) {
    return extrairConteudo(json, etapa, null);
  }

  /**
   * Extrai conteúdo da resposta. Content nulo/vazio, finish_reason=length e objeto error são falhas
   * recuperáveis (o chamador do chat pode avançar para outro modelo).
   */
  private String extrairConteudo(String json, String etapa, String modeloSolicitado) {
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

    JsonNode choice = raiz.path("choices").path(0);
    JsonNode content = choice.path("message").path("content");
    String finishReason = choice.path("finish_reason").asText("");
    String modeloUsado =
        raiz.path("model").asText(modeloSolicitado == null ? "desconhecido" : modeloSolicitado);
    LOG.info("Resposta IA: modelo={} finish_reason={} etapa={}", modeloUsado, finishReason, etapa);

    if ("length".equals(finishReason)) {
      throw new ServicoIaIndisponivelException(
          "O serviço de IA interrompeu a resposta por limite de tamanho.");
    }
    if (content.isMissingNode() || content.isNull()) {
      throw new ServicoIaIndisponivelException("O serviço de IA devolveu uma resposta vazia.");
    }
    String texto = content.asText("");
    if (texto.isBlank()) {
      throw new ServicoIaIndisponivelException("O serviço de IA devolveu uma resposta vazia.");
    }
    return texto;
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
