package com.controlefinanceiro.helpdesk;

import java.util.List;

public record TicketPage(
    List<TicketResponse> tickets, int pagina, int tamanho, long totalElementos) {}
