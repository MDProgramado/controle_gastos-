package com.controlefinanceiro.helpdesk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketMensagemRequest(@NotBlank @Size(max = 5000) String conteudo) {}
