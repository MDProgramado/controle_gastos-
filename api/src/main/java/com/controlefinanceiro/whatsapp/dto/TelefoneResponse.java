package com.controlefinanceiro.whatsapp.dto;

/** Resposta de vinculação/verificação de WhatsApp. */
public record TelefoneResponse(
    Long id, String numeroE164, String numeroMascarado, boolean verificado, boolean ativo) {}
