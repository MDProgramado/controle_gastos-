package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.importacao.AnalisadorConsolidado.Alerta;
import com.controlefinanceiro.importacao.AnalisadorConsolidado.GastoAtipico;
import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes de unidade de {@link AnalisadorConsolidado} (classe pura, sem banco, sem rede). */
class AnalisadorConsolidadoTest {

  private final AnalisadorConsolidado analisador = new AnalisadorConsolidado();

  @Test
  @DisplayName("totais por tipo e saldo previsto (BigDecimal scale 2)")
  void totaisESaldoPrevisto() {
    List<ImportacaoItem> itens =
        List.of(
            item("RECEITA", "1000.00", "EXTRATO", "Rendimentos e reembolsos"),
            item("RECEITA", "500.00", "EXTRATO", "Rendimentos e reembolsos"),
            item("DESPESA_PAGA", "300.00", "EXTRATO", "Alimentação"),
            item("DESPESA_PAGA", "100.00", "EXTRATO", "Transporte"),
            item("A_PAGAR", "200.00", "CARTAO", "Lazer"));

    AnalisadorConsolidado.ResumoAnalitico resumo = analisador.montarResumo(itens, null, null, null);

    assertThat(resumo.totalRecebido()).isEqualByComparingTo("1500.00");
    assertThat(resumo.totalPago()).isEqualByComparingTo("400.00");
    assertThat(resumo.totalComprometido()).isEqualByComparingTo("200.00");
    assertThat(resumo.saldoPrevisto()).isEqualByComparingTo("900.00");
    assertThat(resumo.totalRecebido().scale()).isEqualTo(2); // NUMERIC(14,2)
    assertThat(resumo.totalFatura()).isNull();
  }

  @Test
  @DisplayName("divergência de fatura detectada quando soma difere mais de 0,05")
  void divergenciaDeFatura() {
    List<ImportacaoItem> itens =
        List.of(
            item("A_PAGAR", "950.00", "CARTAO", "Alimentação"),
            item("A_PAGAR", "50.00", "CARTAO", "Lazer"));
    ImportacaoFatura fatura = fatura("1200.00"); // soma dos itens = 1000 → diverge em 200

    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisador.montarResumo(itens, fatura, null, null);

    assertThat(resumo.totalFatura()).isEqualByComparingTo("1200.00");
    assertThat(resumo.alertas()).extracting(Alerta::tipo).contains("DIVERGENCIA_FATURA");
  }

  @Test
  @DisplayName("diferença dentro da tolerância (±0,05) não gera alerta")
  void divergenciaDentroDaTolerancia() {
    List<ImportacaoItem> itens = List.of(item("A_PAGAR", "1000.03", "CARTAO", "Alimentação"));
    ImportacaoFatura fatura = fatura("1000.00");

    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisador.montarResumo(itens, fatura, null, null);

    assertThat(resumo.alertas()).extracting(Alerta::tipo).doesNotContain("DIVERGENCIA_FATURA");
  }

  @Test
  @DisplayName("gasto atípico: valor acima de 2× a mediana da categoria")
  void gastoAtipico() {
    List<ImportacaoItem> itens =
        List.of(
            item("DESPESA_PAGA", "100.00", "EXTRATO", "Alimentação"),
            item("DESPESA_PAGA", "110.00", "EXTRATO", "Alimentação"),
            item("DESPESA_PAGA", "120.00", "EXTRATO", "Alimentação"),
            item("DESPESA_PAGA", "900.00", "EXTRATO", "Alimentação"));

    AnalisadorConsolidado.ResumoAnalitico resumo = analisador.montarResumo(itens, null, null, null);

    assertThat(resumo.gastosAtipicos()).hasSize(1);
    GastoAtipico atipico = resumo.gastosAtipicos().get(0);
    assertThat(atipico.valor()).isEqualByComparingTo("900.00");
    assertThat(atipico.media()).isEqualByComparingTo("115.00"); // mediana de 100,110,120,900
    assertThat(resumo.alertas()).extracting(Alerta::tipo).contains("GASTO_ATIPICO");
    // o item atípico também recebe o alerta (para a tela de revisão)
    assertThat(itens.get(3).getAlerta()).contains("GASTO_ATIPICO");
  }

  @Test
  @DisplayName("item repetido no lote gera alerta de DUPLICIDADE")
  void duplicidadeNoLote() {
    ImportacaoItem original = item("DESPESA_PAGA", "45.00", "EXTRATO", "Alimentação");
    original.setDescricao("Almoço no restaurante");
    original.setData(LocalDate.of(2026, 11, 10));
    ImportacaoItem repetido = item("DESPESA_PAGA", "45.00", "EXTRATO", "Alimentação");
    repetido.setDescricao("Almoço no restaurante");
    repetido.setData(LocalDate.of(2026, 11, 10));

    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisador.montarResumo(List.of(original, repetido), null, null, null);

    assertThat(resumo.alertas()).extracting(Alerta::tipo).contains("DUPLICIDADE");
    assertThat(repetido.getAlerta()).contains("DUPLICIDADE");
  }

  @Test
  @DisplayName("pagamento da própria fatura no extrato gera alerta FATURA_NO_EXTRATO")
  void pagamentoPropriaFatura() {
    ImportacaoItem pagamento = item("DESPESA_PAGA", "1000.00", "EXTRATO", "Outros");
    pagamento.setDescricao("Pagamento fatura do cartão");
    pagamento.setData(LocalDate.of(2026, 11, 10));

    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisador.montarResumo(List.of(pagamento), fatura("1000.00"), null, null);

    assertThat(resumo.alertas()).extracting(Alerta::tipo).contains("FATURA_NO_EXTRATO");
  }

  @Test
  @DisplayName("itens marcados com REVISAR_CATEGORIA geram alerta agregado")
  void categoriaRevisar() {
    ImportacaoItem item = item("DESPESA_PAGA", "45.00", "EXTRATO", "Alimentação");
    item.setAlerta("REVISAR_CATEGORIA: 'X' → Outros");

    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisador.montarResumo(List.of(item), null, null, null);

    assertThat(resumo.alertas()).extracting(Alerta::tipo).contains("CATEGORIA_REVISAR");
    assertThat(resumo.alertas())
        .filteredOn(a -> a.tipo().equals("CATEGORIA_REVISAR"))
        .first()
        .extracting(Alerta::mensagem)
        .asString()
        .contains("1 item");
  }

  @Test
  @DisplayName("filtro de mês: itens de outro mês ficam fora do resumo")
  void filtroDeMes() {
    List<ImportacaoItem> itens =
        List.of(
            itemDoMes("RECEITA", "1000.00", "2026-11-05"),
            itemDoMes("DESPESA_PAGA", "400.00", "2026-10-20"));

    AnalisadorConsolidado.ResumoAnalitico resumo = analisador.montarResumo(itens, null, 2026, 11);

    assertThat(resumo.totalRecebido()).isEqualByComparingTo("1000.00");
    assertThat(resumo.totalPago()).isEqualByComparingTo("0.00");
    assertThat(resumo.saldoPrevisto()).isEqualByComparingTo("1000.00");
  }

  @Test
  @DisplayName("lote vazio: totais zerados, sem alertas")
  void loteVazio() {
    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisador.montarResumo(List.of(), null, null, null);

    assertThat(resumo.totalRecebido()).isEqualByComparingTo("0.00");
    assertThat(resumo.totalPago()).isEqualByComparingTo("0.00");
    assertThat(resumo.totalComprometido()).isEqualByComparingTo("0.00");
    assertThat(resumo.saldoPrevisto()).isEqualByComparingTo("0.00");
    assertThat(resumo.alertas()).isEmpty();
    assertThat(resumo.gastosAtipicos()).isEmpty();
  }

  /* ============ fixtures ============ */

  private static ImportacaoItem item(String tipo, String valor, String origem, String categoria) {
    ImportacaoItem item = new ImportacaoItem();
    item.setTipo(tipo);
    item.setValor(new BigDecimal(valor));
    item.setOrigem(origem);
    item.setCategoriaNome(categoria);
    item.setDescricao("Item " + categoria);
    item.setData(LocalDate.of(2026, 11, 10));
    return item;
  }

  private static ImportacaoItem itemDoMes(String tipo, String valor, String data) {
    ImportacaoItem item = new ImportacaoItem();
    item.setTipo(tipo);
    item.setValor(new BigDecimal(valor));
    item.setOrigem("EXTRATO");
    item.setCategoriaNome("Outros");
    item.setDescricao("Item");
    item.setData(LocalDate.parse(data));
    return item;
  }

  private static ImportacaoFatura fatura(String total) {
    ImportacaoFatura fatura = new ImportacaoFatura();
    fatura.setTotal(new BigDecimal(total));
    fatura.setDataVencimento(LocalDate.of(2026, 12, 10));
    return fatura;
  }
}
