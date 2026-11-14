package com.controlefinanceiro.importacao;

/**
 * Arquivo rejeitado na validação da importação (fase 5, D-04): não é PDF (content-type/magic
 * bytes), excede o tamanho ou o lote passou do limite de arquivos. Mapeada como 400.
 */
public class ArquivoInvalidoException extends RuntimeException {

  public ArquivoInvalidoException(String mensagem) {
    super(mensagem);
  }
}
