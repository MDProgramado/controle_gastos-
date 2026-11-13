package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link ImportacaoItem}.
 *
 * <p>Todas as queries filtram por empresa_id (tenant, R3).
 */
@Repository
public interface ImportacaoItemRepository extends JpaRepository<ImportacaoItem, Long> {

  List<ImportacaoItem> findByDocumentoId(Long documentoId);

  List<ImportacaoItem> findByEmpresaIdAndStatus(Long empresaId, String status);
}
