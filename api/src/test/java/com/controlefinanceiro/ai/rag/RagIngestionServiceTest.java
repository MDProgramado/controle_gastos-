package com.controlefinanceiro.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RagIngestionServiceTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("ingestão lê JSONL, envia embedding enriquecido e faz upsert idempotente")
  void ingestaoLeJsonlEFazUpsert() throws Exception {
    Path corpus = tempDir.resolve("corpus.jsonl");
    Files.writeString(
        corpus,
        "{\"chunk_id\":\"00-dossie::00001\",\"source_id\":\"00-dossie\","
            + "\"titulo\":\"Dossiê\",\"organizacao\":\"Conteúdo original\","
            + "\"tipo\":\"sintese\",\"nivel\":\"iniciante\",\"tema\":[\"orcamento\"],"
            + "\"licenca\":\"livre\",\"url\":\"\",\"vigencia\":\"2026-11\","
            + "\"uso_recomendado\":\"prompt\",\"idioma\":\"pt-BR\","
            + "\"arquivo_origem\":\"00_dossie_frameworks.txt\",\"n_chars\":10,"
            + "\"texto\":\"Use 50/30/20.\"}\n");

    EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
    RagChunkRepository repository = mock(RagChunkRepository.class);
    when(embeddingClient.embedir(anyList())).thenReturn(List.of(new float[] {0.1f, 0.2f}));
    RagIngestionService service = new RagIngestionService(embeddingClient, repository, 64);

    RagIngestaoResultado resultado = service.ingestirSync(corpus.toString());

    assertThat(resultado.processados()).isEqualTo(1);
    assertThat(resultado.batches()).isEqualTo(1);
    verify(embeddingClient)
        .embedir(
            org.mockito.ArgumentMatchers.argThat(
                textos ->
                    textos.size() == 1
                        && textos.get(0).contains("Dossiê")
                        && textos.get(0).contains("orcamento")
                        && textos.get(0).contains("Use 50/30/20.")));
    verify(repository).upsert(anyList());
  }
}
