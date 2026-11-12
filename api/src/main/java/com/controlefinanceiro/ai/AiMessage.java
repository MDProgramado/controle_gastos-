package com.controlefinanceiro.ai;

/** Uma mensagem de conversa (usuário ou assistente) armazenada na sessão. */
public record AiMessage(String role, String content) {}
