package com.controlefinanceiro.helpdesk;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HelpdeskMensagemRepository extends JpaRepository<HelpdeskMensagem, Long> {

  @Query(
      "select m from HelpdeskMensagem m where m.ticket.id = :ticketId"
          + " and m.empresa.id = :empresaId order by m.criadoEm asc, m.id asc")
  List<HelpdeskMensagem> findByTicketIdAndEmpresaIdOrderByCriadoEmAsc(
      @Param("ticketId") Long ticketId, @Param("empresaId") Long empresaId);
}
