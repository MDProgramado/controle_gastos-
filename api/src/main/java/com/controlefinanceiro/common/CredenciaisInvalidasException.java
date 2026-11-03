package com.controlefinanceiro.common;

/**
 * Erro de login (D-05 anti-enumeração): a MESMA mensagem genérica cobre e-mail inexistente, senha
 * errada e usuário desativado — nunca revelar qual campo falhou (T-2-01).
 */
public class CredenciaisInvalidasException extends RuntimeException {

  public CredenciaisInvalidasException() {
    super("E-mail ou senha inválidos");
  }
}
