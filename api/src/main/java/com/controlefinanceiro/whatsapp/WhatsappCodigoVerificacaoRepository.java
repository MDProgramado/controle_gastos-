package com.controlefinanceiro.whatsapp;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link WhatsappCodigoVerificacao}. Gerencia códigos de 6 dígitos para verificação
 * de número, com controle de tentativas e expiração.
 */
@Repository
public interface WhatsappCodigoVerificacaoRepository
    extends JpaRepository<WhatsappCodigoVerificacao, Long> {

  List<WhatsappCodigoVerificacao> findByUsuarioTelefoneIdOrderByCriadoEmDesc(
      Long usuarioTelefoneId);

  List<WhatsappCodigoVerificacao> findByUsuarioTelefoneIdAndUsadoEmIsNullOrderByCriadoEmDesc(
      Long usuarioTelefoneId);

  @Query(
      "select c from WhatsappCodigoVerificacao c "
          + "where c.usuarioTelefone.id = :usuarioTelefoneId "
          + "and c.expiraEm > :agora "
          + "and c.tentativas < 5 "
          + "order by c.criadoEm desc")
  List<WhatsappCodigoVerificacao> findValidByUsuarioTelefoneId(
      @Param("usuarioTelefoneId") Long usuarioTelefoneId, @Param("agora") OffsetDateTime agora);
}
