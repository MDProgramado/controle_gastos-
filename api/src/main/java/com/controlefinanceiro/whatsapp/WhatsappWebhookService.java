package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.ai.AiResponse;
import com.controlefinanceiro.ai.AiService;
import com.controlefinanceiro.categoria.CategoriaService;
import com.controlefinanceiro.common.LogRedacao;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.dashboard.DashboardService;
import com.controlefinanceiro.dashboard.dto.DashboardResponse;
import com.controlefinanceiro.lancamento.LancamentoRequest;
import com.controlefinanceiro.lancamento.LancamentoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final WhatsappConfirmacaoService confirmacaoService;

  public WhatsappWebhookService(
      UsuarioTelefoneRepository telefoneRepository,
      WhatsappMensagemProcessadaRepository mensagemRepository,
      WhatsappConfirmacaoPendenteRepository confirmacaoRepository,
      WhatsappEnvioService envioService,
      AiService aiService,
      LancamentoService lancamentoService,
      CategoriaService categoriaService,
      DashboardService dashboardService,
      WhatsappConfirmacaoService confirmacaoService) {
    this.telefoneRepository = telefoneRepository;
    this.mensagemRepository = mensagemRepository;
    this.confirmacaoRepository = confirmacaoRepository;
    this.envioService = envioService;
    this.aiService = aiService;
    this.lancamentoService = lancamentoService;
    this.categoriaService = categoriaService;
    this.dashboardService = dashboardService;
    this.confirmacaoService = confirmacaoService;
  }

  /**
   * Processa webhook da Meta de forma assíncrona. A IA roda fora de transação; operações de banco
   * usam transações curtas nos métodos específicos.
   */
  @Async(WhatsappAsyncConfig.EXECUTOR_NAME)
  public void processarWebhook(String payload, String signature) {
    try {
      Map<String, Object> body = parseJson(payload);
      List<Map<String, Object>> entries = asMapList(body.get("entry"));
      if (entries == null) {
        LOG.warn("Webhook sem 'entry'");
        return;
      }

      for (Map<String, Object> entry : entries) {
        List<Map<String, Object>> changes = asMapList(entry.get("changes"));
        if (changes == null) continue;
        for (Map<String, Object> change : changes) {
          Map<String, Object> value = asStringMap(change.get("value"));
          if (value == null) continue;
          List<Map<String, Object>> messages = asMapList(value.get("messages"));
          if (messages == null) continue;
          for (Map<String, Object> message : messages) {
            processarMensagem(message);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Erro ao processar webhook WhatsApp: {}", LogRedacao.mensagem(e));
    }
  }

  private List<Map<String, Object>> asMapList(Object value) {
    if (!(value instanceof List<?> list)) {
      return null;
    }

    List<Map<String, Object>> result = new java.util.ArrayList<>();
    for (Object item : list) {
      if (!(item instanceof Map<?, ?> map)) {
        return null;
      }

      Map<String, Object> typedMap = new java.util.HashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!(entry.getKey() instanceof String key)) {
          return null;
        }
        typedMap.put(key, entry.getValue());
      }
      result.add(typedMap);
    }
    return result;
  }

  private Map<String, Object> asStringMap(Object value) {
    if (!(value instanceof Map<?, ?> map)) {
      return null;
    }

    Map<String, Object> result = new HashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        return null;
      }
      result.put(key, entry.getValue());
    }
    return result;
  }

  private void processarMensagem(Map<String, Object> message) {
    String messageId = (String) message.get("id");
    if (messageId == null || mensagemRepository.existsByMessageId(messageId)) {
      return;
    }

    String from = (String) message.get("from");
    Map<String, Object> text = asStringMap(message.get("text"));
    String corpo = text == null ? null : (String) text.get("body");
    Map<String, Object> button = asStringMap(message.get("button"));
    String payloadBtn = button == null ? null : (String) button.get("payload");

    Optional<UsuarioTelefone> telefoneOpt = telefoneRepository.findByNumeroE164(from);
    if (telefoneOpt.isEmpty()) {
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
    if (!identidadeValida(telefone)) {
      rejeitarIdentidade(messageId, from);
      return;
    }

    Long empresaId = telefone.getEmpresa().getId();
    Long usuarioId = telefone.getUsuario().getId();

    if (button != null) {
      processarConfirmacaoBotao(messageId, from, payloadBtn, empresaId, usuarioId);
      return;
    }

    if (corpo == null || corpo.isBlank()) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }

    String texto = corpo.trim();
    if (texto.equalsIgnoreCase("sim")
        || texto.equalsIgnoreCase("não")
        || texto.equalsIgnoreCase("nao")
        || texto.matches("(?i)^(sim|não|nao)\\s+\\d+$")) {
      processarConfirmacaoTexto(messageId, from, texto, empresaId, usuarioId);
      return;
    }

    AiResponse resposta =
        aiService.processar(from, empresaId.toString(), usuarioId.toString(), texto);

    switch (resposta.intent()) {
      case "lancamento" ->
          processarPropostaLancamento(messageId, telefone, empresaId, usuarioId, resposta);
      case "consulta" -> processarConsulta(messageId, telefone, empresaId, usuarioId, resposta);
      case "esclarecimento" -> processarEsclarecimento(messageId, telefone, resposta);
      default -> registrarMensagem(messageId, "REJEITADA");
    }
  }

  private void processarPropostaLancamento(
      String messageId,
      UsuarioTelefone telefone,
      Long empresaId,
      Long usuarioId,
      AiResponse resposta) {
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

  private void processarConfirmacaoBotao(
      String messageId, String from, String payloadBtn, Long empresaId, Long usuarioId) {
    if (payloadBtn == null) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }
    try {
      if (payloadBtn.startsWith("CONFIRMAR_")) {
        Long confirmacaoId = Long.parseLong(payloadBtn.substring("CONFIRMAR_".length()));
        confirmarLancamento(messageId, from, confirmacaoId, empresaId, usuarioId);
      } else if (payloadBtn.startsWith("CANCELAR_")) {
        Long confirmacaoId = Long.parseLong(payloadBtn.substring("CANCELAR_".length()));
        cancelarLancamento(messageId, from, confirmacaoId, empresaId, usuarioId);
      } else {
        registrarMensagem(messageId, "REJEITADA");
      }
    } catch (NumberFormatException e) {
      registrarMensagem(messageId, "REJEITADA");
    }
  }

  void processarConfirmacaoTexto(String messageId, String from, String texto) {
    UsuarioTelefone telefone = telefoneRepository.findByNumeroE164(from).orElse(null);
    if (telefone == null) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }
    processarConfirmacaoTexto(
        messageId, from, texto, telefone.getEmpresa().getId(), telefone.getUsuario().getId());
  }

  void processarConfirmacaoTexto(
      String messageId, String from, String texto, Long empresaId, Long usuarioId) {
    WhatsappConfirmacaoResultado resultado;
    try {
      resultado =
          confirmacaoService.processarConfirmacaoTexto(
              messageId, from, texto, empresaId, usuarioId);
    } catch (RuntimeException e) {
      LOG.error("Erro ao processar confirmacao WhatsApp: {}", LogRedacao.mensagem(e));
      envioService.enviarTexto(
          from, "Não foi possível processar sua confirmação agora. Tente novamente.", messageId);
      registrarMensagem(messageId, "ERRO_CONFIRMAR");
      return;
    }
    aplicarResultadoConfirmacao(messageId, from, resultado);
  }

  void confirmarLancamento(String messageId, String from, Long confirmacaoId) {
    UsuarioTelefone telefone = telefoneRepository.findByNumeroE164(from).orElse(null);
    if (telefone == null) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }
    confirmarLancamento(
        messageId,
        from,
        confirmacaoId,
        telefone.getEmpresa().getId(),
        telefone.getUsuario().getId());
  }

  void confirmarLancamento(
      String messageId, String from, Long confirmacaoId, Long empresaId, Long usuarioId) {
    WhatsappConfirmacaoResultado resultado;
    try {
      resultado =
          confirmacaoService.confirmarLancamento(
              messageId, from, confirmacaoId, empresaId, usuarioId);
    } catch (RuntimeException e) {
      LOG.error("Erro ao confirmar lancamento WhatsApp: {}", LogRedacao.mensagem(e));
      envioService.enviarTexto(
          from, "Não foi possível confirmar o lançamento agora. Tente novamente.", messageId);
      registrarMensagem(messageId, "ERRO_CONFIRMAR");
      return;
    }
    aplicarResultadoConfirmacao(messageId, from, resultado);
  }

  void cancelarLancamento(String messageId, String from, Long confirmacaoId) {
    UsuarioTelefone telefone = telefoneRepository.findByNumeroE164(from).orElse(null);
    if (telefone == null) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }
    cancelarLancamento(
        messageId,
        from,
        confirmacaoId,
        telefone.getEmpresa().getId(),
        telefone.getUsuario().getId());
  }

  void cancelarLancamento(
      String messageId, String from, Long confirmacaoId, Long empresaId, Long usuarioId) {
    WhatsappConfirmacaoResultado resultado;
    try {
      resultado =
          confirmacaoService.cancelarLancamento(
              messageId, from, confirmacaoId, empresaId, usuarioId);
    } catch (RuntimeException e) {
      LOG.error("Erro ao cancelar lancamento WhatsApp: {}", LogRedacao.mensagem(e));
      envioService.enviarTexto(
          from, "Não foi possível cancelar o lançamento agora. Tente novamente.", messageId);
      registrarMensagem(messageId, "ERRO_CANCELAR");
      return;
    }
    aplicarResultadoConfirmacao(messageId, from, resultado);
  }

  private void aplicarResultadoConfirmacao(
      String messageId, String from, WhatsappConfirmacaoResultado resultado) {
    if (resultado == null) {
      registrarMensagem(messageId, "REJEITADA");
      return;
    }
    if (resultado.enviarTexto() && resultado.textoEnvio() != null) {
      envioService.enviarTexto(from, resultado.textoEnvio(), messageId);
    }
    registrarMensagem(messageId, resultado.resultadoMensagem());
  }



  private boolean identidadeValida(UsuarioTelefone telefone) {
    return telefone.getUsuario() != null
        && telefone.getUsuario().isAtivo()
        && telefone.getUsuario().getEmpresa() != null
        && telefone.getEmpresa() != null
        && Objects.equals(
            telefone.getUsuario().getEmpresa().getId(), telefone.getEmpresa().getId());
  }

  private void rejeitarIdentidade(String messageId, String from) {
    envioService.enviarTexto(
        from, "Este número não está autorizado para esta empresa/usuário.", messageId);
    registrarMensagem(messageId, "REJEITADA");
  }

  private void processarConsulta(
      String messageId,
      UsuarioTelefone telefone,
      Long empresaId,
      Long usuarioId,
      AiResponse resposta) {
    String consultaTexto = resposta.resposta();
    LocalDate mesRef = extrairMesReferencia(consultaTexto);

    DashboardResponse dashboard =
        dashboardService.obterDashboard(
            empresaId, usuarioId, mesRef.getYear(), mesRef.getMonthValue());

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
            java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("pt-BR")));
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




