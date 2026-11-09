package com.controlefinanceiro.common;

/**
 * Erro em uma linha de lote de lançamentos (LOT-02): carrega o índice base 0 da linha inválida para
 * que a resposta ProblemDetail informe exatamente qual linha falhou e o lote inteiro seja revertido
 * (tudo-ou-nada).
 */
public class LinhaInvalidaException extends RuntimeException {

  private final int indiceLinha;

  /**
   * @param indiceLinha índice base 0 da linha inválida dentro do lote
   * @param mensagem causa original da falha daquela linha
   */
  public LinhaInvalidaException(int indiceLinha, String mensagem) {
    super("Linha %d inválida: %s".formatted(indiceLinha, mensagem));
    this.indiceLinha = indiceLinha;
  }

  /**
   * @return índice base 0 da linha inválida no lote.
   */
  public int getIndiceLinha() {
    return indiceLinha;
  }
}
