package com.controlefinanceiro.ai.rag;

import java.util.List;

public interface EmbeddingClient {

  List<float[]> embedir(List<String> textos);
}
