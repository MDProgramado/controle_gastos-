package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link ImportacaoDocumento}.
 *
 * <p>Todas as queries filtram por empresa_id (tenant, R3).
 */
@Repository
public interface ImportacaoDocumentoRepository extends JpaRepository<ImportacaoDocumento, Long> {

  ImportacaoDocumento findByEmpresaIdAndHashSha256(Long empresaId, String hashSha256);

  /** Histórico da empresa, mais recente primeiro (tela de importações). */
  Page<ImportacaoDocumento> findByEmpresaIdOrderByIdDesc(Long empresaId, Pageable pageable);

  /** Busca um documento dentro do tenant (isolamento R3 — nunca expõe de outra empresa). */
  @Query("select d from ImportacaoDocumento d where d.id = :id and d.empresa.id = :empresaId")
  Optional<ImportacaoDocumento> findByIdAndEmpresaId(
      @Param("id") Long id, @Param("empresaId") Long empresaId);
}
