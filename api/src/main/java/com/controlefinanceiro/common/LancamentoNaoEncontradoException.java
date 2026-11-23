package com.controlefinanceiro.common;

/** Lançamento inexistente para a empresa/usuário do token → 404 (sem vazar outros tenants). */
public class LancamentoNaoEncontradoException extends RuntimeException {

  public LancamentoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
