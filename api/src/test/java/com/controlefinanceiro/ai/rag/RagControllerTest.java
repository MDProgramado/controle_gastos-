package com.controlefinanceiro.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RagControllerTest {

  @Test
  void ingestaoUsaSomenteOCorpusConfiguradoMesmoComCaminhoNoBody() {
    String caminhoConfigurado = "rag/corpus_financeiro_RAG_chunks.jsonl";
    IngestaoCapturando ingestao = new IngestaoCapturando();
    RagController controller = new RagController(ingestao, null, caminhoConfigurado);

    var resposta = controller.ingestir(new RagController.RagIngestaoRequest("/etc/passwd"));

    assertEquals(HttpStatus.ACCEPTED, resposta.getStatusCode());
    assertEquals(caminhoConfigurado, ingestao.caminho);
    assertEquals(caminhoConfigurado, resposta.getBody().get("caminho"));
  }

  private static class IngestaoCapturando extends RagIngestaoAssincrona {

    private String caminho;

    private IngestaoCapturando() {
      super(null);
    }

    @Override
    public CompletableFuture<RagIngestaoResultado> iniciar(String caminho) {
      this.caminho = caminho;
      return CompletableFuture.completedFuture(null);
    }
  }
}
