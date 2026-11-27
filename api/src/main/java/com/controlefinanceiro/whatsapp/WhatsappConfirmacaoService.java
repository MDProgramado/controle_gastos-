package com.controlefinanceiro.whatsapp;

import com.controlefinanceiro.categoria.CategoriaService;
import com.controlefinanceiro.common.AcessoNegadoException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.lancamento.LancamentoRequest;
import com.controlefinanceiro.lancamento.LancamentoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirmação/cancelamento de pendências WhatsApp em transação curta.
 *
 * <p>Nenhum envio para a Meta deve ocorrer dentro destas transações; o chamador envia após commit.
 */
@Service
public class WhatsappConfirmacaoService {

  private final WhatsappConfirmacaoPendenteRepository confirmacaoRepository;
  private final UsuarioTelefoneRepository telefoneRepository;
  private final LancamentoService lancamentoService;
  private final CategoriaService categoriaService;

  public WhatsappConfirmacaoService(
      WhatsappConfirmacaoPendenteRepository confirmacaoRepository,
      UsuarioTelefoneRepository telefoneRepository,
      LancamentoService lancamentoService,
      CategoriaService categoriaService) {
    this.confirmacaoRepository = confirmacaoRepository;
    this.telefoneRepository = telefoneRepository;
    this.lancamentoService = lancamentoService;
    this.categoriaService = categoriaService;
  }

  @Transactional
  public WhatsappConfirmacaoResultado processarConfirmacaoTexto(
      String messageId, String from, String texto) {
    UsuarioTelefone telefone = telefoneRepository.findByNumeroE164(from).orElse(null);
    if (telefone == null) {
      return WhatsappConfirmacaoResultado.semEnvio("REJEITADA");
    }
    return processarConfirmacaoTexto(
        messageId, from, texto, telefone.getEmpresa().getId(), telefone.getUsuario().getId());
  }

  @Transactional
  public WhatsappConfirmacaoResultado processarConfirmacaoTexto(
      String messageId, String from, String texto, Long empresaId, Long usuarioId) {
    String[] partes = texto.trim().split("\\s+");
    String acao = partes[0].toLowerCase(java.util.Locale.ROOT);
    Long confirmacaoId = null;
    try {
      if (partes.length > 1) {
        confirmacaoId = Long.parseLong(partes[1]);
      }
    } catch (NumberFormatException e) {
      return WhatsappConfirmacaoResultado.semEnvio("REJEITADA");
    }
    if ("não".equals(acao) || "nao".equals(acao)) {
      return cancelarLancamento(messageId, from, confirmacaoId, empresaId, usuarioId);
    }
    return confirmarLancamento(messageId, from, confirmacaoId, empresaId, usuarioId);
  }

  @Transactional
  public WhatsappConfirmacaoResultado confirmarLancamento(
      String messageId, String from, Long confirmacaoId) {
    UsuarioTelefone telefone = telefoneRepository.findByNumeroE164(from).orElse(null);
    if (telefone == null) {
      return WhatsappConfirmacaoResultado.semEnvio("REJEITADA");
    }
    return confirmarLancamento(
        messageId,
        from,
        confirmacaoId,
        telefone.getEmpresa().getId(),
        telefone.getUsuario().getId());
  }

  @Transactional
  public WhatsappConfirmacaoResultado confirmarLancamento(
      String messageId, String from, Long confirmacaoId, Long empresaId, Long usuarioId) {
    Optional<UsuarioTelefone> telefoneOpt = telefoneRepository.findByNumeroE164(from);
    if (telefoneOpt.isEmpty()) {
      return WhatsappConfirmacaoResultado.semEnvio("REJEITADA");
    }
    UsuarioTelefone telefone = telefoneOpt.get();
    if (!identidadeValida(telefone)
        || !Objects.equals(usuarioId, telefone.getUsuario().getId())
        || !Objects.equals(empresaId, telefone.getEmpresa().getId())) {
      return WhatsappConfirmacaoResultado.comEnvio(
          "REJEITADA", "Este número não está autorizado para esta empresa/usuário.");
    }
    Long telefoneId = telefone.getId();

    Optional<WhatsappConfirmacaoPendente> pendenciaOpt =
        buscarPendencia(telefoneId, confirmacaoId, "sim", empresaId, usuarioId);
    if (pendenciaOpt.isEmpty()) {
      return resultadoBusca(telefoneId, confirmacaoId, "sim");
    }
    WhatsappConfirmacaoPendente pendencia = pendenciaOpt.get();
    Long pendenciaId = pendencia.getId();
    OffsetDateTime agora = OffsetDateTime.now();

    if (pendencia.getExpiraEm() != null && !pendencia.getExpiraEm().isAfter(agora)) {
      if (confirmacaoRepository.expirar(pendenciaId, telefoneId, agora) == 1) {
        return WhatsappConfirmacaoResultado.comEnvio(
            "EXPIRADA", "Este lançamento expirou. Envie novamente.");
      }
      return WhatsappConfirmacaoResultado.comEnvio(
          "DUPLICADA", "Este lançamento já foi processado.");
    }

    Map<String, Object> proposta = parseJson(pendencia.getProposta());
    String tipo = (String) proposta.get("tipo");
    BigDecimal valor = new BigDecimal((String) proposta.get("valor"));
    LocalDate data = LocalDate.parse((String) proposta.get("data"));
    String descricao = (String) proposta.getOrDefault("descricao", "");
    String categoriaSugerida = (String) proposta.getOrDefault("categoriaSugerida", "");

    Long categoriaId = resolverCategoria(empresaId, categoriaSugerida, tipo);
    if (categoriaId == null) {
      return WhatsappConfirmacaoResultado.comEnvio(
          "CATEGORIA_NAO_ENCONTRADA",
          "Categoria '%s' não encontrada. Informe uma categoria válida."
              .formatted(categoriaSugerida));
    }

    if (confirmacaoRepository.confirmar(pendenciaId, telefoneId, agora) == 0) {
      return WhatsappConfirmacaoResultado.comEnvio(
          "DUPLICADA", "Este lançamento já foi processado.");
    }

    LancamentoRequest request = new LancamentoRequest(categoriaId, tipo, valor, data, descricao);
    lancamentoService.criarComCanal(empresaId, usuarioId, request, "WHATSAPP", messageId);

    return WhatsappConfirmacaoResultado.comEnvio(
        "GRAVADA",
        "Lançamento gravado com sucesso: %s de %s em %s."
            .formatted(tipo, valor.toPlainString(), data));
  }

  @Transactional
  public WhatsappConfirmacaoResultado cancelarLancamento(
      String messageId, String from, Long confirmacaoId) {
    UsuarioTelefone telefone = telefoneRepository.findByNumeroE164(from).orElse(null);
    if (telefone == null) {
      return WhatsappConfirmacaoResultado.semEnvio("REJEITADA");
    }
    return cancelarLancamento(
        messageId,
        from,
        confirmacaoId,
        telefone.getEmpresa().getId(),
        telefone.getUsuario().getId());
  }

  @Transactional
  public WhatsappConfirmacaoResultado cancelarLancamento(
      String messageId, String from, Long confirmacaoId, Long empresaId, Long usuarioId) {
    Optional<UsuarioTelefone> telefoneOpt = telefoneRepository.findByNumeroE164(from);
    if (telefoneOpt.isEmpty()) {
      return WhatsappConfirmacaoResultado.semEnvio("REJEITADA");
    }
    UsuarioTelefone telefone = telefoneOpt.get();
    if (!identidadeValida(telefone)
        || !Objects.equals(usuarioId, telefone.getUsuario().getId())
        || !Objects.equals(empresaId, telefone.getEmpresa().getId())) {
      return WhatsappConfirmacaoResultado.comEnvio(
          "REJEITADA", "Este número não está autorizado para esta empresa/usuário.");
    }
    Long telefoneId = telefone.getId();

    Optional<WhatsappConfirmacaoPendente> pendenciaOpt =
        buscarPendencia(telefoneId, confirmacaoId, "não", empresaId, usuarioId);
    if (pendenciaOpt.isEmpty()) {
      return resultadoBusca(telefoneId, confirmacaoId, "não");
    }

    if (confirmacaoRepository.cancelar(pendenciaOpt.get().getId(), telefoneId) == 1) {
      return WhatsappConfirmacaoResultado.comEnvio(
          "CANCELADA", "Lançamento cancelado. Nenhum valor foi gravado.");
    }
    return WhatsappConfirmacaoResultado.comEnvio(
        "DUPLICADA", "Este lançamento já foi processado.");
  }

  private WhatsappConfirmacaoResultado resultadoBusca(
      Long telefoneId, Long confirmacaoId, String acao) {
    if (confirmacaoId != null) {
      return WhatsappConfirmacaoResultado.comEnvio(
          "REJEITADA", "Este lançamento já foi processado ou não pertence a este número.");
    }
    long pendentes =
        confirmacaoRepository.countByUsuarioTelefoneIdAndStatus(telefoneId, "PENDENTE");
    if (pendentes > 1) {
      return WhatsappConfirmacaoResultado.comEnvio(
          "REJEITADA",
          "Existe mais de um lançamento pendente. Responda " + acao + " <id> para escolher.");
    }
    return WhatsappConfirmacaoResultado.comEnvio(
        "DUPLICADA", "Este lançamento já foi processado.");
  }

  private Optional<WhatsappConfirmacaoPendente> buscarPendencia(
      Long telefoneId, Long confirmacaoId, String acao, Long empresaId, Long usuarioId) {
    if (confirmacaoId != null) {
      Optional<WhatsappConfirmacaoPendente> pendencia =
          confirmacaoRepository.findById(confirmacaoId);
      if (pendencia.isEmpty()
          || !pendenciaPertenceAoUsuario(pendencia.get(), telefoneId, empresaId, usuarioId)
          || !"PENDENTE".equals(pendencia.get().getStatus())) {
        return Optional.empty();
      }
      return pendencia;
    }

    long pendentes =
        confirmacaoRepository.countByUsuarioTelefoneIdAndStatus(telefoneId, "PENDENTE");
    if (pendentes != 1) {
      return Optional.empty();
    }
    Optional<WhatsappConfirmacaoPendente> pendencia =
        confirmacaoRepository.findFirstByUsuarioTelefoneIdAndStatusOrderByCriadoEmDesc(
            telefoneId, "PENDENTE");
    if (pendencia.isPresent()
        && !pendenciaPertenceAoUsuario(pendencia.get(), telefoneId, empresaId, usuarioId)) {
      return Optional.empty();
    }
    return pendencia;
  }

  private boolean pendenciaPertenceAoUsuario(
      WhatsappConfirmacaoPendente pendencia, Long telefoneId, Long empresaId, Long usuarioId) {
    UsuarioTelefone telefone = pendencia.getUsuarioTelefone();
    return telefone != null
        && Objects.equals(telefoneId, telefone.getId())
        && telefone.getUsuario() != null
        && Objects.equals(usuarioId, telefone.getUsuario().getId())
        && telefone.getEmpresa() != null
        && Objects.equals(empresaId, telefone.getEmpresa().getId());
  }

  private boolean identidadeValida(UsuarioTelefone telefone) {
    return telefone.getUsuario() != null
        && telefone.getUsuario().isAtivo()
        && telefone.getUsuario().getEmpresa() != null
        && telefone.getEmpresa() != null
        && Objects.equals(
            telefone.getUsuario().getEmpresa().getId(), telefone.getEmpresa().getId());
  }

  private Long resolverCategoria(Long empresaId, String nome, String tipo) {
    try {
      java.util.List<com.controlefinanceiro.categoria.CategoriaResponse> categorias =
          categoriaService.listar(empresaId);
      if (categorias == null) return null;
      for (com.controlefinanceiro.categoria.CategoriaResponse c : categorias) {
        if (c.nome().equalsIgnoreCase(nome) && c.tipo().equalsIgnoreCase(tipo)) {
          return c.id();
        }
      }
      return null;
    } catch (NaoAutorizadoException | AcessoNegadoException e) {
      return null;
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
}
