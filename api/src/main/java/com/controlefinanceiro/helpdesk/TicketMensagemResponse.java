package com.controlefinanceiro.helpdesk;

import java.time.OffsetDateTime;

public record TicketMensagemResponse(
    Long id,
    Long ticketId,
    Long usuarioId,
    OrigemMensagem origem,
    String conteudo,
    OffsetDateTime criadoEm) {}
