package com.controlefinanceiro.usuario;

import jakarta.validation.constraints.NotBlank;

public record AlterarRoleRequest(@NotBlank String role) {}
