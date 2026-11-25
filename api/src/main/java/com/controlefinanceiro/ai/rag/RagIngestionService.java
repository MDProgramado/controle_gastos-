package com.controlefinanceiro.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RagIngestionService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final EmbeddingClient embeddingClient;
  private final RagChunkRepository repository;
  private final int batchSize;
  private final RagIngestaoEstado estado;

  @Autowired
  public RagIngestionService(
      EmbeddingClient embeddingClient,
      RagChunkRepository repository,
      @Value("${ai.rag.embedding-batch-size:64}") int batchSize,
      RagIngestaoEstado estado) {
    this.embeddingClient = embeddingClient;
    this.repository = repository;
    this.batchSize = Math.max(1, batchSize);
    this.estado = estado;
  }

  public RagIngestaoResultado ingestirSync(String caminho) {
    estado.iniciar(0);
    try {
      List<RagMeta> metas = ler(caminho);
      estado.iniciar(metas.size());
      int batches = 0;
      for (int inicio = 0; inicio < metas.size(); inicio += batchSize) {
        int fim = Math.min(inicio + batchSize, metas.size());
        List<RagMeta> parte = metas.subList(inicio, fim);
        List<String> textos = parte.stream().map(this::textoParaEmbedding).toList();
        List<float[]> vetores = embeddingClient.embedir(textos);
        if (vetores.size() != parte.size()) {
          throw new IllegalStateException(
              "Serviço de embeddings devolveu "
                  + vetores.size()
                  + " vetores para "
                  + parte.size()
                  + " chunks.");
        }
        List<RagChunk> chunks = new ArrayList<>(parte.size());
        for (int i = 0; i < parte.size(); i++) {
          chunks.add(parte.get(i).paraChunk(vetores.get(i)));
        }
        repository.upsert(chunks);
        estado.registrarProgresso(fim);
        batches++;
      }
      estado.concluir();
      return new RagIngestaoResultado(metas.size(), batches);
    } catch (RuntimeException e) {
      estado.falhar(e.getMessage());
      throw e;
    }
  }

  private List<RagMeta> ler(String caminho) {
    try (Stream<String> linhas = Files.lines(Path.of(caminho), StandardCharsets.UTF_8)) {
      List<RagMeta> metas = new ArrayList<>();
      var iterador = linhas.iterator();
      while (iterador.hasNext()) {
        String linha = iterador.next().trim();
        if (!linha.isEmpty()) {
          metas.add(parse(linha));
        }
      }
      return metas;
    } catch (IOException e) {
      throw new IllegalStateException("Não foi possível ler o corpus: " + caminho, e);
    }
  }

  private RagMeta parse(String linha) {
    try {
      JsonNode json = MAPPER.readTree(linha);
      return new RagMeta(
          texto(json, "chunk_id"),
          texto(json, "source_id"),
          texto(json, "titulo"),
          texto(json, "organizacao"),
          texto(json, "tipo"),
          texto(json, "nivel"),
          json.path("tema").isArray() ? json.path("tema").toString() : texto(json, "tema"),
          texto(json, "licenca"),
          texto(json, "url"),
          texto(json, "vigencia"),
          texto(json, "uso_recomendado"),
          texto(json, "idioma"),
          texto(json, "arquivo_origem"),
          json.path("n_chars").isNumber() ? json.path("n_chars").asInt() : null,
          texto(json, "texto"));
    } catch (Exception e) {
      throw new IllegalStateException("Linha inválida no corpus JSONL.", e);
    }
  }

  private String texto(JsonNode json, String campo) {
    JsonNode valor = json.get(campo);
    return valor == null || valor.isNull() ? null : valor.asText();
  }

  private String textoParaEmbedding(RagMeta meta) {
    StringBuilder sb = new StringBuilder();
    append(sb, meta.titulo());
    append(sb, meta.organizacao());
    append(sb, meta.tipo());
    append(sb, meta.nivel());
    append(sb, meta.tema());
    append(sb, meta.vigencia());
    append(sb, meta.texto());
    return sb.toString();
  }

  private void append(StringBuilder sb, String valor) {
    if (valor != null && !valor.isBlank()) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(valor.trim());
    }
  }

  private record RagMeta(
      String chunkId,
      String sourceId,
      String titulo,
      String organizacao,
      String tipo,
      String nivel,
      String tema,
      String licenca,
      String url,
      String vigencia,
      String usoRecomendado,
      String idioma,
      String arquivoOrigem,
      Integer nChars,
      String texto) {

    RagChunk paraChunk(float[] embedding) {
      return new RagChunk(
          chunkId,
          sourceId,
          titulo,
          organizacao,
          tipo,
          nivel,
          tema,
          licenca,
          url,
          vigencia,
          usoRecomendado,
          idioma,
          arquivoOrigem,
          nChars,
          texto,
          embedding);
    }
  }
}
