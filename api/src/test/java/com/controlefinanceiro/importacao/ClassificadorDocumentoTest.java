package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes de unidade de {@link ClassificadorDocumento} (sem banco, sem rede). */
class ClassificadorDocumentoTest {

  @Test
  @DisplayName("fatura de cartão é classificada como CARTAO")
  void faturaDeCartaoEhClassificadaComoCartao() {
    String resultado =
        ClassificadorDocumento.classificar(
            "fatura_nubank.pdf", "FATURA DO CARTÃO\nVencimento: 10/12/2026\nLimite: 5.000,00");
    assertThat(resultado).isEqualTo("CARTAO");
  }

  @Test
  @DisplayName("extrato bancário é classificado como EXTRATO")
  void extratoBancarioEhClassificadoComoExtrato() {
    String resultado =
        ClassificadorDocumento.classificar(
            "extrato_itau.pdf", "EXTRATO BANCÁRIO\nSaldo anterior: 1.200,00\nPIX recebido: 200,00");
    assertThat(resultado).isEqualTo("EXTRATO");
  }

  @Test
  @DisplayName("arquivo sem indícios usa padrão EXTRATO")
  void arquivoSemIndiciosUsaPadraoExtrato() {
    String resultado = ClassificadorDocumento.classificar("documento.pdf", "texto qualquer aqui");
    assertThat(resultado).isEqualTo("EXTRATO");
  }

  @Test
  @DisplayName("nulo não lança exceção")
  void nuloNaoLancaExcecao() {
    String resultado = ClassificadorDocumento.classificar(null, null);
    assertThat(resultado).isEqualTo("EXTRATO");
  }
}
