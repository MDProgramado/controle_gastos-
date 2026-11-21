package com.controlefinanceiro.ai;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Repositório de conhecimento da IA, escopado por empresa (F-002). O texto definido por uma empresa
 * nunca é injetado no prompt de outra.
 */
@Component
public class AiKnowledgeStore {

  private final Map<String, String> conhecimentos = new ConcurrentHashMap<>();

  public String obterConhecimento(String empresaId) {
    return conhecimentos.getOrDefault(Objects.requireNonNull(empresaId, "empresaId"), "");
  }

  public void definirConhecimento(String empresaId, String texto) {
    conhecimentos.put(Objects.requireNonNull(empresaId, "empresaId"), texto == null ? "" : texto);
  }

  public boolean temConhecimento(String empresaId) {
    return !obterConhecimento(empresaId).isBlank();
  }
}
