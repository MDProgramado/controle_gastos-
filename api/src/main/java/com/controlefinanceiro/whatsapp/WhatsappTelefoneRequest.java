package com.controlefinanceiro.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Requête para cadastro de número de WhatsApp. */
public record WhatsappTelefoneRequest(
    @NotBlank(message = "Número de telefone é obrigatório")
        @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Número deve estar no formato E.164 (ex.: +5571999999999)")
        String numeroE164) {}
