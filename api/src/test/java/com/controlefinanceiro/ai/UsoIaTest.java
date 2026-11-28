package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Item 3.3: acumulação do uso das várias chamadas de uma mesma mensagem. */
class UsoIaTest {

  @Test
  @DisplayName("consolidar soma por operação e modelo")
  void consolidarSoma() {
    List<UsoIa> consolidado =
        UsoIa.consolidar(
            List.of(
                new UsoIa("chat", "m/a", 10, 5, new BigDecimal("0.001"), 1),
                new UsoIa("chat", "m/b", 1, 1, new BigDecimal("0.0001"), 1),
                new UsoIa("chat", "m/a", 20, 7, new BigDecimal("0.002"), 1)));

    assertThat(consolidado).hasSize(2);
    UsoIa a = consolidado.stream().filter(u -> u.modelo().equals("m/a")).findFirst().orElseThrow();
    assertThat(a.tokensEntrada()).isEqualTo(30);
    assertThat(a.tokensSaida()).isEqualTo(12);
    assertThat(a.custoUsd()).isEqualByComparingTo("0.003");
    assertThat(a.chamadas()).isEqualTo(2);
  }

  @Test
  @DisplayName("AiService registra o uso acumulado da chamada inicial + chamada pós-ferramenta")
  @SuppressWarnings("unchecked")
  void aiServiceRegistraUsoAcumulado() {
    AiClient aiClient = mock(AiClient.class);
    when(aiClient.completarComUso(any(), anyLong()))
        .thenReturn(
            new AiClient.RespostaChat(
                "FERRAMENTA|listar_categorias|{}",
                List.of(new UsoIa("chat", "m/a", 100, 10, new BigDecimal("0.01"), 1))),
            new AiClient.RespostaChat(
                "CONSULTA|ok",
                List.of(new UsoIa("chat", "m/a", 150, 20, new BigDecimal("0.02"), 1))));
    com.controlefinanceiro.ai.tools.FerramentaRegistry registry =
        mock(com.controlefinanceiro.ai.tools.FerramentaRegistry.class);
    when(registry.executar(any(), any(), any()))
        .thenReturn(new com.controlefinanceiro.ai.tools.ResultadoFerramenta("{}", List.of()));
    AiContextoService contexto = mock(AiContextoService.class);
    when(contexto.montarContexto(any(), any())).thenReturn("");
    AiUsoService usoService = mock(AiUsoService.class);
    AiService service =
        new AiService(aiClient, new AiMemoryStore(), new AiKnowledgeStore(), contexto, registry);
    service.setUsoService(usoService);

    service.processar("s", "1", "7", "quais categorias?");

    ArgumentCaptor<List<UsoIa>> captor = ArgumentCaptor.forClass(List.class);
    verify(usoService).registrar(eq(1L), eq(7L), captor.capture());
    List<UsoIa> usos = UsoIa.consolidar(captor.getValue());
    assertThat(usos).hasSize(1);
    assertThat(usos.get(0).tokensEntrada()).isEqualTo(250);
    assertThat(usos.get(0).chamadas()).isEqualTo(2);
  }
}
