package com.controlefinanceiro.whatsapp;

/** Resposta de cadastro/verificação de WhatsApp. */
public record WhatsappTelefoneResponse(
    Long id, String numeroE164, boolean verificado, boolean ativo) {}
