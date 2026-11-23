package com.controlefinanceiro.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_memoria_sessao")
public class AiMemoriaSessao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "session_id", nullable = false, length = 255)
  private String sessionId;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  public AiMemoriaSessao() {}

  public AiMemoriaSessao(Long empresaId, Long usuarioId, String sessionId) {
    this.empresaId = empresaId;
    this.usuarioId = usuarioId;
    this.sessionId = sessionId;
  }

  @PrePersist
  void aoPersistir() {
    Instant agora = Instant.now();
    if (criadoEm == null) {
      criadoEm = agora;
    }
    if (atualizadoEm == null) {
      atualizadoEm = agora;
    }
  }

  @PreUpdate
  void aoAtualizar() {
    atualizadoEm = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getEmpresaId() {
    return empresaId;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  public void setAtualizadoEm(Instant atualizadoEm) {
    this.atualizadoEm = atualizadoEm;
  }
}
