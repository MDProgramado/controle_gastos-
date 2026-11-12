package com.controlefinanceiro.ai;

/**
 * Repositório de conhecimento da IA. É um local onde o usuário (ou administrador) pode alimentar
 * textos de contexto que a IA usará ao responder (ex.: regras de negócio, categorias padrão,
 * políticas). Implementação inicial em memória; pode ser migrada para banco quando necessário.
 */
public class AiKnowledgeStore {

  private String conhecimento = "";

  public String obterConhecimento() {
    return conhecimento;
  }

  public void definirConhecimento(String texto) {
    this.conhecimento = texto == null ? "" : texto;
  }

  public boolean temConhecimento() {
    return conhecimento != null && !conhecimento.isBlank();
  }
}
