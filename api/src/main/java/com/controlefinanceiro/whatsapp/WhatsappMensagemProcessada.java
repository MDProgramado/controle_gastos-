package com.controlefinanceiro.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Entidade da tabela whatsapp_mensagem_processada. Garante idempotência: uma mesma message_id da
 * Meta é processada no máximo uma vez.
 */
@Entity
@Table(name = "whatsapp_mensagem_processada")
public class WhatsappMensagemProcessada {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255, unique = true)
  private String messageId;

  @Column(name = "processada_em", nullable = false)
  private OffsetDateTime processadaEm;

  @Column(length = 40)
  private String resultado;

  @PrePersist
  void aoPersistir() {
    if (processadaEm == null) {
      processadaEm = OffsetDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public OffsetDateTime getProcessadaEm() {
    return processadaEm;
  }

  public void setProcessadaEm(OffsetDateTime processadaEm) {
    this.processadaEm = processadaEm;
  }

  public String getResultado() {
    return resultado;
  }

  public void setResultado(String resultado) {
    this.resultado = resultado;
  }
}
