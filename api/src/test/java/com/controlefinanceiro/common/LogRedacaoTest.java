package com.controlefinanceiro.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogRedacaoTest {

  @Test
  @DisplayName("redaciona CPF")
  void redacionaCpf() {
    assertThat(LogRedacao.paraLog("CPF 123.456.789-00")).isEqualTo("CPF ***.***.***-**");
  }

  @Test
  @DisplayName("redaciona CNPJ")
  void redacionaCnpj() {
    assertThat(LogRedacao.paraLog("CNPJ 11.222.333/0001-44")).isEqualTo("CNPJ **.***.***/****-**");
  }

  @Test
  @DisplayName("redaciona cartao mantendo quatro ultimos digitos")
  void redacionaCartao() {
    assertThat(LogRedacao.paraLog("cartão 4532015112830366"))
        .contains("0366")
        .doesNotContain("4532015112830366");
  }

  @Test
  @DisplayName("redaciona telefone E.164")
  void redacionaTelefoneE164() {
    assertThat(LogRedacao.paraLog("de +5511999999999 para o grupo"))
        .isEqualTo("de [REDACTED] para o grupo");
  }

  @Test
  @DisplayName("redaciona e-mail")
  void redacionaEmail() {
    assertThat(LogRedacao.paraLog("contato joao.silva@exemplo.com.br ok"))
        .isEqualTo("contato [REDACTED] ok");
  }

  @Test
  @DisplayName("redaciona JWT")
  void redacionaJwt() {
    String jwt =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    assertThat(LogRedacao.paraLog("token=" + jwt)).isEqualTo("token=[REDACTED]");
  }

  @Test
  @DisplayName("redaciona Bearer")
  void redacionaBearer() {
    assertThat(
            LogRedacao.paraLog(
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.abc123"))
        .isEqualTo("Authorization: [REDACTED]");
  }

  @Test
  @DisplayName("redaciona conviteToken em query string")
  void redacionaConviteTokenEmQueryString() {
    assertThat(LogRedacao.paraLog("https://app/exemplo?conviteToken=abc123XYZ&foo=bar"))
        .isEqualTo("https://app/exemplo?conviteToken=[REDACTED]&foo=bar");
  }

  @Test
  @DisplayName("redaciona conviteToken em par chave valor")
  void redacionaConviteTokenEmPar() {
    assertThat(LogRedacao.paraLog("{\"conviteToken\":\"abc123XYZ\"}"))
        .isEqualTo("{\"conviteToken\":\"[REDACTED]\"}");
  }

  @Test
  @DisplayName("texto nulo retorna vazio")
  void textoNuloRetornaVazio() {
    assertThat(LogRedacao.paraLog(null)).isEmpty();
  }

  @Test
  @DisplayName("trunca para no maximo duzentos caracteres")
  void truncaParaNoMaximoDuzentosCaracteres() {
    String entrada = "A".repeat(250);

    String resultado = LogRedacao.paraLog(entrada);

    assertThat(resultado).hasSize(200).isEqualTo("A".repeat(200));
  }

  @Test
  @DisplayName("mensagem da excecao e redacionada")
  void mensagemDaExcecaoERedacionada() {
    RuntimeException ex =
        new RuntimeException(
            "falha para joao@exemplo.com com Bearer "
                + "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.abc123");

    assertThat(LogRedacao.mensagem(ex)).isEqualTo("falha para [REDACTED] com [REDACTED]");
  }

  @Test
  @DisplayName("mensagem nula de excecao retorna vazio")
  void mensagemNulaDeExcecaoRetornaVazio() {
    assertThat(LogRedacao.mensagem(new RuntimeException())).isEmpty();
  }
}
