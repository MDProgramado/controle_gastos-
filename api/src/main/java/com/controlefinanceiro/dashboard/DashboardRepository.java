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
 * Repositório de agregação do dashboard. Todas as queries filtram empresa_id e usuario_id no SQL —
 * nunca soma na camada web. Índices (empresa, usuario, data) e (empresa, usuario, categoria).
 */
@Repository
public interface DashboardRepository extends JpaRepository<Lancamento, Long> {

  @Query(
      "SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l "
          + "WHERE l.empresa.id = :empresaId AND l.usuario.id = :usuarioId "
          + "AND l.tipo = 'ENTRADA' AND l.data BETWEEN :dataInicio AND :dataFim")
  BigDecimal totalEntradas(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);

  @Query(
      "SELECT COALESCE(SUM(l.valor), 0) FROM Lancamento l "
          + "WHERE l.empresa.id = :empresaId AND l.usuario.id = :usuarioId "
          + "AND l.tipo = 'SAIDA' AND l.data BETWEEN :dataInicio AND :dataFim")
  BigDecimal totalSaidas(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);

  @Query(
      "SELECT new com.controlefinanceiro.dashboard.dto.CategoryExpense("
          + "c.id, c.nome, COALESCE(SUM(l.valor), 0)) "
          + "FROM Categoria c "
          + "LEFT JOIN Lancamento l ON l.categoria = c "
          + "AND l.empresa.id = :empresaId "
          + "AND l.usuario.id = :usuarioId "
          + "AND l.tipo = 'SAIDA' "
          + "AND l.data BETWEEN :dataInicio AND :dataFim "
          + "WHERE c.empresa.id = :empresaId AND c.tipo = 'SAIDA' "
          + "GROUP BY c.id, c.nome "
          + "ORDER BY COALESCE(SUM(l.valor), 0) DESC")
  List<CategoryExpense> breakdownSaida(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);

  @Query(
      "SELECT new com.controlefinanceiro.dashboard.dto.CategoryExpense("
          + "c.id, c.nome, COALESCE(SUM(l.valor), 0)) "
          + "FROM Categoria c "
          + "LEFT JOIN Lancamento l ON l.categoria = c "
          + "AND l.empresa.id = :empresaId "
          + "AND l.usuario.id = :usuarioId "
          + "AND l.tipo = 'ENTRADA' "
          + "AND l.data BETWEEN :dataInicio AND :dataFim "
          + "WHERE c.empresa.id = :empresaId AND c.tipo = 'ENTRADA' "
          + "GROUP BY c.id, c.nome "
          + "ORDER BY COALESCE(SUM(l.valor), 0) DESC")
  List<CategoryExpense> breakdownEntrada(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);
}
