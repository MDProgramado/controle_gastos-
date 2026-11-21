package com.controlefinanceiro.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtServiceTest {

  private static final String SEGREDO = "fixture-jwt-somente-testes-it-0123456789abcdef";

  private final JwtService service = new JwtService(SEGREDO);

  @Test
  void tokenContemUsuarioIdETokenVersion() {
    Empresa empresa = new Empresa();
    empresa.setId(42L);
    Usuario usuario = new Usuario();
    usuario.setId(7L);
    usuario.setEmail("user@teste.com");
    usuario.setEmpresa(empresa);
    usuario.setRole("USER");
    usuario.setTokenVersion(3);

    Jwt jwt = service.jwtDecoder().decode(service.gerarToken(usuario));

    assertThat(jwt.getSubject()).isEqualTo("user@teste.com");
    assertThat((Object) jwt.getClaim("empresa_id")).isEqualTo(42L);
    assertThat((Object) jwt.getClaim("usuario_id")).isEqualTo(7L);
    assertThat((Object) jwt.getClaim("role")).isEqualTo("USER");
    assertThat((Object) jwt.getClaim("token_version")).isEqualTo(3L);
  }
}
