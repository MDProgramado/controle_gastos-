package com.controlefinanceiro.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_conhecimento")
class AiConhecimento {

  @Id
  @Column(name = "empresa_id")
  private Long empresaId;

  @Column(name = "texto", nullable = false, columnDefinition = "TEXT")
  private String texto = "";

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  protected AiConhecimento() {}

  AiConhecimento(Long empresaId, String texto) {
    this.empresaId = empresaId;
    this.texto = texto == null ? "" : texto;
  }

  @PrePersist
  @PreUpdate
  void tocar() {
    this.atualizadoEm = OffsetDateTime.now();
    if (this.texto == null) this.texto = "";
  }

  Long getEmpresaId() {
    return empresaId;
  }

  String getTexto() {
    return texto;
  }

  void setTexto(String texto) {
    this.texto = texto == null ? "" : texto;
  }
}
