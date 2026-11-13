package com.controlefinanceiro.importacao;

/**
 * Lançado quando o PDF excede os limites configurados de páginas ou caracteres, protegendo o
 * backend de arquivos gigantescos (zip-bomb).
 */
public class PdfDemasiadoGrandeException extends RuntimeException {

  public PdfDemasiadoGrandeException(String mensagem) {
    super(mensagem);
  }
}
