package com.controlefinanceiro.importacao;

/**
 * Lançado quando o PDF não possui texto extraível (possivelmente digitalizado/imagem). A importação
 * do lote segue com os demais arquivos; este documento é apenas avisado.
 */
public class SemTextoExtraivelException extends RuntimeException {

  public SemTextoExtraivelException(String mensagem) {
    super(mensagem);
  }
}
