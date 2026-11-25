package com.controlefinanceiro.helpdesk;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, Long> {

  @Query(
      "select t from HelpdeskTicket t where t.empresa.id = :empresaId"
          + " and t.usuario.id = :usuarioId and (:status is null or t.status = :status)"
          + " order by t.criadoEm desc, t.id desc")
  Page<HelpdeskTicket> findByEmpresaIdAndUsuarioIdAndStatus(
      @Param("empresaId") Long empresaId,
      @Param("usuarioId") Long usuarioId,
      @Param("status") TicketStatus status,
      Pageable pageable);

  @Query(
      "select t from HelpdeskTicket t where t.empresa.id = :empresaId"
          + " and (:status is null or t.status = :status)"
          + " order by t.criadoEm desc, t.id desc")
  Page<HelpdeskTicket> findByEmpresaIdAndStatus(
      @Param("empresaId") Long empresaId, @Param("status") TicketStatus status, Pageable pageable);

  @Query(
      "select t from HelpdeskTicket t where t.id = :id and t.empresa.id = :empresaId"
          + " and t.usuario.id = :usuarioId")
  Optional<HelpdeskTicket> findByIdAndEmpresaIdAndUsuarioId(
      @Param("id") Long id, @Param("empresaId") Long empresaId, @Param("usuarioId") Long usuarioId);

  @Query("select t from HelpdeskTicket t where t.id = :id and t.empresa.id = :empresaId")
  Optional<HelpdeskTicket> findByIdAndEmpresaId(
      @Param("id") Long id, @Param("empresaId") Long empresaId);
}
