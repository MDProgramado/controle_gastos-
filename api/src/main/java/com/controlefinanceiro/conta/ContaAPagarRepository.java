package com.controlefinanceiro.conta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link ContaAPagar} (padrão P1). Filtro por empresa_id (tenant, R3). Usa o índice
 * idx_conta_a_pagar_empresa_vencimento.
 */
@Repository
public interface ContaAPagarRepository extends JpaRepository<ContaAPagar, Long> {

  /** Compromissos da empresa por ordem de vencimento (consultas futuras de contas). */
  @Query("select c from ContaAPagar c where c.empresa.id = :empresaId order by c.vencimento asc")
  List<ContaAPagar> findByEmpresaIdOrderByVencimento(@Param("empresaId") Long empresaId);
}
