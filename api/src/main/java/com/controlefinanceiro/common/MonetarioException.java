package com.controlefinanceiro.common;

/**
 * Exceção de validação monetária (LAN-04, LAN-07): lançada quando o valor tem mais de 2 casas
 * decimais (rejeitado, nunca arredondado) ou quando o valor não é positivo.
 */
public class MonetarioException extends RuntimeException {

  public MonetarioException(String mensagem) {
    super(mensagem);
  }
}
