package com.controlefinanceiro.lancamento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTO de lote (LOT-01/02). As linhas cascateiam as constraints de {@link LancamentoRequest}. */
public record LancamentoLoteRequest(@Valid List<LancamentoLinha> linhas) {

  /** Uma linha do lote — mesmos campos de {@link LancamentoRequest}, sem tenant. */
  public record LancamentoLinha(
      @NotNull Long categoriaId,
      @NotNull String tipo,
      @NotNull @Digits(integer = 12, fraction = 2) BigDecimal valor,
      @NotNull LocalDate data,
      String descricao) {

    public LancamentoRequest toRequest() {
      return new LancamentoRequest(categoriaId, tipo, valor, data, descricao);
    }
  }
}
