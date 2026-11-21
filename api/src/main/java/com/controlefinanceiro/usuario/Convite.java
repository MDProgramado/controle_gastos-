package com.controlefinanceiro.usuario;

import com.controlefinanceiro.empresa.Empresa;
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

@Entity
@Table(name = "usuario_convite")
public class Convite {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(nullable = false, length = 160)
  private String email;

  @Column(nullable = false, length = 16)
  private String role;

  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  @Column(name = "expira_em", nullable = false)
  private OffsetDateTime expiraEm;

  @Column(name = "usado_em")
  private OffsetDateTime usadoEm;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "criado_por", nullable = false)
  private Usuario criadoPor;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @PrePersist
  void aoPersistir() {
    if (criadoEm == null) {
      criadoEm = OffsetDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Empresa getEmpresa() {
    return empresa;
  }

  public void setEmpresa(Empresa empresa) {
    this.empresa = empresa;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public OffsetDateTime getExpiraEm() {
    return expiraEm;
  }

  public void setExpiraEm(OffsetDateTime expiraEm) {
    this.expiraEm = expiraEm;
  }

  public OffsetDateTime getUsadoEm() {
    return usadoEm;
  }

  public void setUsadoEm(OffsetDateTime usadoEm) {
    this.usadoEm = usadoEm;
  }

  public Usuario getCriadoPor() {
    return criadoPor;
  }

  public void setCriadoPor(Usuario criadoPor) {
    this.criadoPor = criadoPor;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
