package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints autenticados para cadastro e verificação de números de WhatsApp.
 *
 * <p>POST /api/whatsapp/telefone — cadastra um número e envia código de verificação via Meta. POST
 * /api/whatsapp/verificar — valida o código de 6 dígitos e marca o número como verificado.
 *
 * <p>O tenant (empresa_id) vem exclusivamente do token JWT via {@link TenantContext}, nunca do
 * corpo da requisição.
 */
@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappCadastroController {

  private final WhatsappCadastroService cadastroService;
  private final UsuarioRepository usuarioRepository;

  public WhatsappCadastroController(
      WhatsappCadastroService cadastroService, UsuarioRepository usuarioRepository) {
    this.cadastroService = cadastroService;
    this.usuarioRepository = usuarioRepository;
  }

  /**
   * Cadastra um número de telefone para o usuário autenticado. Gera código de 6 dígitos, válido por
   * 10 minutos, e envia via Meta (template aprovado).
   */
  @PostMapping("/telefone")
  public ResponseEntity<WhatsappTelefoneResponse> cadastrarTelefone(
      @Valid @RequestBody WhatsappTelefoneRequest request) {
    Long empresaId = TenantContext.getEmpresaId();
    Usuario usuario = usuarioAtual();

    if (!usuario.getEmpresa().getId().equals(empresaId)) {
      throw new NaoAutorizadoException("Usuário não pertence à empresa do token");
    }

    cadastroService.cadastrarNumero(usuario.getId(), empresaId, request.numeroE164());

    UsuarioTelefone telefone = cadastroService.buscarPorNumero(request.numeroE164());
    return ResponseEntity.ok(
        new WhatsappTelefoneResponse(
            telefone.getId(),
            telefone.getNumeroE164(),
            telefone.isVerificado(),
            telefone.isAtivo()));
  }

  /** Verifica o código de 6 dígitos informado pelo usuário e marca o número como verificado. */
  @PostMapping("/verificar")
  public ResponseEntity<WhatsappTelefoneResponse> verificarCodigo(
      @Valid @RequestBody WhatsappVerificarRequest request) {
    Long empresaId = TenantContext.getEmpresaId();
    Usuario usuario = usuarioAtual();

    if (!usuario.getEmpresa().getId().equals(empresaId)) {
      throw new NaoAutorizadoException("Usuário não pertence à empresa do token");
    }

    boolean verificado =
        cadastroService.verificarCodigo(usuario.getId(), request.numeroE164(), request.codigo());

    UsuarioTelefone telefone = cadastroService.buscarPorNumero(request.numeroE164());
    return ResponseEntity.ok(
        new WhatsappTelefoneResponse(
            telefone.getId(),
            telefone.getNumeroE164(),
            telefone.isVerificado(),
            telefone.isAtivo()));
  }

  /** Retorna o usuário autenticado a partir do principal no SecurityContext. */
  private Usuario usuarioAtual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UsuarioAuth usuarioAuth) {
      return usuarioRepository
          .findByEmail(usuarioAuth.subject())
          .orElseThrow(() -> new NaoAutorizadoException("Usuário não encontrado"));
    }
    throw new NaoAutorizadoException();
  }
}
