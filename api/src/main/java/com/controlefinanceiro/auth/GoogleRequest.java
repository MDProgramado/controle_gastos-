package com.controlefinanceiro.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleRequest(
    @NotBlank String idToken, @NotBlank String intencao, String conviteToken) {}
