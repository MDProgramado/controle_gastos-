package com.controlefinanceiro.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uma sessão de conversa com a IA. Armazena o histórico de mensagens para que a IA possa se
 * retroalimentar (contexto de conversa).
 */
public class AiSession {

  private final String sessionId;
  private final String empresaId;
  private final List<AiMessage> mensagens = new ArrayList<>();
  private Instant ultimaAtualizacao = Instant.now();

  public AiSession(String sessionId, String empresaId) {
    this.sessionId = sessionId;
    this.empresaId = empresaId;
  }

  public String sessionId() {
    return sessionId;
  }

  public String empresaId() {
    return empresaId;
  }

  public List<AiMessage> mensagens() {
    return Collections.unmodifiableList(mensagens);
  }

  public Instant ultimaAtualizacao() {
    return ultimaAtualizacao;
  }

  public void adicionar(AiMessage mensagem) {
    mensagens.add(mensagem);
    ultimaAtualizacao = Instant.now();
  }

  public void limpar() {
    mensagens.clear();
    ultimaAtualizacao = Instant.now();
  }
}
