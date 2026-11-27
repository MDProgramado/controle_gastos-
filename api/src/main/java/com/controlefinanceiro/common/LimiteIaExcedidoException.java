package com.controlefinanceiro.common;

public class LimiteIaExcedidoException extends RuntimeException {
  public LimiteIaExcedidoException(String mensagem) {
    super(mensagem);
  }
}
