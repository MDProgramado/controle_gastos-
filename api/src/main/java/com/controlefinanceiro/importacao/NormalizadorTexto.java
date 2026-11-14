package com.controlefinanceiro.importacao;

import java.text.Normalizer;

/**
 * Normalização de texto para casamento de nomes (fase 5): remove acentos, minúsculas, colapsa
 * espaços. Usado por {@link MapeadorCategoria} e {@link Deduplicador}.
 */
final class NormalizadorTexto {

  private NormalizadorTexto() {}

  /**
   * @return texto em minúsculas, sem acentos, com espaços simples (nunca nulo)
   */
  static String normalizar(String texto) {
    if (texto == null) {
      return "";
    }
    String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    return semAcento.toLowerCase().replaceAll("\\s+", " ").trim();
  }
}
