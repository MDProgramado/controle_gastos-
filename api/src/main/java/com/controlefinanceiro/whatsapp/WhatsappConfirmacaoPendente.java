package com.controlefinanceiro.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Entidade da tabela whatsapp_confirmacao_pendente. Armazena uma proposta de lançamento aguardando
 * confirmação explícita do usuário pelo WhatsApp.
 */
@Entity
@Table(name = "whatsapp_confirmacao_pendente")
public class WhatsappConfirmacaoPendente {

  public enum Status {
    PENDENTE,
    CONFIRMADA,
    CANCELADA,
    EXPIRADA
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_telefone_id", nullable = false)
  private UsuarioTelefone usuarioTelefone;

  @Column(nullable = false, columnDefinition = "jsonb")
  private String proposta;

  @Column(name = "expira_em", nullable = false)
  private OffsetDateTime expiraEm;

  @Column(nullable = false, length = 16)
  private String status = "PENDENTE";

  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @PrePersist
  void aoPersistir() {
    if (criadoEm == null) {
      criadoEm = OffsetDateTime.now();
    }
    if (status == null) {
      status = "PENDENTE";
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UsuarioTelefone getUsuarioTelefone() {
    return usuarioTelefone;
  }

  public void setUsuarioTelefone(UsuarioTelefone usuarioTelefone) {
    this.usuarioTelefone = usuarioTelefone;
  }

  public String getProposta() {
    return proposta;
  }

  public void setProposta(String proposta) {
    this.proposta = proposta;
  }

  public OffsetDateTime getExpiraEm() {
    return expiraEm;
  }

  public void setExpiraEm(OffsetDateTime expiraEm) {
    this.expiraEm = expiraEm;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }

  public boolean isPendente() {
    return "PENDENTE".equals(status);
  }

  public boolean isExpirado() {
    return "PENDENTE".equals(status) && expiraEm != null && expiraEm.isBefore(OffsetDateTime.now());
  }
}
