package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de chat com a IA (fase 4 — OpenRouter + Gemini).
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

  public AiController(AiService aiService) {
    this.aiService = aiService;
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
    String empresaId = TenantContext.getEmpresaId().toString();
    String sessionId =
        request.sessionId() != null ? request.sessionId() : java.util.UUID.randomUUID().toString();

    AiResponse resposta = aiService.processar(sessionId, empresaId, request.mensagem());
    return ResponseEntity.ok(resposta);
  }

  /* ========================================================================
  APIs de administração de conhecimento e memória (úteis para testes
  e para alimentar a base de conhecimento da IA)
  ======================================================================== */

  @GetMapping("/chat/conhecimento")
  public ResponseEntity<String> obterConhecimento() {
    return ResponseEntity.ok(aiService.obterConhecimento());
  }

  @PostMapping("/chat/conhecimento")
  public ResponseEntity<Void> definirConhecimento(@RequestBody AiConhecimentoRequest request) {
    aiService.definirConhecimento(request.conhecimento());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/chat/conhecimento/limpar")
  public ResponseEntity<Void> limparConhecimento() {
    aiService.definirConhecimento("");
    return ResponseEntity.ok().build();
  }

  @PostMapping("/chat/sessao/{sessionId}/limpar")
  public ResponseEntity<Void> limparSessao(@PathVariable String sessionId) {
    aiService.limparSessao(sessionId);
    return ResponseEntity.ok().build();
  }

  public record AiConhecimentoRequest(String conhecimento) {}
}
