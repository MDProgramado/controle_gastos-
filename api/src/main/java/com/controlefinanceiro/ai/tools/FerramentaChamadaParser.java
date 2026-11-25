package com.controlefinanceiro.ai.tools;

public final class FerramentaChamadaParser {

  private FerramentaChamadaParser() {}

  public static FerramentaChamada parse(String linha) {
    if (linha == null || linha.isBlank()) {
      return null;
    }
    String[] partes = linha.trim().split("\\|", 3);
    if (partes.length != 3 || !"FERRAMENTA".equals(partes[0].trim())) {
      return null;
    }
    String nome = partes[1].trim();
    String args = partes[2].trim();
    if (nome.isEmpty() || args.isEmpty()) {
      return null;
    }
    return new FerramentaChamada(nome, args);
  }
}
