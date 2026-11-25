package com.controlefinanceiro.helpdesk;

import java.time.OffsetDateTime;

public record TicketResponse(
    Long id,
    Long usuarioId,
    String assunto,
    String descricao,
    TicketStatus status,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm) {}
