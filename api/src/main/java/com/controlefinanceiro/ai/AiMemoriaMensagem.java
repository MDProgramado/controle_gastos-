package com.controlefinanceiro.ai;

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
import java.time.Instant;

@Entity
@Table(name = "ai_memoria_mensagem")
public class AiMemoriaMensagem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sessao_id", nullable = false)
  private AiMemoriaSessao sessao;

  @Column(nullable = false, length = 16)
  private String papel;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String conteudo;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  public AiMemoriaMensagem() {}

  public AiMemoriaMensagem(AiMemoriaSessao sessao, String papel, String conteudo) {
    this.sessao = sessao;
    this.papel = papel;
    this.conteudo = conteudo;
  }

  @PrePersist
  void aoPersistir() {
    if (criadoEm == null) {
      criadoEm = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public AiMemoriaSessao getSessao() {
    return sessao;
  }

  public String getPapel() {
    return papel;
  }

  public String getConteudo() {
    return conteudo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
