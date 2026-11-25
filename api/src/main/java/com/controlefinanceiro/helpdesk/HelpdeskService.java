package com.controlefinanceiro.helpdesk;

import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.TicketNaoEncontradoException;
import com.controlefinanceiro.common.UsuarioContext;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HelpdeskService {

  private static final int TAMANHO_MAX_ASSUNTO = 120;
  private static final int TAMANHO_MAX_TEXTO = 5000;

  private final HelpdeskTicketRepository ticketRepository;
  private final HelpdeskMensagemRepository mensagemRepository;

  public HelpdeskService(
      HelpdeskTicketRepository ticketRepository, HelpdeskMensagemRepository mensagemRepository) {
    this.ticketRepository = ticketRepository;
    this.mensagemRepository = mensagemRepository;
  }

  @Transactional
  public TicketResponse criarTicket(TicketRequest request) {
    validarTexto(request.assunto(), "assunto", TAMANHO_MAX_ASSUNTO);
    validarTexto(request.descricao(), "descricao", TAMANHO_MAX_TEXTO);

    HelpdeskTicket ticket = new HelpdeskTicket();
    ticket.setEmpresa(referenciaEmpresa());
    ticket.setUsuario(referenciaUsuario(UsuarioContext.getUsuarioId()));
    ticket.setAssunto(request.assunto().trim());
    ticket.setDescricao(request.descricao().trim());
    ticket.setStatus(TicketStatus.ABERTO);
    return paraResponse(ticketRepository.save(ticket));
  }

  @Transactional(readOnly = true)
  public TicketPage listarDoUsuario(TicketStatus status, Pageable pageable) {
    Page<HelpdeskTicket> pagina =
        ticketRepository.findByEmpresaIdAndUsuarioIdAndStatus(
            TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), status, pageable);
    return paraPage(pagina);
  }

  @Transactional(readOnly = true)
  public TicketDetalheResponse buscarDoUsuario(Long ticketId) {
    HelpdeskTicket ticket =
        ticketRepository
            .findByIdAndEmpresaIdAndUsuarioId(
                ticketId, TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId())
            .orElseThrow(() -> new TicketNaoEncontradoException("Ticket não encontrado"));
    List<TicketMensagemResponse> mensagens =
        mensagemRepository
            .findByTicketIdAndEmpresaIdOrderByCriadoEmAsc(ticketId, TenantContext.getEmpresaId())
            .stream()
            .map(this::paraMensagemResponse)
            .toList();
    return paraDetalhe(ticket, mensagens);
  }

  @Transactional
  public TicketMensagemResponse responderComoUsuario(Long ticketId, TicketMensagemRequest request) {
    validarTexto(request.conteudo(), "conteudo", TAMANHO_MAX_TEXTO);
    HelpdeskTicket ticket = buscarTicketDoUsuario(ticketId);
    return salvarMensagem(
        ticket, OrigemMensagem.USER, request.conteudo(), UsuarioContext.getUsuarioId());
  }

  @Transactional(readOnly = true)
  public TicketPage listarDaEmpresa(TicketStatus status, Pageable pageable) {
    Page<HelpdeskTicket> pagina =
        ticketRepository.findByEmpresaIdAndStatus(TenantContext.getEmpresaId(), status, pageable);
    return paraPage(pagina);
  }

  @Transactional(readOnly = true)
  public TicketDetalheResponse buscarDaEmpresa(Long ticketId) {
    HelpdeskTicket ticket = buscarTicketDaEmpresa(ticketId);
    List<TicketMensagemResponse> mensagens =
        mensagemRepository
            .findByTicketIdAndEmpresaIdOrderByCriadoEmAsc(ticketId, TenantContext.getEmpresaId())
            .stream()
            .map(this::paraMensagemResponse)
            .toList();
    return paraDetalhe(ticket, mensagens);
  }

  @Transactional
  public TicketMensagemResponse responderComoAdmin(Long ticketId, TicketMensagemRequest request) {
    validarTexto(request.conteudo(), "conteudo", TAMANHO_MAX_TEXTO);
    HelpdeskTicket ticket = buscarTicketDaEmpresa(ticketId);
    return salvarMensagem(
        ticket, OrigemMensagem.ADMIN, request.conteudo(), UsuarioContext.getUsuarioId());
  }

  @Transactional
  public TicketResponse alterarStatus(Long ticketId, TicketStatusRequest request) {
    TicketStatus status = TicketStatus.from(request.status());
    HelpdeskTicket ticket = buscarTicketDaEmpresa(ticketId);
    ticket.setStatus(status);
    HelpdeskTicket salvo = ticketRepository.save(ticket);
    ticketRepository.flush();
    return paraResponse(salvo);
  }

  private HelpdeskTicket buscarTicketDoUsuario(Long ticketId) {
    return ticketRepository
        .findByIdAndEmpresaIdAndUsuarioId(
            ticketId, TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId())
        .orElseThrow(() -> new TicketNaoEncontradoException("Ticket não encontrado"));
  }

  private HelpdeskTicket buscarTicketDaEmpresa(Long ticketId) {
    return ticketRepository
        .findByIdAndEmpresaId(ticketId, TenantContext.getEmpresaId())
        .orElseThrow(() -> new TicketNaoEncontradoException("Ticket não encontrado"));
  }

  private TicketMensagemResponse salvarMensagem(
      HelpdeskTicket ticket, OrigemMensagem origem, String conteudo, Long usuarioId) {
    HelpdeskMensagem mensagem = new HelpdeskMensagem();
    mensagem.setTicket(ticket);
    mensagem.setEmpresa(referenciaEmpresa());
    mensagem.setUsuario(referenciaUsuario(usuarioId));
    mensagem.setOrigem(origem);
    mensagem.setConteudo(conteudo.trim());
    return paraMensagemResponse(mensagemRepository.save(mensagem));
  }

  private Empresa referenciaEmpresa() {
    Empresa empresa = new Empresa();
    empresa.setId(TenantContext.getEmpresaId());
    return empresa;
  }

  private Usuario referenciaUsuario(Long usuarioId) {
    Usuario usuario = new Usuario();
    usuario.setId(usuarioId);
    return usuario;
  }

  private void validarTexto(String valor, String nome, int tamanhoMaximo) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalArgumentException(nome + " é obrigatório");
    }
    if (valor.length() > tamanhoMaximo) {
      throw new IllegalArgumentException(nome + " excede " + tamanhoMaximo + " caracteres");
    }
  }

  private TicketPage paraPage(Page<HelpdeskTicket> pagina) {
    return new TicketPage(
        pagina.getContent().stream().map(this::paraResponse).toList(),
        pagina.getNumber(),
        pagina.getSize(),
        pagina.getTotalElements());
  }

  private TicketResponse paraResponse(HelpdeskTicket ticket) {
    return new TicketResponse(
        ticket.getId(),
        ticket.getUsuario() == null ? null : ticket.getUsuario().getId(),
        ticket.getAssunto(),
        ticket.getDescricao(),
        ticket.getStatus(),
        ticket.getCriadoEm(),
        ticket.getAtualizadoEm());
  }

  private TicketDetalheResponse paraDetalhe(
      HelpdeskTicket ticket, List<TicketMensagemResponse> mensagens) {
    return new TicketDetalheResponse(
        ticket.getId(),
        ticket.getUsuario() == null ? null : ticket.getUsuario().getId(),
        ticket.getAssunto(),
        ticket.getDescricao(),
        ticket.getStatus(),
        ticket.getCriadoEm(),
        ticket.getAtualizadoEm(),
        mensagens);
  }

  private TicketMensagemResponse paraMensagemResponse(HelpdeskMensagem mensagem) {
    return new TicketMensagemResponse(
        mensagem.getId(),
        mensagem.getTicket() == null ? null : mensagem.getTicket().getId(),
        mensagem.getUsuario() == null ? null : mensagem.getUsuario().getId(),
        mensagem.getOrigem(),
        mensagem.getConteudo(),
        mensagem.getCriadoEm());
  }
}
