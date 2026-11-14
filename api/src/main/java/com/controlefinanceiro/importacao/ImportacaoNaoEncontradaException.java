package com.controlefinanceiro.importacao;

/**
 * Documento de importação não encontrado para a empresa autenticada (fase 5). Mapeada como 404 —
 * sem revelar se o id existe em outra empresa (isolamento de tenant, R3).
 */
public class ImportacaoNaoEncontradaException extends RuntimeException {

  public ImportacaoNaoEncontradaException(String mensagem) {
    super(mensagem);
  }
}
