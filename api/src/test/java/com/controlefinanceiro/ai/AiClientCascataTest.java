package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

/**
 * Testes de unidade da cascata de modelos de {@link AiClient} (fase 5, sem rede e sem custo de
 * cota).
 *
 * <p>Usa {@link MockRestServiceServer} ligado ao {@link RestClient.Builder}: as respostas da
 * "OpenRouter" são um roteiro sequencial e o corpo de cada requisição pode ser capturado para
 * inspeção (guarda de modelo pago, temperature, max_tokens).
 */
class AiClientCascataTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Cascata gratuita: nunca contém modelo pago (a reserva paga fica na segunda lista). */
  private static final List<String> CASCATA_FREE =
      List.of("google/gemma-4-31b-it:free", "google/gemma-4-26b-a4b-it:free", "openrouter/free");

  private static final String BASE_URL = "https://openrouter.test/api/v1";
  private static final String URL_CHAT = BASE_URL + "/chat/completions";

  private static final List<Map<String, Object>> MENSAGENS =
      List.of(Map.of("role", "user", "content", "x"));

  @Test
  @DisplayName("429 no primário serve o secundário: modeloUsado e degradado corretos")
  void quatroNovesNoPrimarioServeSecundario() {
    RestClient restClient =
        novoRestClient(
            fila(erro429(), sucesso(resposta("{\"itens\":[]}", "google/gemma-4-26b-a4b-it:free"))),
            null);

    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    AiClient.RespostaCompletar resposta = client.completarComCascata(MENSAGENS, opcoes(1, null));

    assertThat(resposta.conteudo()).isEqualTo("{\"itens\":[]}");
    assertThat(resposta.modeloUsado()).isEqualTo("google/gemma-4-26b-a4b-it:free");
    assertThat(resposta.degradado()).isTrue();
    assertThat(resposta.chamadas()).isEqualTo(2);
  }

  @Test
  @DisplayName(
      "429 em todos os modelos gratuitos (sem reserva) lança ServicoIaIndisponivelException")
  void quatroNovesEmTodosLancaExcecao() {
    RestClient restClient = novoRestClient(fila(erro429(), erro429(), erro429()), null);

    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    assertThatThrownBy(() -> client.completarComCascata(MENSAGENS, opcoes(1, null)))
        .isInstanceOf(ServicoIaIndisponivelException.class)
        .hasMessageContaining("Tente novamente");
  }

  @Test
  @DisplayName("conteúdo que falha no validador avança para o secundário")
  void conteudoReprovadoAvancaParaSecundario() {
    RestClient restClient =
        novoRestClient(
            fila(
                // JSON válido, mas fora do formato esperado da extração
                sucesso(resposta("{\"foo\":\"bar\"}", "google/gemma-4-31b-it:free")),
                sucesso(resposta("{\"itens\":[]}", "google/gemma-4-26b-a4b-it:free"))),
            null);

    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    AiClient.RespostaCompletar resposta =
        client.completarComCascata(
            MENSAGENS,
            new AiClient.OpcoesCompletar(
                CASCATA_FREE, 0.0, 8000, 1, 10L, c -> c.trim().startsWith("{\"itens\"")));

    assertThat(resposta.conteudo()).isEqualTo("{\"itens\":[]}");
    assertThat(resposta.modeloUsado()).isEqualTo("google/gemma-4-26b-a4b-it:free");
    assertThat(resposta.degradado()).isTrue();
  }

  @Test
  @DisplayName("nenhum modelo pago entra no corpo da requisição")
  void nenhumModeloPagoNaRequisicao() {
    List<Map<String, Object>> corposEnviados = new ArrayList<>();
    RestClient restClient =
        novoRestClient(fila(sucesso(resposta("ok", "google/gemma-4-31b-it:free"))), corposEnviados);

    AiClient client = new AiClient(CASCATA_FREE, List.of("google/gemma-4-31b-it"), restClient);

    client.completarComCascata(MENSAGENS, opcoes(1, null));

    assertThat(corposEnviados).isNotEmpty();
    for (Map<String, Object> corpo : corposEnviados) {
      String model = (String) corpo.get("model");
      assertThat(model).as("corpo enviado: %s", corpo).isNotNull();
      assertThat(model.endsWith(":free") || model.equals("openrouter/free"))
          .as("modelo da requisição (%s) deve ser gratuito", model)
          .isTrue();
      assertThat(model)
          .as("reserva paga nunca é usada nesta chamada")
          .isNotEqualTo("google/gemma-4-31b-it");
    }
  }

  @Test
  @DisplayName("parâmetros da extração vão no corpo: temperature 0.0 e max_tokens 8000")
  void parametrosDaExtracaoVaoNoCorpo() {
    List<Map<String, Object>> corposEnviados = new ArrayList<>();
    RestClient restClient =
        novoRestClient(fila(sucesso(resposta("ok", "google/gemma-4-31b-it:free"))), corposEnviados);

    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    client.completarComCascata(MENSAGENS, opcoes(1, null));

    for (Map<String, Object> corpo : corposEnviados) {
      assertThat(corpo.get("temperature")).isEqualTo(0.0);
      assertThat(corpo.get("max_tokens")).isEqualTo(8000);
      assertThat(corpo).doesNotContainKey("route");
    }
  }

  @Test
  @DisplayName("header Retry-After é respeitado e o primário atende na segunda tentativa")
  void retryAfterEhRespeitado() {
    RestClient restClient =
        novoRestClient(
            fila(
                erro429ComRetryAfter("1"),
                sucesso(resposta("ok após retry", "google/gemma-4-31b-it:free"))),
            null);

    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    AiClient.RespostaCompletar resposta = client.completarComCascata(MENSAGENS, opcoes(2, null));

    assertThat(resposta.conteudo()).isEqualTo("ok após retry");
    assertThat(resposta.modeloUsado()).isEqualTo("google/gemma-4-31b-it:free");
    assertThat(resposta.degradado()).isFalse(); // primário atendeu
    assertThat(resposta.chamadas()).isEqualTo(2);
  }

  @Test
  @DisplayName("backoff exponencial: sem Retry-After, o primário atende na terceira tentativa")
  void backoffExponencialSemRetryAfter() {
    // Pausas de 10ms, 20ms (espera inicial 10ms) — rápido o bastante para teste.
    RestClient restClient =
        novoRestClient(
            fila(erro429(), erro429(), sucesso(resposta("ok", "google/gemma-4-31b-it:free"))),
            null);

    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    AiClient.RespostaCompletar resposta = client.completarComCascata(MENSAGENS, opcoes(3, null));

    assertThat(resposta.conteudo()).isEqualTo("ok");
    assertThat(resposta.chamadas()).isEqualTo(3);
  }

  @Test
  @DisplayName("chat: timeout/conexão faz fallback para próximo modelo")
  void chatTimeoutFazFallback() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    servidor
        .expect(requestTo(URL_CHAT))
        .andRespond(
            request -> {
              throw new org.springframework.web.client.ResourceAccessException("timeout");
            });
    servidor
        .expect(requestTo(URL_CHAT))
        .andRespond(
            withSuccess(
                resposta("ok", "google/gemma-4-26b-a4b-it:free"), MediaType.APPLICATION_JSON));
    RestClient restClient = builder.build();

    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);
    String conteudo = client.completar(MENSAGENS);

    assertThat(conteudo).isEqualTo("ok");
    servidor.verify();
  }

  @Test
  @DisplayName("chat: content vazio faz fallback para próximo modelo")
  void chatContentVazioFazFallback() {
    String vazio =
        """
        {"model":"google/gemma-4-31b-it:free","choices":[{"message":{"role":"assistant","content":""},"finish_reason":"stop"}]}
        """;
    RestClient restClient =
        novoRestClient(
            fila(sucesso(vazio), sucesso(resposta("ok", "google/gemma-4-26b-a4b-it:free"))), null);
    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    assertThat(client.completar(MENSAGENS)).isEqualTo("ok");
  }

  @Test
  @DisplayName("chat: finish_reason length faz fallback para próximo modelo")
  void chatFinishReasonLengthFazFallback() {
    String truncado =
        """
        {"model":"google/gemma-4-31b-it:free","choices":[{"message":{"role":"assistant","content":"parcial"},"finish_reason":"length"}]}
        """;
    RestClient restClient =
        novoRestClient(
            fila(sucesso(truncado), sucesso(resposta("ok", "google/gemma-4-26b-a4b-it:free"))),
            null);
    AiClient client = new AiClient(CASCATA_FREE, List.of(), restClient);

    assertThat(client.completar(MENSAGENS)).isEqualTo("ok");
  }

  /* ========================================================================
  Helpers (roteiro sequencial de respostas da "OpenRouter")
  ======================================================================== */

  private AiClient.OpcoesCompletar opcoes(
      int tentativas, java.util.function.Predicate<String> validador) {
    return new AiClient.OpcoesCompletar(CASCATA_FREE, 0.0, 8000, tentativas, 10L, validador);
  }

  /** Monta um RestClient cujo servidor devolve as respostas do roteiro, em ordem. */
  private RestClient novoRestClient(
      Deque<ResponseCreator> roteiro, List<Map<String, Object>> corposCapturados) {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

    while (!roteiro.isEmpty()) {
      servidor
          .expect(requestTo(URL_CHAT))
          .andExpect(
              request -> {
                if (corposCapturados != null) {
                  String corpo =
                      ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                  corposCapturados.add(
                      MAPPER.readValue(corpo, new TypeReference<Map<String, Object>>() {}));
                }
              })
          .andRespond(roteiro.poll());
    }

    return builder.build();
  }

  private static Deque<ResponseCreator> fila(ResponseCreator... respostas) {
    return new ArrayDeque<>(List.of(respostas));
  }

  private static ResponseCreator sucesso(String json) {
    return withSuccess(json, MediaType.APPLICATION_JSON);
  }

  private static ResponseCreator erro429() {
    return withStatus(HttpStatus.TOO_MANY_REQUESTS);
  }

  private static ResponseCreator erro429ComRetryAfter(String segundos) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.RETRY_AFTER, segundos);
    return withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers);
  }

  private static String resposta(String conteudo, String modelo) {
    // Escapa o conteúdo para embutir como string JSON (ex.: {"itens":[]} → {\"itens\":[]}).
    String conteudoEscapado = conteudo.replace("\\", "\\\\").replace("\"", "\\\"");
    return """
        {
          "id": "chatcmpl-teste",
          "model": "%s",
          "choices": [
            {
              "index": 0,
              "message": { "role": "assistant", "content": "%s" }
            }
          ]
        }
        """
        .formatted(modelo, conteudoEscapado);
  }
}
