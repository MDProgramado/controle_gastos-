package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.ai.AiResponse;
import com.controlefinanceiro.ai.AiService;
import com.controlefinanceiro.categoria.CategoriaService;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.dashboard.DashboardService;
import com.controlefinanceiro.dashboard.dto.DashboardResponse;
import com.controlefinanceiro.lancamento.LancamentoRequest;
import com.controlefinanceiro.lancamento.LancamentoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Núcleo do webhook do WhatsApp. Chama os mesmos services dos controllers REST (LancamentoService,
 * CategoriaService, DashboardService) — não há lógica de negócio nova.
 *
 * <p>Fluxo: valida assinatura → idempotência → busca telefone → interpreta com AiService →
 * lançamento cria pendência + botões (não grava) → confirmação chama LancamentoService → consulta
 * chama DashboardService → registra message_id.
 */
@Service
public class WhatsappWebhookService {

  private static final Logger LOG = LoggerFactory.getLogger(WhatsappWebhookService.class);
  private static final int CONFIRMACAO_VALIDADE_HORAS = 24;

  private final UsuarioTelefoneRepository telefoneRepository;
  private final WhatsappMensagemProcessadaRepository mensagemRepository;
  private final WhatsappConfirmacaoPendenteRepository confirmacaoRepository;
  private final WhatsappEnvioService envioService;
  private final AiService aiService;
  private final LancamentoService lancamentoService;
  private final CategoriaService categoriaService;
  private final DashboardService dashboardService;

  public WhatsappWebhookService(
      UsuarioTelefoneRepository telefoneRepository,
      WhatsappMensagemProcessadaRepository mensagemRepository,
      WhatsappConfirmacaoPendenteRepository confirmacaoRepository,
      WhatsappEnvioService envioService,
      AiService aiService,
      LancamentoService lancamentoService,
      CategoriaService categoriaService,
      DashboardService dashboardService) {
    this.telefoneRepository = telefoneRepository;
    this.mensagemRepository = mensagemRepository;
    this.confirmacaoRepository = confirmacaoRepository;
    this.envioService = envioService;
    this.aiService = aiService;
    this.lancamentoService = lancamentoService;
    this.categoriaService = categoriaService;
    this.dashboardService = dashboardService;
  }

  /**
   * Processa um webhook da Meta. Deve ser chamado com o corpo já verificado quanto à assinatura. O
   * processamento pesado é assíncrono (@Async) para responder 200 à Meta em até 5s.
   *
   * <p>Os métodos transacionais de confirmação são chamados por self-invocation; a atomicidade
   * depende da transação deste método. Não remover @Transactional aqui sem extrair a confirmação
   * para outro bean.
   *
   * @param payload corpo JSON do webhook da Meta
   * @param signature valor do header X-Hub-Signature-256
   */
  @Async
  @Transactional
  public void processarWebhook(String payload, String signature) {
    try {
      // Parsing simplificado do payload da Meta. Em produção, usar um DTO Jackson bem tipado.
      @SuppressWarnings("unchecked")
      Map<String, Object> body = parseJson(payload);

      List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get("entry");
      if (entries == null) {
        LOG.warn("Webhook sem 'entry'");
        return;
      }

      for (Map<String, Object> entry : entries) {
        List<Map<String, Object>> changes =
            (List<Map<String, Object>>) entry.getOrDefault("changes", List.of());
        for (Map<String, Object> change : changes) {
          Map<String, Object> value = (Map<String, Object>) change.getOrDefault("value", Map.of());
          List<Map<String, Object>> messages =
              (List<Map<String, Object>>) value.getOrDefault("messages", List.of());
          if (messages == null) {
            continue;
          }
          for (Map<String, Object> message : messages) {
            processarMensagem(message);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Erro ao processar webhook WhatsApp: {}", e.getMessage());
    }
  }

  private void processarMensagem(Map<String, Object> message) {
    String messageId = (String) message.get("id");
    if (messageId == null || mensagemRepository.existsByMessageId(messageId)) {
      return; // idempotência
    }

    String from = (String) message.get("from"); // número remetente
    Map<String, Object> text = (Map<String, Object>) message.get("text");
    String corpo = text == null ? null : (String) text.get("body");

    // Verifica se é um clique em botão (confirmação/cancelamento)
    Map<String, Object> button = (Map<String, Object>) message.get("button");
    if (button != null) {
      String payloadBtn = (String) button.get("payload");
      processarConfirmacaoBotao(messageId, from, payloadBtn);
      return;
    }

    if (corpo == null || corpo.isBlank()) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }

    // Busca o telefone vinculado
    Optional<UsuarioTelefone> telefoneOpt = telefoneRepository.findByNumeroE164(from);
    if (telefoneOpt.isEmpty()) {
      // Número não cadastrado: instrução de cadastro única
      envioService.enviarInstrucaoCadastro(from, messageId);
      registrarMensagem(messageId, "NAO_CADASTRADO");
      return;
    }

    UsuarioTelefone telefone = telefoneOpt.get();
    if (!telefone.isVerificado() || !telefone.isAtivo()) {
      envioService.enviarInstrucaoCadastro(from, messageId);
      registrarMensagem(messageId, "NAO_VERIFICADO");
      return;
    }

    String texto = corpo.trim();
    if (texto.equalsIgnoreCase("sim")
        || texto.equalsIgnoreCase("não")
        || texto.equalsIgnoreCase("nao")
        || texto.matches("(?i)^(sim|não|nao)\\s+\\d+$")) {
      processarConfirmacaoTexto(messageId, from, texto);
      return;
    }

    // Interpreta com a mesma IA do chat do site
    Long empresaId = telefone.getEmpresa().getId();
    AiResponse resposta = aiService.processar(from, empresaId.toString(), corpo);

    switch (resposta.intent()) {
      case "lancamento" -> processarPropostaLancamento(messageId, telefone, empresaId, resposta);
      case "consulta" -> processarConsulta(messageId, telefone, empresaId, resposta);
      case "esclarecimento" -> processarEsclarecimento(messageId, telefone, resposta);
      default -> registrarMensagem(messageId, "REJEITADA");
    }
  }

  private void processarPropostaLancamento(
      String messageId, UsuarioTelefone telefone, Long empresaId, AiResponse resposta) {
    AiResponse.AiPropostaLancamento proposta = resposta.proposta();
    if (proposta == null || proposta.valor() == null) {
      envioService.enviarTexto(
          telefone.getNumeroE164(),
          "Valor inválido ou zero. Envie novamente com um valor válido.",
          messageId);
      registrarMensagem(messageId, "REJEITADA");
      return;
    }

    // Valida valor (mesma regra do LancamentoService: positivo, max 2 casas)
    if (proposta.valor().signum() <= 0) {
      envioService.enviarTexto(
          telefone.getNumeroE164(), "Valor deve ser positivo. Envie novamente.", messageId);
      registrarMensagem(messageId, "REJEITADA");
      return;
    }

    if (proposta.valor().scale() > 2) {
      envioService.enviarTexto(
          telefone.getNumeroE164(),
          "Valor deve ter no máximo 2 casas decimais. Envie novamente.",
          messageId);
      registrarMensagem(messageId, "REJEITADA");
      return;
    }

    // Cria pendência de confirmação (NÃO grava lançamento ainda)
    WhatsappConfirmacaoPendente pendencia = new WhatsappConfirmacaoPendente();
    pendencia.setUsuarioTelefone(telefone);
    pendencia.setProposta(
        toJson(
            Map.of(
                "tipo", proposta.tipo(),
                "valor", proposta.valor().toPlainString(),
                "data", proposta.data(),
                "categoriaSugerida", proposta.categoriaSugerida(),
                "descricao", proposta.descricao())));
    pendencia.setExpiraEm(OffsetDateTime.now().plus(CONFIRMACAO_VALIDADE_HORAS, ChronoUnit.HOURS));
    pendencia.setStatus("PENDENTE");
    pendencia = confirmacaoRepository.saveAndFlush(pendencia);

    // Envia resumo com botões Confirmar/Cancelar
    String resumo =
        String.format(
            "Entendi este lançamento: %s de %s em %s.%s Deseja confirmar? Responda sim %d ou não %d.",
            proposta.tipo().equals("ENTRADA") ? "Entrada" : "Saída",
            proposta.valor().toPlainString(),
            proposta.data(),
            proposta.categoriaSugerida() != null
                ? " Categoria sugerida: " + proposta.categoriaSugerida()
                : "",
            pendencia.getId(),
            pendencia.getId());
    envioService.enviarConfirmacao(
        telefone.getNumeroE164(),
        resumo,
        "CONFIRMAR_" + pendencia.getId(),
        "CANCELAR_" + pendencia.getId(),
        messageId);
    registrarMensagem(messageId, "CONFIRMACAO_PENDENTE");
  }

  private void processarConfirmacaoBotao(String messageId, String from, String payloadBtn) {
    if (payloadBtn == null) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }

    if (payloadBtn.startsWith("CONFIRMAR_")) {
      Long confirmacaoId = Long.parseLong(payloadBtn.substring("CONFIRMAR_".length()));
      confirmarLancamento(messageId, from, confirmacaoId);
    } else if (payloadBtn.startsWith("CANCELAR_")) {
      Long confirmacaoId = Long.parseLong(payloadBtn.substring("CANCELAR_".length()));
      cancelarLancamento(messageId, from, confirmacaoId);
    } else {
      registrarMensagem(messageId, "REJEITADA");
    }
  }

  @Transactional
  void processarConfirmacaoTexto(String messageId, String from, String texto) {
    String[] partes = texto.trim().split("\\s+");
    String acao = partes[0].toLowerCase(java.util.Locale.ROOT);
    Long confirmacaoId = partes.length > 1 ? Long.parseLong(partes[1]) : null;
    if ("não".equals(acao) || "nao".equals(acao)) {
      cancelarLancamento(messageId, from, confirmacaoId);
    } else {
      confirmarLancamento(messageId, from, confirmacaoId);
    }
  }

  @Transactional
  void confirmarLancamento(String messageId, String from, Long confirmacaoId) {
    Optional<UsuarioTelefone> telefoneOpt = telefoneRepository.findByNumeroE164(from);
    if (telefoneOpt.isEmpty()) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }
    UsuarioTelefone telefone = telefoneOpt.get();
    Long empresaId = telefone.getEmpresa().getId();
    Long usuarioId = telefone.getUsuario().getId();
    Long telefoneId = telefone.getId();
    String numeroE164 = telefone.getNumeroE164();

    Optional<WhatsappConfirmacaoPendente> pendenciaOpt =
        buscarPendencia(telefoneId, numeroE164, messageId, confirmacaoId, "sim");
    if (pendenciaOpt.isEmpty()) {
      return;
    }
    WhatsappConfirmacaoPendente pendencia = pendenciaOpt.get();
    Long pendenciaId = pendencia.getId();
    OffsetDateTime agora = OffsetDateTime.now();

    if (pendencia.getExpiraEm() != null && !pendencia.getExpiraEm().isAfter(agora)) {
      if (confirmacaoRepository.expirar(pendenciaId, telefoneId, agora) == 1) {
        envioService.enviarTexto(
            numeroE164, "Este lançamento expirou. Envie novamente.", messageId);
        registrarMensagem(messageId, "EXPIRADA");
      } else {
        envioService.enviarTexto(numeroE164, "Este lançamento já foi processado.", messageId);
        registrarMensagem(messageId, "DUPLICADA");
      }
      return;
    }

    Map<String, Object> proposta = parseJson(pendencia.getProposta());
    String tipo = (String) proposta.get("tipo");
    BigDecimal valor = new BigDecimal((String) proposta.get("valor"));
    LocalDate data = LocalDate.parse((String) proposta.get("data"));
    String descricao = (String) proposta.getOrDefault("descricao", "");
    String categoriaSugerida = (String) proposta.getOrDefault("categoriaSugerida", "");

    Long categoriaId = resolverCategoria(empresaId, categoriaSugerida, tipo);
    if (categoriaId == null) {
      envioService.enviarTexto(
          numeroE164,
          "Categoria '%s' não encontrada. Informe uma categoria válida."
              .formatted(categoriaSugerida),
          messageId);
      registrarMensagem(messageId, "CATEGORIA_NAO_ENCONTRADA");
      return;
    }

    if (confirmacaoRepository.confirmar(pendenciaId, telefoneId, agora) == 0) {
      envioService.enviarTexto(numeroE164, "Este lançamento já foi processado.", messageId);
      registrarMensagem(messageId, "DUPLICADA");
      return;
    }

    LancamentoRequest request = new LancamentoRequest(categoriaId, tipo, valor, data, descricao);
    lancamentoService.criarComCanal(empresaId, usuarioId, request, "WHATSAPP", messageId);

    envioService.enviarTexto(
        numeroE164,
        "Lançamento gravado com sucesso: %s de %s em %s."
            .formatted(tipo, valor.toPlainString(), data),
        messageId);
    registrarMensagem(messageId, "GRAVADA");
  }

  @Transactional
  void cancelarLancamento(String messageId, String from, Long confirmacaoId) {
    Optional<UsuarioTelefone> telefoneOpt = telefoneRepository.findByNumeroE164(from);
    if (telefoneOpt.isEmpty()) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }
    UsuarioTelefone telefone = telefoneOpt.get();
    Long telefoneId = telefone.getId();
    String numeroE164 = telefone.getNumeroE164();

    Optional<WhatsappConfirmacaoPendente> pendenciaOpt =
        buscarPendencia(telefoneId, numeroE164, messageId, confirmacaoId, "não");
    if (pendenciaOpt.isEmpty()) {
      return;
    }

    if (confirmacaoRepository.cancelar(pendenciaOpt.get().getId(), telefoneId) == 1) {
      envioService.enviarTexto(
          numeroE164, "Lançamento cancelado. Nenhum valor foi gravado.", messageId);
      registrarMensagem(messageId, "CANCELADA");
    } else {
      envioService.enviarTexto(numeroE164, "Este lançamento já foi processado.", messageId);
      registrarMensagem(messageId, "DUPLICADA");
    }
  }

  private Optional<WhatsappConfirmacaoPendente> buscarPendencia(
      Long telefoneId, String numeroE164, String messageId, Long confirmacaoId, String acao) {
    if (confirmacaoId != null) {
      Optional<WhatsappConfirmacaoPendente> pendencia =
          confirmacaoRepository.findById(confirmacaoId);
      if (pendencia.isEmpty()
          || !pendencia.get().getUsuarioTelefone().getId().equals(telefoneId)
          || !"PENDENTE".equals(pendencia.get().getStatus())) {
        envioService.enviarTexto(
            numeroE164,
            "Este lançamento já foi processado ou não pertence a este número.",
            messageId);
        registrarMensagem(messageId, "REJEITADA");
        return Optional.empty();
      }
      return pendencia;
    }

    long pendentes =
        confirmacaoRepository.countByUsuarioTelefoneIdAndStatus(telefoneId, "PENDENTE");
    if (pendentes > 1) {
      envioService.enviarTexto(
          numeroE164,
          "Existe mais de um lançamento pendente. Responda " + acao + " <id> para escolher.",
          messageId);
      registrarMensagem(messageId, "REJEITADA");
      return Optional.empty();
    }
    if (pendentes == 0) {
      envioService.enviarTexto(numeroE164, "Este lançamento já foi processado.", messageId);
      registrarMensagem(messageId, "DUPLICADA");
      return Optional.empty();
    }
    return confirmacaoRepository.findFirstByUsuarioTelefoneIdAndStatusOrderByCriadoEmDesc(
        telefoneId, "PENDENTE");
  }

  private void processarConsulta(
      String messageId, UsuarioTelefone telefone, Long empresaId, AiResponse resposta) {
    // Extrai mês da resposta de consulta (formato simples: mês/ano ou usa mês corrente)
    String consultaTexto = resposta.resposta();
    LocalDate mesRef = extrairMesReferencia(consultaTexto);

    DashboardResponse dashboard =
        dashboardService.obterDashboard(empresaId, mesRef.getYear(), mesRef.getMonthValue());

    String texto =
        String.format(
            "Resumo %02d/%d:\nEntradas: %s\nSaídas: %s\nSaldo: %s",
            mesRef.getMonthValue(),
            mesRef.getYear(),
            formatarMoeda(dashboard.totalEntradas()),
            formatarMoeda(dashboard.totalSaidas()),
            formatarMoeda(dashboard.saldo()));

    envioService.enviarTexto(telefone.getNumeroE164(), texto, messageId);
    registrarMensagem(messageId, "CONSULTA");
  }

  private void processarEsclarecimento(
      String messageId, UsuarioTelefone telefone, AiResponse resposta) {
    envioService.enviarTexto(telefone.getNumeroE164(), resposta.resposta(), messageId);
    registrarMensagem(messageId, "ESCLARECIMENTO");
  }

  private Long resolverCategoria(Long empresaId, String nome, String tipo) {
    // Usa CategoriaService para listar e buscar por nome sugerido
    // A IA não tem acesso aos IDs reais, então resolvemos pelo nome
    List<com.controlefinanceiro.categoria.CategoriaResponse> categorias = null;
    try {
      categorias = categoriaService.listar(empresaId);
    } catch (NaoAutorizadoException | AccessDeniedException e) {
      return null;
    }
    if (categorias == null) {
      return null;
    }
    for (com.controlefinanceiro.categoria.CategoriaResponse c : categorias) {
      if (c.nome().equalsIgnoreCase(nome) && c.tipo().equalsIgnoreCase(tipo)) {
        return c.id();
      }
    }
    // Se houver múltiplas com o mesmo nome mas tipos diferentes, já filtramos por tipo acima.
    // Se não encontrou exatamente, tenta match parcial por nome e tipo
    for (com.controlefinanceiro.categoria.CategoriaResponse c : categorias) {
      if (c.nome().equalsIgnoreCase(nome) && c.tipo().equalsIgnoreCase(tipo)) {
        return c.id();
      }
    }
    return null;
  }

  private LocalDate extrairMesReferencia(String texto) {
    // Simplificado: usa o mês corrente. Em produção, extrair "janeiro/2026" etc.
    return LocalDate.now();
  }

  private String formatarMoeda(BigDecimal valor) {
    if (valor == null) {
      return "R$ 0,00";
    }
    java.text.DecimalFormat formato =
        new java.text.DecimalFormat(
            "#,##0.00",
            java.text.DecimalFormatSymbols.getInstance(new java.util.Locale("pt", "BR")));
    return "R$ " + formato.format(valor.setScale(2, java.math.RoundingMode.HALF_EVEN));
  }

  private void registrarMensagem(String messageId, String resultado) {
    if (messageId != null && !mensagemRepository.existsByMessageId(messageId)) {
      WhatsappMensagemProcessada m = new WhatsappMensagemProcessada();
      m.setMessageId(messageId);
      m.setResultado(resultado);
      mensagemRepository.save(m);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseJson(String json) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper()
          .readValue(
              json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String toJson(Map<String, Object> map) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
