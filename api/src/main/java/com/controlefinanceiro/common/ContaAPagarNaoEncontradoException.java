package com.controlefinanceiro.common;

public class ContaAPagarNaoEncontradoException extends RuntimeException {
  public ContaAPagarNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
