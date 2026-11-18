package com.controlefinanceiro.common;

/**
 * Exceção lançada quando o número de telefone já está vinculado a uma empresa diferente da que está
 * tentando cadastrar.
 */
public class TelefoneJaVinculadoException extends RuntimeException {

  public TelefoneJaVinculadoException(String mensagem) {
    super(mensagem);
  }
}
