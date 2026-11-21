package com.controlefinanceiro.lancamento;

import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioContext;
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

/** Controller fino: lê empresa/usuário do token e delega ao serviço; nunca aceita IDs no body. */
@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoController {

  private final LancamentoService lancamentoService;

  public LancamentoController(LancamentoService lancamentoService) {
    this.lancamentoService = lancamentoService;
  }

  @PostMapping
  public ResponseEntity<LancamentoResponse> criar(@Valid @RequestBody LancamentoRequest request) {
    return ResponseEntity.status(201)
        .body(
            lancamentoService.criar(
                TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), request));
  }

  @PostMapping("/lote")
  public ResponseEntity<List<LancamentoResponse>> processarLote(
      @Valid @RequestBody LancamentoLoteRequest lote) {
    return ResponseEntity.status(201)
        .body(
            lancamentoService.processarLote(
                TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), lote));
  }

  @PutMapping("/{id}")
  public ResponseEntity<LancamentoResponse> editar(
      @PathVariable Long id, @Valid @RequestBody LancamentoRequest request) {
    return ResponseEntity.ok(
        lancamentoService.editar(
            id, TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(@PathVariable Long id) {
    lancamentoService.excluir(id, TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<LancamentoPage> listar(
      @RequestParam(required = false) Integer ano,
      @RequestParam(required = false) Integer mes,
      @RequestParam(required = false) String tipo,
      @RequestParam(required = false) Long categoriaId) {
    return ResponseEntity.ok(
        lancamentoService.listar(
            TenantContext.getEmpresaId(),
            UsuarioContext.getUsuarioId(),
            ano,
            mes,
            tipo,
            categoriaId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<LancamentoResponse> buscarPorId(@PathVariable Long id) {
    return ResponseEntity.ok(
        lancamentoService.buscarPorId(
            id, TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId()));
  }
}
