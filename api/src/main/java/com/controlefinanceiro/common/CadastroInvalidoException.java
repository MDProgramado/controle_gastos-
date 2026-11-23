package com.controlefinanceiro.common;

public class CadastroInvalidoException extends RuntimeException {

  public CadastroInvalidoException() {
    super("Não foi possível concluir o cadastro");
  }
}
