package com.controlefinanceiro.importacao;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Body do POST /api/importacoes/{id}/confirmar (fase 5, 05-05).
 *
 * <p>O usuário confirma quais itens de staging serão gravados: itens ausentes da lista ou com
 * {@code incluir=false} são ignorados (status IGNORADO); {@code categoriaIdOverride} substitui a
 * categoria mapeada pela IA. Nenhum campo de empresa — o tenant vem do token (R3).
 */
public record ConfirmarRequest(@NotNull List<ConfirmarItem> itens) {

  /**
   * Confirmação de um item de staging.
   *
   * @param itemId id do item de staging ({@code importacao_item.id})
   * @param incluir true para gravar; false ou ausente = ignorar o item
   * @param categoriaIdOverride categoria escolhida pelo usuário na revisão (nula = manter a
   *     mapeada)
   */
  public record ConfirmarItem(@NotNull Long itemId, Boolean incluir, Long categoriaIdOverride) {}
}
