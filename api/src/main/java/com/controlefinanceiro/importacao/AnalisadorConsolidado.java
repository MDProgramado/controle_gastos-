package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Análise consolidada do lote importado (fase 5, D-26/D-27): totais do mês, saldo previsto e
 * alertas (duplicidade, divergência de fatura, pagamento da própria fatura, gasto atípico,
 * categoria a revisar).
 *
 * <p>Classe pura (sem banco): recebe itens de staging já mapeados, marca alertas nos próprios itens
 * (o serviço grava o staging depois) e devolve o resumo. Dinheiro exclusivamente em {@link
 * BigDecimal} com scale 2 e {@link RoundingMode#HALF_EVEN} — nenhum double/float (D-27).
 */
@Component
public class AnalisadorConsolidado {

  /** Divergência aceitável entre soma dos itens e total da fatura (centavos). */
  private static final BigDecimal TOLERANCIA_FATURA = new BigDecimal("0.05");

  /** Valor atípico quando excede 2× a mediana da própria categoria. */
  private static final BigDecimal FATOR_ATIPICO = new BigDecimal("2");

  private static final BigDecimal ZERO = new BigDecimal("0.00");

  /** Alerta do resumo: tipo (estável, para o frontend), severidade e mensagem em pt-BR. */
  public record Alerta(String tipo, String severidade, String mensagem) {}

  /** Gasto atípico: item bem acima da mediana da própria categoria (D-27). */
  public record GastoAtipico(
      String categoria, BigDecimal valor, BigDecimal media, String descricao, LocalDate data) {}

  /** Resumo analítico do lote/mês (D-26). */
  public record ResumoAnalitico(
      BigDecimal totalRecebido,
      BigDecimal totalPago,
      BigDecimal totalComprometido,
      BigDecimal saldoPrevisto,
      BigDecimal totalFatura,
      LocalDate dataVencimento,
      List<Alerta> alertas,
      List<GastoAtipico> gastosAtipicos) {}

  /**
   * Monta o resumo do lote (itens do mês informado, quando ano/mes vierem).
   *
   * <p>Efeitos colaterais documentados: marca {@code alerta} nos itens (duplicidade, gasto atípico,
   * pagamento de fatura) — o chamador grava o staging depois dessas marcações.
   */
  public ResumoAnalitico montarResumo(
      List<ImportacaoItem> itens, ImportacaoFatura fatura, Integer ano, Integer mes) {
    List<ImportacaoItem> doMes = filtrarMes(itens, ano, mes);

    BigDecimal totalRecebido = soma(doMes, "RECEITA");
    BigDecimal totalPago = soma(doMes, "DESPESA_PAGA");
    BigDecimal totalComprometido = soma(doMes, "A_PAGAR");
    BigDecimal saldoPrevisto =
        totalRecebido
            .subtract(totalPago)
            .subtract(totalComprometido)
            .setScale(2, RoundingMode.HALF_EVEN);

    List<Alerta> alertas = new ArrayList<>();
    detetarDuplicidade(doMes, alertas);
    detetarDivergenciaFatura(doMes, fatura, alertas);
    detetarPagamentoFatura(doMes, fatura, alertas);
    detetarCategoriaRevisar(doMes, alertas);
    List<GastoAtipico> gastosAtipicos = detetarGastosAtipicos(doMes, alertas);

    BigDecimal totalFatura = fatura != null ? fatura.getTotal() : null;
    LocalDate vencimento = fatura != null ? fatura.getDataVencimento() : null;

    return new ResumoAnalitico(
        totalRecebido,
        totalPago,
        totalComprometido,
        saldoPrevisto,
        totalFatura,
        vencimento,
        alertas,
        gastosAtipicos);
  }

  /* ============================ totais ============================ */

  private BigDecimal soma(List<ImportacaoItem> itens, String tipo) {
    BigDecimal total = ZERO;
    for (ImportacaoItem item : itens) {
      if (tipo.equals(item.getTipo()) && item.getValor() != null) {
        total = total.add(item.getValor());
      }
    }
    return total.setScale(2, RoundingMode.HALF_EVEN);
  }

  /* ============================ alertas ============================ */

  /** Item repetido no lote: mesmo tipo+data+valor+descrição aparece mais de uma vez. */
  private void detetarDuplicidade(List<ImportacaoItem> itens, List<Alerta> alertas) {
    Map<String, List<ImportacaoItem>> grupos = new LinkedHashMap<>();
    for (ImportacaoItem item : itens) {
      String chave =
          Deduplicador.gerarChaveDedupe(
              item.getTipo(), item.getData(), item.getValor(), item.getDescricao());
      grupos.computeIfAbsent(chave, k -> new ArrayList<>()).add(item);
    }
    for (List<ImportacaoItem> grupo : grupos.values()) {
      if (grupo.size() > 1) {
        ImportacaoItem primeiro = grupo.get(0);
        String mensagem =
            "Item repetido no documento: \"%s\" (%s) aparece %d vezes"
                .formatted(
                    primeiro.getDescricao(), primeiro.getValor().toPlainString(), grupo.size());
        alertas.add(new Alerta("DUPLICIDADE", "ALTA", mensagem));
        for (int i = 1; i < grupo.size(); i++) {
          ImportacaoItem repetido = grupo.get(i);
          repetido.setAlerta(
              Deduplicador.acrescentarAlerta(
                  repetido.getAlerta(), "DUPLICIDADE: item repetido no documento"));
        }
      }
    }
  }

  /**
   * Soma dos itens de CARTÃO (DESPESA_PAGA + A_PAGAR) deve conferir com o total da fatura (±0,05).
   * Divergência maior → alerta de revisão (ALTA).
   */
  private void detetarDivergenciaFatura(
      List<ImportacaoItem> itens, ImportacaoFatura fatura, List<Alerta> alertas) {
    if (fatura == null || fatura.getTotal() == null) {
      return;
    }
    List<ImportacaoItem> doCartao =
        itens.stream().filter(i -> "CARTAO".equals(i.getOrigem())).toList();
    if (doCartao.isEmpty()) {
      return;
    }
    BigDecimal somaCartao =
        doCartao.stream()
            .filter(i -> "DESPESA_PAGA".equals(i.getTipo()) || "A_PAGAR".equals(i.getTipo()))
            .map(i -> i == null || i.getValor() == null ? ZERO : i.getValor())
            .reduce(ZERO, (total, valor) -> total.add(valor))
            .setScale(2, RoundingMode.HALF_EVEN);
    BigDecimal diferenca = somaCartao.subtract(fatura.getTotal()).abs();
    if (diferenca.compareTo(TOLERANCIA_FATURA) > 0) {
      alertas.add(
          new Alerta(
              "DIVERGENCIA_FATURA",
              "ALTA",
              "A soma dos itens do cartão (%s) não confere com o total da fatura (%s)"
                  .formatted(somaCartao.toPlainString(), fatura.getTotal().toPlainString())));
    }
  }

  /** Extrato mostra o pagamento da própria fatura (marca itens; ver {@link Deduplicador}). */
  private void detetarPagamentoFatura(
      List<ImportacaoItem> itens, ImportacaoFatura fatura, List<Alerta> alertas) {
    if (fatura == null) {
      return;
    }
    int marcados = Deduplicador.detetarPagamentoPropriaFatura(itens, fatura);
    if (marcados > 0) {
      alertas.add(
          new Alerta(
              "FATURA_NO_EXTRATO",
              "ALTA",
              "O extrato mostra o pagamento da própria fatura (R$ %s) — ao confirmar, não lance"
                  + " de novo".formatted(fatura.getTotal().toPlainString())));
    }
  }

  /** Itens marcados com {@code REVISAR_CATEGORIA} pelo mapeador (D-25) viram alerta do resumo. */
  private void detetarCategoriaRevisar(List<ImportacaoItem> itens, List<Alerta> alertas) {
    long pendentes =
        itens.stream()
            .filter(i -> i.getAlerta() != null && i.getAlerta().contains("REVISAR_CATEGORIA"))
            .count();
    if (pendentes > 0) {
      alertas.add(
          new Alerta(
              "CATEGORIA_REVISAR",
              "MEDIA",
              "%d item(ns) com categoria sugerida que não existe — revise na confirmação"
                  .formatted(pendentes)));
    }
  }

  /** Valor > 2× mediana da própria categoria (dentro do lote) → gasto atípico (D-27). */
  private List<GastoAtipico> detetarGastosAtipicos(
      List<ImportacaoItem> itens, List<Alerta> alertas) {
    Map<String, List<ImportacaoItem>> porCategoria = new HashMap<>();
    for (ImportacaoItem item : itens) {
      if ("DESPESA_PAGA".equals(item.getTipo()) || "A_PAGAR".equals(item.getTipo())) {
        String categoria =
            item.getCategoriaNome() != null ? item.getCategoriaNome() : "Sem categoria";
        porCategoria.computeIfAbsent(categoria, k -> new ArrayList<>()).add(item);
      }
    }

    List<GastoAtipico> atipicos = new ArrayList<>();
    for (Map.Entry<String, List<ImportacaoItem>> entrada : porCategoria.entrySet()) {
      List<BigDecimal> valores =
          entrada.getValue().stream()
              .map(item -> item.getValor())
              .filter(v -> v != null)
              .sorted()
              .toList();
      if (valores.isEmpty()) {
        continue;
      }
      BigDecimal mediana = mediana(valores);
      for (ImportacaoItem item : entrada.getValue()) {
        if (item.getValor() != null
            && item.getValor().compareTo(mediana.multiply(FATOR_ATIPICO)) > 0) {
          String mensagem =
              "R$ %s em \"%s\" está bem acima da mediana da categoria (R$ %s)"
                  .formatted(
                      item.getValor().toPlainString(),
                      item.getDescricao(),
                      mediana.toPlainString());
          alertas.add(new Alerta("GASTO_ATIPICO", "MEDIA", mensagem));
          item.setAlerta(
              Deduplicador.acrescentarAlerta(item.getAlerta(), "GASTO_ATIPICO: " + mensagem));
          atipicos.add(
              new GastoAtipico(
                  entrada.getKey(),
                  item.getValor().setScale(2, RoundingMode.HALF_EVEN),
                  mediana,
                  item.getDescricao(),
                  item.getData()));
        }
      }
    }
    return atipicos;
  }

  /** Mediana com BigDecimal (par: média dos dois centrais; scale 2, HALF_EVEN). */
  private BigDecimal mediana(List<BigDecimal> valoresOrdenados) {
    int n = valoresOrdenados.size();
    if (n % 2 == 1) {
      return valoresOrdenados.get(n / 2).setScale(2, RoundingMode.HALF_EVEN);
    }
    BigDecimal somaCentros = valoresOrdenados.get(n / 2 - 1).add(valoresOrdenados.get(n / 2));
    return somaCentros
        .divide(new BigDecimal("2"), 2, RoundingMode.HALF_EVEN)
        .setScale(2, RoundingMode.HALF_EVEN);
  }

  /* ============================ helpers ============================ */

  private List<ImportacaoItem> filtrarMes(List<ImportacaoItem> itens, Integer ano, Integer mes) {
    if (itens == null) {
      return List.of();
    }
    if (ano == null || mes == null) {
      return new ArrayList<>(itens);
    }
    LocalDate inicio = LocalDate.of(ano, mes, 1);
    LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
    return itens.stream()
        .filter(
            i -> i.getData() != null && !i.getData().isBefore(inicio) && !i.getData().isAfter(fim))
        .toList();
  }
}
