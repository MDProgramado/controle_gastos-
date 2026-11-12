package com.controlefinanceiro.ai;

import java.util.List;

/** Requente de chat com a IA (fase 4 — OpenRouter + Gemma 4). */
public record AiRequest(String mensagem, List<AiMessage> historico, String sessionId) {}
