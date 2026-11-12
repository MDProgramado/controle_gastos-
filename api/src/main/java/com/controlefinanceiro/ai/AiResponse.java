package com.controlefinanceiro.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta da IA (fase 4 — OpenRouter + Gemini). O contrato espelha o que o frontend consome
 * (lib/ai/types.ts).
 *
 * <p>Valores monetários usam {@link BigDecimal} (R1: nunca {@code double}/{@code float} no código).
 *
 * <p>Uma mesma mensagem pode conter VÁRIOS lançamentos (ex.: "salário 2400, aluguel 900, luz
 * 100..."). Nesse caso {@link #propostas()} traz todos, e {@link #proposta()} traz o primeiro
 * (compatibilidade com o fluxo de lançamento único).
 */
public record AiResponse(
    String intent,
    String resposta,
    List<AiConsultaDado> dados,
    AiPropostaLancamento proposta,
    List<AiPropostaLancamento> propostas,
    List<String> opcoes) {

  public record AiConsultaDado(String label, BigDecimal valor) {}

  public record AiPropostaLancamento(
      String tipo,
      BigDecimal valor,
      String data,
      String categoriaId,
      String categoriaSugerida,
      String descricao) {}
}
