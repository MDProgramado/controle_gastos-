package com.controlefinanceiro.ai;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Armazena sessões de conversa em memória. A chave inclui sempre o tenant: um sessionId fornecido
 * pelo cliente nunca pode endereçar a sessão de outra empresa (F-001).
 */
@Component
public class AiMemoryStore {

  private final Map<String, AiSession> sessoes = new ConcurrentHashMap<>();

  private static String chave(String sessionId, String empresaId) {
    return empresaId + "::" + sessionId;
  }

  public AiSession obterOuCriar(String sessionId, String empresaId) {
    Objects.requireNonNull(sessionId, "sessionId");
    Objects.requireNonNull(empresaId, "empresaId");
    return sessoes.computeIfAbsent(chave(sessionId, empresaId), k -> new AiSession(k, empresaId));
  }

  public AiSession obter(String sessionId, String empresaId) {
    if (sessionId == null || empresaId == null) {
      return null;
    }
    AiSession sessao = sessoes.get(chave(sessionId, empresaId));
    return sessao != null && empresaId.equals(sessao.empresaId()) ? sessao : null;
  }

  public void adicionarMensagem(String sessionId, String empresaId, AiMessage mensagem) {
    AiSession sessao = obter(sessionId, empresaId);
    if (sessao != null) {
      sessao.adicionar(mensagem);
    }
  }

  public List<AiMessage> obterHistorico(String sessionId, String empresaId) {
    AiSession sessao = obter(sessionId, empresaId);
    return sessao == null ? List.of() : sessao.mensagens();
  }

  public void limpar(String sessionId, String empresaId) {
    AiSession sessao = obter(sessionId, empresaId);
    if (sessao != null) {
      sessao.limpar();
    }
  }

  public int totalSessoes() {
    return sessoes.size();
  }
}
