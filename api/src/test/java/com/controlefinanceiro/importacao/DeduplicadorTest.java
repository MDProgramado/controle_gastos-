package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes de unidade de {@link Deduplicador} (classe pura, sem banco). */
class DeduplicadorTest {

  @Test
  @DisplayName("chave de dedupe é estável: mesmo input → mesmo hash")
  void chaveEhEstavel() {
    String primeira =
        Deduplicador.gerarChaveDedupe(
            "DESPESA_PAGA", LocalDate.of(2026, 11, 3), new BigDecimal("123.45"), "Alimentação");
    String segunda =
        Deduplicador.gerarChaveDedupe(
            "DESPESA_PAGA", LocalDate.of(2026, 11, 3), new BigDecimal("123.45"), "Alimentação");

    assertThat(primeira).isEqualTo(segunda);
    assertThat(primeira).hasSize(64); // SHA-256 hex
  }

  @Test
  @DisplayName("chave muda quando qualquer componente muda")
  void chaveMudaComComponentes() {
    LocalDate data = LocalDate.of(2026, 11, 3);
    BigDecimal valor = new BigDecimal("123.45");
    String categoria = "Alimentação";

    String base = Deduplicador.gerarChaveDedupe("DESPESA_PAGA", data, valor, categoria);

    assertThat(Deduplicador.gerarChaveDedupe("RECEITA", data, valor, categoria)).isNotEqualTo(base);
    assertThat(Deduplicador.gerarChaveDedupe("DESPESA_PAGA", data.plusDays(1), valor, categoria))
        .isNotEqualTo(base);
    assertThat(
            Deduplicador.gerarChaveDedupe(
                "DESPESA_PAGA", data, new BigDecimal("123.46"), categoria))
        .isNotEqualTo(base);
    assertThat(Deduplicador.gerarChaveDedupe("DESPESA_PAGA", data, valor, "Transporte"))
        .isNotEqualTo(base);
  }

  @Test
  @DisplayName("chave normaliza categoria (acento/caixa) e valor com 2 casas")
  void chaveNormalizaCategoriaEValor() {
    LocalDate data = LocalDate.of(2026, 11, 3);

    String comAcento =
        Deduplicador.gerarChaveDedupe(
            "DESPESA_PAGA", data, new BigDecimal("123.45"), "Alimentação");
    String semAcento =
        Deduplicador.gerarChaveDedupe(
            "DESPESA_PAGA", data, new BigDecimal("123.45"), "alimentacao");

    assertThat(comAcento).isEqualTo(semAcento);

    String valorInteiro =
        Deduplicador.gerarChaveDedupe("DESPESA_PAGA", data, new BigDecimal("100"), "Alimentação");
    String valorDuasCasas =
        Deduplicador.gerarChaveDedupe(
            "DESPESA_PAGA", data, new BigDecimal("100.00"), "Alimentação");

    assertThat(valorInteiro).isEqualTo(valorDuasCasas);
  }

  @Test
  @DisplayName("ehDuplicado detecta item já existente pela chave")
  void ehDuplicadoDetecta() {
    ImportacaoItem existente = item("DESPESA_PAGA", "2026-11-03", "123.45", "Alimentação");
    ImportacaoItem candidato = item("DESPESA_PAGA", "2026-11-03", "123.45", "alimentação");

    Set<String> chavesExistentes = Deduplicador.chavesDe(List.of(existente));

    assertThat(Deduplicador.ehDuplicado(candidato, chavesExistentes)).isTrue();

    ImportacaoItem diferente = item("DESPESA_PAGA", "2026-11-04", "123.45", "Alimentação");
    assertThat(Deduplicador.ehDuplicado(diferente, chavesExistentes)).isFalse();
  }

  @Test
  @DisplayName("pagamento da própria fatura: extrato + fatura/cartão + valor ≈ total")
  void pagamentoDaPropriaFaturaEhDetectado() {
    ImportacaoItem pagamento = itemExtrato("Pagamento fatura do cartão Nubank", "1000.00");
    ImportacaoFatura fatura = fatura("1000.00");

    int marcados = Deduplicador.detetarPagamentoPropriaFatura(List.of(pagamento), fatura);

    assertThat(marcados).isEqualTo(1);
    assertThat(pagamento.getAlerta()).contains("FATURA_NO_EXTRATO");
  }

  @Test
  @DisplayName("valor que não confere (fora de ±0,05) não marca")
  void valorDivergenteNaoMarca() {
    ImportacaoItem pagamento = itemExtrato("Pagamento fatura do cartão", "990.00");
    ImportacaoFatura fatura = fatura("1000.00");

    assertThat(Deduplicador.detetarPagamentoPropriaFatura(List.of(pagamento), fatura)).isZero();
    assertThat(pagamento.getAlerta()).isNull();
  }

  @Test
  @DisplayName("item do CARTÃO não é 'pagamento de fatura' (só EXTRATO)")
  void itemDeCartaoNaoMarca() {
    ImportacaoItem item = item("DESPESA_PAGA", "2026-11-03", "1000.00", "Outros");
    item.setOrigem("CARTAO");
    item.setDescricao("Pagamento fatura do cartão");
    ImportacaoFatura fatura = fatura("1000.00");

    assertThat(Deduplicador.detetarPagamentoPropriaFatura(List.of(item), fatura)).isZero();
  }

  @Test
  @DisplayName("descrição sem 'fatura'/'cartão' não marca")
  void descricaoSemFaturaNaoMarca() {
    ImportacaoItem item = itemExtrato("Transferência para João", "1000.00");
    ImportacaoFatura fatura = fatura("1000.00");

    assertThat(Deduplicador.detetarPagamentoPropriaFatura(List.of(item), fatura)).isZero();
  }

  @Test
  @DisplayName("alerta existente do item é preservado ao marcar")
  void alertaExistenteEhPreservado() {
    ImportacaoItem item = itemExtrato("Pagamento fatura do cartão", "1000.00");
    item.setAlerta("REVISAR_CATEGORIA: 'X' → Outros");
    ImportacaoFatura fatura = fatura("1000.00");

    Deduplicador.detetarPagamentoPropriaFatura(List.of(item), fatura);

    assertThat(item.getAlerta()).contains("REVISAR_CATEGORIA").contains("FATURA_NO_EXTRATO");
  }

  @Test
  @DisplayName("nenhum double/float na comparação de fatura (BigDecimal puro)")
  void comparacaoBigDecimal() {
    assertThat(Deduplicador.valorProximo(new BigDecimal("1000.00"), new BigDecimal("1000.04")))
        .isTrue();
    assertThat(Deduplicador.valorProximo(new BigDecimal("1000.00"), new BigDecimal("1000.05")))
        .isTrue();
    assertThat(Deduplicador.valorProximo(new BigDecimal("1000.00"), new BigDecimal("1000.06")))
        .isFalse();
  }

  /* ============ fixtures ============ */

  private static ImportacaoItem item(String tipo, String data, String valor, String categoria) {
    ImportacaoItem item = new ImportacaoItem();
    item.setTipo(tipo);
    item.setData(LocalDate.parse(data));
    item.setValor(new BigDecimal(valor));
    item.setCategoriaNome(categoria);
    item.setOrigem("EXTRATO");
    return item;
  }

  private static ImportacaoItem itemExtrato(String descricao, String valor) {
    ImportacaoItem item = new ImportacaoItem();
    item.setTipo("DESPESA_PAGA");
    item.setData(LocalDate.of(2026, 11, 10));
    item.setValor(new BigDecimal(valor));
    item.setDescricao(descricao);
    item.setCategoriaNome("Outros");
    item.setOrigem("EXTRATO");
    return item;
  }

  private static ImportacaoFatura fatura(String total) {
    ImportacaoFatura fatura = new ImportacaoFatura();
    fatura.setTotal(new BigDecimal(total));
    fatura.setDataVencimento(LocalDate.of(2026, 12, 10));
    return fatura;
  }
}
