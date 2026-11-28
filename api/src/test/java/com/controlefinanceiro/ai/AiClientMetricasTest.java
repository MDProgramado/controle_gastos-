package com.controlefinanceiro.ai;

import static com.controlefinanceiro.ai.AiClientPrivacidadeTest.restClient;
import static com.controlefinanceiro.ai.AiClientPrivacidadeTest.sucesso;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Item 3.2/3.3: métricas ia.* e uso (tokens/custo) devolvido junto com o conteúdo. */
class AiClientMetricasTest {

  private static final List<String> MODELOS = List.of("modelo/a", "modelo/b");
  private static final List<Map<String, Object>> MENSAGENS =
      List.of(Map.of("role", "user", "content", "x"));

  static String respostaComUso(
      String conteudo, String modelo, int entrada, int saida, String custo) {
    return """
        {"model":"%s","choices":[{"message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}],
         "usage":{"prompt_tokens":%d,"completion_tokens":%d,"total_tokens":%d,"cost":%s}}
        """
        .formatted(modelo, conteudo, entrada, saida, entrada + saida, custo);
  }

  @Test
  @DisplayName("chat devolve uso e registra tokens, custo, chamadas e latência por modelo")
  void chatRegistraMetricasEUso() {
    MeterRegistry registry = new SimpleMeterRegistry();
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(null, sucesso(respostaComUso("ok", "modelo/a", 120, 30, "0.00042"))));
    client.setMeterRegistry(registry);

    AiClient.RespostaChat r =
        client.completarComUso(MENSAGENS, System.nanoTime() + 60_000_000_000L);

    assertThat(r.conteudo()).isEqualTo("ok");
    assertThat(r.usos()).hasSize(1);
    UsoIa uso = r.usos().get(0);
    assertThat(uso.modelo()).isEqualTo("modelo/a");
    assertThat(uso.operacao()).isEqualTo("chat");
    assertThat(uso.tokensEntrada()).isEqualTo(120);
    assertThat(uso.tokensSaida()).isEqualTo(30);
    assertThat(uso.custoUsd()).isEqualByComparingTo("0.00042");
    assertThat(uso.chamadas()).isEqualTo(1);

    assertThat(contador(registry, "ia.tokens", "tipo", "entrada")).isEqualTo(120);
    assertThat(contador(registry, "ia.tokens", "tipo", "saida")).isEqualTo(30);
    assertThat(registry.get("ia.custo.usd").tag("modelo", "modelo/a").counter().count())
        .isEqualTo(0.00042);
    assertThat(contador(registry, "ia.chamadas", "resultado", "ok")).isEqualTo(1);
    assertThat(
            registry
                .get("ia.latencia")
                .tag("modelo", "modelo/a")
                .tag("operacao", "chat")
                .timer()
                .count())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("falha no primeiro modelo conta erro; o segundo conta fallback")
  void fallbackContaResultados() {
    MeterRegistry registry = new SimpleMeterRegistry();
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(
                null,
                withStatus(HttpStatus.TOO_MANY_REQUESTS),
                sucesso(respostaComUso("ok", "modelo/b", 10, 5, "0.0001"))));
    client.setMeterRegistry(registry);

    AiClient.RespostaChat r =
        client.completarComUso(MENSAGENS, System.nanoTime() + 60_000_000_000L);

    assertThat(r.usos()).extracting(UsoIa::modelo).containsExactly("modelo/a", "modelo/b");
    assertThat(r.usos().get(0).chamadas()).isEqualTo(1);
    assertThat(r.usos().get(0).tokensEntrada()).isZero();
    assertThat(
            registry
                .get("ia.chamadas")
                .tag("modelo", "modelo/a")
                .tag("resultado", "erro")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(
            registry
                .get("ia.chamadas")
                .tag("modelo", "modelo/b")
                .tag("resultado", "fallback")
                .counter()
                .count())
        .isEqualTo(1);
  }

  @Test
  @DisplayName("extração devolve uso com operacao=extracao")
  void extracaoDevolveUso() {
    MeterRegistry registry = new SimpleMeterRegistry();
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(null, sucesso(respostaComUso("{}", "modelo/a", 900, 100, "0.002"))));
    client.setMeterRegistry(registry);

    AiClient.RespostaCompletar r =
        client.completarComCascata(
            MENSAGENS, new AiClient.OpcoesCompletar(MODELOS, 0.0, 100, 1, 1L, null));

    assertThat(r.usos()).hasSize(1);
    assertThat(r.usos().get(0).operacao()).isEqualTo("extracao");
    assertThat(r.usos().get(0).tokensEntrada()).isEqualTo(900);
    assertThat(registry.get("ia.tokens").tag("operacao", "extracao").counters()).hasSize(2);
  }

  @Test
  @DisplayName("nenhuma métrica ia.* usa empresa ou usuário como tag (cardinalidade)")
  void semTagsDeTenant() {
    MeterRegistry registry = new SimpleMeterRegistry();
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(null, sucesso(respostaComUso("ok", "modelo/a", 1, 1, "0"))));
    client.setMeterRegistry(registry);
    client.completarComUso(MENSAGENS, System.nanoTime() + 60_000_000_000L);

    for (Meter meter : registry.getMeters()) {
      for (var tag : meter.getId().getTags()) {
        assertThat(tag.getKey().toLowerCase()).doesNotContain("empresa").doesNotContain("usuario");
      }
    }
  }

  @Test
  @DisplayName("resposta sem objeto usage não quebra: uso com zero tokens e custo zero")
  void semUsage() {
    AiClient client =
        new AiClient(
            MODELOS,
            List.of(),
            restClient(null, sucesso(AiClientPrivacidadeTest.resposta("ok", "m"))));
    client.setMeterRegistry(new SimpleMeterRegistry());

    AiClient.RespostaChat r =
        client.completarComUso(MENSAGENS, System.nanoTime() + 60_000_000_000L);

    assertThat(r.usos().get(0).tokensEntrada()).isZero();
    assertThat(r.usos().get(0).custoUsd()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  private static double contador(MeterRegistry registry, String nome, String tag, String valor) {
    return registry.get(nome).tag(tag, valor).counter().count();
  }
}
