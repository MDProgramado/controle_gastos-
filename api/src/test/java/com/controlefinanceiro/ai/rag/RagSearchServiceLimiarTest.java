package com.controlefinanceiro.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Item 2.5: RAG com limiar mínimo de similaridade. */
class RagSearchServiceLimiarTest {

  private static RagTrecho trecho(String id) {
    return new RagTrecho(id + "::1", "t", "o", "v", "1", "texto " + id);
  }

  @Test
  @DisplayName("trecho abaixo do limiar é descartado; acima é mantido; limiar vai para o SQL")
  void descartaAbaixoDoLimiar() {
    EmbeddingClient embedding = mock(EmbeddingClient.class);
    when(embedding.embedir(any())).thenReturn(List.of(new float[] {0.1f}));
    RagChunkRepository repo = mock(RagChunkRepository.class);
    when(repo.buscarPorVetor(any(), anyInt(), anyDouble()))
        .thenReturn(
            List.of(
                new RagChunkRepository.TrechoPontuado(trecho("bom"), 0.81),
                new RagChunkRepository.TrechoPontuado(trecho("ruim"), 0.42)));
    when(repo.buscarPorTexto(anyString(), anyInt())).thenReturn(List.of());

    List<RagTrecho> r = new RagSearchService(embedding, repo, 0.60).buscar("juros", 5);

    assertThat(r).extracting(RagTrecho::chunkId).containsExactly("bom::1");
    verify(repo).buscarPorVetor(any(), eq(5), eq(0.60));
  }

  @Test
  @DisplayName("nada acima do limiar e nada na busca lexical: lista vazia")
  void semNadaRelevanteListaVazia() {
    EmbeddingClient embedding = mock(EmbeddingClient.class);
    when(embedding.embedir(any())).thenReturn(List.of(new float[] {0.1f}));
    RagChunkRepository repo = mock(RagChunkRepository.class);
    when(repo.buscarPorVetor(any(), anyInt(), anyDouble()))
        .thenReturn(List.of(new RagChunkRepository.TrechoPontuado(trecho("ruim"), 0.10)));
    when(repo.buscarPorTexto(anyString(), anyInt())).thenReturn(List.of());

    assertThat(new RagSearchService(embedding, repo, 0.60).buscar("receita de bolo", 5)).isEmpty();
  }

  @Test
  @DisplayName("busca lexical continua somando resultados")
  void lexicalSoma() {
    EmbeddingClient embedding = mock(EmbeddingClient.class);
    when(embedding.embedir(any())).thenReturn(List.of(new float[] {0.1f}));
    RagChunkRepository repo = mock(RagChunkRepository.class);
    when(repo.buscarPorVetor(any(), anyInt(), anyDouble())).thenReturn(List.of());
    when(repo.buscarPorTexto(anyString(), anyInt())).thenReturn(List.of(trecho("lex")));

    assertThat(new RagSearchService(embedding, repo, 0.60).buscar("cdc", 5))
        .extracting(RagTrecho::chunkId)
        .containsExactly("lex::1");
  }
}
