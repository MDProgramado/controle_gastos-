package com.controlefinanceiro.ai;

import java.util.List;

/**
 * Resposta da IA (fase 4 — OpenRouter + Gemini). O contrato espelha o que o frontend já consome
 * (lib/ai/types.ts).
 */
public record AiResponse(
    String intent,
    String resposta,
    List<AiConsultaDado> dados,
    AiPropostaLancamento proposta,
    List<String> opcoes) {

  public record AiConsultaDado(String label, double valor) {}

  public record AiPropostaLancamento(
      String tipo,
      double valor,
      String data,
      String categoriaId,
      String categoriaSugerida,
      String descricao) {}
}
