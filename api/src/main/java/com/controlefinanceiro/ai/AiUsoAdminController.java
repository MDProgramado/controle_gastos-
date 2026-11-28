package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.TenantContext;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Uso/custo da IA da empresa autenticada (somente ADMIN — SecurityConfig). O tenant vem
 * exclusivamente do JWT; não há parâmetro de empresa.
 */
@RestController
@RequestMapping("/api/admin/ia")
public class AiUsoAdminController {

  private final AiUsoService usoService;

  public AiUsoAdminController(AiUsoService usoService) {
    this.usoService = usoService;
  }

  @GetMapping("/uso")
  public AiUsoService.RelatorioUso uso(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
    return usoService.relatorio(TenantContext.getEmpresaId(), de, ate);
  }
}
