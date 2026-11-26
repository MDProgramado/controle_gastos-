package com.controlefinanceiro.conta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ContaAPagarResponse(
    Long id,
    String descricao,
    BigDecimal valor,
    LocalDate dataVencimento,
    String status,
    Long categoriaId,
    String categoriaNome,
    boolean pago,
    OffsetDateTime pagoEm) {

  public ContaAPagarResponse(ContaAPagar conta) {
    this(
        conta.getId(),
        conta.getDescricao(),
        conta.getValor(),
        conta.getVencimento(),
        status(conta),
        conta.getCategoria() == null ? null : conta.getCategoria().getId(),
        conta.getCategoria() == null ? null : conta.getCategoria().getNome(),
        conta.isPago(),
        conta.getPagoEm());
  }

  private static String status(ContaAPagar conta) {
    if (conta.isPago()) {
      return "PAGO";
    }
    if (conta.getVencimento() != null && conta.getVencimento().isBefore(LocalDate.now())) {
      return "VENCIDO";
    }
    return "PENDENTE";
  }
}
