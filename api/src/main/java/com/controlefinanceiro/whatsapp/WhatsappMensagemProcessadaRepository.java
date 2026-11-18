package com.controlefinanceiro.whatsapp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositório de {@link WhatsappMensagemProcessada}. Garante idempotência por message_id. */
@Repository
public interface WhatsappMensagemProcessadaRepository
    extends JpaRepository<WhatsappMensagemProcessada, Long> {

  Optional<WhatsappMensagemProcessada> findByMessageId(String messageId);

  boolean existsByMessageId(String messageId);
}
