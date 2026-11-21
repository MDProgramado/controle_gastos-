package com.controlefinanceiro.conta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositório de contas a pagar, escalonado por empresa_id e usuario_id do token. */
@Repository
public interface ContaAPagarRepository extends JpaRepository<ContaAPagar, Long> {

  @Query(
      "select c from ContaAPagar c where c.empresa.id = :empresaId"
          + " and c.usuario.id = :usuarioId order by c.vencimento asc")
  List<ContaAPagar> findByEmpresaIdAndUsuarioIdOrderByVencimento(
      @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);

  @Query(
      "select c from ContaAPagar c where c.id = :id and c.empresa.id = :empresaId"
          + " and c.usuario.id = :usuarioId")
  java.util.Optional<ContaAPagar> findByIdAndEmpresaIdAndUsuarioId(
      @Param("id") Long id, @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);
}
