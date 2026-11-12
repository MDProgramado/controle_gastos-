package com.controlefinanceiro.ai;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armazena sessões de conversa em memória (por empresa e sessionId). Serve como "memória de curto
 * prazo" para a IA se retroalimentar. Quando persistência em banco for necessária, basta trocar a
 * implementação aqui.
 */
public class AiMemoryStore {

  private final Map<String, AiSession> sessoes = new ConcurrentHashMap<>();

  public AiSession obterOuCriar(String sessionId, String empresaId) {
    return sessoes.computeIfAbsent(sessionId, k -> new AiSession(k, empresaId));
  }

  public AiSession obter(String sessionId) {
    return sessoes.get(sessionId);
  }

  public void adicionarMensagem(String sessionId, AiMessage mensagem) {
    AiSession sessao = sessoes.get(sessionId);
    if (sessao != null) {
      sessao.adicionar(mensagem);
    }
  }

  public List<AiMessage> obterHistorico(String sessionId) {
    AiSession sessao = sessoes.get(sessionId);
    if (sessao == null) {
      return List.of();
    }
    return sessao.mensagens();
  }

  public void limpar(String sessionId) {
    AiSession sessao = sessoes.get(sessionId);
    if (sessao != null) {
      sessao.limpar();
    }
  }

  public int totalSessoes() {
    return sessoes.size();
  }
}
