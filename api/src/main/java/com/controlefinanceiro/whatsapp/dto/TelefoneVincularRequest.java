package com.controlefinanceiro.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Requête para vincular um número de WhatsApp a um usuário. */
public record TelefoneVincularRequest(
    @NotBlank(message = "Número de telefone é obrigatório")
        @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Número deve estar no formato E.164 (ex.: +5571999999999)")
        String numeroE164) {}
