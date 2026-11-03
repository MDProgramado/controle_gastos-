package com.controlefinanceiro.common;

/**
 * Principal populado pelo {@code jwtAuthenticationConverter} a partir dos claims do JWT (key-link
 * do plano: claim {@code empresa_id} → principal → SecurityContext). É o que {@link TenantContext}
 * lê — nunca um body/header/query (R3/AUTH-03).
 *
 * @param subject e-mail do usuário (claim {@code sub})
 * @param empresaId tenant do token (claim {@code empresa_id})
 * @param role papel no token (claim {@code role}: ADMIN | USER)
 */
public record UsuarioAuth(String subject, Long empresaId, String role) {}
