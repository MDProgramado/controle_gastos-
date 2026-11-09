package com.controlefinanceiro.dashboard;

import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.dashboard.dto.DashboardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/dashboard — expõe agregação de totais, comparação mensal e breakdown por categoria.
 * Controller é fino: só valida formato e delega ao {@link DashboardService}; nunca contém regra de
 * negócio nem lê tenant (R3). Empresa_id vem exclusivamente de TenantContext.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /** GET /api/dashboard?ano=&mes= — retorna DashboardResponse da empresa autenticada. */
  @GetMapping
  public ResponseEntity<DashboardResponse> obter(
      @RequestParam Integer ano, @RequestParam Integer mes) {
    return ResponseEntity.ok(
        dashboardService.obterDashboard(TenantContext.getEmpresaId(), ano, mes));
  }
}
