package com.controlefinanceiro.common;

/**
 * Exceção lançada quando o código de verificação é inválido, expirado ou excedeu o limite de
 * tentativas.
 */
public class CodigoVerificacaoInvalidoException extends RuntimeException {

  public CodigoVerificacaoInvalidoException(String mensagem) {
    super(mensagem);
  }
}
