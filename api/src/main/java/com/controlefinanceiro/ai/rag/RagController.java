package com.controlefinanceiro.ai.rag;

import java.util.LinkedHashMap;
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
  private final RagIngestaoEstado estado;

  public RagController(
      RagIngestaoAssincrona ingestao,
      RagChunkRepository repository,
      @Value("${ai.rag.corpus-path:rag/corpus_financeiro_RAG_chunks.jsonl}") String caminhoPadrao,
      RagIngestaoEstado estado) {
    this.ingestao = ingestao;
    this.repository = repository;
    this.caminhoPadrao = caminhoPadrao;
    this.estado = estado;
  }

  @PostMapping("/ingestir")
  public ResponseEntity<Map<String, Object>> ingestir(
      @RequestBody(required = false) RagIngestaoRequest request) {
    if (!estado.reservar()) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(Map.of("aceito", false, "erro", "Já existe uma reindexação em andamento."));
    }
    try {
      ingestao.iniciar(caminhoPadrao);
    } catch (RuntimeException e) {
      estado.falhar(e.getMessage());
      throw e;
    }
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(Map.of("aceito", true, "caminho", caminhoPadrao));
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> status() {
    var atual = estado.status();
    Map<String, Object> corpo = new LinkedHashMap<>();
    corpo.put("chunks", repository.contar());
    corpo.put("caminho", caminhoPadrao);
    corpo.put("situacao", atual.situacao());
    corpo.put("total", atual.total());
    corpo.put("processados", atual.processados());
    corpo.put("erro", atual.erro());
    corpo.put("iniciadoEm", atual.iniciadoEm() == null ? null : atual.iniciadoEm().toString());
    corpo.put(
        "finalizadoEm", atual.finalizadoEm() == null ? null : atual.finalizadoEm().toString());
    return ResponseEntity.ok(corpo);
  }

  public record RagIngestaoRequest(String caminho) {}
}
