package com.controlefinanceiro.categoria;

import com.controlefinanceiro.common.TenantContext;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
