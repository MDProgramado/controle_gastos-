package com.controlefinanceiro.lancamento;

import com.controlefinanceiro.common.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/lancamentos — expõe leitura de lançamentos (segue o padrão de {@link
 * com.controlefinanceiro.auth.AuthController}). Controller é fino: só valida formato e delega ao
 * {@link LancamentoService}; nunca contém regra de negócio nem lê tenant (R3).
 */
@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

  private final LancamentoService lancamentoService;

  public LancamentoController(LancamentoService lancamentoService) {
    this.lancamentoService = lancamentoService;
  }

  /** POST /api/lancamentos — cria lançamento. */
  @PostMapping
  public ResponseEntity<LancamentoResponse> criar(@Valid @RequestBody LancamentoRequest request) {
    return ResponseEntity.status(201)
        .body(lancamentoService.criar(TenantContext.getEmpresaId(), request));
  }

  /**
   * POST /api/lancamentos/lote — cria um lote de até 500 lançamentos em transação tudo-ou-nada
   * (LOT-01, LOT-02). Em falha de qualquer linha, retorna 400 com o índice da linha inválida e nada
   * é gravado.
   */
  @PostMapping("/lote")
  public ResponseEntity<List<LancamentoResponse>> processarLote(
      @Valid @RequestBody LancamentoLoteRequest lote) {
    return ResponseEntity.status(201)
        .body(lancamentoService.processarLote(TenantContext.getEmpresaId(), lote));
  }

  /** PUT /api/lancamentos/{id} — edita lançamento. */
  @PutMapping("/{id}")
  public ResponseEntity<LancamentoResponse> editar(
      @PathVariable Long id, @Valid @RequestBody LancamentoRequest request) {
    return ResponseEntity.ok(lancamentoService.editar(id, TenantContext.getEmpresaId(), request));
  }

  /** DELETE /api/lancamentos/{id} — exclui lançamento. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable Long id) {
    lancamentoService.excluir(id, TenantContext.getEmpresaId());
    return ResponseEntity.noContent().build();
  }

  /**
   * GET /api/lancamentos — lista lançamentos com filtros opcionais de mês, ano, tipo, categoria.
   */
  @GetMapping
  public ResponseEntity<LancamentoPage> listar(
      @RequestParam(required = false) Integer ano,
      @RequestParam(required = false) Integer mes,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) Long categoriaId) {
    return ResponseEntity.ok(
        lancamentoService.listar(TenantContext.getEmpresaId(), ano, mes, tipo, categoriaId));
  }

  /** GET /api/lancamentos/{id} — busca lançamento por id. */
  @GetMapping("/{id}")
  public ResponseEntity<LancamentoResponse> buscarPorId(@PathVariable Long id) {
    LancamentoPage pagina =
        lancamentoService.listar(TenantContext.getEmpresaId(), null, null, null, null);
    return pagina.lancamentos().stream()
        .filter(l -> l.id().equals(id))
        .findFirst()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
