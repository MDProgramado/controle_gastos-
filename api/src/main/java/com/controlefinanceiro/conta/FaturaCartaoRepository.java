package com.controlefinanceiro.conta;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link FaturaCartao} (padrão P1). Filtro por empresa_id (tenant, R3).
 *
 * <p>Uma fatura por documento confirmado: {@link #findByDocumentoId} alimenta a idempotência da
 * confirmação.
 */
@Repository
public interface FaturaCartaoRepository extends JpaRepository<FaturaCartao, Long> {

  /** Fatura gerada a partir de um documento de importação (0 ou 1 — idempotência). */
  Optional<FaturaCartao> findByDocumentoId(Long documentoId);

  /** Faturas da empresa, mais recentes primeiro (consultas futuras de contas). */
  @Query("select f from FaturaCartao f where f.empresa.id = :empresaId order by f.id desc")
  java.util.List<FaturaCartao> findByEmpresaId(@Param("empresaId") Long empresaId);
}
