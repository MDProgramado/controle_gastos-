package com.controlefinanceiro.importacao;

/**
 * Classificação heurística de documento (fatura de cartão × extrato bancário).
 *
 * <p>O resultado é apenas uma indicação inicial; a validação cruzada com a resposta da IA ocorre no
 * serviço de extração (conflito → alerta).
 */
public final class ClassificadorDocumento {

  private ClassificadorDocumento() {}

  private static final String[] INDICIOS_CARTAO = {
    "fatura", "vencimento", "cartão", "cartao", "limite", "parcela", "fatura do", "fechamento"
  };

  private static final String[] INDICIOS_EXTRATO = {
    "extrato",
    "saldo",
    "saldo anterior",
    "pix",
    "transferência",
    "transferencia",
    "boleto",
    "débito",
    "debito",
    "saque",
    "depósito",
    "deposito",
    "tarifa"
  };

  /**
   * Classifica com base no nome do arquivo e no texto extraído.
   *
   * @return {@code CARTAO} ou {@code EXTRATO}
   */
  public static String classificar(String nomeArquivo, String texto) {
    String nome = (nomeArquivo == null ? "" : nomeArquivo.toLowerCase());
    String corpo = (texto == null ? "" : texto.toLowerCase());
    String tudo = nome + " " + corpo;

    int pontosCartao = 0;
    for (String indice : INDICIOS_CARTAO) {
      if (tudo.contains(indice)) {
        pontosCartao++;
      }
    }

    int pontosExtrato = 0;
    for (String indice : INDICIOS_EXTRATO) {
      if (tudo.contains(indice)) {
        pontosExtrato++;
      }
    }

    if (pontosCartao > pontosExtrato) {
      return "CARTAO";
    }
    if (pontosExtrato > pontosCartao) {
      return "EXTRATO";
    }
    // Empate ou sem indícios: padrão conservador é EXTRATO (maior volume de documentos
    // bancários esperados), mas a IA pode sobrescrever na validação cruzada.
    return "EXTRATO";
  }
}
