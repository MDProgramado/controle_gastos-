package com.controlefinanceiro.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resposta da IA (fase 4 — OpenRouter + Gemma 4). O contrato espelha o que o frontend consome
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
    List<String> opcoes,
    List<AiCitacao> citacoes) {

  public AiResponse(
      String intent,
      String resposta,
      List<AiConsultaDado> dados,
      AiPropostaLancamento proposta,
      List<AiPropostaLancamento> propostas,
      List<String> opcoes) {
    this(intent, resposta, dados, proposta, propostas, opcoes, List.of());
  }

  public AiResponse comCitacoes(List<AiCitacao> novasCitacoes) {
    return new AiResponse(intent, resposta, dados, proposta, propostas, opcoes, novasCitacoes);
  }

  public record AiCitacao(
      String sourceId, String titulo, String organizacao, String vigencia, String dispositivo) {}

  public record AiConsultaDado(String label, BigDecimal valor) {}

  public record AiPropostaLancamento(
      String tipo,
      BigDecimal valor,
      String data,
      String categoriaId,
      String categoriaSugerida,
      String descricao) {}
}
