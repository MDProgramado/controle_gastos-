package com.controlefinanceiro.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceitarConviteRequest(
    @NotBlank String token, String nome, @Size(max = 72) String senha) {}
