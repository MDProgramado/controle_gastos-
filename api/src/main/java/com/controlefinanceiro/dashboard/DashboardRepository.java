package com.controlefinanceiro.dashboard;

import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.controlefinanceiro.lancamento.Lancamento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório de agregação do dashboard. Todas as queries filtram por empresa_id (tenant, R3) e
 * executam SUM/GROUP BY no SQL — nunca soma na camada web. Usa os índices existentes
 * idx_lanc_empresa_data e idx_cat_empresa_tipo.
 */
@Repository
public interface DashboardRepository extends JpaRepository<Lancamento, Long> {

  /**
   * Total de entradas da empresa no mês. Retorna 0 (não null) para meses vazios via COALESCE.
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query(
      "SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l "
          + "WHERE l.empresa.id = :empresaId AND l.tipo = 'ENTRADA' AND l.data BETWEEN :dataInicio AND :dataFim")
  BigDecimal totalEntradas(
      @Param("empresaId") Long empresaId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);

  /**
   * Total de saídas da empresa no mês. Retorna 0 (não null) para meses vazios via COALESCE.
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query(
      "SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l "
          + "WHERE l.empresa.id = :empresaId AND l.tipo = 'SAIDA' AND l.data BETWEEN :dataInicio AND :dataFim")
  BigDecimal totalSaidas(
      @Param("empresaId") Long empresaId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);

  /**
   * Breakdown por categoria de SAÍDA: LEFT JOIN traz categorias zeradas, ordenado por valor
   * decrescente. Filtra apenas tipo='SAIDA' (categorias de entrada não aparecem).
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query(
      "SELECT new com.controlefinanceiro.dashboard.dto.CategoryExpense("
          + "c.id, c.nome, COALESCE(SUM(l.valor), 0)) "
          + "FROM Categoria c "
          + "LEFT JOIN Lancamento l ON l.categoria = c "
          + "AND l.empresa.id = :empresaId "
          + "AND l.tipo = 'SAIDA' "
          + "AND l.data BETWEEN :dataInicio AND :dataFim "
          + "WHERE c.empresa.id = :empresaId AND c.tipo = 'SAIDA' "
          + "GROUP BY c.id, c.nome "
          + "ORDER BY COALESCE(SUM(l.valor), 0) DESC")
  List<CategoryExpense> breakdownSaida(
      @Param("empresaId") Long empresaId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);
}
