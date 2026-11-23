package com.controlefinanceiro.usuario;

public record UsuarioContaResponse(
    Long id, String nome, String email, String role, boolean ativo) {}
