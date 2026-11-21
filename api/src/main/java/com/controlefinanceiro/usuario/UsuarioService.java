package com.controlefinanceiro.usuario;

import com.controlefinanceiro.common.Role;
import com.controlefinanceiro.common.UsuarioNaoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;

  public UsuarioService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public List<UsuarioContaResponse> listar(Long empresaId) {
    return usuarioRepository.findByEmpresaId(empresaId).stream()
        .map(
            usuario ->
                new UsuarioContaResponse(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    usuario.getRole(),
                    usuario.isAtivo()))
        .toList();
  }

  @Transactional
  public void alterarRole(Long empresaId, Long id, String role) {
    Role papel = Role.from(role);
    Usuario usuario = buscar(empresaId, id);
    usuario.setRole(papel.valor());
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    usuarioRepository.save(usuario);
  }

  @Transactional
  public void desativar(Long empresaId, Long id) {
    Usuario usuario = buscar(empresaId, id);
    usuario.setAtivo(false);
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    usuarioRepository.save(usuario);
  }

  private Usuario buscar(Long empresaId, Long id) {
    return usuarioRepository
        .findByIdAndEmpresaId(id, empresaId)
        .orElseThrow(UsuarioNaoEncontradoException::new);
  }
}
