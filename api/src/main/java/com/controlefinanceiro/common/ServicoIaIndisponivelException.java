package com.controlefinanceiro.common;

/**
 * Serviço de IA (OpenRouter) indisponível ou sobrecarregado no momento (fase 4).
 *
 * <p>Serve para devolver 503 com mensagem legível em vez de um 500 genérico: o usuário precisa
 * saber que pode tentar de novo em instantes, e não que o sistema quebrou.
 */
public class ServicoIaIndisponivelException extends RuntimeException {

  public ServicoIaIndisponivelException(String mensagem) {
    super(mensagem);
  }

  public ServicoIaIndisponivelException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
