package com.controlefinanceiro.common;

/** Exceção lançada quando o telefone não pertence ao usuário autenticado ou não existe. */
public class TelefoneNaoPertenceAoUsuarioException extends RuntimeException {

  public TelefoneNaoPertenceAoUsuarioException(String mensagem) {
    super(mensagem);
  }
}
