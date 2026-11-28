package com.controlefinanceiro.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistência da tabela {@code ai_uso_diario} (sempre escopada por {@code empresa_id}). */
@Repository
public class AiUsoRepository {

  private static final String UPSERT =
      """
      INSERT INTO ai_uso_diario (
        empresa_id, usuario_id, data, operacao, modelo,
        tokens_entrada, tokens_saida, custo_usd, chamadas
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT (empresa_id, usuario_id, data, operacao, modelo) DO UPDATE SET
        tokens_entrada = ai_uso_diario.tokens_entrada + EXCLUDED.tokens_entrada,
        tokens_saida = ai_uso_diario.tokens_saida + EXCLUDED.tokens_saida,
        custo_usd = ai_uso_diario.custo_usd + EXCLUDED.custo_usd,
        chamadas = ai_uso_diario.chamadas + EXCLUDED.chamadas
      """;

  private static final String POR_DIA_E_USUARIO =
      """
      SELECT data, usuario_id,
             SUM(tokens_entrada) AS tokens_entrada,
             SUM(tokens_saida) AS tokens_saida,
             SUM(custo_usd) AS custo_usd,
             SUM(chamadas) AS chamadas
        FROM ai_uso_diario
       WHERE empresa_id = ? AND data BETWEEN ? AND ?
       GROUP BY data, usuario_id
       ORDER BY data, usuario_id
      """;

  /** Linha agregada por dia e usuário. */
  public record UsoPorDia(
      LocalDate data,
      Long usuarioId,
      long tokensEntrada,
      long tokensSaida,
      BigDecimal custoUsd,
      long chamadas) {}

  private final JdbcTemplate jdbc;

  public AiUsoRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void acumular(Long empresaId, Long usuarioId, LocalDate data, UsoIa uso) {
    jdbc.update(
        UPSERT,
        empresaId,
        usuarioId,
        Date.valueOf(data),
        uso.operacao(),
        uso.modelo(),
        uso.tokensEntrada(),
        uso.tokensSaida(),
        uso.custoUsd().setScale(6, RoundingMode.HALF_EVEN),
        uso.chamadas());
  }

  public List<UsoPorDia> porDiaEUsuario(Long empresaId, LocalDate de, LocalDate ate) {
    return jdbc.query(
        POR_DIA_E_USUARIO,
        (rs, i) ->
            new UsoPorDia(
                rs.getDate("data").toLocalDate(),
                rs.getLong("usuario_id"),
                rs.getLong("tokens_entrada"),
                rs.getLong("tokens_saida"),
                rs.getBigDecimal("custo_usd").setScale(6, RoundingMode.HALF_EVEN),
                rs.getLong("chamadas")),
        empresaId,
        Date.valueOf(de),
        Date.valueOf(ate));
  }
}
