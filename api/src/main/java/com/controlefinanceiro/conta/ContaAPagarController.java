package com.controlefinanceiro.conta;

import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contas-a-pagar")
public class ContaAPagarController {

  private final ContaAPagarService service;

  public ContaAPagarController(ContaAPagarService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<ContaAPagarResponse>> listar(
      @RequestParam(required = false) Boolean pago,
      @RequestParam(required = false) LocalDate vencimentoDe,
      @RequestParam(required = false) LocalDate vencimentoAte) {
    return ResponseEntity.ok(
        service.listar(
            TenantContext.getEmpresaId(),
            UsuarioContext.getUsuarioId(),
            pago,
            vencimentoDe,
            vencimentoAte));
  }

  @PatchMapping("/{id}/pagar")
  public ResponseEntity<ContaAPagarResponse> pagar(
      @PathVariable Long id, @RequestBody(required = false) ContaAPagarPagamentoRequest request) {
    LocalDate dataPagamento = request == null ? null : request.dataPagamento();
    return ResponseEntity.ok(
        service.pagar(
            id, TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), dataPagamento));
  }
}
