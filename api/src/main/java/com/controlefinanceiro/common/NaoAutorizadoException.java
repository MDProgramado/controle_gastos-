package com.controlefinanceiro.common;

/**
 * Sem autenticação válida no {@code SecurityContext} (R3: o tenant só existe quando o token
 * autentica). Único caminho para erro de tenant/autenticação fora do filtro de segurança.
 */
public class NaoAutorizadoException extends RuntimeException {

  public NaoAutorizadoException() {
    super("Não autenticado");
  }

  public NaoAutorizadoException(String mensagem) {
    super(mensagem);
  }
}
