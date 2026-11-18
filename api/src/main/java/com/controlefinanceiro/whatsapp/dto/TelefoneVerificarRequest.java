package com.controlefinanceiro.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Requête para verificar o código de 6 dígitos recebido por WhatsApp. */
public record TelefoneVerificarRequest(
    @NotBlank(message = "Número de telefone é obrigatório") String numeroE164,
    @NotBlank(message = "Código é obrigatório")
        @Size(min = 6, max = 6, message = "Código deve ter 6 dígitos")
        String codigo) {}
