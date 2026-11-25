package com.controlefinanceiro.usuario;

import com.controlefinanceiro.common.AcessoNegadoException;
import com.controlefinanceiro.common.Role;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UltimoAdminAtivoException;
import com.controlefinanceiro.common.UsuarioContext;
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
    return usuarioRepository.findByEmpresaId(empresaId).stream().map(this::paraResponse).toList();
  }

  @Transactional
  public UsuarioContaResponse atualizar(Long id, AtualizarUsuarioRequest request) {
    Long empresaId = TenantContext.getEmpresaId();
    Role papel = Role.from(request.role());
    Usuario usuario = buscar(empresaId, id);
    validarAlteracaoDeRole(empresaId, id, usuario, papel);

    usuario.setNome(request.nome());
    usuario.setRole(papel.valor());
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    usuarioRepository.save(usuario);
    return paraResponse(usuario);
  }

  @Transactional
  public void alterarRole(Long empresaId, Long id, String role) {
    Role papel = Role.from(role);
    Usuario usuario = buscar(empresaId, id);
    validarAlteracaoDeRole(empresaId, id, usuario, papel);

    usuario.setRole(papel.valor());
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    usuarioRepository.save(usuario);
  }

  @Transactional
  public void desativar(Long empresaId, Long id) {
    Usuario usuario = buscar(empresaId, id);

    if (ePropriaConta(id)) {
      throw new AcessoNegadoException("Não é possível desativar sua própria conta.");
    }
    if ("ADMIN".equals(usuario.getRole()) && usuario.isAtivo() && ultimoAdminAtivo(empresaId)) {
      throw new UltimoAdminAtivoException(
          "A empresa precisa de pelo menos um ADMIN ativo. Defina outro administrador antes.");
    }

    usuario.setAtivo(false);
    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
    usuarioRepository.save(usuario);
  }

  @Transactional
  public void excluir(Long id) {
    desativar(TenantContext.getEmpresaId(), id);
  }

  private void validarAlteracaoDeRole(Long empresaId, Long id, Usuario usuario, Role papel) {
    if (ePropriaConta(id) && "ADMIN".equals(usuario.getRole()) && !"ADMIN".equals(papel.valor())) {
      throw new AcessoNegadoException("Não é possível remover seu próprio perfil de ADMIN.");
    }
    if ("ADMIN".equals(usuario.getRole())
        && !"ADMIN".equals(papel.valor())
        && ultimoAdminAtivo(empresaId)) {
      throw new UltimoAdminAtivoException(
          "A empresa precisa de pelo menos um ADMIN ativo. Defina outro administrador antes.");
    }
  }

  private UsuarioContaResponse paraResponse(Usuario usuario) {
    return new UsuarioContaResponse(
        usuario.getId(),
        usuario.getNome(),
        usuario.getEmail(),
        usuario.getRole(),
        usuario.isAtivo());
  }

  private boolean ePropriaConta(Long id) {
    return id != null && id.equals(UsuarioContext.getUsuarioId());
  }

  private boolean ultimoAdminAtivo(Long empresaId) {
    List<Usuario> usuarios = usuarioRepository.findByEmpresaId(empresaId);
    if (usuarios == null) {
      return true;
    }
    return usuarios.stream()
            .filter(usuario -> usuario.isAtivo() && "ADMIN".equals(usuario.getRole()))
            .count()
        <= 1;
  }

  private Usuario buscar(Long empresaId, Long id) {
    return usuarioRepository
        .findByIdAndEmpresaId(id, empresaId)
        .orElseThrow(UsuarioNaoEncontradoException::new);
  }
}
