package com.controlefinanceiro.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ConviteRequest(@Email @NotBlank String email, @NotBlank String role) {}
