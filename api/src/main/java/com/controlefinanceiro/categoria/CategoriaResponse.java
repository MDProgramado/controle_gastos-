package com.controlefinanceiro.categoria;

/**
 * Resposta de categoria (D-04/DTO de saída). Segue o padrão de {@link
 * com.controlefinanceiro.auth.LoginResponse} (record imutável). R3 gate: SEM campo empresa_id — o
 * tenant vem exclusivamente do token JWT.
 */
public record CategoriaResponse(Long id, String nome, String tipo, boolean pessoal, boolean ativo) {

  public CategoriaResponse(Categoria categoria) {
    this(
        categoria.getId(),
        categoria.getNome(),
        categoria.getTipo(),
        categoria.isPessoal(),
        categoria.isAtivo());
  }
}
