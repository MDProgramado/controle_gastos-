package com.controlefinanceiro.dashboard;

import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.controlefinanceiro.dashboard.dto.DashboardResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de agregação do dashboard. LAN-05: todo cálculo monetário usa BigDecimal, scale=2,
 * HALF_EVEN; nenhum double/float. T-03-02/T-03-03: COALESCE para meses vazios, tratamento de base
 * <= 0 para percentual (sem divisão por zero).
 */
@Service
public class DashboardService {

  private static final BigDecimal CEM = new BigDecimal("100");

  private final DashboardRepository dashboardRepository;

  public DashboardService(DashboardRepository dashboardRepository) {
    this.dashboardRepository = dashboardRepository;
  }

  /**
   * Obtém o dashboard da empresa autenticada para o mês informado.
   *
   * @param empresaId tenant extraído do token via TenantContext (R3)
   * @param ano ano do mês
   * @param mes mês (1-12)
   * @response DashboardResponse com totais, comparação mês anterior e breakdown por categoria
   */
  @Transactional(readOnly = true)
  public DashboardResponse obterDashboard(Long empresaId, Integer ano, Integer mes) {
    LocalDate dataInicio = LocalDate.of(ano, mes, 1);
    LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());

    BigDecimal totalEntradas = dashboardRepository.totalEntradas(empresaId, dataInicio, dataFim);
    BigDecimal totalSaidas = dashboardRepository.totalSaidas(empresaId, dataInicio, dataFim);
    BigDecimal saldo = totalEntradas.subtract(totalSaidas);

    List<CategoryExpense> categorias =
        dashboardRepository.breakdownSaida(empresaId, dataInicio, dataFim);

    // Mês anterior para comparação
    LocalDate mesAnteriorInicio = dataInicio.minusMonths(1);
    LocalDate mesAnteriorFim = mesAnteriorInicio.withDayOfMonth(mesAnteriorInicio.lengthOfMonth());

    BigDecimal totalEntradasAnt =
        dashboardRepository.totalEntradas(empresaId, mesAnteriorInicio, mesAnteriorFim);
    BigDecimal totalSaidasAnt =
        dashboardRepository.totalSaidas(empresaId, mesAnteriorInicio, mesAnteriorFim);
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

  /**
   * Calcula o percentual de variação: (atual - anterior) / anterior * 100. Se a base (anterior) for
   * <= 0, retorna "—" (sem divisão por zero). Resultado com scale=2, HALF_EVEN.
   */
  private String calcularPercentual(BigDecimal atual, BigDecimal anterior) {
    if (anterior == null || anterior.compareTo(BigDecimal.ZERO) <= 0) {
      return "—";
    }
    BigDecimal variacao = atual.subtract(anterior);
    BigDecimal percentual = variacao.multiply(CEM).divide(anterior, 2, RoundingMode.HALF_EVEN);
    return percentual.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
  }
}
