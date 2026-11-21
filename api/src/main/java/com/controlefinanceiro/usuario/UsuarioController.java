package com.controlefinanceiro.usuario;

import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioAuth;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

  private final UsuarioService usuarioService;
  private final ConviteService conviteService;

  public UsuarioController(UsuarioService usuarioService, ConviteService conviteService) {
    this.usuarioService = usuarioService;
    this.conviteService = conviteService;
  }

  @GetMapping
  public List<UsuarioContaResponse> listar() {
    return usuarioService.listar(TenantContext.getEmpresaId());
  }

  @PostMapping("/convites")
  public ResponseEntity<ConviteResponse> convidar(@Valid @RequestBody ConviteRequest request) {
    return ResponseEntity.ok(
        conviteService.criar(
            TenantContext.getEmpresaId(), request.email(), request.role(), emailAutenticado()));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> alterarRole(
      @PathVariable Long id, @Valid @RequestBody AlterarRoleRequest request) {
    usuarioService.alterarRole(TenantContext.getEmpresaId(), id, request.role());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/desativar")
  public ResponseEntity<Void> desativar(@PathVariable Long id) {
    usuarioService.desativar(TenantContext.getEmpresaId(), id);
    return ResponseEntity.noContent().build();
  }

  private String emailAutenticado() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAuth usuario) {
      return usuario.subject();
    }
    throw new NaoAutorizadoException();
  }
}
