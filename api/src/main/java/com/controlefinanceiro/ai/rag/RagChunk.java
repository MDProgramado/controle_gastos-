package com.controlefinanceiro.ai.rag;

public record RagChunk(
    String chunkId,
    String sourceId,
    String titulo,
    String organizacao,
    String tipo,
    String nivel,
    String tema,
    String licenca,
    String url,
    String vigencia,
    String usoRecomendado,
    String idioma,
    String arquivoOrigem,
    Integer nChars,
    String texto,
    float[] embedding) {}
