package com.controlefinanceiro.dashboard.dto;

import java.math.BigDecimal;

/**
 * Item do breakdown por categoria de SAÍDA. R3 gate: SEM campo empresa_id — o tenant vem
 * exclusivamente do token JWT.
 */
public record CategoryExpense(Long id, String nome, BigDecimal total) {}
