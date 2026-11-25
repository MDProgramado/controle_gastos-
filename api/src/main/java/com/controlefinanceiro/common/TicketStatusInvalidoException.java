package com.controlefinanceiro.common;

public class TicketStatusInvalidoException extends RuntimeException {
  public TicketStatusInvalidoException(String mensagem) {
    super(mensagem);
  }
}
