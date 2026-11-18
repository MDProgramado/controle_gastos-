package com.controlefinanceiro.whatsapp;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link AuditoriaTelefone}. Registra quem vinculou/trocou o telefone de qual
 * usuário.
 */
@Repository
public interface AuditoriaTelefoneRepository extends JpaRepository<AuditoriaTelefone, Long> {

  @Query(
      "select a from AuditoriaTelefone a where a.usuario.id = :usuarioId order by a.criadoEm desc")
  List<AuditoriaTelefone> findByUsuarioIdOrderByCriadoEmDesc(@Param("usuarioId") Long usuarioId);
}
