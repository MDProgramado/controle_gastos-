package com.controlefinanceiro.lancamento;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de lançamento (padrão P1; segue {@link
 * com.controlefinanceiro.categoria.CategoriaRepository}). Filtra por empresa_id (tenant, R3) e usa
 * os índices existentes idx_lanc_empresa_data e idx_lanc_empresa_cat.
 */
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

  /**
   * Lançamentos de uma empresa entre duas datas (inclusive). Usa o índice idx_lanc_empresa_data.
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.data between :dataInicio and :dataFim order by l.data desc")
  List<Lancamento> findByEmpresaIdAndDataBetween(
      @Param("empresaId") Long empresaId,
      @Param("dataInicio") java.time.LocalDate dataInicio,
      @Param("dataFim") java.time.LocalDate dataFim);

  /**
   * Lançamentos de uma empresa por tipo. Usa o índice idx_lanc_empresa_data.
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.tipo = :tipo order by l.data desc")
  List<Lancamento> findByEmpresaIdAndTipo(
      @Param("empresaId") Long empresaId, @Param("tipo") String tipo);

  /**
   * Lançamentos de uma empresa por categoria. Usa o índice idx_lanc_empresa_cat.
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.categoria.id = :categoriaId order by l.data desc")
  List<Lancamento> findByEmpresaIdAndCategoriaId(
      @Param("empresaId") Long empresaId, @Param("categoriaId") Long categoriaId);

  /**
   * Lançamentos de uma empresa com filtros opcionais de mês, ano, tipo e categoria. Paginação para
   * suporte a listagem grande.
   *
   * @param empresaId tenant do token (via TenantContext, nunca do body)
   */
  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId"
          + " and (:ano is null or extract(year from l.data) = :ano)"
          + " and (:mes is null or extract(month from l.data) = :mes)"
          + " and (:tipo is null or l.tipo = :tipo)"
          + " and (:categoriaId is null or l.categoria.id = :categoriaId)"
          + " order by l.data desc")
  Page<Lancamento> findByEmpresaIdAndFiltros(
      @Param("empresaId") Long empresaId,
      @Param("ano") Integer ano,
      @Param("mes") Integer mes,
      @Param("tipo") String tipo,
      @Param("categoriaId") Long categoriaId,
      Pageable pageable);
}
