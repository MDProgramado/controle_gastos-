package com.controlefinanceiro.common;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.ai.AiContextoService;
import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.dashboard.DashboardRepository;
import com.controlefinanceiro.dashboard.DashboardService;
import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.importacao.AnalisadorConsolidado;
import com.controlefinanceiro.importacao.IaExtracaoService;
import com.controlefinanceiro.importacao.ImportacaoDocumentoRepository;
import com.controlefinanceiro.importacao.ImportacaoFaturaRepository;
import com.controlefinanceiro.importacao.ImportacaoItemRepository;
import com.controlefinanceiro.importacao.ImportacaoService;
import com.controlefinanceiro.importacao.MapeadorCategoria;
import com.controlefinanceiro.importacao.PdfTextoExtractor;
import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
import com.controlefinanceiro.lancamento.Lancamento;
import com.controlefinanceiro.lancamento.LancamentoRepository;
import com.controlefinanceiro.lancamento.LancamentoService;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Isolamento financeiro por (empresa_id, usuario_id) do token — testes rápidos, sem Docker. */
class IsolamentoUsuarioServicosTest {

  private static final Long EMPRESA = 1L;
  private static final Long USUARIO = 7L;

  @BeforeEach
  void autenticar() {
    autenticarComo("USER", USUARIO);
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("listagem de lancamentos usa empresa_id e usuario_id do token")
  void listarLancamentosUsaEmpresaEUsuario() {
    LancamentoRepository repository = mock(LancamentoRepository.class);
    when(repository.findByEmpresaIdAndUsuarioIdAndFiltros(
            EMPRESA, USUARIO, null, null, null, null, PageRequest.of(0, 50)))
        .thenReturn(new PageImpl<>(List.of()));

    new LancamentoService(
            repository,
            mock(CategoriaRepository.class),
            mock(UsuarioRepository.class),
            mock(EntityManager.class))
        .listar(EMPRESA, USUARIO, null, null, null, null, 0, 50);

    verify(repository)
        .findByEmpresaIdAndUsuarioIdAndFiltros(
            EMPRESA, USUARIO, null, null, null, null, PageRequest.of(0, 50));
  }

  @Test
  @DisplayName("ADMIN nao consegue ler lancamento de outro usuario da mesma empresa")
  void adminNaoLeLancamentoDeOutroUsuario() {
    autenticarComo("ADMIN", 8L);
    LancamentoRepository repository = mock(LancamentoRepository.class);
    when(repository.findByIdAndEmpresaIdAndUsuarioId(99L, EMPRESA, 8L))
        .thenReturn(java.util.Optional.empty());
    LancamentoService service =
        new LancamentoService(
            repository,
            mock(CategoriaRepository.class),
            mock(UsuarioRepository.class),
            mock(EntityManager.class));

    assertThatThrownBy(() -> service.buscarPorId(99L, EMPRESA, 8L))
        .isInstanceOf(RuntimeException.class);

    verify(repository).findByIdAndEmpresaIdAndUsuarioId(99L, EMPRESA, 8L);
  }

  @Test
  @DisplayName("busca por id usa query escalonada e nao filtro em memoria")
  void buscarLancamentoPorIdUsaQueryEscalonada() {
    LancamentoRepository repository = mock(LancamentoRepository.class);
    when(repository.findByIdAndEmpresaIdAndUsuarioId(5L, EMPRESA, USUARIO))
        .thenReturn(java.util.Optional.of(lancamento()));

    new LancamentoService(
            repository,
            mock(CategoriaRepository.class),
            mock(UsuarioRepository.class),
            mock(EntityManager.class))
        .buscarPorId(5L, EMPRESA, USUARIO);

    verify(repository).findByIdAndEmpresaIdAndUsuarioId(5L, EMPRESA, USUARIO);
    verify(repository, times(0)).findById(5L);
  }

  @Test
  @DisplayName("dashboard agrega somente lancamentos do usuario autenticado")
  void dashboardUsaEmpresaEUsuarioEmTodasAgregacoes() {
    DashboardRepository repository = mock(DashboardRepository.class);
    when(repository.totalEntradas(eq(EMPRESA), eq(USUARIO), any(), any()))
        .thenReturn(BigDecimal.ZERO);
    when(repository.totalSaidas(eq(EMPRESA), eq(USUARIO), any(), any()))
        .thenReturn(BigDecimal.ZERO);
    when(repository.breakdownSaida(eq(EMPRESA), eq(USUARIO), any(), any()))
        .thenReturn(List.<CategoryExpense>of());

    new DashboardService(repository).obterDashboard(EMPRESA, USUARIO, 2026, 11);

    verify(repository, times(2)).totalEntradas(eq(EMPRESA), eq(USUARIO), any(), any());
    verify(repository, times(2)).totalSaidas(eq(EMPRESA), eq(USUARIO), any(), any());
    verify(repository).breakdownSaida(eq(EMPRESA), eq(USUARIO), any(), any());
  }

  @Test
  @DisplayName("detalhe da importacao so consulta documentos e itens do usuario")
  void detalharImportacaoUsaEmpresaEUsuario() {
    ImportacaoDocumentoRepository documentos = mock(ImportacaoDocumentoRepository.class);
    ImportacaoItemRepository itens = mock(ImportacaoItemRepository.class);
    ImportacaoFaturaRepository faturas = mock(ImportacaoFaturaRepository.class);
    ImportacaoDocumento documento = new ImportacaoDocumento();
    documento.setId(10L);
    when(documentos.findByIdAndEmpresaIdAndUsuarioId(10L, EMPRESA, USUARIO))
        .thenReturn(java.util.Optional.of(documento));
    when(itens.findByDocumentoIdAndEmpresaIdAndUsuarioId(10L, EMPRESA, USUARIO))
        .thenReturn(List.of());
    when(faturas.findByDocumentoIdAndEmpresaIdAndUsuarioId(10L, EMPRESA, USUARIO)).thenReturn(null);
    ImportacaoService service =
        new ImportacaoService(
            mock(PdfTextoExtractor.class),
            mock(IaExtracaoService.class),
            mock(MapeadorCategoria.class),
            new AnalisadorConsolidado(),
            documentos,
            itens,
            faturas,
            mock(com.controlefinanceiro.conta.FaturaCartaoRepository.class),
            mock(com.controlefinanceiro.conta.ContaAPagarRepository.class),
            mock(CategoriaRepository.class),
            mock(LancamentoRepository.class),
            mock(UsuarioRepository.class),
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    service.detalhar(EMPRESA, USUARIO, 10L);

    verify(documentos).findByIdAndEmpresaIdAndUsuarioId(10L, EMPRESA, USUARIO);
    verify(itens).findByDocumentoIdAndEmpresaIdAndUsuarioId(10L, EMPRESA, USUARIO);
    verify(faturas).findByDocumentoIdAndEmpresaIdAndUsuarioId(10L, EMPRESA, USUARIO);
  }

  @Test
  @DisplayName("contexto da IA usa empresa_id e usuario_id em todas as consultas")
  void contextoIaUsaEmpresaEUsuario() {
    DashboardRepository dashboard = mock(DashboardRepository.class);
    LancamentoRepository lancamentos = mock(LancamentoRepository.class);
    when(dashboard.totalEntradas(eq(EMPRESA), eq(USUARIO), any(), any()))
        .thenReturn(BigDecimal.ZERO);
    when(dashboard.totalSaidas(eq(EMPRESA), eq(USUARIO), any(), any())).thenReturn(BigDecimal.ZERO);
    when(dashboard.breakdownSaida(eq(EMPRESA), eq(USUARIO), any(), any())).thenReturn(List.of());
    when(dashboard.breakdownEntrada(eq(EMPRESA), eq(USUARIO), any(), any())).thenReturn(List.of());
    when(lancamentos.findByEmpresaIdAndUsuarioIdAndFiltros(
            eq(EMPRESA), eq(USUARIO), isNull(), isNull(), isNull(), isNull(), any()))
        .thenReturn(new PageImpl<>(List.of()));

    new AiContextoService(dashboard, lancamentos).montarContexto(EMPRESA);

    verify(dashboard, times(3)).totalEntradas(eq(EMPRESA), eq(USUARIO), any(), any());
    verify(dashboard, times(3)).totalSaidas(eq(EMPRESA), eq(USUARIO), any(), any());
    verify(dashboard).breakdownSaida(eq(EMPRESA), eq(USUARIO), any(), any());
    verify(dashboard).breakdownEntrada(eq(EMPRESA), eq(USUARIO), any(), any());
    verify(lancamentos)
        .findByEmpresaIdAndUsuarioIdAndFiltros(
            EMPRESA, USUARIO, null, null, null, null, PageRequest.of(0, 12));
  }

  @Test
  @DisplayName("usuario de outra empresa nao ve dados pela mesma consulta escalonada")
  void usuarioDeOutraEmpresaNaoVeDados() {
    autenticarComo("USER", 99L);
    LancamentoRepository repository = mock(LancamentoRepository.class);
    when(repository.findByIdAndEmpresaIdAndUsuarioId(5L, 2L, 99L))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(
            () ->
                new LancamentoService(
                        repository,
                        mock(CategoriaRepository.class),
                        mock(UsuarioRepository.class),
                        mock(EntityManager.class))
                    .buscarPorId(5L, 2L, 99L))
        .isInstanceOf(RuntimeException.class);

    verify(repository).findByIdAndEmpresaIdAndUsuarioId(5L, 2L, 99L);
  }

  private void autenticarComo(String role, Long usuarioId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("usuario@teste.com", EMPRESA, usuarioId, 0, role),
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  private Lancamento lancamento() {
    Empresa empresa = new Empresa();
    empresa.setId(EMPRESA);
    Usuario usuario = new Usuario();
    usuario.setId(USUARIO);
    usuario.setEmpresa(empresa);
    Lancamento lancamento = new Lancamento();
    lancamento.setId(5L);
    Categoria categoria = new Categoria();
    categoria.setId(1L);
    categoria.setNome("Alimentação");
    categoria.setTipo("SAIDA");
    lancamento.setCategoria(categoria);
    lancamento.setEmpresa(empresa);
    lancamento.setUsuario(usuario);
    lancamento.setValor(new BigDecimal("10.00"));
    lancamento.setData(LocalDate.of(2026, 11, 1));
    return lancamento;
  }
}
