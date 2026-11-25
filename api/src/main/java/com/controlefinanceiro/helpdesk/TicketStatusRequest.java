package com.controlefinanceiro.helpdesk;

import jakarta.validation.constraints.NotBlank;

public record TicketStatusRequest(@NotBlank String status) {}
