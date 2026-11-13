package com.controlefinanceiro.importacao;

import com.controlefinanceiro.common.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints da importação de PDFs (fase 5, 05-04-t5). Controller é fino: valida formato e delega ao
 * {@link ImportacaoService} — nunca contém regra de negócio nem lê tenant (R3: {@code empresa_id}
 * vem só do token, via {@link TenantContext}; nenhum parâmetro de empresa nos DTOs).
 */
@RestController
@RequestMapping("/api/importacoes")
public class ImportacaoController {

  private final ImportacaoService importacaoService;

  public ImportacaoController(ImportacaoService importacaoService) {
    this.importacaoService = importacaoService;
  }

  /**
   * POST /api/importacoes/pdf — analisa um lote de 1..10 PDFs (fatura de cartão ou extrato).
   *
   * <p>Grava apenas o staging (análise): nada em {@code lancamento}/{@code conta_a_pagar} — a
   * confirmação é endpoint separado (05-05). Erros: 400 (sem arquivo/tipo inválido), 413 (lote
   * acima do limite do servidor), 503 (IA indisponível e nenhum documento analisado). Falha em um
   * arquivo individual não derruba o lote — entra como status ERRO na resposta.
   */
  @PostMapping("/pdf")
  public ResponseEntity<ImportacaoService.ResultadoAnalise> analisar(
      @RequestParam("arquivos") MultipartFile[] arquivos) {
    return ResponseEntity.ok(importacaoService.analisar(arquivos));
  }

  /** GET /api/importacoes — histórico de importações da empresa, paginado. */
  @GetMapping
  public ResponseEntity<Page<ImportacaoService.DocumentoResumo>> listar(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    Page<ImportacaoService.DocumentoResumo> historico =
        importacaoService.listar(PageRequest.of(pagina, tamanho));
    return ResponseEntity.ok(historico);
  }

  /** GET /api/importacoes/{id} — detalhe de uma importação (itens + fatura + resumo). */
  @GetMapping("/{id}")
  public ResponseEntity<ImportacaoService.DocumentoDetalhe> detalhar(@PathVariable Long id) {
    return ResponseEntity.ok(importacaoService.detalhar(id));
  }

  /**
   * DELETE /api/importacoes/{id} — descarta uma importação pendente (status → DESCARTADO).
   * Importação já confirmada não pode ser descartada → 409.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> descartar(@PathVariable Long id) {
    importacaoService.descartar(id);
    return ResponseEntity.noContent().build();
  }
}
