package com.controlefinanceiro.ai.rag;

public record RagTrecho(
    String chunkId,
    String titulo,
    String organizacao,
    String vigencia,
    String dispositivo,
    String texto) {

  public String sourceId() {
    int indice = chunkId.indexOf("::");
    return indice < 0 ? chunkId : chunkId.substring(0, indice);
  }
}
