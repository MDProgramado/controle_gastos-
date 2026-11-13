package com.controlefinanceiro.importacao;

/**
 * Operação inválida para o estado atual da importação (fase 5) — ex.: descartar uma importação já
 * confirmada. Mapeada como 409.
 */
public class ImportacaoEstadoInvalidoException extends RuntimeException {

  public ImportacaoEstadoInvalidoException(String mensagem) {
    super(mensagem);
  }
}
