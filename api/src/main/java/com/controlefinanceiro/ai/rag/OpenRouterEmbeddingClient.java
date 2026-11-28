package com.controlefinanceiro.ai.rag;

import com.controlefinanceiro.ai.AiClient;
import com.controlefinanceiro.ai.UsoIa;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenRouterEmbeddingClient implements EmbeddingClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final RestClient restClient;
  private final String modelo;
  private final boolean configurado;
  private final String dataCollection;
  private final boolean zdr;
  private MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Autowired(required = false)
  public void setMeterRegistry(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Autowired
  public OpenRouterEmbeddingClient(
      @Value("${ai.openrouter.api-key:}") String apiKey,
      @Value("${ai.openrouter.embedding-model:openai/text-embedding-3-small}") String modelo,
      @Value("${ai.openrouter.connect-timeout-ms:3000}") int connectTimeoutMs,
      @Value("${ai.openrouter.read-timeout-ms:30000}") int readTimeoutMs,
      @Value("${ai.openrouter.data-collection:deny}") String dataCollection,
      @Value("${ai.openrouter.zdr:true}") boolean zdr) {
    this.modelo = modelo;
    this.dataCollection = dataCollection;
    this.zdr = zdr;
    this.configurado = apiKey != null && !apiKey.isBlank();
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

  /** Construtor para testes de unidade: RestClient mockado (sem rede). */
  OpenRouterEmbeddingClient(
      String modelo, RestClient restClient, String dataCollection, boolean zdr) {
    this.modelo = modelo;
    this.restClient = restClient;
    this.configurado = true;
    this.dataCollection = dataCollection;
    this.zdr = zdr;
  }

  @Override
  public List<float[]> embedir(List<String> textos) {
    if (!configurado) {
      throw new IllegalStateException(
          "ai.openrouter.api-key não configurada. Defina a env var OPENROUTER_API_KEY.");
    }
    if (textos == null || textos.isEmpty()) {
      return List.of();
    }
    Map<String, Object> corpo = new LinkedHashMap<>();
    corpo.put("model", modelo);
    corpo.put("input", textos);
    // Mesma política de dados do chat: sem coleta/treino e somente Zero Data Retention.
    corpo.put("provider", Map.of("data_collection", dataCollection, "zdr", zdr));
    long inicio = System.nanoTime();
    String json;
    try {
      json =
          restClient
              .post()
              .uri("/embeddings")
              .contentType(MediaType.APPLICATION_JSON)
              .body(corpo)
              .retrieve()
              .body(String.class);
    } catch (org.springframework.web.client.ResourceAccessException e) {
      metricas(UsoIa.falha(OP_EMBEDDING, modelo), "timeout", inicio);
      throw e;
    } catch (RuntimeException e) {
      metricas(UsoIa.falha(OP_EMBEDDING, modelo), "erro", inicio);
      throw e;
    }
    try {
      List<float[]> vetores = extrairVetores(json, textos.size());
      metricas(uso(json), "ok", inicio);
      return vetores;
    } catch (RuntimeException e) {
      metricas(uso(json), "erro", inicio);
      throw e;
    }
  }

  static final String OP_EMBEDDING = "embedding";

  private UsoIa uso(String json) {
    try {
      JsonNode usage = MAPPER.readTree(json).path("usage");
      JsonNode custo = usage.path("cost");
      return new UsoIa(
          OP_EMBEDDING,
          modelo,
          usage.path("prompt_tokens").asLong(0),
          0,
          custo.isNumber() || custo.isTextual() ? new BigDecimal(custo.asText()) : BigDecimal.ZERO,
          1);
    } catch (Exception e) {
      return UsoIa.falha(OP_EMBEDDING, modelo);
    }
  }

  private void metricas(UsoIa uso, String resultado, long inicio) {
    AiClient.registrarMetricas(
        meterRegistry, uso, resultado, Duration.ofNanos(System.nanoTime() - inicio));
  }

  private List<float[]> extrairVetores(String json, int esperado) {
    if (json == null || json.isBlank()) {
      throw new IllegalStateException("Serviço de embeddings devolveu resposta vazia.");
    }
    try {
      JsonNode raiz = MAPPER.readTree(json);
      JsonNode erro = raiz.get("error");
      if (erro != null && !erro.isNull()) {
        throw new IllegalStateException(
            "Serviço de embeddings retornou erro: " + erro.path("message").asText("desconhecido"));
      }
      JsonNode data = raiz.path("data");
      if (!data.isArray() || data.size() != esperado) {
        throw new IllegalStateException("Serviço de embeddings devolveu quantidade inesperada.");
      }
      List<float[]> vetores = new ArrayList<>(esperado);
      for (JsonNode item : data) {
        JsonNode array = item.path("embedding");
        if (!array.isArray()) {
          throw new IllegalStateException("Embedding ausente na resposta.");
        }
        float[] vetor = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
          vetor[i] = array.get(i).floatValue();
        }
        vetores.add(vetor);
      }
      return vetores;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Não foi possível interpretar os embeddings.", e);
    }
  }
}
