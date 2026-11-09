package com.controlefinanceiro.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de entrada para lote de lançamentos (LOT-01, LOT-02), no padrão de {@link
 * com.controlefinanceiro.auth.LoginRequest}.
 *
 * <p><strong>R3 — gate estrutural:</strong> NUNCA campo empresa_id (nem no lote nem nas linhas) — o
 * tenant vem exclusivamente do token JWT via {@link
 * com.controlefinanceiro.common.TenantContext#getEmpresaId()}.
 *
 * <p>Lan-05: valores em {@link BigDecimal}; a validação de escala (máx. 2 casas) e positividade é
 * feita por linha no service, o que permite reportar o índice exato da linha inválida.
 */
public record LancamentoLoteRequest(List<LancamentoLinha> linhas) {

  /** Uma linha do lote — mesmos campos de {@link LancamentoRequest}, sem tenant. */
  public record LancamentoLinha(
      Long categoriaId, String tipo, BigDecimal valor, LocalDate data, String descricao) {

    /** Converte a linha do lote para o DTO unitário reutilizado pelo service. */
    public LancamentoRequest toRequest() {
      return new LancamentoRequest(categoriaId, tipo, valor, data, descricao);
    }
  }
}
