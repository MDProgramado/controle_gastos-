package com.controlefinanceiro.common;

/**
 * Exceção lançada quando uma categoria não é encontrada para o id e empresa informados (isolamento
 * de tenant, R3).
 */
public class CategoriaNaoEncontradaException extends RuntimeException {

  public CategoriaNaoEncontradaException(String mensagem) {
    super(mensagem);
  }
}
