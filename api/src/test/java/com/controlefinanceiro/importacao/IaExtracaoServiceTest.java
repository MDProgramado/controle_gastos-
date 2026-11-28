package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.ai.AiClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Testes de unidade de {@link IaExtracaoService} (fase 5). Mockam {@link AiClient} — nenhuma
 * requisição real à OpenRouter, nenhum custo de cota.
 */
class IaExtracaoServiceTest {

  private static final String MODELOS =
      "google/gemma-4-31b-it:free,google/gemma-4-26b-a4b-it:free,openrouter/free";

  private AiClient aiClient;
  private IaExtracaoService service;

  @BeforeEach
  void setup() {
    aiClient = mock(AiClient.class);
    service =
        new IaExtracaoService(aiClient, MODELOS, "google/gemma-4-31b-it:free", 0.0, 8000, 3, 1500L);
  }

  @Test
  @DisplayName("chama completarComCascata com temperature=0.0 e max_tokens=8000")
  void chamaComTemperatureZeroEMaxTokens8000() {
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar("{\"itens\":[]}", "m1", false, 1));

    service.extrair("texto do extrato", List.of("Alimentação"), "empresa-1");

    ArgumentCaptor<AiClient.OpcoesCompletar> captor =
        ArgumentCaptor.forClass(AiClient.OpcoesCompletar.class);
    verify(aiClient).completarComCascata(anyList(), captor.capture());

    AiClient.OpcoesCompletar opcoes = captor.getValue();
    assertThat(opcoes.temperatura()).isEqualTo(0.0);
    assertThat(opcoes.maxTokens()).isEqualTo(8000);
    assertThat(opcoes.tentativasPorModelo()).isEqualTo(3);
    assertThat(opcoes.esperaInicialMs()).isEqualTo(1500L);
    assertThat(opcoes.modelos())
        .containsExactly(
            "google/gemma-4-31b-it:free", "google/gemma-4-26b-a4b-it:free", "openrouter/free");
  }

  @Test
  @DisplayName("prompt exige apenas JSON e injeta as categorias do tenant")
  void promptExigeJsonEInjetaCategorias() {
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar("{\"itens\":[]}", "m1", false, 1));

    service.extrair("texto do extrato", List.of("Alimentação", "Transporte"), "empresa-1");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
    verify(aiClient).completarComCascata(captor.capture(), any(AiClient.OpcoesCompletar.class));

    List<Map<String, Object>> mensagens = captor.getValue();
    String system = (String) mensagens.get(0).get("content");
    String user = (String) mensagens.get(1).get("content");

    assertThat(system.toLowerCase()).contains("json").contains("markdown");
    assertThat(user)
        .contains("Alimentação")
        .contains("Transporte")
        .contains("RECEITA")
        .contains("DESPESA_PAGA")
        .contains("A_PAGAR")
        .contains("CARTAO")
        .contains("EXTRATO");
  }

  @Test
  @DisplayName("resposta com cercas de markdown ```json``` é parseada")
  void respostaComCercasDeMarkdownEhParseada() {
    String conteudo =
        """
        ```json
        {"itens":[{"data":"2026-11-10","descricao":"Almoço no restaurante","valor":"45.00",
        "tipo":"DESPESA_PAGA","categoria":"Alimentação","origem":"CARTAO",
        "parcelaAtual":1,"parcelaTotal":3}],
        "fatura":{"total":"1500.00","vencimento":"2026-12-10","mesReferencia":"2026-11-01"}}
        ```""";
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar(conteudo, "m1", false, 1));

    IaExtracaoService.ResultadoExtracao resultado =
        service.extrair("texto da fatura", List.of("Alimentação"), "empresa-1");

    assertThat(resultado.itens()).hasSize(1);
    IaExtracaoService.ItemExtraido item = resultado.itens().get(0);
    assertThat(item.data()).isEqualTo(LocalDate.of(2026, 11, 10));
    assertThat(item.descricao()).isEqualTo("Almoço no restaurante");
    assertThat(item.valor()).isEqualByComparingTo("45.00");
    assertThat(item.valor().scale()).isEqualTo(2); // NUMERIC(14,2) — sempre 2 casas
    assertThat(item.tipo()).isEqualTo("DESPESA_PAGA");
    assertThat(item.categoria()).isEqualTo("Alimentação");
    assertThat(item.origem()).isEqualTo("CARTAO");
    assertThat(item.parcelaAtual()).isEqualTo(1);
    assertThat(item.parcelaTotal()).isEqualTo(3);

    assertThat(resultado.fatura()).isNotNull();
    assertThat(resultado.fatura().total()).isEqualByComparingTo("1500.00");
    assertThat(resultado.fatura().vencimento()).isEqualTo(LocalDate.of(2026, 12, 10));
    assertThat(resultado.modeloUsado()).isEqualTo("m1");
    assertThat(resultado.degradado()).isFalse();
  }

  @Test
  @DisplayName("valor em formato brasileiro 1.234,56 é normalizado")
  void valorFormatoBrasileiroEhNormalizado() {
    String conteudo =
        """
        {"itens":[{"data":"2026-11-10","descricao":"Compra no mercado","valor":"1.234,56",
        "tipo":"DESPESA_PAGA","categoria":"Supermercado","origem":"EXTRATO"}]}""";
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar(conteudo, "m1", false, 1));

    IaExtracaoService.ResultadoExtracao resultado =
        service.extrair("texto", List.of("Supermercado"), "empresa-1");

    assertThat(resultado.itens()).hasSize(1);
    assertThat(resultado.itens().get(0).valor()).isEqualByComparingTo("1234.56");
  }

  @Test
  @DisplayName("JSON inválido na resposta vira rejeitado, não derruba o documento")
  void jsonInvalidoViraRejeitado() {
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(
            new AiClient.RespostaCompletar("Desculpe, não consegui extrair.", "m1", false, 1));

    IaExtracaoService.ResultadoExtracao resultado =
        service.extrair("texto", List.of("Alimentação"), "empresa-1");

    assertThat(resultado.itens()).isEmpty();
    assertThat(resultado.rejeitados()).hasSize(1);
    assertThat(resultado.rejeitados().get(0).motivo()).isEqualTo("JSON_INVALIDO");
  }

  @Test
  @DisplayName("itens inválidos (valor <= 0, tipo desconhecido) vão para rejeitados com motivo")
  void itensInvalidosVaoParaRejeitados() {
    String conteudo =
        """
        {"itens":[
          {"data":"2026-11-10","descricao":"Cinema","valor":"0.00","tipo":"DESPESA_PAGA",
           "categoria":"Lazer","origem":"CARTAO"},
          {"data":"2026-11-10","descricao":"Mercado","valor":"120.50","tipo":"DESPESA_PAGA",
           "categoria":"Supermercado","origem":"EXTRATO"},
          {"data":"2026-11-10","descricao":"Teste","valor":"10.00","tipo":"SAIDA",
           "categoria":"Outros","origem":"EXTRATO"}
        ]}""";
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar(conteudo, "m1", false, 1));

    IaExtracaoService.ResultadoExtracao resultado =
        service.extrair("texto", List.of("Lazer", "Supermercado"), "empresa-1");

    assertThat(resultado.itens()).hasSize(1);
    assertThat(resultado.itens().get(0).descricao()).isEqualTo("Mercado");
    assertThat(resultado.rejeitados()).hasSize(2);
    assertThat(resultado.rejeitados())
        .extracting(IaExtracaoService.ItemRejeitado::motivo)
        .containsExactlyInAnyOrder("VALOR_INVALIDO", "TIPO_INVALIDO");
  }

  @Test
  @DisplayName("texto em branco não chama a IA (protege a cota)")
  void textoEmBrancoNaoChamaIa() {
    IaExtracaoService.ResultadoExtracao resultado =
        service.extrair("   ", List.of("Alimentação"), "empresa-1");

    assertThat(resultado.itens()).isEmpty();
    verify(aiClient, times(0)).completarComCascata(anyList(), any());
  }

  @Test
  @DisplayName("texto acima de ~25k caracteres é dividido em chunks e resultados mesclados")
  void textoLongoEDivididoEmChunks() {
    StringBuilder construtor = new StringBuilder();
    while (construtor.length() < 26_000) {
      construtor.append("linha qualquer do extrato bancario\n");
    }
    String textoLongo = construtor.toString();

    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(
            new AiClient.RespostaCompletar(
                "{\"itens\":[{\"data\":\"2026-11-01\",\"descricao\":\"A\",\"valor\":\"10.00\","
                    + "\"tipo\":\"DESPESA_PAGA\",\"categoria\":\"C\",\"origem\":\"EXTRATO\"}]}",
                "m1",
                false,
                1));

    IaExtracaoService.ResultadoExtracao resultado =
        service.extrair(textoLongo, List.of("C"), "empresa-1");

    verify(aiClient, times(2)).completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class));
    assertThat(resultado.itens()).hasSize(2); // um item por chunk, mesclados
  }

  @Test
  @DisplayName("degradado e modeloUsado da cascata são propagados no resultado")
  void degradadoEModeloUsadoSaoPropagados() {
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar("{\"itens\":[]}", "m2", true, 3));

    IaExtracaoService.ResultadoExtracao resultado =
        service.extrair("texto", List.of("C"), "empresa-1");

    assertThat(resultado.degradado()).isTrue();
    assertThat(resultado.modeloUsado()).isEqualTo("m2");
  }

  @Test
  @DisplayName("3.3: extração registra o uso de tokens/custo da empresa e do usuário")
  void extracaoRegistraUso() {
    com.controlefinanceiro.ai.UsoIa uso =
        new com.controlefinanceiro.ai.UsoIa(
            "extracao", "m1", 900, 100, new java.math.BigDecimal("0.002"), 1);
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar("{\"itens\":[]}", "m1", false, 1, List.of(uso)));
    com.controlefinanceiro.ai.AiUsoService usoService =
        mock(com.controlefinanceiro.ai.AiUsoService.class);
    service.setUsoService(usoService);

    service.extrair("texto do extrato", List.of("Alimentação"), "3", 9L);

    verify(usoService).registrar(3L, 9L, List.of(uso));
  }
}
