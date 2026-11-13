package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link ImportacaoFatura}.
 *
 * <p>Todas as queries filtram por empresa_id (tenant, R3).
 */
@Repository
public interface ImportacaoFaturaRepository extends JpaRepository<ImportacaoFatura, Long> {

  ImportacaoFatura findByDocumentoId(Long documentoId);
}
