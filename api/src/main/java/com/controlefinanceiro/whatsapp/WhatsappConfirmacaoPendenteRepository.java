package com.controlefinanceiro.whatsapp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  long countByUsuarioTelefoneIdAndStatus(Long usuarioTelefoneId, String status);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update WhatsappConfirmacaoPendente p set p.status = 'CONFIRMADA' "
          + "where p.id = :id and p.usuarioTelefone.id = :usuarioTelefoneId "
          + "and p.status = 'PENDENTE' and p.expiraEm > :agora")
  int confirmar(
      @Param("id") Long id,
      @Param("usuarioTelefoneId") Long usuarioTelefoneId,
      @Param("agora") OffsetDateTime agora);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update WhatsappConfirmacaoPendente p set p.status = 'EXPIRADA' "
          + "where p.id = :id and p.usuarioTelefone.id = :usuarioTelefoneId "
          + "and p.status = 'PENDENTE' and p.expiraEm <= :agora")
  int expirar(
      @Param("id") Long id,
      @Param("usuarioTelefoneId") Long usuarioTelefoneId,
      @Param("agora") OffsetDateTime agora);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update WhatsappConfirmacaoPendente p set p.status = 'CANCELADA' "
          + "where p.id = :id and p.usuarioTelefone.id = :usuarioTelefoneId "
          + "and p.status = 'PENDENTE'")
  int cancelar(@Param("id") Long id, @Param("usuarioTelefoneId") Long usuarioTelefoneId);
}
