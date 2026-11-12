package com.controlefinanceiro.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Serviço de IA (fase 4 — OpenRouter + Gemini).
 *
 * <p>Responsabilidades: 1. Montar o contexto da conversa: system prompt + conhecimento + histórico
 * da sessão + mensagem do usuário. 2. Chamar a OpenRouter e obter a resposta. 3. Interpretar a
 * resposta para extrair o intent (consulta/lancamento/esclarecimento). 4. Persistir a conversa na
 * memória (AiMemoryStore) para retroalimentação. 5. Manter o repositório de conhecimento
 * (AiKnowledgeStore) alimentável.
 */
@Service
public class AiService {

  private static final int MAX_MENSAGENS_HISTORICO = 20;

  private final AiClient aiClient;
  private final AiMemoryStore memoryStore;
  private final AiKnowledgeStore knowledgeStore;

  public AiService(AiClient aiClient, AiMemoryStore memoryStore, AiKnowledgeStore knowledgeStore) {
    this.aiClient = aiClient;
    this.memoryStore = memoryStore;
    this.knowledgeStore = knowledgeStore;
  }

  /**
   * Processa uma mensagem do usuário.
   *
   * <p>Fluxo:
   *
   * <ol>
   *   <li>Obtém (ou cria) a sessão da empresa/usuário.
   *   <li>Adiciona a mensagem do usuário ao histórico.
   *   <li>Monta a lista de mensagens para a OpenRouter (sistema + conhecimento + histórico truncado
   *       + usuário).
   *   <li>Chama a OpenRouter e obtém o texto da IA.
   *   <li>Interpreta o texto para extrair o intent.
   *   <li>Adiciona a resposta da IA ao histórico.
   *   <li>Retorna a resposta estruturada.
   * </ol>
   *
   * @param sessionId identificador da sessão de chat (ex.: ID do usuário ou cookie)
   * @param empresaId tenant extraído do JWT (R3 — nunca vem do body)
   * @param mensagem texto digitado pelo usuário
   * @return resposta estruturada da IA
   */
  public AiResponse processar(String sessionId, String empresaId, String mensagem) {
    if (mensagem == null || mensagem.isBlank()) {
      return new AiResponse("esclarecimento", "Informe uma mensagem.", List.of(), null, List.of());
    }

    // 1. Obter sessão (memória de curto prazo)
    AiSession sessao = memoryStore.obterOuCriar(sessionId, empresaId);

    // 2. Adicionar mensagem do usuário ao histórico
    sessao.adicionar(new AiMessage("user", mensagem));

    // 3. Montar mensagens para a OpenRouter
    List<Map<String, Object>> mensagens = montarMensagens(sessao, mensagem);

    // 4. Chamar a OpenRouter
    String respostaTexto = aiClient.completar(mensagens);

    // 5. Interpretar a resposta para extrair o intent
    AiResponse resposta = interpretar(respostaTexto);

    // 6. Adicionar a resposta da IA ao histórico (retroalimentação)
    sessao.adicionar(new AiMessage("assistant", resposta.resposta()));

    return resposta;
  }

  /**
   * Monta a lista de mensagens no formato da OpenAI/OpenRouter: system (regras + conhecimento) +
   * histórico truncado + usuário.
   */
  private List<Map<String, Object>> montarMensagens(AiSession sessao, String mensagemUsuario) {
    List<Map<String, Object>> mensagens = new ArrayList<>();

    // System prompt com regras e conhecimento da empresa
    String systemPrompt =
        """
      Você é um assistente financeiro de uma empresa. Responda em português do Brasil.

      Regras de ouro:
      - Nunca invente valores financeiros. Se não souber, diga que não tem a informação.
      - Nunca confirme ou grave lançamentos automaticamente. Apenas proponha; o usuário confirma.
      - Se o usuário pedir para criar um lançamento, responda com o formato:
        LANCAMENTO|tipo|valor|data|categoriaId|categoriaSugerida|descricao
        onde tipo é ENTRADA ou SAIDA, valor é um número positivo, data é YYYY-MM-DD,
        categoriaId pode ser vazio se não tiver certeza, categoriaSugerida é um nome de categoria.
      - Se o usuário pedir uma consulta (quanto gastou, total, etc.), responda com:
        CONSULTA|texto da resposta
      - Se não souber ou precisar de esclarecimento, responda com:
        ESCLARECIMENTO|texto da resposta|opcao1|opcao2|...

      Conhecimento da empresa (definido pelo administrador):
      %s
      """
            .formatted(knowledgeStore.obterConhecimento());

    mensagens.add(Map.of("role", "system", "content", systemPrompt));

    // Histórico de conversa (últimas N mensagens para não estourar o contexto)
    List<AiMessage> historico = sessao.mensagens();
    int inicio = Math.max(0, historico.size() - MAX_MENSAGENS_HISTORICO);
    for (int i = inicio; i < historico.size(); i++) {
      AiMessage m = historico.get(i);
      mensagens.add(Map.of("role", m.role(), "content", m.content()));
    }

    // Se a última mensagem do histórico não for a do usuário (deve ser, mas garantimos),
    // adicionamos a mensagem atual
    mensagens.add(Map.of("role", "user", "content", mensagemUsuario));

    return mensagens;
  }

  /**
   * Interpreta o texto bruto da IA e converte para AiResponse. Formatos esperados:
   * LANCAMENTO|tipo|valor|data|categoriaId|categoriaSugerida|descricao CONSULTA|texto
   * ESCLARECIMENTO|texto|opcao1|opcao2|...
   */
  private AiResponse interpretar(String texto) {
    if (texto == null || texto.isBlank()) {
      return new AiResponse(
          "esclarecimento",
          "Não entendi. Pode repetir?",
          List.of(),
          null,
          List.of("Tentar novamente", "Falar com atendente"));
    }

    String t = texto.trim();

    if (t.startsWith("LANCAMENTO|")) {
      return interpretarLancamento(t);
    }
    if (t.startsWith("CONSULTA|")) {
      String corpo = t.substring("CONSULTA|".length()).trim();
      return new AiResponse("consulta", corpo, List.of(), null, List.of());
    }
    if (t.startsWith("ESCLARECIMENTO|")) {
      return interpretarEsclarecimento(t);
    }

    // Fallback: trata como esclarecimento com o texto bruto
    return new AiResponse("esclarecimento", t, List.of(), null, List.of());
  }

  private AiResponse interpretarLancamento(String t) {
    String[] partes = t.split("\\|", -1);
    // Esperado: LANCAMENTO|tipo|valor|data|categoriaId|categoriaSugerida|descricao
    String tipo = partes.length > 1 ? partes[1].trim() : "SAIDA";
    double valor = parseValor(partes.length > 2 ? partes[2].trim() : "0");
    String data = partes.length > 3 ? partes[3].trim() : "";
    String categoriaId = partes.length > 4 ? partes[4].trim() : "";
    if (categoriaId.isBlank()) categoriaId = null;
    String categoriaSugerida = partes.length > 5 ? partes[5].trim() : "";
    if (categoriaSugerida.isBlank()) categoriaSugerida = null;
    String descricao = partes.length > 6 ? partes[6].trim() : "";

    AiResponse.AiPropostaLancamento proposta =
        new AiResponse.AiPropostaLancamento(
            tipo, valor, data, categoriaId, categoriaSugerida, descricao);

    String resumo =
        String.format(
            "Entendi este lançamento: %s de R$ %.2f em %s.%s",
            tipo.equals("ENTRADA") ? "Entrada" : "Saída",
            valor,
            data,
            categoriaSugerida != null ? " Categoria sugerida: " + categoriaSugerida : "");

    return new AiResponse("lancamento", resumo, List.of(), proposta, List.of());
  }

  private AiResponse interpretarEsclarecimento(String t) {
    String[] partes = t.split("\\|", -1);
    String corpo = partes.length > 1 ? partes[1].trim() : "Não entendi. Pode explicar melhor?";
    List<String> opcoes = new ArrayList<>();
    for (int i = 2; i < partes.length; i++) {
      String opcao = partes[i].trim();
      if (!opcao.isBlank()) {
        opcoes.add(opcao);
      }
    }
    if (opcoes.isEmpty()) {
      opcoes.add("Tentar novamente");
    }
    return new AiResponse("esclarecimento", corpo, List.of(), null, opcoes);
  }

  private double parseValor(String s) {
    try {
      return Double.parseDouble(s.replace(",", ".").replaceAll("[^0-9.-]", ""));
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  /* ========================================================================
  API de conhecimento (alimentável pelo administrador)
  ======================================================================== */

  public String obterConhecimento() {
    return knowledgeStore.obterConhecimento();
  }

  public void definirConhecimento(String texto) {
    knowledgeStore.definirConhecimento(texto);
  }

  public boolean temConhecimento() {
    return knowledgeStore.temConhecimento();
  }

  /* ========================================================================
  API de memória (útil para depuração e admin)
  ======================================================================== */

  public int totalSessoes() {
    return memoryStore.totalSessoes();
  }

  public List<AiMessage> obterHistorico(String sessionId) {
    return memoryStore.obterHistorico(sessionId);
  }

  public void limparSessao(String sessionId) {
    memoryStore.limpar(sessionId);
  }
}
