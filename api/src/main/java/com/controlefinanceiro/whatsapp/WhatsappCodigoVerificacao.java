package com.controlefinanceiro.whatsapp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Entidade da tabela whatsapp_codigo_verificacao. Armazena o código de 6 dígitos (com hash) para
 * verificação de número de telefone, com limite de tentativas e expiração.
 */
@Entity
@Table(name = "whatsapp_codigo_verificacao")
public class WhatsappCodigoVerificacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "telefone_id", nullable = false)
  private UsuarioTelefone telefone;

  @Column(name = "codigo_hash", nullable = false, length = 100)
  private String codigoHash;

  @Column(name = "expira_em", nullable = false)
  private OffsetDateTime expiraEm;

  @Column(nullable = false)
  private int tentativas = 0;

  @Column(name = "usado_em")
  private OffsetDateTime usadoEm;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @PrePersist
  void aoPersistir() {
    if (criadoEm == null) {
      criadoEm = OffsetDateTime.now();
    }
    if (tentativas == 0) {
      tentativas = 0;
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public UsuarioTelefone getTelefone() {
    return telefone;
  }

  public void setTelefone(UsuarioTelefone telefone) {
    this.telefone = telefone;
  }

  public String getCodigoHash() {
    return codigoHash;
  }

  public void setCodigoHash(String codigoHash) {
    this.codigoHash = codigoHash;
  }

  public OffsetDateTime getExpiraEm() {
    return expiraEm;
  }

  public void setExpiraEm(OffsetDateTime expiraEm) {
    this.expiraEm = expiraEm;
  }

  public int getTentativas() {
    return tentativas;
  }

  public void setTentativas(int tentativas) {
    this.tentativas = tentativas;
  }

  public OffsetDateTime getUsadoEm() {
    return usadoEm;
  }

  public void setUsadoEm(OffsetDateTime usadoEm) {
    this.usadoEm = usadoEm;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }

  public boolean isExpirado() {
    return expiraEm != null && expiraEm.isBefore(OffsetDateTime.now());
  }

  public boolean isUsado() {
    return usadoEm != null;
  }

  public void incrementarTentativa() {
    this.tentativas++;
  }
}
