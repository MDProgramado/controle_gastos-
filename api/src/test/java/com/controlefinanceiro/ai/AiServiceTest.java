package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

/**
 * Testes de unidade do AiService (fase 4 — IA). Testam a interpretação de intents e a persistência
 * de histórico na memória, sem depender de rede externa nem de banco de dados.
 */
class AiServiceTest {

  private AiClient aiClient;
  private AiMemoryStore memoryStore;
  private AiKnowledgeStore knowledgeStore;
  private AiContextoService contextoService;
  private AiService aiService;

  @BeforeEach
  void setup() {
    aiClient = mock(AiClient.class);
    memoryStore = new AiMemoryStore();
    knowledgeStore = new AiKnowledgeStore();
    contextoService = mock(AiContextoService.class);
    when(contextoService.montarContexto(any(), any())).thenReturn("");
    aiService = new AiService(aiClient, memoryStore, knowledgeStore, contextoService);
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
  @DisplayName("mensagem com gastei retorna lancamento com proposta")
  void mensagemComGasteiRetornaLancamentoComProposta() {
    when(aiClient.completar(any())).thenReturn("LANCAMENTO|SAIDA|45.00|2026-11-10||Almoço");

    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "gastei 45 no almoço");

    assertThat(resposta.intent()).isEqualTo("lancamento");
    assertThat(resposta.proposta()).isNotNull();
    assertThat(resposta.proposta().tipo()).isEqualTo("SAIDA");
    assertThat(resposta.proposta().valor()).isEqualByComparingTo("45.00");
    assertThat(resposta.proposta().data()).isEqualTo("2026-11-10");
    assertThat(resposta.proposta().categoriaSugerida()).isEqualTo("Almoço");
  }

  @Test
  @DisplayName("mensagem com varios itens retorna multiplas propostas")
  void mensagemComVariosItensRetornaMultiplasPropostas() {
    when(aiClient.completar(any()))
        .thenReturn(
            """
            LANCAMENTO|ENTRADA|2400.00|2026-11-12||Salário|Salário do mês
            LANCAMENTO|SAIDA|900.00|2026-11-12||Aluguel|Aluguel
            LANCAMENTO|SAIDA|100.00|2026-11-12||Energia|Luz
            LANCAMENTO|SAIDA|65.00|2026-11-12||Água|Água
            LANCAMENTO|SAIDA|600.00|2026-11-12||Alimentação|Alimentação
            """);

    AiResponse resposta =
        aiService.processar(
            "sessao-1",
            "empresa-1",
            "entrada do mes 2400 de salario, paguei 900 de aluguel 100 de luz, 65 de agua 600 de"
                + " alimentacao");

    assertThat(resposta.intent()).isEqualTo("lancamento");
    assertThat(resposta.propostas()).hasSize(5);
    assertThat(resposta.proposta()).isEqualTo(resposta.propostas().get(0));
    assertThat(resposta.propostas().get(0).tipo()).isEqualTo("ENTRADA");
    assertThat(resposta.propostas().get(0).valor()).isEqualByComparingTo("2400.00");
    assertThat(resposta.propostas().get(1).tipo()).isEqualTo("SAIDA");
    assertThat(resposta.propostas().get(1).valor()).isEqualByComparingTo("900.00");
    // categoriaId nunca é inventado pela IA
    assertThat(resposta.propostas()).allMatch(p -> p.categoriaId() == null);
  }

  @Test
  @DisplayName("propostas duplicadas do modelo sao descartadas")
  void propostasDuplicadasDoModeloSaoDescartadas() {
    when(aiClient.completar(any()))
        .thenReturn(
            """
            LANCAMENTO|SAIDA|45.00|2026-11-12||Alimentação|Almoço
            LANCAMENTO|SAIDA|45.00|2026-11-12||Alimentação|Almoço
            """);

    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "gastei 45 no almoço");

    assertThat(resposta.intent()).isEqualTo("lancamento");
    assertThat(resposta.propostas()).hasSize(1);
    assertThat(resposta.resposta()).contains("R$ 45,00");
  }

  @Test
  @DisplayName("dados reais da empresa vao no prompt enviado a IA")
  void dadosReaisDaEmpresaVaoNoPromptEnviadoIa() {
    when(contextoService.montarContexto(1L, 7L)).thenReturn("Totais por mês: novembro de 2026");
    when(aiClient.completar(any())).thenReturn("CONSULTA|R$ 1.234,56");

    aiService.processar("sessao-1", "1", "quanto gastei?");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
    verify(aiClient).completar(captor.capture());

    List<Map<String, Object>> enviados = captor.getValue();
    String system = (String) enviados.get(0).get("content");
    assertThat(system).contains("Totais por mês: novembro de 2026");
    assertThat(system).contains("DADOS REAIS DA EMPRESA");
  }

  @Test
  @DisplayName("mensagem com quanto retorna consulta com dados")
  void mensagemComQuantoRetornaConsultaComDados() {
    when(aiClient.completar(any())).thenReturn("CONSULTA|Gastou R$ 342,50 com alimentação");

    AiResponse resposta =
        aiService.processar("sessao-1", "empresa-1", "quanto gastei com alimentação");

    assertThat(resposta.intent()).isEqualTo("consulta");
    assertThat(resposta.resposta()).contains("342,50");
    assertThat(resposta.dados()).isNotNull();
    assertThat(resposta.proposta()).isNull();
  }

  @Test
  @DisplayName("mensagem sem intencao retorna esclarecimento com opcoes")
  void mensagemSemIntencaoRetornaEsclarecimentoComOpcoes() {
    when(aiClient.completar(any()))
        .thenReturn("ESCLARECIMENTO|Não entendi. Tente: X?|Opção 1|Opção 2");

    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "qualquer coisa");

    assertThat(resposta.intent()).isEqualTo("esclarecimento");
    assertThat(resposta.opcoes()).hasSize(2);
    assertThat(resposta.opcoes()).contains("Opção 1", "Opção 2");
  }

  @Test
  @DisplayName("categoriaId nulo quando IA nao tem certeza")
  void categoriaIdNuloQuandoIaNaoTemCerteza() {
    when(aiClient.completar(any())).thenReturn("LANCAMENTO|ENTRADA|100.00|2026-11-10||Salário");

    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "recebi 100 de salário");

    assertThat(resposta.intent()).isEqualTo("lancamento");
    assertThat(resposta.proposta().categoriaId()).isNull();
  }

  @Test
  @DisplayName("historico e persistido na sessao para retroalimentacao")
  void historicoEPersistidoNaSessaoParaRetroalimentacao() {
    when(aiClient.completar(any())).thenReturn("CONSULTA|Resposta do assistente");

    AiResponse primeira = aiService.processar("sessao-1", "empresa-1", "mensagem um");
    AiResponse segunda = aiService.processar("sessao-1", "empresa-1", "mensagem dois");

    List<AiMessage> historico = aiService.obterHistorico("sessao-1", "empresa-1");
    assertThat(historico).hasSize(4); // usuário + assistente × 2
    assertThat(historico.get(0).role()).isEqualTo("user");
    assertThat(historico.get(0).content()).isEqualTo("mensagem um");
    assertThat(historico.get(1).role()).isEqualTo("assistant");
    assertThat(historico.get(2).role()).isEqualTo("user");
    assertThat(historico.get(3).role()).isEqualTo("assistant");
  }

  @Test
  @DisplayName("conhecimento alimentado e recuperado")
  void conhecimentoEhAlimentadoERecuperado() {
    assertThat(aiService.temConhecimento("empresa-1")).isFalse();

    aiService.definirConhecimento(
        "empresa-1", "Categorias: Alimentação, Transporte, Supermercado.");

    assertThat(aiService.temConhecimento("empresa-1")).isTrue();
    assertThat(aiService.obterConhecimento("empresa-1"))
        .contains("Alimentação", "Transporte", "Supermercado");
  }

  @Test
  @DisplayName("mensagem vazia retorna esclarecimento")
  void mensagemVaziaRetornaEsclarecimento() {
    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "   ");

    assertThat(resposta.intent()).isEqualTo("esclarecimento");
    assertThat(resposta.resposta()).contains("Informe uma mensagem");
  }

  @Test
  @DisplayName("limpar sessao remove o historico")
  void limparSessaoRemoveHistorico() {
    when(aiClient.completar(any())).thenReturn("CONSULTA|Resposta");

    aiService.processar("sessao-1", "empresa-1", "mensagem um");
    assertThat(aiService.obterHistorico("sessao-1", "empresa-1")).hasSize(2);

    aiService.limparSessao("sessao-1", "empresa-1");
    assertThat(aiService.obterHistorico("sessao-1", "empresa-1")).isEmpty();
  }

  @Test
  @DisplayName("interpreta lancamento em resposta multiline com markdown")
  void interpretaLancamentoMultilineComMarkdown() {
    when(aiClient.completar(any()))
        .thenReturn(
            "Claro!\n```text\nLANCAMENTO|SAIDA|45,90|2026-11-12||Alimentação|Almoço\n```\n");
    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "gastei 45,90 almoço");
    assertThat(resposta.intent()).isEqualTo("lancamento");
    assertThat(resposta.proposta().valor()).isEqualByComparingTo("45.90");
  }

  @Test
  @DisplayName("interpreta consulta em resposta multiline com markdown")
  void interpretaConsultaMultilineComMarkdown() {
    when(aiClient.completar(any()))
        .thenReturn(
            "Segue o resumo:\n```text\nCONSULTA|Você gastou R$ 342,50 em alimentação.\n```");
    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "quanto gastei?");

    assertThat(resposta.intent()).isEqualTo("consulta");
    assertThat(resposta.resposta()).contains("R$ 342,50");
    assertThat(resposta.proposta()).isNull();
  }

  @Test
  @DisplayName("parseValor aceita formatos BR e US")
  void parseValorAceitaFormatos() {
    when(aiClient.completar(any()))
        .thenReturn("LANCAMENTO|SAIDA|1.234,56|2026-11-12||Mercado|Compra");
    AiResponse br = aiService.processar("s1", "empresa-1", "gastei 1.234,56");
    assertThat(br.proposta().valor()).isEqualByComparingTo("1234.56");
    when(aiClient.completar(any()))
        .thenReturn("LANCAMENTO|SAIDA|1,234.56|2026-11-12||Mercado|Compra");
    AiResponse us = aiService.processar("s2", "empresa-1", "gastei 1,234.56");
    assertThat(us.proposta().valor()).isEqualByComparingTo("1234.56");
  }

  @Test
  @DisplayName("nao persiste mensagens quando IA falha")
  void naoPersisteMensagensQuandoIaFalha() {
    when(aiClient.completar(any())).thenThrow(new RuntimeException("falha IA"));
    AiResponse resposta = aiService.processar("sessao-falha", "empresa-1", "oi");
    assertThat(resposta.intent()).isEqualTo("esclarecimento");
    assertThat(aiService.obterHistorico("sessao-falha", "empresa-1")).isEmpty();
  }
}
