package com.controlefinanceiro.common;

/** Último ADMIN ativo da empresa não pode ser rebaixado nem desativado → 409. */
public class UltimoAdminAtivoException extends RuntimeException {

  public UltimoAdminAtivoException(String mensagem) {
    super(mensagem);
  }
}
