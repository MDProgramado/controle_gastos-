package com.controlefinanceiro.dashboard;

import com.controlefinanceiro.common.UsuarioContext;
import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.controlefinanceiro.dashboard.dto.DashboardResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agregação financeira sempre escalonada por empresa_id e usuario_id do token. */
@Service
public class DashboardService {

  private static final BigDecimal CEM = new BigDecimal("100");

  private final DashboardRepository dashboardRepository;

  public DashboardService(DashboardRepository dashboardRepository) {
    this.dashboardRepository = dashboardRepository;
  }

  /** Sobrecarga usada por fluxos internos que já possuem apenas o tenant explícito. */
  @Transactional(readOnly = true)
  public DashboardResponse obterDashboard(Long empresaId, Integer ano, Integer mes) {
    return obterDashboard(empresaId, UsuarioContext.getUsuarioId(), ano, mes);
  }

  @Transactional(readOnly = true)
  public DashboardResponse obterDashboard(
      Long empresaId, Long usuarioId, Integer ano, Integer mes) {
    LocalDate dataInicio = LocalDate.of(ano, mes, 1);
    LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());

    BigDecimal totalEntradas =
        dashboardRepository.totalEntradas(empresaId, usuarioId, dataInicio, dataFim);
    BigDecimal totalSaidas =
        dashboardRepository.totalSaidas(empresaId, usuarioId, dataInicio, dataFim);
    BigDecimal saldo = totalEntradas.subtract(totalSaidas);

    List<CategoryExpense> categorias =
        dashboardRepository.breakdownSaida(empresaId, usuarioId, dataInicio, dataFim);

    LocalDate mesAnteriorInicio = dataInicio.minusMonths(1);
    LocalDate mesAnteriorFim = mesAnteriorInicio.withDayOfMonth(mesAnteriorInicio.lengthOfMonth());

    BigDecimal totalEntradasAnt =
        dashboardRepository.totalEntradas(empresaId, usuarioId, mesAnteriorInicio, mesAnteriorFim);
    BigDecimal totalSaidasAnt =
        dashboardRepository.totalSaidas(empresaId, usuarioId, mesAnteriorInicio, mesAnteriorFim);
    BigDecimal saldoAnt = totalEntradasAnt.subtract(totalSaidasAnt);

    BigDecimal variacaoEntradas = totalEntradas.subtract(totalEntradasAnt);
    BigDecimal variacaoSaidas = totalSaidas.subtract(totalSaidasAnt);
    BigDecimal variacaoSaldo = saldo.subtract(saldoAnt);

    return new DashboardResponse(
        totalEntradas,
        totalSaidas,
        saldo,
        variacaoEntradas,
        variacaoSaidas,
        variacaoSaldo,
        calcularPercentual(totalEntradas, totalEntradasAnt),
        calcularPercentual(totalSaidas, totalSaidasAnt),
        calcularPercentual(saldo, saldoAnt),
        categorias);
  }

  private String calcularPercentual(BigDecimal atual, BigDecimal anterior) {
    if (anterior == null || anterior.compareTo(BigDecimal.ZERO) <= 0) {
      return "—";
    }
    BigDecimal variacao = atual.subtract(anterior);
    BigDecimal percentual = variacao.multiply(CEM).divide(anterior, 2, RoundingMode.HALF_EVEN);
    return percentual.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
  }
}
