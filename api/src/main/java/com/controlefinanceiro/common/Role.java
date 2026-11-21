package com.controlefinanceiro.common;

public enum Role {
  ADMIN,
  USER;

  public static Role from(String valor) {
    if (valor == null) {
      throw new RoleInvalidoException("Role inválida: null");
    }
    try {
      return valueOf(valor);
    } catch (IllegalArgumentException ex) {
      throw new RoleInvalidoException("Role inválida: " + valor);
    }
  }

  public String valor() {
    return name();
  }
}
