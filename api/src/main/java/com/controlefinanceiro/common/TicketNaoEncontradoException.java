package com.controlefinanceiro.common;

public class TicketNaoEncontradoException extends RuntimeException {
  public TicketNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
