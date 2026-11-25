package com.controlefinanceiro.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RagControllerTest {

  @Test
  void ingestaoUsaSomenteOCorpusConfiguradoMesmoComCaminhoNoBody() {
    String caminhoConfigurado = "rag/corpus_financeiro_RAG_chunks.jsonl";
    IngestaoCapturando ingestao = new IngestaoCapturando();
    RagController controller =
        new RagController(ingestao, null, caminhoConfigurado, new RagIngestaoEstado());

    var resposta = controller.ingestir(new RagController.RagIngestaoRequest("/etc/passwd"));

    assertEquals(HttpStatus.ACCEPTED, resposta.getStatusCode());
    assertEquals(caminhoConfigurado, ingestao.caminho);
    assertEquals(caminhoConfigurado, resposta.getBody().get("caminho"));
  }

  @Test
  void ingestaoRejeitaSegundaExecucaoEnquantoEstiverEmAndamento() {
    RagIngestaoEstado estado = new RagIngestaoEstado();
    estado.iniciar(10);
    IngestaoCapturando ingestao = new IngestaoCapturando();
    RagController controller = new RagController(ingestao, null, "caminho", estado);

    var resposta = controller.ingestir(new RagController.RagIngestaoRequest("/etc/passwd"));

    assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
    assertEquals(null, ingestao.caminho);
  }

  @Test
  void statusExpõeContagemEEstadoDaIngestão() {
    RagChunkRepository repository = mock(RagChunkRepository.class);
    when(repository.contar()).thenReturn(10L);
    RagIngestaoEstado estado = new RagIngestaoEstado();
    estado.iniciar(100);
    estado.registrarProgresso(40);
    RagController controller =
        new RagController(new IngestaoCapturando(), repository, "caminho", estado);

    var resposta = controller.status();

    assertEquals(HttpStatus.OK, resposta.getStatusCode());
    assertEquals(10L, resposta.getBody().get("chunks"));
    assertEquals("caminho", resposta.getBody().get("caminho"));
    assertEquals("EM_ANDAMENTO", resposta.getBody().get("situacao"));
    assertEquals(100, resposta.getBody().get("total"));
    assertEquals(40, resposta.getBody().get("processados"));
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
