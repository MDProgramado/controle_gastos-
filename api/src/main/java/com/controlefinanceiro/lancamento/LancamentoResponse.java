package com.controlefinanceiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta de lançamento (DTO de saída). Segue o padrão de {@link
 * com.controlefinanceiro.auth.LoginResponse} (record imutável). R3 gate: SEM campo empresa_id — o
 * tenant vem exclusivamente do token JWT.
 */
public record LancamentoResponse(
    Long id,
    String tipo,
    BigDecimal valor,
    LocalDate data,
    String descricao,
    String categoriaNome,
    String categoriaTipo) {

  public LancamentoResponse(Lancamento lancamento) {
    this(
        lancamento.getId(),
        lancamento.getTipo(),
        lancamento.getValor(),
        lancamento.getData(),
        lancamento.getDescricao(),
        lancamento.getCategoria().getNome(),
        lancamento.getCategoria().getTipo());
  }
}
