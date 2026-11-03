package com.controlefinanceiro.auth;

/** Resposta do login: o JWT de 8h (D-01). O BFF do web grava em cookie httpOnly (D-02). */
public record LoginResponse(String token) {}
