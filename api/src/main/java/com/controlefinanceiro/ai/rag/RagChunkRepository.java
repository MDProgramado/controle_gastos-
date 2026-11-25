package com.controlefinanceiro.ai.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RagChunkRepository {

  private static final String UPSERT =
      """
      INSERT INTO rag_chunk (
        chunk_id, source_id, titulo, organizacao, tipo, nivel, tema, licenca, url,
        vigencia, uso_recomendado, idioma, arquivo_origem, n_chars, texto, embedding
      ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS vector))
      ON CONFLICT (chunk_id) DO UPDATE SET
        source_id = EXCLUDED.source_id,
        titulo = EXCLUDED.titulo,
        organizacao = EXCLUDED.organizacao,
        tipo = EXCLUDED.tipo,
        nivel = EXCLUDED.nivel,
        tema = EXCLUDED.tema,
        licenca = EXCLUDED.licenca,
        url = EXCLUDED.url,
        vigencia = EXCLUDED.vigencia,
        uso_recomendado = EXCLUDED.uso_recomendado,
        idioma = EXCLUDED.idioma,
        arquivo_origem = EXCLUDED.arquivo_origem,
        n_chars = EXCLUDED.n_chars,
        texto = EXCLUDED.texto,
        embedding = EXCLUDED.embedding
      """;

  private static final String COLUNAS =
      "chunk_id, titulo, organizacao, vigencia, split_part(chunk_id, '::', 2), texto";

  private final JdbcTemplate jdbc;

  public RagChunkRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void upsert(List<RagChunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return;
    }
    List<Object[]> lote = new ArrayList<>(chunks.size());
    for (RagChunk c : chunks) {
      lote.add(
          new Object[] {
            c.chunkId(),
            c.sourceId(),
            c.titulo(),
            c.organizacao(),
            c.tipo(),
            c.nivel(),
            c.tema() == null ? "[]" : c.tema(),
            c.licenca(),
            c.url(),
            c.vigencia(),
            c.usoRecomendado(),
            c.idioma(),
            c.arquivoOrigem(),
            c.nChars(),
            c.texto(),
            paraLiteralVetor(c.embedding())
          });
    }
    jdbc.batchUpdate(UPSERT, lote);
  }

  public List<RagTrecho> buscarPorVetor(float[] embedding, int limite) {
    String sql =
        "SELECT " + COLUNAS + " FROM rag_chunk ORDER BY embedding <=> CAST(? AS vector) LIMIT ?";
    return jdbc.query(sql, this::mapear, paraLiteralVetor(embedding), limite);
  }

  public List<RagTrecho> buscarPorTexto(String consulta, int limite) {
    String sql =
        "SELECT "
            + COLUNAS
            + " FROM rag_chunk WHERE tsv @@ websearch_to_tsquery('portuguese', ?) "
            + "ORDER BY ts_rank(tsv, websearch_to_tsquery('portuguese', ?)) DESC LIMIT ?";
    return jdbc.query(sql, this::mapear, consulta, consulta, limite);
  }

  public long contar() {
    Long total = jdbc.queryForObject("SELECT COUNT(*) FROM rag_chunk", Long.class);
    return total == null ? 0L : total;
  }

  private RagTrecho mapear(java.sql.ResultSet rs, int linha) throws java.sql.SQLException {
    return new RagTrecho(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        rs.getString(6));
  }

  private String paraLiteralVetor(float[] vetor) {
    if (vetor == null || vetor.length == 0) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder(vetor.length * 10 + 2).append('[');
    for (int i = 0; i < vetor.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(Float.toString(vetor[i]).replace(',', '.'));
    }
    return sb.append(']').toString().toUpperCase(Locale.ROOT);
  }
}
