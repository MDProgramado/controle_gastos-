package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositório de cabeçalhos de staging, escalonado por empresa_id e usuario_id do token. */
@Repository
public interface ImportacaoFaturaRepository extends JpaRepository<ImportacaoFatura, Long> {

  ImportacaoFatura findByDocumentoIdAndEmpresaIdAndUsuarioId(
      Long documentoId, Long empresaId, Long usuarioId);
}
