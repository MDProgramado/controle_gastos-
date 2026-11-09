package com.controlefinanceiro.common;

/**
 * Lote de lançamentos acima do limite permitido (LOT-01, T-02-09): protege a API contra lotes
 * excessivos que degradariam a transação e a latência HTTP. Mapeada para 400 pelo {@code
 * ProblemDetailExceptionHandler}.
 */
public class LoteDemasiadoGrandeException extends RuntimeException {

  public LoteDemasiadoGrandeException(String mensagem) {
    super(mensagem);
  }
}
