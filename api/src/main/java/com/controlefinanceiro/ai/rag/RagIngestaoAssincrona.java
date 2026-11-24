package com.controlefinanceiro.ai.rag;

import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RagIngestaoAssincrona {

  private final RagIngestionService ingestionService;

  public RagIngestaoAssincrona(RagIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @Async
  public CompletableFuture<RagIngestaoResultado> iniciar(String caminho) {
    return CompletableFuture.completedFuture(ingestionService.ingestirSync(caminho));
  }
}
