package com.controlefinanceiro.auth;

/** ID token Google recusado (assinatura, audience, issuer, expiração ou e-mail não verificado). */
public class GoogleTokenInvalidoException extends RuntimeException {

  public GoogleTokenInvalidoException() {
    super("Token Google inválido");
  }
}
