package com.controlefinanceiro.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.AcessoNegadoException;
import com.controlefinanceiro.common.RoleInvalidoException;
import com.controlefinanceiro.common.UltimoAdminAtivoException;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import com.controlefinanceiro.common.UsuarioNaoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class UsuarioServiceTest {

  private UsuarioRepository usuarioRepository;
  private UsuarioService service;

  @BeforeEach
  void setup() {
    usuarioRepository = mock(UsuarioRepository.class);
    service = new UsuarioService(usuarioRepository);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("gestor@teste.com", 10L, 2L, 0, "USER"), List.of()));
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
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

  @Test
  @DisplayName("usuário não pode remover próprio ADMIN nem se desativar")
  void naoPodeAlterarPropriaConta() {
    Usuario alvo = usuario(2L, "ADMIN", 0);
    when(usuarioRepository.findByIdAndEmpresaId(2L, 10L)).thenReturn(Optional.of(alvo));

    assertThatThrownBy(() -> service.alterarRole(10L, 2L, "USER"))
        .isInstanceOf(AcessoNegadoException.class)
        .hasMessageContaining("próprio");
    assertThatThrownBy(() -> service.desativar(10L, 2L))
        .isInstanceOf(AcessoNegadoException.class)
        .hasMessageContaining("própria");

    assertThat(alvo.getRole()).isEqualTo("ADMIN");
    assertThat(alvo.isAtivo()).isTrue();
    verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("último ADMIN ativo não pode ser rebaixado nem desativado")
  void ultimoAdminAtivoProtegido() {
    Usuario unicoAdmin = usuario(1L, "ADMIN", 0);
    when(usuarioRepository.findByIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(unicoAdmin));
    when(usuarioRepository.findByEmpresaId(10L)).thenReturn(List.of(unicoAdmin));

    assertThatThrownBy(() -> service.alterarRole(10L, 1L, "USER"))
        .isInstanceOf(UltimoAdminAtivoException.class);
    assertThatThrownBy(() -> service.desativar(10L, 1L))
        .isInstanceOf(UltimoAdminAtivoException.class);

    assertThat(unicoAdmin.getRole()).isEqualTo("ADMIN");
    assertThat(unicoAdmin.isAtivo()).isTrue();
    verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void atualizarAtualizaNomeERoleERetornaConta() {
    Usuario usuario = usuario(2L, "USER", 1);
    usuario.setNome("Nome Antigo");
    usuario.setEmail("alvo@teste.com");
    when(usuarioRepository.findByIdAndEmpresaId(2L, 10L)).thenReturn(Optional.of(usuario));

    UsuarioContaResponse resposta =
        service.atualizar(2L, new AtualizarUsuarioRequest("Nome Novo", "ADMIN"));

    assertThat(resposta)
        .isEqualTo(new UsuarioContaResponse(2L, "Nome Novo", "alvo@teste.com", "ADMIN", true));
    assertThat(usuario.getNome()).isEqualTo("Nome Novo");
    assertThat(usuario.getRole()).isEqualTo("ADMIN");
    assertThat(usuario.getTokenVersion()).isEqualTo(2);
    verify(usuarioRepository).save(usuario);
  }

  @Test
  void atualizarRejeitaRoleForaDoEnum() {
    assertThatThrownBy(() -> service.atualizar(2L, new AtualizarUsuarioRequest("Nome", "ROOT")))
        .isInstanceOf(RoleInvalidoException.class);
    verify(usuarioRepository, never()).findByIdAndEmpresaId(2L, 10L);
  }

  @Test
  void atualizarNaoPermiteRemoverProprioAdmin() {
    Usuario alvo = usuario(2L, "ADMIN", 0);
    alvo.setNome("Gestor");
    when(usuarioRepository.findByIdAndEmpresaId(2L, 10L)).thenReturn(Optional.of(alvo));

    assertThatThrownBy(() -> service.atualizar(2L, new AtualizarUsuarioRequest("Gestor", "USER")))
        .isInstanceOf(AcessoNegadoException.class)
        .hasMessageContaining("próprio");

    assertThat(alvo.getNome()).isEqualTo("Gestor");
    assertThat(alvo.getRole()).isEqualTo("ADMIN");
    verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void atualizarNaoRebaixaUltimoAdminAtivo() {
    Usuario unicoAdmin = usuario(1L, "ADMIN", 0);
    when(usuarioRepository.findByIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(unicoAdmin));
    when(usuarioRepository.findByEmpresaId(10L)).thenReturn(List.of(unicoAdmin));

    assertThatThrownBy(() -> service.atualizar(1L, new AtualizarUsuarioRequest("Admin", "USER")))
        .isInstanceOf(UltimoAdminAtivoException.class);

    assertThat(unicoAdmin.getRole()).isEqualTo("ADMIN");
    verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void excluirMantemRegistroDesativaEIncrementaTokenVersion() {
    Usuario usuario = usuario(3L, "USER", 3);
    when(usuarioRepository.findByIdAndEmpresaId(3L, 10L)).thenReturn(Optional.of(usuario));

    service.excluir(3L);

    assertThat(usuario.isAtivo()).isFalse();
    assertThat(usuario.getTokenVersion()).isEqualTo(4);
    verify(usuarioRepository).save(usuario);
    verify(usuarioRepository, never()).deleteById(org.mockito.ArgumentMatchers.any());
    verify(usuarioRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void excluirNaoPermiteDesativarPropriaConta() {
    Usuario alvo = usuario(2L, "USER", 0);
    when(usuarioRepository.findByIdAndEmpresaId(2L, 10L)).thenReturn(Optional.of(alvo));

    assertThatThrownBy(() -> service.excluir(2L))
        .isInstanceOf(AcessoNegadoException.class)
        .hasMessageContaining("própria");

    assertThat(alvo.isAtivo()).isTrue();
    verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void excluirNaoDesativaUltimoAdminAtivo() {
    Usuario unicoAdmin = usuario(1L, "ADMIN", 0);
    when(usuarioRepository.findByIdAndEmpresaId(1L, 10L)).thenReturn(Optional.of(unicoAdmin));
    when(usuarioRepository.findByEmpresaId(10L)).thenReturn(List.of(unicoAdmin));

    assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(UltimoAdminAtivoException.class);

    assertThat(unicoAdmin.isAtivo()).isTrue();
    verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  private Usuario usuario(String role, int tokenVersion) {
    return usuario(1L, role, tokenVersion);
  }

  private Usuario usuario(Long id, String role, int tokenVersion) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setRole(role);
    usuario.setTokenVersion(tokenVersion);
    usuario.setAtivo(true);
    return usuario;
  }
}
