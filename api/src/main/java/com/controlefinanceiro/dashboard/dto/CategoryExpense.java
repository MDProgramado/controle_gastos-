package com.controlefinanceiro.dashboard.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;

/**
 * Item do breakdown por categoria de SAÍDA. R3 gate: SEM campo empresa_id — o tenant vem
 * exclusivamente do token JWT.
 */
public record CategoryExpense(
    Long id, String nome, @JsonSerialize(using = ToStringSerializer.class) BigDecimal total) {}
