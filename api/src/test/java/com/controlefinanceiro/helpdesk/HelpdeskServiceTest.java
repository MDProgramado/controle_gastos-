package com.controlefinanceiro.helpdesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.TicketNaoEncontradoException;
import com.controlefinanceiro.common.TicketStatusInvalidoException;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;

class HelpdeskServiceTest {

  private static final Long EMPRESA_ID = 10L;
  private static final Long USUARIO_ID = 2L;

  private HelpdeskTicketRepository ticketRepository;
  private HelpdeskMensagemRepository mensagemRepository;
  private HelpdeskService service;

  @BeforeEach
  void setup() {
    ticketRepository = mock(HelpdeskTicketRepository.class);
    mensagemRepository = mock(HelpdeskMensagemRepository.class);
    service = new HelpdeskService(ticketRepository, mensagemRepository);
    autenticar("USER", USUARIO_ID);
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("criar ticket define autor a partir do contexto")
  void criarTicketDefineAutorDoContexto() {
    when(ticketRepository.save(any(HelpdeskTicket.class)))
        .thenAnswer(
            inv -> {
              HelpdeskTicket t = inv.getArgument(0);
              t.setId(1L);
              t.setCriadoEm(OffsetDateTime.now());
              return t;
            });

    TicketResponse response =
        service.criarTicket(new TicketRequest("Assunto", "Descricao do problema"));

    assertThat(response.assunto()).isEqualTo("Assunto");
    assertThat(response.status()).isEqualTo(TicketStatus.ABERTO);
    verify(ticketRepository).save(any(HelpdeskTicket.class));
  }

  @Test
  @DisplayName("listar do usuario filtra por empresa e usuario")
  void listarDoUsuarioFiltraPorEmpresaEUsuario() {
    when(ticketRepository.findByEmpresaIdAndUsuarioIdAndStatus(
            eq(EMPRESA_ID), eq(USUARIO_ID), eq(TicketStatus.ABERTO), any()))
        .thenReturn(new PageImpl<>(List.of(ticket(1L, USUARIO_ID))));

    TicketPage page = service.listarDoUsuario(TicketStatus.ABERTO, PageRequest.of(0, 20));

    assertThat(page.tickets()).hasSize(1);
    verify(ticketRepository)
        .findByEmpresaIdAndUsuarioIdAndStatus(
            EMPRESA_ID, USUARIO_ID, TicketStatus.ABERTO, PageRequest.of(0, 20));
  }

  @Test
  @DisplayName("buscar do usuario usa escopo empresa e usuario")
  void buscarDoUsuarioEscopado() {
    HelpdeskTicket t = ticket(5L, USUARIO_ID);
    when(ticketRepository.findByIdAndEmpresaIdAndUsuarioId(5L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.of(t));
    when(mensagemRepository.findByTicketIdAndEmpresaIdOrderByCriadoEmAsc(5L, EMPRESA_ID))
        .thenReturn(List.of());

    TicketDetalheResponse detalhe = service.buscarDoUsuario(5L);

    assertThat(detalhe.id()).isEqualTo(5L);
    verify(ticketRepository).findByIdAndEmpresaIdAndUsuarioId(5L, EMPRESA_ID, USUARIO_ID);
  }

  @Test
  @DisplayName("ticket de outro usuario retorna 404")
  void ticketDeOutroUsuarioRetorna404() {
    when(ticketRepository.findByIdAndEmpresaIdAndUsuarioId(9L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscarDoUsuario(9L))
        .isInstanceOf(TicketNaoEncontradoException.class);
  }

  @Test
  @DisplayName("ticket de outro tenant retorna 404")
  void ticketDeOutroTenantRetorna404() {
    when(ticketRepository.findByIdAndEmpresaIdAndUsuarioId(9L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.responderComoUsuario(9L, new TicketMensagemRequest("oi")))
        .isInstanceOf(TicketNaoEncontradoException.class);
  }

  @Test
  @DisplayName("responder como usuario registra origem USER")
  void responderComoUsuarioOrigemUser() {
    HelpdeskTicket t = ticket(3L, USUARIO_ID);
    when(ticketRepository.findByIdAndEmpresaIdAndUsuarioId(3L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.of(t));
    when(mensagemRepository.save(any(HelpdeskMensagem.class)))
        .thenAnswer(
            inv -> {
              HelpdeskMensagem m = inv.getArgument(0);
              m.setId(11L);
              m.setCriadoEm(OffsetDateTime.now());
              return m;
            });

    TicketMensagemResponse msg =
        service.responderComoUsuario(3L, new TicketMensagemRequest("Minha resposta"));

    assertThat(msg.origem()).isEqualTo(OrigemMensagem.USER);
    assertThat(msg.conteudo()).isEqualTo("Minha resposta");
  }

  @Test
  @DisplayName("fila admin lista por empresa sem filtrar usuario")
  void filaAdminListaPorEmpresa() {
    autenticar("ADMIN", 99L);
    when(ticketRepository.findByEmpresaIdAndStatus(eq(EMPRESA_ID), eq(null), any()))
        .thenReturn(new PageImpl<>(List.of(ticket(1L, 2L), ticket(2L, 3L))));

    TicketPage page = service.listarDaEmpresa(null, PageRequest.of(0, 20));

    assertThat(page.tickets()).hasSize(2);
    verify(ticketRepository).findByEmpresaIdAndStatus(EMPRESA_ID, null, PageRequest.of(0, 20));
  }

  @Test
  @DisplayName("admin busca detalhe por empresa")
  void adminBuscaPorEmpresa() {
    autenticar("ADMIN", 99L);
    HelpdeskTicket t = ticket(7L, 2L);
    when(ticketRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(t));
    when(mensagemRepository.findByTicketIdAndEmpresaIdOrderByCriadoEmAsc(7L, EMPRESA_ID))
        .thenReturn(List.of());

    TicketDetalheResponse detalhe = service.buscarDaEmpresa(7L);

    assertThat(detalhe.id()).isEqualTo(7L);
    verify(ticketRepository).findByIdAndEmpresaId(7L, EMPRESA_ID);
  }

  @Test
  @DisplayName("admin responde com origem ADMIN")
  void adminRespondeOrigemAdmin() {
    autenticar("ADMIN", 99L);
    HelpdeskTicket t = ticket(7L, 2L);
    when(ticketRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(t));
    when(mensagemRepository.save(any(HelpdeskMensagem.class)))
        .thenAnswer(
            inv -> {
              HelpdeskMensagem m = inv.getArgument(0);
              m.setId(12L);
              m.setCriadoEm(OffsetDateTime.now());
              return m;
            });

    TicketMensagemResponse msg =
        service.responderComoAdmin(7L, new TicketMensagemRequest("Resposta admin"));

    assertThat(msg.origem()).isEqualTo(OrigemMensagem.ADMIN);
  }

  @Test
  @DisplayName("alterar status invalido lanca excecao")
  void alterarStatusInvalido() {
    autenticar("ADMIN", 99L);

    assertThatThrownBy(() -> service.alterarStatus(1L, new TicketStatusRequest("INVALIDO")))
        .isInstanceOf(TicketStatusInvalidoException.class);
  }

  @Test
  @DisplayName("alterar status valido atualiza ticket")
  void alterarStatusValido() {
    autenticar("ADMIN", 99L);
    HelpdeskTicket t = ticket(4L, 2L);
    when(ticketRepository.findByIdAndEmpresaId(4L, EMPRESA_ID)).thenReturn(Optional.of(t));
    when(ticketRepository.save(any(HelpdeskTicket.class))).thenAnswer(inv -> inv.getArgument(0));

    TicketResponse response = service.alterarStatus(4L, new TicketStatusRequest("RESOLVIDO"));

    assertThat(response.status()).isEqualTo(TicketStatus.RESOLVIDO);
  }

  @Test
  @DisplayName("alterar status devolve atualizadoEm persistido no response")
  void alterarStatusDevolveAtualizadoEmPersistido() {
    autenticar("ADMIN", 99L);
    HelpdeskTicket t = ticket(4L, 2L);
    when(ticketRepository.findByIdAndEmpresaId(4L, EMPRESA_ID)).thenReturn(Optional.of(t));
    when(ticketRepository.save(any(HelpdeskTicket.class))).thenAnswer(inv -> inv.getArgument(0));
    doAnswer(
            inv -> {
              t.aoAtualizar();
              return null;
            })
        .when(ticketRepository)
        .flush();

    TicketResponse response = service.alterarStatus(4L, new TicketStatusRequest("RESOLVIDO"));

    assertThat(t.getAtualizadoEm()).isNotNull();
    assertThat(response.atualizadoEm()).isNotNull().isEqualTo(t.getAtualizadoEm());
  }

  @Test
  @DisplayName("validacao rejeita assunto e conteudo em branco")
  void validacaoCamposObrigatorios() {
    assertThatThrownBy(() -> service.criarTicket(new TicketRequest(" ", "desc")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.criarTicket(new TicketRequest("Assunto", "")))
        .isInstanceOf(IllegalArgumentException.class);

    HelpdeskTicket t = ticket(3L, USUARIO_ID);
    when(ticketRepository.findByIdAndEmpresaIdAndUsuarioId(3L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.of(t));
    assertThatThrownBy(() -> service.responderComoUsuario(3L, new TicketMensagemRequest(" ")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void autenticar(String role, Long usuarioId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("user@teste.com", EMPRESA_ID, usuarioId, 0, role), List.of()));
  }

  private HelpdeskTicket ticket(Long id, Long usuarioId) {
    Empresa empresa = new Empresa();
    empresa.setId(EMPRESA_ID);
    Usuario usuario = new Usuario();
    usuario.setId(usuarioId);
    HelpdeskTicket t = new HelpdeskTicket();
    t.setId(id);
    t.setEmpresa(empresa);
    t.setUsuario(usuario);
    t.setAssunto("Assunto");
    t.setDescricao("Descricao");
    t.setStatus(TicketStatus.ABERTO);
    t.setCriadoEm(OffsetDateTime.now());
    return t;
  }
}
