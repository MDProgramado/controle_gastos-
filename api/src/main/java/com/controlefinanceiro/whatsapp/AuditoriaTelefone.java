package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.usuario.Usuario;
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
 * Entidade da tabela auditoria_telefone. Registra quem vinculou/trocou o telefone de qual usuário.
 */
@Entity
@Table(name = "auditoria_telefone")
public class AuditoriaTelefone {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "admin_id", nullable = false)
  private Usuario admin;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "numero_mascarado", nullable = false, length = 40)
  private String numeroMascarado;

  @Column(nullable = false, length = 20)
  private String acao; // VINCULAR | TROCAR

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

  public Usuario getAdmin() {
    return admin;
  }

  public void setAdmin(Usuario admin) {
    this.admin = admin;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public String getNumeroMascarado() {
    return numeroMascarado;
  }

  public void setNumeroMascarado(String numeroMascarado) {
    this.numeroMascarado = numeroMascarado;
  }

  public String getAcao() {
    return acao;
  }

  public void setAcao(String acao) {
    this.acao = acao;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
