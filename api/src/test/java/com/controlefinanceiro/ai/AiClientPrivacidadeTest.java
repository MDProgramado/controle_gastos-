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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

/** Item 1.1/1.2: política de dados (sem coleta, ZDR) em toda requisição à OpenRouter. */
class AiClientPrivacidadeTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String BASE_URL = "https://openrouter.test/api/v1";
  private static final String URL_CHAT = BASE_URL + "/chat/completions";
  private static final List<String> MODELOS = List.of("modelo/a", "modelo/b");
  private static final List<Map<String, Object>> MENSAGENS =
      List.of(Map.of("role", "user", "content", "x"));

  private static final String SEM_PROVEDOR =
      "{\"error\":{\"code\":404,\"message\":\"No endpoints found matching your data policy."
          + " Configure provider data_collection / zdr settings.\"}}";

  @Test
  @DisplayName("chat envia provider.data_collection=deny e provider.zdr=true por padrão")
  void chatEnviaPoliticaDeDados() {
    List<Map<String, Object>> corpos = new ArrayList<>();
    AiClient client =
        new AiClient(MODELOS, List.of(), restClient(corpos, sucesso(resposta("ok", "modelo/a"))));

    client.completar(MENSAGENS);

    assertThat(corpos).hasSize(1);
    assertThat(corpos.get(0))
        .containsEntry("provider", Map.of("data_collection", "deny", "zdr", true));
  }

  @Test
  @DisplayName("extração (cascata) também envia a política de dados")
  void cascataEnviaPoliticaDeDados() {
    List<Map<String, Object>> corpos = new ArrayList<>();
    AiClient client =
        new AiClient(MODELOS, List.of(), restClient(corpos, sucesso(resposta("ok", "modelo/a"))));

    client.completarComCascata(
        MENSAGENS, new AiClient.OpcoesCompletar(MODELOS, 0.0, 100, 1, 1L, null));

    assertThat(corpos.get(0))
        .containsEntry("provider", Map.of("data_collection", "deny", "zdr", true));
  }

  @Test
  @DisplayName("política de dados é configurável")
  void politicaConfiguravel() {
    List<Map<String, Object>> corpos = new ArrayList<>();
    AiClient client =
        new AiClient(MODELOS, List.of(), restClient(corpos, sucesso(resposta("ok", "modelo/a"))));
    client.configurarPoliticaDados("allow", false);

    client.completar(MENSAGENS);

    assertThat(corpos.get(0))
        .containsEntry("provider", Map.of("data_collection", "allow", "zdr", false));
  }

  @Test
  @DisplayName("chat: 404 'nenhum provedor com a política' avança para o próximo modelo")
  void chatSemProvedorAvanca() {
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(
                null,
                withStatus(HttpStatus.NOT_FOUND)
                    .body(SEM_PROVEDOR)
                    .contentType(MediaType.APPLICATION_JSON),
                sucesso(resposta("ok do segundo", "modelo/b"))));

    assertThat(client.completar(MENSAGENS)).isEqualTo("ok do segundo");
  }

  @Test
  @DisplayName("chat: 400 citando provider também é tratado como sem provedor compatível")
  void chat400SemProvedorAvanca() {
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(
                null,
                withStatus(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":{\"message\":\"No allowed providers are available\"}}")
                    .contentType(MediaType.APPLICATION_JSON),
                sucesso(resposta("ok", "modelo/b"))));

    assertThat(client.completar(MENSAGENS)).isEqualTo("ok");
  }

  @Test
  @DisplayName("chat: 404 sem relação com provedor continua não recuperável")
  void chat404ComumEncerra() {
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(
                null,
                withStatus(HttpStatus.NOT_FOUND)
                    .body("{\"error\":{\"message\":\"Not found\"}}")
                    .contentType(MediaType.APPLICATION_JSON)));

    assertThatThrownBy(() -> client.completar(MENSAGENS))
        .isInstanceOf(ServicoIaIndisponivelException.class);
  }

  @Test
  @DisplayName("extração: 404 sem provedor avança para o próximo modelo")
  void cascataSemProvedorAvanca() {
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(
                null,
                withStatus(HttpStatus.NOT_FOUND)
                    .body(SEM_PROVEDOR)
                    .contentType(MediaType.APPLICATION_JSON),
                sucesso(resposta("ok", "modelo/b"))));

    AiClient.RespostaCompletar r =
        client.completarComCascata(
            MENSAGENS, new AiClient.OpcoesCompletar(MODELOS, 0.0, 100, 2, 1L, null));

    assertThat(r.modeloUsado()).isEqualTo("modelo/b");
    assertThat(r.chamadas()).isEqualTo(2);
  }

  static RestClient restClient(List<Map<String, Object>> corpos, ResponseCreator... respostas) {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    for (ResponseCreator resposta : respostas) {
      servidor
          .expect(requestTo(URL_CHAT))
          .andExpect(
              request -> {
                if (corpos != null) {
                  String corpo =
                      ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);
                  corpos.add(MAPPER.readValue(corpo, new TypeReference<Map<String, Object>>() {}));
                }
              })
          .andRespond(resposta);
    }
    return builder.build();
  }

  static ResponseCreator sucesso(String json) {
    return withSuccess(json, MediaType.APPLICATION_JSON);
  }

  static String resposta(String conteudo, String modelo) {
    return """
        {"model":"%s","choices":[{"message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}]}
        """
        .formatted(modelo, conteudo);
  }
}
