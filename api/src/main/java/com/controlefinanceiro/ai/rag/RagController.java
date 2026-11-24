package com.controlefinanceiro.ai.rag;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/rag")
public class RagController {

  private final RagIngestaoAssincrona ingestao;
  private final RagChunkRepository repository;
  private final String caminhoPadrao;

  public RagController(
      RagIngestaoAssincrona ingestao,
      RagChunkRepository repository,
      @Value("${ai.rag.corpus-path:rag/corpus_financeiro_RAG_chunks.jsonl}") String caminhoPadrao) {
    this.ingestao = ingestao;
    this.repository = repository;
    this.caminhoPadrao = caminhoPadrao;
  }

  @PostMapping("/ingestir")
  public ResponseEntity<Map<String, Object>> ingestir(
      @RequestBody(required = false) RagIngestaoRequest request) {
    ingestao.iniciar(caminhoPadrao);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(Map.of("aceito", true, "caminho", caminhoPadrao));
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> status() {
    return ResponseEntity.ok(Map.of("chunks", repository.contar(), "caminho", caminhoPadrao));
  }

  public record RagIngestaoRequest(String caminho) {}
}
