package com.controlefinanceiro.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
    @NotBlank String empresaNome,
    @NotBlank String nome,
    @Email @NotBlank String email,
    @NotBlank @Size(max = 72) String senha) {}
