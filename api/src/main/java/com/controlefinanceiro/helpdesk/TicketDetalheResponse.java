package com.controlefinanceiro.helpdesk;

import java.time.OffsetDateTime;
import java.util.List;

public record TicketDetalheResponse(
    Long id,
    Long usuarioId,
    String assunto,
    String descricao,
    TicketStatus status,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm,
    List<TicketMensagemResponse> mensagens) {}
