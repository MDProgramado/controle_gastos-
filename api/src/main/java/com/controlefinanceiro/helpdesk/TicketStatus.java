package com.controlefinanceiro.helpdesk;

import com.controlefinanceiro.common.TicketStatusInvalidoException;

public enum TicketStatus {
  ABERTO,
  EM_ANDAMENTO,
  RESOLVIDO,
  FECHADO;

  public static TicketStatus from(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new TicketStatusInvalidoException("Status de ticket inválido");
    }
    try {
      return TicketStatus.valueOf(valor.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new TicketStatusInvalidoException("Status de ticket inválido: " + valor);
    }
  }
}
