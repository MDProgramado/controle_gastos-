package com.controlefinanceiro.whatsapp;

/** Resultado de confirmação/cancelamento de pendência WhatsApp, para envio pós-commit. */
public record WhatsappConfirmacaoResultado(
    String resultadoMensagem, String textoEnvio, boolean enviarTexto) {

  static WhatsappConfirmacaoResultado semEnvio(String resultadoMensagem) {
    return new WhatsappConfirmacaoResultado(resultadoMensagem, null, false);
  }

  static WhatsappConfirmacaoResultado comEnvio(String resultadoMensagem, String textoEnvio) {
    return new WhatsappConfirmacaoResultado(resultadoMensagem, textoEnvio, true);
  }
}
