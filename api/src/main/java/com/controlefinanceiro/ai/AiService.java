package com.controlefinanceiro.ai;

import com.controlefinanceiro.common.UsuarioContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Serviço de IA (fase 4 — OpenRouter + Gemma 4).
 *
 * <p>Responsabilidades: 1. Montar o contexto da conversa: system prompt + conhecimento + histórico
 * da sessão + mensagem do usuário. 2. Chamar a OpenRouter e obter a resposta. 3. Interpretar a
 * resposta para extrair o intent (inclusive VÁRIOS lançamentos numa mesma mensagem). 4. Persistir a
 * conversa na memória (AiMemoryStore) para retroalimentação. 5. Manter o repositório de
 * conhecimento (AiKnowledgeStore) alimentável.
 *
 * <p>Valores monetários usam {@link BigDecimal} com escala 2 e {@link RoundingMode#HALF_EVEN} (R1).
 */
@Service
public class AiService {

  private static final int MAX_MENSAGENS_HISTORICO = 20;
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

  private final AiClient aiClient;
  private final AiMemoryStore memoryStore;
  private final AiKnowledgeStore knowledgeStore;
  private final AiContextoService contextoService;

  public AiService(
      AiClient aiClient,
      AiMemoryStore memoryStore,
      AiKnowledgeStore knowledgeStore,
      AiContextoService contextoService) {
    this.aiClient = aiClient;
    this.memoryStore = memoryStore;
    this.knowledgeStore = knowledgeStore;
    this.contextoService = contextoService;
  }

  /**
   * Processa uma mensagem do usuário.
   *
   * @param sessionId identificador da sessão de chat (ex.: ID do usuário ou cookie)
   * @param empresaId tenant extraído do JWT (R3 — nunca vem do body)
   * @param mensagem texto digitado pelo usuário
   * @return resposta estruturada da IA
   */
  public AiResponse processar(String sessionId, String empresaId, String mensagem) {
    return processar(sessionId, empresaId, UsuarioContext.getUsuarioId().toString(), mensagem);
  }

  public AiResponse processar(
      String sessionId, String empresaId, String usuarioId, String mensagem) {
    if (mensagem == null || mensagem.isBlank()) {
      return new AiResponse(
          "esclarecimento", "Informe uma mensagem.", List.of(), null, List.of(), List.of());
    }

    memoryStore.adicionarMensagem(sessionId, empresaId, usuarioId, new AiMessage("user", mensagem));

    List<AiMessage> historico = memoryStore.obterHistorico(sessionId, empresaId, usuarioId);
    List<Map<String, Object>> mensagens =
        montarMensagens(historico, mensagem, empresaId, usuarioId);

    String respostaTexto = aiClient.completar(mensagens);
    AiResponse resposta = interpretar(respostaTexto);

    memoryStore.adicionarMensagem(
        sessionId, empresaId, usuarioId, new AiMessage("assistant", resposta.resposta()));

    return resposta;
  }

  /**
   * Monta a lista de mensagens no formato da OpenAI/OpenRouter: system (regras + dados reais +
   * conhecimento) + histórico truncado + usuário.
   *
   * @param empresaId tenant extraído do JWT (R3) — usado para montar os dados reais
   */
  private List<Map<String, Object>> montarMensagens(
      List<AiMessage> historico, String mensagemUsuario, String empresaId, String usuarioId) {
    List<Map<String, Object>> mensagens = new ArrayList<>();

    String hoje = LocalDate.now(FUSO).toString();
    String dadosReais = montarDadosReais(empresaId, usuarioId);

    String systemPrompt =
        """
      Você é um assistente financeiro de uma empresa. Responda em português do Brasil.
      Data de hoje: %s (America/Sao_Paulo). Use sempre o ano corrente correto quando a data
      não for informada.

      Regras de ouro:
      - Nunca invente valores financeiros. Para consultas, use EXATAMENTE os DADOS REAIS da
        empresa informados abaixo. Se o período perguntado não estiver nos dados, diga isso e
        pergunte qual período consultar.
      - Nunca confirme ou grave lançamentos automaticamente. Apenas proponha; o usuário confirma.
      - Se o usuário relatar UM OU MAIS lançamentos, responda com UMA LINHA POR LANÇAMENTO,
        sem texto extra, no formato:
        LANCAMENTO|tipo|valor|data|categoriaId|categoriaSugerida|descricao
        onde:
          tipo = ENTRADA ou SAIDA
          valor = número positivo (use ponto como separador decimal, ex.: 2400.00)
          data = YYYY-MM-DD (se não informada, use a data de hoje)
          categoriaId = deixe SEMPRE vazio (você não tem acesso aos IDs reais do banco)
          categoriaSugerida = nome da categoria (ex.: Salário, Aluguel, Energia, Água, Alimentação)
          descricao = texto curto do lançamento
        Exemplo com vários itens:
        LANCAMENTO|ENTRADA|2400.00|%s||Salário|Salário do mês
        LANCAMENTO|SAIDA|900.00|%s||Aluguel|Aluguel
      - Se o usuário pedir uma consulta (quanto gastou, total, saldo, etc.), responda com:
        CONSULTA|texto da resposta
        Cite os valores em reais no padrão brasileiro (ex.: R$ 1.234,56).
      - Se não souber ou precisar de esclarecimento, responda com:
        ESCLARECIMENTO|texto da resposta|opcao1|opcao2|...

      DADOS REAIS DA EMPRESA (use estes números nas consultas):
      %s

      Conhecimento da empresa (definido pelo administrador):
      %s
      """
            .formatted(hoje, hoje, hoje, dadosReais, knowledgeStore.obterConhecimento(empresaId));

    mensagens.add(Map.of("role", "system", "content", systemPrompt));

    // Histórico de conversa (últimas N mensagens para não estourar o contexto).
    // A mensagem do usuário JÁ está no histórico (adicionada no processar), por isso não a
    // repetimos aqui — senão o modelo receberia a pergunta em duplicidade.
    int inicio = Math.max(0, historico.size() - MAX_MENSAGENS_HISTORICO);
    for (int i = inicio; i < historico.size(); i++) {
      AiMessage m = historico.get(i);
      mensagens.add(Map.of("role", m.role(), "content", m.content()));
    }

    // Rede de segurança: se o histórico veio vazio, garante a mensagem do usuário.
    if (mensagens.size() == 1) {
      mensagens.add(Map.of("role", "user", "content", mensagemUsuario));
    }

    return mensagens;
  }

  /**
   * Monta o bloco de dados reais da empresa. Se a empresa ainda não tiver lançamentos, devolve um
   * aviso para a IA não inventar números.
   */
  private String montarDadosReais(String empresaId, String usuarioId) {
    try {
      String contexto =
          contextoService.montarContexto(Long.valueOf(empresaId), Long.valueOf(usuarioId));
      if (contexto == null || contexto.isBlank()) {
        return "(a empresa ainda não possui lançamentos registrados)";
      }
      return contexto;
    } catch (RuntimeException e) {
      // Falha ao ler os dados não pode derrubar o chat: a IA_AVISA que não tem os números.
      return "(não foi possível carregar os dados da empresa agora)";
    }
  }

  /**
   * Interpreta o texto bruto da IA. Aceita várias linhas: cada linha {@code LANCAMENTO|...} vira
   * uma proposta; uma linha {@code CONSULTA|...} ou {@code ESCLARECIMENTO|...} define os demais
   * intents.
   */
  private AiResponse interpretar(String texto) {
    if (texto == null || texto.isBlank()) {
      return new AiResponse(
          "esclarecimento",
          "Não entendi. Pode repetir?",
          List.of(),
          null,
          List.of(),
          List.of("Tentar novamente", "Falar com atendente"));
    }

    String t = texto.trim();
    List<AiResponse.AiPropostaLancamento> propostas = new ArrayList<>();
    String consultaCorpo = null;
    String esclarecimentoLinha = null;

    for (String linha : t.split("\\R")) {
      String l = linha.trim();
      if (l.isEmpty()) {
        continue;
      }
      if (l.startsWith("LANCAMENTO|")) {
        AiResponse.AiPropostaLancamento p = parseLancamento(l);
        if (p != null && !contemProposta(propostas, p)) {
          propostas.add(p);
        }
      } else if (l.startsWith("CONSULTA|") && consultaCorpo == null) {
        consultaCorpo = l.substring("CONSULTA|".length()).trim();
      } else if (l.startsWith("ESCLARECIMENTO|") && esclarecimentoLinha == null) {
        esclarecimentoLinha = l;
      }
    }

    if (!propostas.isEmpty()) {
      return montarRespostaLancamento(propostas);
    }
    if (consultaCorpo != null) {
      return new AiResponse("consulta", consultaCorpo, List.of(), null, List.of(), List.of());
    }
    if (esclarecimentoLinha != null) {
      return interpretarEsclarecimento(esclarecimentoLinha);
    }

    // Fallback: trata como esclarecimento com o texto bruto
    return new AiResponse("esclarecimento", t, List.of(), null, List.of(), List.of());
  }

  /**
   * Remove propostas duplicadas: o modelo às vezes repete a mesma linha LANCAMENTO (por exemplo ao
   * repetir o exemplo do prompt). Duas propostas são iguais quando tipo, valor, data e categoria
   * sugerida coincidem.
   */
  private boolean contemProposta(
      List<AiResponse.AiPropostaLancamento> propostas, AiResponse.AiPropostaLancamento candidata) {
    for (AiResponse.AiPropostaLancamento p : propostas) {
      if (p.tipo().equals(candidata.tipo())
          && p.valor().compareTo(candidata.valor()) == 0
          && p.data().equals(candidata.data())
          && java.util.Objects.equals(p.categoriaSugerida(), candidata.categoriaSugerida())
          && java.util.Objects.equals(p.descricao(), candidata.descricao())) {
        return true;
      }
    }
    return false;
  }

  private AiResponse.AiPropostaLancamento parseLancamento(String linha) {
    String[] partes = linha.split("\\|", -1);
    if (partes.length < 4) {
      return null;
    }

    String tipo = partes[1].trim().toUpperCase();
    if (!tipo.equals("ENTRADA") && !tipo.equals("SAIDA")) {
      return null;
    }

    BigDecimal valor = parseValor(partes[2]);
    if (valor.signum() <= 0) {
      return null;
    }

    String data = partes[3].trim();
    if (!data.matches("\\d{4}-\\d{2}-\\d{2}")) {
      data = LocalDate.now(FUSO).toString();
    }

    // categoriaId sempre nulo: a IA não tem acesso aos IDs reais do banco (evita id inventado).
    String categoriaSugerida = partes.length > 5 ? partes[5].trim() : "";
    if (categoriaSugerida.isBlank()) {
      categoriaSugerida = null;
    }
    String descricao = partes.length > 6 ? partes[6].trim() : "";
    if (descricao.isBlank()) {
      descricao = null;
    }

    return new AiResponse.AiPropostaLancamento(
        tipo, valor, data, null, categoriaSugerida, descricao);
  }

  private AiResponse montarRespostaLancamento(List<AiResponse.AiPropostaLancamento> propostas) {
    AiResponse.AiPropostaLancamento primeira = propostas.get(0);
    String resumo;

    if (propostas.size() == 1) {
      resumo =
          String.format(
              "Entendi este lançamento: %s de %s em %s.%s",
              primeira.tipo().equals("ENTRADA") ? "Entrada" : "Saída",
              formatarMoeda(primeira.valor()),
              primeira.data(),
              primeira.categoriaSugerida() != null
                  ? " Categoria sugerida: " + primeira.categoriaSugerida()
                  : "");
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("Entendi ").append(propostas.size()).append(" lançamentos:");
      for (AiResponse.AiPropostaLancamento p : propostas) {
        String nome =
            p.categoriaSugerida() != null
                ? p.categoriaSugerida()
                : (p.descricao() != null ? p.descricao() : "sem categoria");
        sb.append(
            String.format(
                " %s %s (%s);",
                p.tipo().equals("ENTRADA") ? "Entrada" : "Saída", formatarMoeda(p.valor()), nome));
      }
      resumo = sb.toString();
    }

    return new AiResponse("lancamento", resumo, List.of(), primeira, propostas, List.of());
  }

  /** Formata o valor no padrão brasileiro (R$ 1.234,56) a partir do BigDecimal (R1). */
  private String formatarMoeda(BigDecimal valor) {
    java.text.DecimalFormat formato =
        new java.text.DecimalFormat(
            "#,##0.00",
            java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("pt-BR")));
    return "R$ " + formato.format(valor);
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
    return new AiResponse("esclarecimento", corpo, List.of(), null, List.of(), opcoes);
  }

  /** Converte o texto em BigDecimal com escala 2 e HALF_EVEN (R1 — sem double/float). */
  private BigDecimal parseValor(String s) {
    if (s == null) {
      return ZERO;
    }
    String limpo = s.trim().replace(",", ".").replaceAll("[^0-9.]", "");
    if (limpo.isBlank()) {
      return ZERO;
    }
    try {
      return new BigDecimal(limpo).setScale(2, RoundingMode.HALF_EVEN);
    } catch (NumberFormatException e) {
      return ZERO;
    }
  }

  /* ========================================================================
  API de conhecimento (alimentável pelo administrador)
  ======================================================================== */

  public String obterConhecimento(String empresaId) {
    return knowledgeStore.obterConhecimento(empresaId);
  }

  public void definirConhecimento(String empresaId, String texto) {
    knowledgeStore.definirConhecimento(empresaId, texto);
  }

  public boolean temConhecimento(String empresaId) {
    return knowledgeStore.temConhecimento(empresaId);
  }

  /* ========================================================================
  API de memória (útil para depuração e admin)
  ======================================================================== */

  public int totalSessoes() {
    return memoryStore.totalSessoes();
  }

  public List<AiMessage> obterHistorico(String sessionId, String empresaId) {
    return obterHistorico(sessionId, empresaId, UsuarioContext.getUsuarioId().toString());
  }

  public List<AiMessage> obterHistorico(String sessionId, String empresaId, String usuarioId) {
    return memoryStore.obterHistorico(sessionId, empresaId, usuarioId);
  }

  public void limparSessao(String sessionId, String empresaId) {
    limparSessao(sessionId, empresaId, UsuarioContext.getUsuarioId().toString());
  }

  public void limparSessao(String sessionId, String empresaId, String usuarioId) {
    memoryStore.limpar(sessionId, empresaId, usuarioId);
  }
}
