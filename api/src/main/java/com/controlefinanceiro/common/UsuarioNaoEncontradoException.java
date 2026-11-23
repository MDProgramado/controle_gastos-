package com.controlefinanceiro.common;

public class UsuarioNaoEncontradoException extends RuntimeException {

  public UsuarioNaoEncontradoException() {
    super("Usuário não encontrado");
  }
}
