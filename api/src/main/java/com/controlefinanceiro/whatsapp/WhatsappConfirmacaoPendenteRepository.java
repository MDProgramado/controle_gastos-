package com.controlefinanceiro.whatsapp;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositório de {@link WhatsappConfirmacaoPendente}. Busca pendências por telefone e status. */
@Repository
public interface WhatsappConfirmacaoPendenteRepository
    extends JpaRepository<WhatsappConfirmacaoPendente, Long> {

  Optional<WhatsappConfirmacaoPendente> findFirstByUsuarioTelefoneIdAndStatusOrderByCriadoEmDesc(
      Long usuarioTelefoneId, String status);

  @Query(
      "select c from WhatsappConfirmacaoPendente c "
          + "where c.usuarioTelefone.id = :usuarioTelefoneId "
          + "and c.status = 'PENDENTE' "
          + "and c.expiraEm < CURRENT_TIMESTAMP")
  List<WhatsappConfirmacaoPendente> findExpiredByUsuarioTelefoneId(
      @Param("usuarioTelefoneId") Long usuarioTelefoneId);
}
