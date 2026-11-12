package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de unidade do AiService (fase 4 — IA). Testam a interpretação de intents e a persistência
 * de histórico na memória, sem depender de rede externa nem de banco de dados.
 */
class AiServiceTest {

  private AiClient aiClient;
  private AiMemoryStore memoryStore;
  private AiKnowledgeStore knowledgeStore;
  private AiService aiService;

  @BeforeEach
  void setup() {
    aiClient = mock(AiClient.class);
    memoryStore = new AiMemoryStore();
    knowledgeStore = new AiKnowledgeStore();
    aiService = new AiService(aiClient, memoryStore, knowledgeStore);
  }

  @Test
  @DisplayName("mensagem com gastei retorna lancamento com proposta")
  void mensagemComGasteiRetornaLancamentoComProposta() {
    // RED: AiClient mockado retorna proposta de lançamento
    when(aiClient.completar(any())).thenReturn("LANCAMENTO|SAIDA|45.00|2026-11-10||Almoço");

    AiResponse resposta = aiService.processar("sessao-1", "empresa-1", "gastei 45 no almoço");

    assertThat(resposta.intent()).isEqualTo("lancamento");
    assertThat(resposta.proposta()).isNotNull();
    assertThat(resposta.proposta().tipo()).isEqualTo("SAIDA");
    assertThat(resposta.proposta().valor()).isEqualTo(45.0);
    assertThat(resposta.proposta().data()).isEqualTo("2026-11-10");
    assertThat(resposta.proposta().categoriaSugerida()).isEqualTo("Almoço");
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

    List<AiMessage> historico = aiService.obterHistorico("sessao-1");
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
    assertThat(aiService.temConhecimento()).isFalse();

    aiService.definirConhecimento("Categorias: Alimentação, Transporte, Supermercado.");

    assertThat(aiService.temConhecimento()).isTrue();
    assertThat(aiService.obterConhecimento()).contains("Alimentação", "Transporte", "Supermercado");
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
    assertThat(aiService.obterHistorico("sessao-1")).hasSize(2);

    aiService.limparSessao("sessao-1");
    assertThat(aiService.obterHistorico("sessao-1")).isEmpty();
  }

  @Test
  @DisplayName("obter conhecimento vazio quando nao foi definido")
  void obterConhecimentoVazioQuandoNaoDefinido() {
    assertThat(aiService.obterConhecimento()).isEmpty();
    assertThat(aiService.temConhecimento()).isFalse();
  }

  @Test
  @DisplayName("obter conhecimento apos definir")
  void obterConhecimentoAposDefinir() {
    aiService.definirConhecimento("Categorias: Alimentação, Transporte.");

    assertThat(aiService.temConhecimento()).isTrue();
    assertThat(aiService.obterConhecimento()).contains("Alimentação", "Transporte");
  }
}
