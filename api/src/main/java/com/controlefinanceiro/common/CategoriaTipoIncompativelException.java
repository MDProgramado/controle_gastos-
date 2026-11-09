package com.controlefinanceiro.common;

/**
 * Exceção de integridade categoria×tipo (CAT-04): lançada quando o tipo da categoria é incompatível
 * com o tipo do lançamento ou quando o tipo informado não é ENTRADA nem SAIDA.
 */
public class CategoriaTipoIncompativelException extends RuntimeException {

  public CategoriaTipoIncompativelException(String mensagem) {
    super(mensagem);
  }
}
