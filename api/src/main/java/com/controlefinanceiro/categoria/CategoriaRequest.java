package com.controlefinanceiro.categoria;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criar/editar categoria (DTO de entrada, padrão P4).
 *
 * <p><strong>R3 — gate estrutural:</strong> NUNCA campo empresa_id — o tenant vem exclusivamente do
 * token JWT via {@link TenantContext#getEmpresaId()}.
 */
public record CategoriaRequest(
    @NotBlank @Size(max = 80) String nome, @NotNull @Size(max = 10) String tipo, boolean pessoal) {}
