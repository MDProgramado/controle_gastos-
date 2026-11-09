package com.controlefinanceiro.common;

/**
 * Exceção lançada quando já existe uma categoria com o mesmo nome, tipo e empresa (unicidade: nome
 * único por empresa_id + nome + tipo).
 */
public class CategoriaDuplicadaException extends RuntimeException {

  public CategoriaDuplicadaException(String mensagem) {
    super(mensagem);
  }
}
