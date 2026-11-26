package com.controlefinanceiro.ai;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Conhecimento da IA por empresa. Persiste em Postgres quando repositório está disponível. */
@Component
public class AiKnowledgeStore {

  private final AiConhecimentoRepository repository;
  private final Map<String, String> fallback = new ConcurrentHashMap<>();

  @Autowired
  public AiKnowledgeStore(AiConhecimentoRepository repository) {
    this.repository = repository;
  }

  /** Construtor para testes unitários sem JPA. */
  public AiKnowledgeStore() {
    this.repository = null;
  }

  @Transactional(readOnly = true)
  public String obterConhecimento(String empresaId) {
    String id = Objects.requireNonNull(empresaId, "empresaId");
    if (repository == null) {
      return fallback.getOrDefault(id, "");
    }
    return repository.findByEmpresaId(Long.valueOf(id)).map(AiConhecimento::getTexto).orElse("");
  }

  @Transactional
  public void definirConhecimento(String empresaId, String texto) {
    String id = Objects.requireNonNull(empresaId, "empresaId");
    String valor = texto == null ? "" : texto;
    if (repository == null) {
      fallback.put(id, valor);
      return;
    }
    Long empresa = Long.valueOf(id);
    AiConhecimento entidade =
        repository.findByEmpresaId(empresa).orElseGet(() -> new AiConhecimento(empresa, ""));
    entidade.setTexto(valor);
    repository.save(entidade);
  }

  public boolean temConhecimento(String empresaId) {
    return !obterConhecimento(empresaId).isBlank();
  }
}
