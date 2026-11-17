package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.common.AcessoNegadoException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import com.controlefinanceiro.whatsapp.dto.TelefoneResponse;
import com.controlefinanceiro.whatsapp.dto.TelefoneVerificarRequest;
import com.controlefinanceiro.whatsapp.dto.TelefoneVincularRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de vínculo e verificação de números de WhatsApp.
 *
 * <p>POST /api/usuarios/{id}/telefone — ADMIN vincula/troca telefone de um usuário da mesma
 * empresa. Exige role ADMIN (verificado no SecurityConfig e no service).
 *
 * <p>POST /api/telefones/verificar — o próprio usuário confirma o código de 6 dígitos do seu
 * número.
 *
 * <p>O tenant (empresa_id) vem exclusivamente do token JWT via {@link TenantContext}, nunca do
 * corpo da requisição.
 */
@RestController
@RequestMapping("/api")
public class UsuarioTelefoneController {

  private final UsuarioTelefoneService telefoneService;
  private final UsuarioRepository usuarioRepository;

  public UsuarioTelefoneController(
      UsuarioTelefoneService telefoneService, UsuarioRepository usuarioRepository) {
    this.telefoneService = telefoneService;
    this.usuarioRepository = usuarioRepository;
  }

  /**
   * Vincula (ou troca) um número de WhatsApp a um usuário.
   *
   * <p>Exige role ADMIN. O usuário alvo deve pertencer à mesma empresa do ADMIN.
   */
  @PostMapping("/usuarios/{usuarioId}/telefone")
  public ResponseEntity<TelefoneResponse> vincularTelefone(
      @PathVariable Long usuarioId, @Valid @RequestBody TelefoneVincularRequest request) {

    Long empresaId = TenantContext.getEmpresaId();
    Usuario admin = usuarioAutenticado();

    if (!"ADMIN".equals(admin.getRole())) {
      throw new AcessoNegadoException("Apenas administradores podem vincular telefones");
    }
    if (!admin.getEmpresa().getId().equals(empresaId)) {
      throw new AcessoNegadoException("Administrador não pertence a esta empresa");
    }

    TelefoneResponse response =
        telefoneService.vincularTelefone(admin.getId(), empresaId, usuarioId, request.numeroE164());
    return ResponseEntity.ok(response);
  }

  /**
   * Verifica o código de 6 dígitos do número de WhatsApp.
   *
   * <p>Somente o próprio usuário dono do número pode confirmar.
   */
  @PostMapping("/telefones/verificar")
  public ResponseEntity<TelefoneResponse> verificarCodigo(
      @Valid @RequestBody TelefoneVerificarRequest request) {

    Long empresaId = TenantContext.getEmpresaId();
    Usuario usuario = usuarioAutenticado();

    if (!usuario.getEmpresa().getId().equals(empresaId)) {
      throw new AcessoNegadoException("Usuário não pertence a esta empresa");
    }

    TelefoneResponse response =
        telefoneService.verificarCodigo(usuario.getId(), request.numeroE164(), request.codigo());
    return ResponseEntity.ok(response);
  }

  /** Retorna o usuário autenticado a partir do principal no SecurityContext. */
  private Usuario usuarioAutenticado() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UsuarioAuth usuarioAuth) {
      return usuarioRepository
          .findByEmail(usuarioAuth.subject())
          .orElseThrow(() -> new NaoAutorizadoException("Usuário não encontrado"));
    }
    throw new NaoAutorizadoException();
  }
}
