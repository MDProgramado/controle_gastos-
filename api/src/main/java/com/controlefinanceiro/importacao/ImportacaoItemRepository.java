package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositório de itens de staging, escalonado por empresa_id e usuario_id do token. */
@Repository
public interface ImportacaoItemRepository extends JpaRepository<ImportacaoItem, Long> {

  List<ImportacaoItem> findByDocumentoIdAndEmpresaIdAndUsuarioId(
      Long documentoId, Long empresaId, Long usuarioId);

  List<ImportacaoItem> findByEmpresaIdAndUsuarioIdAndStatus(
      Long empresaId, Long usuarioId, String status);
}
