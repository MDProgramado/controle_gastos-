package com.controlefinanceiro.ai.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(RagSearchService.class);

  private final EmbeddingClient embeddingClient;
  private final RagChunkRepository repository;
  private final double similaridadeMinima;

  @Autowired
  public RagSearchService(
      EmbeddingClient embeddingClient,
      RagChunkRepository repository,
      @Value("${ai.rag.similaridade-minima:0.60}") double similaridadeMinima) {
    this.embeddingClient = embeddingClient;
    this.repository = repository;
    this.similaridadeMinima = similaridadeMinima;
  }

  public RagSearchService(EmbeddingClient embeddingClient, RagChunkRepository repository) {
    this(embeddingClient, repository, 0.60);
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
        for (RagChunkRepository.TrechoPontuado p :
            repository.buscarPorVetor(vetores.get(0), limite, similaridadeMinima)) {
          // Filtro também em memória (defesa em profundidade, caso o SQL mude).
          if (p.similaridade() >= similaridadeMinima) {
            encontrados.putIfAbsent(p.trecho().chunkId(), p.trecho());
          }
        }
      }
    } catch (RuntimeException e) {
      LOG.warn(
          "Busca vetorial indisponível; usando apenas busca lexical: {}",
          com.controlefinanceiro.common.LogRedacao.mensagem(e));
    }

    for (RagTrecho trecho : repository.buscarPorTexto(consulta, limite)) {
      encontrados.putIfAbsent(trecho.chunkId(), trecho);
    }

    return new ArrayList<>(encontrados.values()).stream().limit(limite).toList();
  }
}
