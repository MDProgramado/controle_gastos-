package com.controlefinanceiro.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleRequest(
    @NotBlank @Size(max = 4096) String idToken, @NotBlank String intencao, String conviteToken) {}
