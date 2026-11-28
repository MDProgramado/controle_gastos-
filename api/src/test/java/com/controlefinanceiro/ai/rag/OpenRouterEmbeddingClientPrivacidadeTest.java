package com.controlefinanceiro.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenRouterEmbeddingClientPrivacidadeTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  @DisplayName("embeddings enviam provider.data_collection=deny e provider.zdr=true")
  void embeddingsEnviamPoliticaDeDados() {
    List<Map<String, Object>> corpos = new ArrayList<>();
    RestClient.Builder builder = RestClient.builder().baseUrl("https://openrouter.test/api/v1");
    MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    servidor
        .expect(requestTo("https://openrouter.test/api/v1/embeddings"))
        .andExpect(
            request ->
                corpos.add(
                    MAPPER.readValue(
                        ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8),
                        new TypeReference<Map<String, Object>>() {})))
        .andRespond(
            withSuccess(
                "{\"data\":[{\"embedding\":[0.1,0.2]}],\"model\":\"m\","
                    + "\"usage\":{\"prompt_tokens\":8,\"cost\":0.0000016}}",
                MediaType.APPLICATION_JSON));

    OpenRouterEmbeddingClient client =
        new OpenRouterEmbeddingClient("m", builder.build(), "deny", true);
    io.micrometer.core.instrument.MeterRegistry registry =
        new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    client.setMeterRegistry(registry);
    List<float[]> vetores = client.embedir(List.of("texto"));

    // 3.2: métricas com operacao=embedding, sem tags de tenant.
    assertThat(
            registry
                .get("ia.tokens")
                .tag("operacao", "embedding")
                .tag("tipo", "entrada")
                .counter()
                .count())
        .isEqualTo(8);
    assertThat(
            registry
                .get("ia.chamadas")
                .tag("operacao", "embedding")
                .tag("resultado", "ok")
                .counter()
                .count())
        .isEqualTo(1);

    assertThat(vetores).hasSize(1);
    assertThat(corpos.get(0))
        .containsEntry("provider", Map.of("data_collection", "deny", "zdr", true));
  }
}
