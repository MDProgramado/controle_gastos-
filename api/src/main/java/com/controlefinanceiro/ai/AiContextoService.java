package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.UsuarioContext;
import com.controlefinanceiro.dashboard.DashboardRepository;
import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta o contexto de DADOS REAIS da empresa que é injetado no prompt da IA.
 *
 * <p>Sem isso a IA não tem como responder consultas ("quanto gastei esse mês?") sem inventar
 * números — o que quebraria a proposta central do produto (números confiáveis ao centavo).
 *
 * <p>Todos os valores vêm do banco com SUM/SQL e são formatados em pt-BR. Todo cálculo monetário
 * usa {@link BigDecimal} com scale=2 e {@link RoundingMode#HALF_EVEN} (R1). O tenant vem
 * exclusivamente do JWT (R3).
 *
 * <p>Minimização de dados (LGPD): o contexto fixo traz só totais do mês atual e do anterior e o
 * resumo por categoria do mês atual. Nenhuma descrição de lançamento (texto livre do usuário) e
 * nenhum dado pessoal (nome, e-mail, telefone, CPF) entra aqui; detalhes são obtidos sob demanda
 * pela ferramenta {@code consultar_historico_e_comparativo}.
 */
@Service
public class AiContextoService {

  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  /** Mês atual + mês anterior. */
  private static final int MESES_NO_HISTORICO = 2;

  private final DashboardRepository dashboardRepository;

  public AiContextoService(DashboardRepository dashboardRepository) {
    this.dashboardRepository = dashboardRepository;
  }

  /**
   * Gera o texto de contexto com os números reais da empresa.
   *
   * @param empresaId tenant extraído do token (R3)
   * @return texto pronto para o prompt
   */
  public String montarContexto(Long empresaId) {
    return montarContexto(empresaId, UsuarioContext.getUsuarioId());
  }

  @Transactional(readOnly = true)
  public String montarContexto(Long empresaId, Long usuarioId) {
    if (empresaId == null) {
      return "";
    }

    LocalDate hoje = LocalDate.now(FUSO);
    StringBuilder sb = new StringBuilder();

    sb.append("Totais por mês (entradas / saídas / saldo):\n");
    for (int i = 0; i < MESES_NO_HISTORICO; i++) {
      LocalDate inicio = hoje.withDayOfMonth(1).minusMonths(i);
      LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
      BigDecimal entradas =
          normalizar(dashboardRepository.totalEntradas(empresaId, usuarioId, inicio, fim));
      BigDecimal saidas =
          normalizar(dashboardRepository.totalSaidas(empresaId, usuarioId, inicio, fim));
      BigDecimal saldo = entradas.subtract(saidas);
      sb.append("- ")
          .append(nomeMes(inicio))
          .append(" de ")
          .append(inicio.getYear())
          .append(": entradas ")
          .append(formatarMoeda(entradas))
          .append(" | saídas ")
          .append(formatarMoeda(saidas))
          .append(" | saldo ")
          .append(formatarMoeda(saldo))
          .append("\n");
    }

    LocalDate inicioMes = hoje.withDayOfMonth(1);
    LocalDate fimMes = inicioMes.withDayOfMonth(inicioMes.lengthOfMonth());

    List<CategoryExpense> saidasPorCategoria =
        dashboardRepository.breakdownSaida(empresaId, usuarioId, inicioMes, fimMes);
    List<CategoryExpense> entradasPorCategoria =
        dashboardRepository.breakdownEntrada(empresaId, usuarioId, inicioMes, fimMes);

    sb.append("\nCategorias no mês atual (").append(nomeMes(inicioMes)).append("):\n");
    for (CategoryExpense c : saidasPorCategoria) {
      if (c.total() != null && c.total().signum() > 0) {
        sb.append("- SAIDA ")
            .append(formatarMoeda(c.total()))
            .append(" em ")
            .append(c.nome())
            .append("\n");
      }
    }
    for (CategoryExpense c : entradasPorCategoria) {
      if (c.total() != null && c.total().signum() > 0) {
        sb.append("- ENTRADA ")
            .append(formatarMoeda(c.total()))
            .append(" em ")
            .append(c.nome())
            .append("\n");
      }
    }

    return sb.toString();
  }

  private String nomeMes(LocalDate data) {
    return data.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
  }

  private BigDecimal normalizar(BigDecimal valor) {
    if (valor == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }
    return valor.setScale(2, RoundingMode.HALF_EVEN);
  }

  private String formatarMoeda(BigDecimal valor) {
    java.text.DecimalFormat formato =
        new java.text.DecimalFormat(
            "#,##0.00", java.text.DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
    return "R$ " + formato.format(normalizar(valor));
  }
}
