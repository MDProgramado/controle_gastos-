package com.controlefinanceiro.categoria;

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
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/categorias — expõe leitura de categorias (segue o padrão de {@link
 * com.controlefinanceiro.auth.AuthController}). Controller é fino: só valida formato e delega ao
 * {@link CategoriaService}; nunca contém regra de negócio nem lê tenant (R3).
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

  private final CategoriaService categoriaService;

  public CategoriaController(CategoriaService categoriaService) {
    this.categoriaService = categoriaService;
  }

  @GetMapping
  public ResponseEntity<List<CategoriaResponse>> listar() {
    return ResponseEntity.ok(categoriaService.listar(TenantContext.getEmpresaId()));
  }

  /** POST /api/categorias — cria categoria (restrito a ADMIN via SecurityConfig). */
  @PostMapping
  public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
    return ResponseEntity.status(201)
        .body(categoriaService.criar(TenantContext.getEmpresaId(), request));
  }

  /** PUT /api/categorias/{id} — edita categoria (restrito a ADMIN via SecurityConfig). */
  @PutMapping("/{id}")
  public ResponseEntity<CategoriaResponse> editar(
      @PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
    return ResponseEntity.ok(categoriaService.editar(id, TenantContext.getEmpresaId(), request));
  }

  /** DELETE /api/categorias/{id} — desativa categoria (restrito a ADMIN via SecurityConfig). */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> desativar(@PathVariable Long id) {
    categoriaService.desativar(id, TenantContext.getEmpresaId());
    return ResponseEntity.noContent().build();
  }
}
