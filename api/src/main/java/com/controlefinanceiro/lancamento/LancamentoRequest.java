package com.controlefinanceiro.lancamento;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para criar/editar lançamento (DTO de entrada, padrão P4).
 *
 * <p><strong>R3 — gate estrutural:</strong> NUNCA campo empresa_id — o tenant vem exclusivamente do
 * token JWT via {@link com.controlefinanceiro.common.TenantContext#getEmpresaId()}.
 *
 * <p>LAN-04: {@code @Digits(integer=12, fraction=2)} rejeita precoce valores com mais de 2 casas
 * decimais; o serviço faz defesa em profundidade com {@code scale() > 2}.
 */
public record LancamentoRequest(
    @NotNull Long categoriaId,
    @NotNull String tipo,
    @NotNull @Digits(integer = 12, fraction = 2) BigDecimal valor,
    @NotNull LocalDate data,
    String descricao) {}
