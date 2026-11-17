package com.controlefinanceiro.common;

/**
 * Autenticado, mas sem permissão para a operação (violação de isolamento multi-tenant ou regra de
 * negócio de autorização). Resultado: 403 Forbidden.
 */
public class AcessoNegadoException extends RuntimeException {

  public AcessoNegadoException() {
    super("Acesso negado");
  }

  public AcessoNegadoException(String mensagem) {
    super(mensagem);
  }
}
