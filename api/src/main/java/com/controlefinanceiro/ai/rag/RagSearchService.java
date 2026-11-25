package com.controlefinanceiro.ai.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RagSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(RagSearchService.class);

  private final EmbeddingClient embeddingClient;
  private final RagChunkRepository repository;

  public RagSearchService(EmbeddingClient embeddingClient, RagChunkRepository repository) {
    this.embeddingClient = embeddingClient;
    this.repository = repository;
  }

  public List<RagTrecho> buscar(String consulta, int topK) {
    if (consulta == null || consulta.isBlank()) {
      return List.of();
    }
    int limite = Math.max(1, Math.min(topK, 10));
    Map<String, RagTrecho> encontrados = new LinkedHashMap<>();

    try {
      List<float[]> vetores = embeddingClient.embedir(List.of(consulta));
      if (vetores != null && !vetores.isEmpty() && vetores.get(0) != null) {
        for (RagTrecho trecho : repository.buscarPorVetor(vetores.get(0), limite)) {
          encontrados.putIfAbsent(trecho.chunkId(), trecho);
        }
      }
    } catch (RuntimeException e) {
      LOG.warn("Busca vetorial indisponível; usando apenas busca lexical: {}", e.getMessage());
    }

    for (RagTrecho trecho : repository.buscarPorTexto(consulta, limite)) {
      encontrados.putIfAbsent(trecho.chunkId(), trecho);
    }

    return new ArrayList<>(encontrados.values()).stream().limit(limite).toList();
  }
}
