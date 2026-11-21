package com.controlefinanceiro.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.RoleInvalidoException;
import com.controlefinanceiro.common.UsuarioNaoEncontradoException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UsuarioServiceTest {

  private UsuarioRepository usuarioRepository;
  private UsuarioService service;

  @BeforeEach
  void setup() {
    usuarioRepository = mock(UsuarioRepository.class);
    service = new UsuarioService(usuarioRepository);
  }

  @Test
  void alterarRoleAtualizaRoleEIncrementaTokenVersion() {
    Usuario usuario = usuario("USER", 0);
    when(usuarioRepository.findByIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(usuario));

    service.alterarRole(10L, 1L, "ADMIN");

    assertThat(usuario.getRole()).isEqualTo("ADMIN");
    assertThat(usuario.getTokenVersion()).isEqualTo(1);
    verify(usuarioRepository).save(usuario);
  }

  @Test
  void desativarInvalidaTokenAtual() {
    Usuario usuario = usuario("USER", 3);
    when(usuarioRepository.findByIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(usuario));

    service.desativar(10L, 1L);

    assertThat(usuario.isAtivo()).isFalse();
    assertThat(usuario.getTokenVersion()).isEqualTo(4);
    verify(usuarioRepository).save(usuario);
  }

  @Test
  void alterarRoleRejeitaRoleForaDoEnum() {
    assertThatThrownBy(() -> service.alterarRole(10L, 1L, "ROOT"))
        .isInstanceOf(RoleInvalidoException.class);
    verify(usuarioRepository, never()).findByIdAndEmpresaId(1L, 10L);
  }

  @Test
  void usuarioDeOutraEmpresaNaoEAlterado() {
    when(usuarioRepository.findByIdAndEmpresaId(1L, 99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.desativar(99L, 1L))
        .isInstanceOf(UsuarioNaoEncontradoException.class);
    verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private Usuario usuario(String role, int tokenVersion) {
    Usuario usuario = new Usuario();
    usuario.setId(1L);
    usuario.setRole(role);
    usuario.setTokenVersion(tokenVersion);
    usuario.setAtivo(true);
    return usuario;
  }
}
