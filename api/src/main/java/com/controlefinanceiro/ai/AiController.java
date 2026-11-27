package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de chat com a IA (fase 4 — OpenRouter + Gemma 4).
 *
 * <p>O tenant (empresa_id) vem exclusivamente do token JWT via {@link
 * TenantContext#getEmpresaId()}, nunca do corpo da requisição.
 *
 * <p>O sessionId é opcional e vem no corpo da requisição; se ausente, um identificador é gerado
 * automaticamente.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

  private final AiService aiService;
  private final AiRateLimiter rateLimiter;

  public AiController(AiService aiService, AiRateLimiter rateLimiter) {
    this.aiService = aiService;
    this.rateLimiter = rateLimiter;
  }

  /**
   * POST /api/ai/chat — envia uma mensagem para a IA e obtém a resposta estruturada (consulta,
   * lançamento ou esclarecimento).
   *
   * <p>Nenhum lançamento é gravado automaticamente pela IA. Apenas propostas são retornadas; o
   * usuário confirma via frontend.
   */
  @PostMapping("/chat")
  public ResponseEntity<AiResponse> chat(@RequestBody AiRequest request) {
    Long usuarioId = UsuarioContext.getUsuarioId();
    rateLimiter.verificar(usuarioId);
    String empresaId = empresaId();
    String sessionId =
        request.sessionId() != null ? request.sessionId() : java.util.UUID.randomUUID().toString();

    AiResponse resposta =
        aiService.processar(sessionId, empresaId, usuarioId.toString(), request.mensagem());
    return ResponseEntity.ok(resposta);
  }

  /* ========================================================================
  Administração do conhecimento: restrita a ADMIN no SecurityConfig e sempre
  escopada pela empresa do JWT (F-002). Limpeza de sessão também usa o tenant.
  ======================================================================== */

  @GetMapping("/chat/conhecimento")
  public ResponseEntity<String> obterConhecimento() {
    return ResponseEntity.ok(aiService.obterConhecimento(empresaId()));
  }

  @PostMapping("/chat/conhecimento")
  public ResponseEntity<Void> definirConhecimento(@RequestBody AiConhecimentoRequest request) {
    aiService.definirConhecimento(empresaId(), request.conhecimento());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/chat/conhecimento/limpar")
  public ResponseEntity<Void> limparConhecimento() {
    aiService.definirConhecimento(empresaId(), "");
    return ResponseEntity.ok().build();
  }

  @PostMapping("/chat/sessao/{sessionId}/limpar")
  public ResponseEntity<Void> limparSessao(@PathVariable String sessionId) {
    aiService.limparSessao(sessionId, empresaId(), UsuarioContext.getUsuarioId().toString());
    return ResponseEntity.ok().build();
  }

  private String empresaId() {
    return TenantContext.getEmpresaId().toString();
  }

  public record AiConhecimentoRequest(String conhecimento) {}
}
