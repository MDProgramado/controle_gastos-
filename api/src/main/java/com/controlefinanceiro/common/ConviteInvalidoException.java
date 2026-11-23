package com.controlefinanceiro.common;

public class ConviteInvalidoException extends RuntimeException {

  public ConviteInvalidoException() {
    super("Convite inválido ou expirado");
  }
}
