package com.controlefinanceiro.helpdesk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketRequest(
    @NotBlank @Size(max = 120) String assunto, @NotBlank @Size(max = 5000) String descricao) {}
