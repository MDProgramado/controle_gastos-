package com.controlefinanceiro.dashboard.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de resposta do dashboard com totais, comparação e breakdown. R3 gate: SEM campo empresa_id —
 * o tenant vem exclusivamente do token JWT. Campos BigDecimal serializados via ToStringSerializer
 * (defesa em profundidade além do write-bigdecimal-as-plain no application.yml). Campos percentuais
 * são String para suportar "—" quando a base do mês anterior é zero.
 */
public record DashboardResponse(
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal totalEntradas,
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal totalSaidas,
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal saldo,
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal variacaoEntradas,
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal variacaoSaidas,
    @JsonSerialize(using = ToStringSerializer.class) BigDecimal variacaoSaldo,
    String percentualEntradas,
    String percentualSaidas,
    String percentualSaldo,
    List<CategoryExpense> categorias) {}
