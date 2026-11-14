package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Testes de unidade de {@link SanitizadorTexto} (sem banco, sem rede). */
class SanitizadorTextoTest {

  @Test
  @DisplayName("mascara CPF")
  void mascaraCpf() {
    String resultado = SanitizadorTexto.sanitizar("CPF 123.456.789-00");
    assertThat(resultado).isEqualTo("CPF ***.***.***-**");
  }

  @Test
  @DisplayName("mascara CNPJ")
  void mascaraCnpj() {
    String resultado = SanitizadorTexto.sanitizar("CNPJ 11.222.333/0001-44");
    assertThat(resultado).isEqualTo("CNPJ **.***.***/****-**");
  }

  @Test
  @DisplayName("mascara numero de cartao mantendo quatro ultimos digitos")
  void mascaraCartaoMantendoUltimosQuatroDigitos() {
    String resultado = SanitizadorTexto.sanitizar("cartão 4532015112830366");
    assertThat(resultado).contains("0366");
    assertThat(resultado).doesNotContain("45320151128303");
  }

  @Test
  @DisplayName("texto sem dados sensíveis permanece inalterado")
  void textoSemDadosSensiveisPermaneceInalterado() {
    String entrada = "Supermercado X - compra no cartão";
    assertThat(SanitizadorTexto.sanitizar(entrada)).isEqualTo(entrada);
  }

  @Test
  @DisplayName("texto nulo retorna nulo")
  void textoNuloRetornaNulo() {
    assertThat(SanitizadorTexto.sanitizar(null)).isNull();
  }

  @Test
  @DisplayName("CPF e cartão na mesma frase são mascarados")
  void cpfECartaoNaMesmaFraseSaoMascarados() {
    String resultado =
        SanitizadorTexto.sanitizar("João CPF 999.888.777-66 cartão 1234567890123456");
    assertThat(resultado).contains("***.***.***-**");
    assertThat(resultado).contains("3456");
    assertThat(resultado).doesNotContain("999.888.777-66");
    assertThat(resultado).doesNotContain("1234567890123456");
  }
}
