package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.ai.tools.FerramentaChamada;
import com.controlefinanceiro.ai.tools.FerramentaRegistry;
import com.controlefinanceiro.ai.tools.ResultadoFerramenta;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AiServiceFerramentaTest {

  private AiClient aiClient;
  private FerramentaRegistry registry;
  private AiService aiService;

  @BeforeEach
  void setup() {
    aiClient = mock(AiClient.class);
    registry = mock(FerramentaRegistry.class);
    AiMemoryStore memoryStore = new AiMemoryStore();
    AiKnowledgeStore knowledgeStore = new AiKnowledgeStore();
    AiContextoService contextoService = mock(AiContextoService.class);
    when(contextoService.montarContexto(any(), any())).thenReturn("");
    aiService = new AiService(aiClient, memoryStore, knowledgeStore, contextoService, registry);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("usuario@teste.com", 1L, 7L, 0, "USER"),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("chamada de ferramenta executa no registry e faz segunda chamada ao modelo")
  void chamadaDeFerramentaFazSegundaChamada() {
    when(aiClient.completar(any(), anyLong()))
        .thenReturn(
            "FERRAMENTA|consultar_base_conhecimento|{\"query\":\"teto de juros|cartão\",\"topK\":3}",
            "CONSULTA|O teto é de 100%.");
    when(registry.executar(any(FerramentaChamada.class), eq(1L), eq(7L)))
        .thenReturn(
            new ResultadoFerramenta(
                "{\"trechos\":[]}",
                List.of(
                    new AiResponse.AiCitacao(
                        "02-bcb-res4549",
                        "Resolução CMN 4.549",
                        "Banco Central do Brasil",
                        "2023-12-21",
                        "art. 2-C"))));

    AiResponse resposta = aiService.processar("sessao-tool", "1", "qual o teto de juros?");

    verify(aiClient, times(2)).completar(any(), anyLong());
    assertThat(resposta.intent()).isEqualTo("consulta");
    assertThat(resposta.resposta()).contains("100%");
    assertThat(resposta.citacoes()).hasSize(1);
    assertThat(resposta.citacoes().get(0).sourceId()).isEqualTo("02-bcb-res4549");

    ArgumentCaptor<FerramentaChamada> captor = ArgumentCaptor.forClass(FerramentaChamada.class);
    verify(registry).executar(captor.capture(), eq(1L), eq(7L));
    assertThat(captor.getValue().argsJson()).contains("teto de juros|cartão");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Map<String, Object>>> mensagensCaptor = ArgumentCaptor.forClass(List.class);
    verify(aiClient, times(2)).completar(mensagensCaptor.capture(), anyLong());
    String ultima =
        (String)
            mensagensCaptor.getValue().get(mensagensCaptor.getValue().size() - 1).get("content");
    assertThat(ultima).contains("RESULTADO_DA_FERRAMENTA");
    assertThat(ultima).contains("art. 2-C");
  }

  @Test
  @DisplayName("loop de ferramenta excedido retorna esclarecimento amigavel sem erro cru")
  void loopFerramentaExcedidoRetornaEsclarecimento() {
    when(aiClient.completar(any(), anyLong()))
        .thenReturn(
            "FERRAMENTA|consultar_base_conhecimento|{\"query\":\"a\"}",
            "FERRAMENTA|consultar_base_conhecimento|{\"query\":\"b\"}",
            "FERRAMENTA|consultar_base_conhecimento|{\"query\":\"c\"}");
    when(registry.executar(any(FerramentaChamada.class), eq(1L), eq(7L)))
        .thenReturn(new ResultadoFerramenta("{\"trechos\":[]}", List.of()));

    AiResponse resposta = aiService.processar("sessao-loop", "1", "consulta complexa");

    assertThat(resposta.intent()).isEqualTo("esclarecimento");
    assertThat(resposta.resposta()).doesNotContain("Exception").contains("reformular");
    assertThat(resposta.opcoes()).contains("Falar com atendente");
  }
}
