package com.controlefinanceiro.lancamento;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório de lançamento. Toda consulta financeira é escalonada por empresa_id (tenant, R3) e
 * usuario_id (isolamento por usuário), usando os índices (empresa, usuario, data/categoria).
 */
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.usuario.id = :usuarioId"
          + " and l.data between :dataInicio and :dataFim order by l.data desc")
  List<Lancamento> findByEmpresaIdAndUsuarioIdAndDataBetween(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("dataInicio") java.time.LocalDate dataInicio,
      @Param("dataFim") java.time.LocalDate dataFim);

  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.usuario.id = :usuarioId"
          + " and l.tipo = :tipo order by l.data desc")
  List<Lancamento> findByEmpresaIdAndUsuarioIdAndTipo(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("tipo") String tipo);

  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.usuario.id = :usuarioId"
          + " and l.categoria.id = :categoriaId order by l.data desc")
  List<Lancamento> findByEmpresaIdAndUsuarioIdAndCategoriaId(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("categoriaId") Long categoriaId);

  @Query(
      "select l from Lancamento l where l.empresa.id = :empresaId and l.usuario.id = :usuarioId"
          + " and (:ano is null or extract(year from l.data) = :ano)"
          + " and (:mes is null or extract(month from l.data) = :mes)"
          + " and (:tipo is null or l.tipo = :tipo)"
          + " and (:categoriaId is null or l.categoria.id = :categoriaId)"
          + " order by l.data desc")
  Page<Lancamento> findByEmpresaIdAndUsuarioIdAndFiltros(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("ano") Integer ano,
      @Param("mes") Integer mes,
      @Param("tipo") String tipo,
      @Param("categoriaId") Long categoriaId,
      Pageable pageable);

  @Query(
      "select l from Lancamento l where l.id = :id and l.empresa.id = :empresaId"
          + " and l.usuario.id = :usuarioId")
  java.util.Optional<Lancamento> findByIdAndEmpresaIdAndUsuarioId(
      @Param("id") Long id, @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);

  @Query(
      "select l.chaveDedupe from Lancamento l where l.empresa.id = :empresaId"
          + " and l.usuario.id = :usuarioId and l.chaveDedupe is not null")
  List<String> findChavesDedupeByEmpresaIdAndUsuarioId(
      @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);
}
