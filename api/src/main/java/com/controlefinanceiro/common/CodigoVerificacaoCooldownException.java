package com.controlefinanceiro.common;

/** Exceção lançada quando um novo código é solicitado antes do cooldown de envio. */
public class CodigoVerificacaoCooldownException extends RuntimeException {

  public CodigoVerificacaoCooldownException(String mensagem) {
    super(mensagem);
  }
}
