package com.controlefinanceiro.common;

/**
 * Principal populado pelo {@code jwtAuthenticationConverter} a partir dos claims do JWT (key-link
 * do plano: claim {@code empresa_id} → principal → SecurityContext → {@link TenantContext}). É o
 * que {@link TenantContext} e {@link UsuarioContext} leem — nunca um body/header/query
 * (R3/AUTH-03).
 *
 * @param subject e-mail do usuário (claim {@code sub})
 * @param empresaId tenant do token (claim {@code empresa_id})
 * @param usuarioId usuário do token (claim {@code usuario_id})
 * @param tokenVersion versão do token (claim {@code token_version}) — validada a cada request
 * @param role papel no token (claim {@code role}: ADMIN | USER)
 */
public record UsuarioAuth(
    String subject, Long empresaId, Long usuarioId, int tokenVersion, String role) {}
