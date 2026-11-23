package com.controlefinanceiro.auth;

/** Falha de I/O ao consultar as chaves públicas do Google — não é um token inválido (503). */
public class GoogleVerificacaoIndisponivelException extends RuntimeException {

  public GoogleVerificacaoIndisponivelException() {
    super("Verificação do token Google indisponível");
  }
}
