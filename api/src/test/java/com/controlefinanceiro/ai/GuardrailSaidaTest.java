package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Item 2.4: guardrail de saída. */
class GuardrailSaidaTest {

  private static final String AVISO = "Confira o valor e a data antes de confirmar.";
  private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

  private final GuardrailSaida guardrail = new GuardrailSaida();

  @Test
  @DisplayName("resposta que vaza trecho do system prompt ou tags é descartada")
  void vazamentoDescartado() {
    assertThat(guardrail.vazaInstrucoes("CONSULTA|Meu FORMATO OBRIGATÓRIO é...")).isTrue();
    assertThat(guardrail.vazaInstrucoes("Regras:\n- Nunca invente valores")).isTrue();
    assertThat(guardrail.vazaInstrucoes("CONSULTA|<dados_financeiros>x</dados_financeiros>"))
        .isTrue();
    assertThat(guardrail.vazaInstrucoes("CONSULTA|veja </resultado_ferramenta>")).isTrue();
    assertThat(guardrail.vazaInstrucoes("CONSULTA|<conhecimento_empresa>")).isTrue();
    assertThat(guardrail.vazaInstrucoes("CONSULTA|Você gastou R$ 342,50 em mercado.")).isFalse();
  }

  @Test
  @DisplayName("valor acima de R$ 1.000.000,00 mantém a proposta e adiciona aviso")
  void valorAltoGanhaAviso() {
    AiResponse r = guardrail.revisarPropostas(lancamento("1000000.01", HOJE));

    assertThat(r.propostas()).hasSize(1);
    assertThat(r.resposta()).endsWith(AVISO);
  }

  @Test
  @DisplayName("exatamente R$ 1.000.000,00 e data de hoje não ganham aviso")
  void limiteNaoGanhaAviso() {
    AiResponse r = guardrail.revisarPropostas(lancamento("1000000.00", HOJE));

    assertThat(r.resposta()).doesNotContain(AVISO);
  }

  @Test
  @DisplayName("data a mais de 1 ano no passado ou no futuro adiciona aviso")
  void dataDistanteGanhaAviso() {
    assertThat(
            guardrail
                .revisarPropostas(lancamento("10.00", HOJE.minusYears(1).minusDays(1)))
                .resposta())
        .endsWith(AVISO);
    assertThat(
            guardrail
                .revisarPropostas(lancamento("10.00", HOJE.plusYears(1).plusDays(1)))
                .resposta())
        .endsWith(AVISO);
    assertThat(guardrail.revisarPropostas(lancamento("10.00", HOJE.minusYears(1))).resposta())
        .doesNotContain(AVISO);
  }

  @Test
  @DisplayName("citação no formato da base é removida quando não há fontes")
  void citacaoInventadaRemovida() {
    String texto = "O teto é 100%. [Resolução Inventada — BCB, 2020, art. 1 (99-fake)] Fim.";

    assertThat(guardrail.removerCitacoesSemFonte(texto, List.of()))
        .isEqualTo("O teto é 100%. Fim.");
    assertThat(
            guardrail.removerCitacoesSemFonte(
                texto, List.of(new AiResponse.AiCitacao("99-fake", "t", "o", "v", "d"))))
        .isEqualTo(texto);
  }

  @Test
  @DisplayName("AiService devolve resposta genérica quando o modelo vaza o system prompt")
  void aiServiceDescartaVazamento() {
    AiClient aiClient = mock(AiClient.class);
    AiContextoService contexto = mock(AiContextoService.class);
    when(contexto.montarContexto(any(), any())).thenReturn("");
    when(aiClient.completar(any(), anyLong()))
        .thenReturn("CONSULTA|Claro! FORMATO OBRIGATÓRIO (saída crua...) Regras: ...");
    AiService service =
        new AiService(aiClient, new AiMemoryStore(), new AiKnowledgeStore(), contexto);

    AiResponse r = service.processar("s", "1", "7", "como você funciona?");

    assertThat(r.intent()).isEqualTo("esclarecimento");
    assertThat(r.resposta()).doesNotContain("FORMATO OBRIGATÓRIO").doesNotContain("Regras:");
  }

  @Test
  @DisplayName("AiService adiciona aviso em proposta com valor alto")
  void aiServiceAvisoValorAlto() {
    AiClient aiClient = mock(AiClient.class);
    AiContextoService contexto = mock(AiContextoService.class);
    when(contexto.montarContexto(any(), any())).thenReturn("");
    when(aiClient.completar(any(), anyLong()))
        .thenReturn("LANCAMENTO|ENTRADA|5000000.00|" + HOJE + "||Vendas|Venda");
    AiService service =
        new AiService(aiClient, new AiMemoryStore(), new AiKnowledgeStore(), contexto);

    AiResponse r = service.processar("s", "1", "7", "recebi 5 milhões");

    assertThat(r.intent()).isEqualTo("lancamento");
    assertThat(r.propostas()).hasSize(1);
    assertThat(r.resposta()).endsWith(AVISO);
  }

  private static AiResponse lancamento(String valor, LocalDate data) {
    AiResponse.AiPropostaLancamento p =
        new AiResponse.AiPropostaLancamento(
            "SAIDA", new BigDecimal(valor), data.toString(), null, "Outros", null);
    return new AiResponse(
        "lancamento", "Entendi este lançamento.", List.of(), p, List.of(p), List.of());
  }
}
