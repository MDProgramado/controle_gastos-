package com.controlefinanceiro.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO mínimo de login (D-05): apenas {@code email} e {@code senha}.
 *
 * <p><strong>R3 — gate estrutural:</strong> NUNCA campo {@code empresaId}/"empresa" — o tenant vem
 * exclusivamente do token (teste de reflexão em {@code LoginRequestEstruturaTest}).
 *
 * <p>{@code @Size(max = 72)} — BCrypt trunca silenciosamente senhas &gt; 72 bytes (Pitfall 5).
 */
public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(max = 72) String senha) {}
