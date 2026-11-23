package com.controlefinanceiro.conta;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositório de faturas de cartão, escalonado por empresa_id e usuario_id do token. */
@Repository
public interface FaturaCartaoRepository extends JpaRepository<FaturaCartao, Long> {

  @Query(
      "select f from FaturaCartao f where f.documento.id = :documentoId"
          + " and f.empresa.id = :empresaId and f.usuario.id = :usuarioId")
  Optional<FaturaCartao> findByDocumentoIdAndEmpresaIdAndUsuarioId(
      @Param("documentoId") Long documentoId,
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId);

  @Query(
      "select f from FaturaCartao f where f.id = :id and f.empresa.id = :empresaId"
          + " and f.usuario.id = :usuarioId")
  Optional<FaturaCartao> findByIdAndEmpresaIdAndUsuarioId(
      @Param("id") Long id, @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);

  @Query(
      "select f from FaturaCartao f where f.empresa.id = :empresaId"
          + " and f.usuario.id = :usuarioId order by f.id desc")
  List<FaturaCartao> findByEmpresaIdAndUsuarioId(
      @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);
}
