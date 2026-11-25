package com.controlefinanceiro.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(@NotBlank @Size(max = 120) String nome, String role) {}
