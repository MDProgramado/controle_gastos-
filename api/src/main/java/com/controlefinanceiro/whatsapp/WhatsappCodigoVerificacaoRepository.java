package com.controlefinanceiro.whatsapp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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

  Optional<WhatsappCodigoVerificacao>
      findFirstByUsuarioTelefone_EmpresaIdAndUsuarioTelefone_NumeroE164OrderByCriadoEmDesc(
          Long empresaId, String numeroE164);

  @Query(
      "select coalesce(sum(c.tentativas), 0) from WhatsappCodigoVerificacao c "
          + "where c.usuarioTelefone.id = :usuarioTelefoneId and c.criadoEm >= :desde")
  long contarTentativasRecentes(
      @Param("usuarioTelefoneId") Long usuarioTelefoneId, @Param("desde") OffsetDateTime desde);
}
