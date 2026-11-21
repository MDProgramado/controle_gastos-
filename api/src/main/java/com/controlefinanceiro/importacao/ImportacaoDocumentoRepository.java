package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositório de documentos de importação, escalonado por empresa_id e usuario_id do token. */
@Repository
public interface ImportacaoDocumentoRepository extends JpaRepository<ImportacaoDocumento, Long> {

  ImportacaoDocumento findByEmpresaIdAndUsuarioIdAndHashSha256(
      Long empresaId, Long usuarioId, String hashSha256);

  Page<ImportacaoDocumento> findByEmpresaIdAndUsuarioIdOrderByIdDesc(
      Long empresaId, Long usuarioId, Pageable pageable);

  @Query(
      "select d from ImportacaoDocumento d where d.id = :id and d.empresa.id = :empresaId"
          + " and d.usuario.id = :usuarioId")
  Optional<ImportacaoDocumento> findByIdAndEmpresaIdAndUsuarioId(
      @Param("id") Long id, @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);
}
