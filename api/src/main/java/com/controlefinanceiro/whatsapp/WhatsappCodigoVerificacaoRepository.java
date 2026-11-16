package com.controlefinanceiro.whatsapp;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório de {@link WhatsappCodigoVerificacao}. Gerencia códigos de 6 dígitos para verificação
 * de número, com controle de tentativas e expiração.
 */
@Repository
public interface WhatsappCodigoVerificacaoRepository
    extends JpaRepository<WhatsappCodigoVerificacao, Long> {

  List<WhatsappCodigoVerificacao> findByTelefoneIdAndUsadoEmIsNullOrderByCriadoEmDesc(
      Long telefoneId);
}
