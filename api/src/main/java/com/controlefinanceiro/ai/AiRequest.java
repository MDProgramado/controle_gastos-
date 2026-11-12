package com.controlefinanceiro.ai;

import java.util.List;

/** Requente de chat com a IA (fase 4 — OpenRouter + Gemini). */
public record AiRequest(String mensagem, List<AiMessage> historico, String sessionId) {}
