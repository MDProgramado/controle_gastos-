package com.controlefinanceiro.lancamento;

import java.util.List;

/**
 * Resposta paginada de listagem de lançamentos.
 *
 * <p>Encapsula a lista de lançamentos (até 50 itens por página) e o total real de lançamentos no
 * banco para o mês/empresa informados, obtido via {@code Page#getTotalElements()}.
 */
public record LancamentoPage(List<LancamentoResponse> lancamentos, long total) {}
