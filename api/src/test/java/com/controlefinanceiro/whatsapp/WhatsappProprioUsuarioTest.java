package com.controlefinanceiro.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.ai.AiService;
import com.controlefinanceiro.categoria.CategoriaResponse;
import com.controlefinanceiro.categoria.CategoriaService;
import com.controlefinanceiro.dashboard.DashboardService;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.lancamento.LancamentoService;
import com.controlefinanceiro.usuario.Usuario;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** A identidade do webhook é o dono do telefone — nunca um id enviado pelo cliente. */
class WhatsappProprioUsuarioTest {

  private static final String NUMERO = "+5511999999999";

  private UsuarioTelefoneRepository telefoneRepository;
  private WhatsappMensagemProcessadaRepository mensagemRepository;
  private WhatsappConfirmacaoPendenteRepository confirmacaoRepository;
  private WhatsappEnvioService envioService;
  private LancamentoService lancamentoService;
  private WhatsappWebhookService service;

  private Empresa empresa;
  private Usuario usuarioA;
  private Usuario usuarioB;
  private UsuarioTelefone telefoneA;
  private UsuarioTelefone telefoneB;
  private WhatsappConfirmacaoPendente pendencia;

  @BeforeEach
  void setup() {
    telefoneRepository = mock(UsuarioTelefoneRepository.class);
    mensagemRepository = mock(WhatsappMensagemProcessadaRepository.class);
    confirmacaoRepository = mock(WhatsappConfirmacaoPendenteRepository.class);
    envioService = mock(WhatsappEnvioService.class);
    AiService aiService = mock(AiService.class);
    lancamentoService = mock(LancamentoService.class);
    CategoriaService categoriaService = mock(CategoriaService.class);
    DashboardService dashboardService = mock(DashboardService.class);

    empresa = new Empresa();
    empresa.setId(1L);
    usuarioA = usuario(1L, empresa, true);
    usuarioB = usuario(2L, empresa, true);
    telefoneA = telefone(10L, usuarioA, empresa);
    telefoneB = telefone(10L, usuarioB, empresa);
    pendencia = pendencia(42L, telefoneA);

    when(telefoneRepository.findByNumeroE164(NUMERO)).thenReturn(Optional.of(telefoneB));
    when(confirmacaoRepository.findById(42L)).thenReturn(Optional.of(pendencia));
    when(confirmacaoRepository.confirmar(anyLong(), anyLong(), any())).thenReturn(1);
    when(categoriaService.listar(1L))
        .thenReturn(List.of(new CategoriaResponse(5L, "Alimentação", "SAIDA", true, true)));

    service =
        new WhatsappWebhookService(
            telefoneRepository,
            mensagemRepository,
            confirmacaoRepository,
            envioService,
            aiService,
            lancamentoService,
            categoriaService,
            dashboardService);
  }

  @Test
  @DisplayName("confirmacao cuja pendencia pertence a outro usuario e rejeitada")
  void confirmacaoDeOutroUsuarioEhRejeitada() {
    service.confirmarLancamento("wamid-1", NUMERO, 42L);

    verify(lancamentoService, never()).criarComCanal(any(), any(), any(), any(), any());
    verify(confirmacaoRepository, never()).confirmar(anyLong(), anyLong(), any());
    assertThat(resultadoRegistrado()).isEqualTo("REJEITADA");
  }

  @Test
  @DisplayName("confirmacao de usuario inativo e rejeitada")
  void confirmacaoDeUsuarioInativoEhRejeitada() {
    usuarioB.setAtivo(false);
    pendencia.setUsuarioTelefone(telefoneB);

    service.confirmarLancamento("wamid-1", NUMERO, 42L);

    verify(lancamentoService, never()).criarComCanal(any(), any(), any(), any(), any());
    verify(confirmacaoRepository, never()).confirmar(anyLong(), anyLong(), any());
    assertThat(resultadoRegistrado()).isEqualTo("REJEITADA");
  }

  @Test
  @DisplayName("confirmacao com empresa do usuario diferente da empresa do telefone e rejeitada")
  void confirmacaoComEmpresaInconsistenteEhRejeitada() {
    Empresa outraEmpresa = new Empresa();
    outraEmpresa.setId(2L);
    usuarioB.setEmpresa(outraEmpresa);
    pendencia.setUsuarioTelefone(telefoneB);

    service.confirmarLancamento("wamid-1", NUMERO, 42L);

    verify(lancamentoService, never()).criarComCanal(any(), any(), any(), any(), any());
    verify(confirmacaoRepository, never()).confirmar(anyLong(), anyLong(), any());
    assertThat(resultadoRegistrado()).isEqualTo("REJEITADA");
  }

  @Test
  @DisplayName("confirmacao valida usa explicitamente empresa e usuario donos do telefone")
  void confirmacaoValidaUsaDonoDoTelefone() {
    pendencia.setUsuarioTelefone(telefoneB);

    service.confirmarLancamento("wamid-1", NUMERO, 42L);

    verify(lancamentoService).criarComCanal(eq(1L), eq(2L), any(), eq("WHATSAPP"), eq("wamid-1"));
  }

  private String resultadoRegistrado() {
    ArgumentCaptor<WhatsappMensagemProcessada> captor =
        ArgumentCaptor.forClass(WhatsappMensagemProcessada.class);
    verify(mensagemRepository).save(captor.capture());
    return captor.getValue().getResultado();
  }

  private static Usuario usuario(Long id, Empresa empresa, boolean ativo) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setEmpresa(empresa);
    usuario.setAtivo(ativo);
    return usuario;
  }

  private static UsuarioTelefone telefone(Long id, Usuario usuario, Empresa empresa) {
    UsuarioTelefone telefone = new UsuarioTelefone();
    telefone.setId(id);
    telefone.setUsuario(usuario);
    telefone.setEmpresa(empresa);
    telefone.setNumeroE164(NUMERO);
    telefone.setVerificadoEm(OffsetDateTime.now().minusDays(1));
    return telefone;
  }

  private static WhatsappConfirmacaoPendente pendencia(Long id, UsuarioTelefone usuarioTelefone) {
    WhatsappConfirmacaoPendente pendencia = new WhatsappConfirmacaoPendente();
    pendencia.setId(id);
    pendencia.setUsuarioTelefone(usuarioTelefone);
    pendencia.setStatus("PENDENTE");
    pendencia.setExpiraEm(OffsetDateTime.now().plusHours(1));
    pendencia.setProposta(
        """
        {"tipo":"SAIDA","valor":"45.00","data":"2026-11-10",\
        "categoriaSugerida":"Alimentação","descricao":"Almoço"}
        """);
    return pendencia;
  }
}
