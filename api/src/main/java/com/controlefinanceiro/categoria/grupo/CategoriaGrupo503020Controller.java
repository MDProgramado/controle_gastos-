package com.controlefinanceiro.categoria.grupo;

import com.controlefinanceiro.common.TenantContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias/grupos-503020")
public class CategoriaGrupo503020Controller {

  private final CategoriaGrupo503020Service service;

  public CategoriaGrupo503020Controller(CategoriaGrupo503020Service service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<CategoriaGrupo503020Response>> listar() {
    return ResponseEntity.ok(service.listar(TenantContext.getEmpresaId()));
  }

  @PutMapping("/{categoriaId}")
  public ResponseEntity<CategoriaGrupo503020Response> salvar(
      @PathVariable Long categoriaId, @Valid @RequestBody CategoriaGrupo503020Request request) {
    return ResponseEntity.ok(
        service.salvar(TenantContext.getEmpresaId(), categoriaId, request.grupo()));
  }
}
