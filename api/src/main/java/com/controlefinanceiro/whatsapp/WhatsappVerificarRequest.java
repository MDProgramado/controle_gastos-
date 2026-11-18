package com.controlefinanceiro.whatsapp;

import jakarta.validation.constraints.NotBlank;

/** Requête para verificação do código de 6 dígitos. */
public record WhatsappVerificarRequest(
    @NotBlank(message = "Número de telefone é obrigatório") String numeroE164,
    @NotBlank(message = "Código é obrigatório") String codigo) {}
