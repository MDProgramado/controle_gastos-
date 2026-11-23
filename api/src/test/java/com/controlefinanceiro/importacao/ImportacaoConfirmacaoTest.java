package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.conta.ContaAPagar;
import com.controlefinanceiro.conta.ContaAPagarRepository;
import com.controlefinanceiro.conta.FaturaCartao;
import com.controlefinanceiro.conta.FaturaCartaoRepository;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
import com.controlefinanceiro.lancamento.Lancamento;
import com.controlefinanceiro.lancamento.LancamentoRepository;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Testes de unidade da confirmação (fase 5, 05-05): gravação tudo-ou-nada, idempotência, dedupe
 * (D-07/D-08), A_PAGAR → conta_a_pagar sem tocar o saldo do dashboard (D-11), CAT-04 e R3.
 *
 * <p>{@link MapeadorCategoria} real com {@link CategoriaRepository} mockado — cobre o casamento de
 * categoria de ponta a ponta. Transação com {@link PlatformTransactionManager} mockado.
 */
class ImportacaoConfirmacaoTest {

  private static final Long EMPRESA_ID = 1L;
  private static final String EMAIL = "usuario@test.com";

  private ImportacaoDocumentoRepository documentoRepository;
  private ImportacaoItemRepository itemRepository;
  private ImportacaoFaturaRepository faturaRepository;
  private FaturaCartaoRepository faturaCartaoRepository;
  private ContaAPagarRepository contaAPagarRepository;
  private CategoriaRepository categoriaRepository;
  private LancamentoRepository lancamentoRepository;
  private ImportacaoService service;

  private Categoria entrada;
  private Categoria saida;
  private Empresa empresa;
  private Usuario usuario;

  @BeforeEach
  void setup() {
    documentoRepository = mock(ImportacaoDocumentoRepository.class);
    itemRepository = mock(ImportacaoItemRepository.class);
    faturaRepository = mock(ImportacaoFaturaRepository.class);
    faturaCartaoRepository = mock(FaturaCartaoRepository.class);
    contaAPagarRepository = mock(ContaAPagarRepository.class);
    categoriaRepository = mock(CategoriaRepository.class);
    lancamentoRepository = mock(LancamentoRepository.class);
    UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    MapeadorCategoria mapeadorCategoria = new MapeadorCategoria(categoriaRepository);

    service =
        new ImportacaoService(
            mock(PdfTextoExtractor.class),
            mock(IaExtracaoService.class),
            mapeadorCategoria,
            new AnalisadorConsolidado(),
            documentoRepository,
            itemRepository,
            faturaRepository,
            faturaCartaoRepository,
            contaAPagarRepository,
            categoriaRepository,
            lancamentoRepository,
            usuarioRepository,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    // R3: principal do token no SecurityContext (autenticado: 3º argumento).
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UsuarioAuth(EMAIL, EMPRESA_ID, 7L, 0, "ADMIN"), null, List.of()));

    empresa = new Empresa();
    empresa.setId(EMPRESA_ID);
    empresa.setNome("Empresa X");
    usuario = new Usuario();
    usuario.setId(7L);
    usuario.setEmail(EMAIL);
    usuario.setEmpresa(empresa);
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    when(usuarioRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(usuario));

    entrada = categoria(10L, "Rendimentos e reembolsos", "ENTRADA");
    saida = categoria(1L, "Alimentação", "SAIDA");
    when(categoriaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(entrada, saida));
    when(categoriaRepository.findByIdAndEmpresaId(10L, EMPRESA_ID))
        .thenReturn(Optional.of(entrada));
    when(categoriaRepository.findByIdAndEmpresaId(1L, EMPRESA_ID)).thenReturn(Optional.of(saida));

    // save devolve a própria entidade com id (simula IDENTITY).
    when(lancamentoRepository.save(any(Lancamento.class)))
        .thenAnswer(
            inv -> {
              Lancamento l = inv.getArgument(0);
              if (l.getId() == null) {
                l.setId(900L + sequencial.incrementAndGet());
              }
              return l;
            });
    when(contaAPagarRepository.save(any(ContaAPagar.class)))
        .thenAnswer(
            inv -> {
              ContaAPagar c = inv.getArgument(0);
              if (c.getId() == null) {
                c.setId(800L);
              }
              return c;
            });
    when(faturaCartaoRepository.save(any(FaturaCartao.class)))
        .thenAnswer(
            inv -> {
              FaturaCartao f = inv.getArgument(0);
              if (f.getId() == null) {
                f.setId(300L);
              }
              return f;
            });
    when(lancamentoRepository.findChavesDedupeByEmpresaIdAndUsuarioId(EMPRESA_ID, 7L))
        .thenReturn(List.of());
  }

  /** Contador para ids de lançamento no stub de save. */
  private final java.util.concurrent.atomic.AtomicInteger sequencial =
      new java.util.concurrent.atomic.AtomicInteger();

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("RECEITA→lancamento ENTRADA e DESPESA_PAGA→SAIDA, com chave_dedupe (D-24/D-08)")
  void confirmaItensGravaLancamentos() {
    ImportacaoItem receita = item(11L, "RECEITA", "Salário", "1000.00", "EXTRATO");
    ImportacaoItem despesa = item(12L, "DESPESA_PAGA", "Alimentação", "45.00", "EXTRATO");
    prepararDocumento(100L, List.of(receita, despesa));

    ImportacaoService.ResultadoConfirmacao resultado =
        service.confirmar(
            100L,
            new ConfirmarRequest(
                List.of(
                    new ConfirmarRequest.ConfirmarItem(11L, true, null),
                    new ConfirmarRequest.ConfirmarItem(12L, true, null))));

    assertThat(resultado.importados()).isEqualTo(2);
    assertThat(resultado.lancamentoIds()).hasSize(2);
    assertThat(resultado.contaAPagarIds()).isEmpty();
    assertThat(resultado.duplicados()).isZero();

    ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
    verify(lancamentoRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues())
        .extracting(Lancamento::getTipo)
        .containsExactlyInAnyOrder("ENTRADA", "SAIDA");
    assertThat(captor.getAllValues())
        .allSatisfy(
            l -> {
              assertThat(l.getChaveDedupe()).hasSize(64); // SHA-256 hex
              assertThat(l.getDocumentoId()).isEqualTo(100L);
              assertThat(l.getValor().scale()).isEqualTo(2);
            });

    // item marcado IMPORTADO com o id do lançamento; documento CONFIRMADO
    assertThat(receita.getStatus()).isEqualTo("IMPORTADO");
    assertThat(receita.getLancamentoId()).isNotNull();
    assertThat(despesa.getStatus()).isEqualTo("IMPORTADO");
    assertThat(consultarStatusDocumento()).isEqualTo("CONFIRMADO");
  }

  @Test
  @DisplayName("A_PAGAR → conta_a_pagar + fatura_cartao 1 vez; nenhum lancamento (D-11)")
  void aPagarVaiParaContaAPagar() {
    ImportacaoItem parcela = item(13L, "A_PAGAR", "Alimentação", "500.00", "CARTAO");
    parcela.setParcelaAtual(1);
    parcela.setParcelaTotal(3);
    prepararDocumento(100L, List.of(parcela));
    prepararFaturaStaging("1500.00"); // cabeçalho da fatura no staging

    ImportacaoService.ResultadoConfirmacao resultado =
        service.confirmar(
            100L,
            new ConfirmarRequest(List.of(new ConfirmarRequest.ConfirmarItem(13L, true, null))));

    assertThat(resultado.importados()).isEqualTo(1);
    assertThat(resultado.contaAPagarIds()).containsExactly(800L);
    assertThat(resultado.lancamentoIds()).isEmpty(); // D-11: não entra no saldo do dashboard

    verify(lancamentoRepository, never()).save(any());
    verify(faturaCartaoRepository, times(1)).save(any(FaturaCartao.class));

    ArgumentCaptor<ContaAPagar> captor = ArgumentCaptor.forClass(ContaAPagar.class);
    verify(contaAPagarRepository).save(captor.capture());
    ContaAPagar conta = captor.getValue();
    assertThat(conta.getFaturaId()).isEqualTo(300L);
    assertThat(conta.getCategoria().getTipo()).isEqualTo("SAIDA"); // CAT-04
    assertThat(conta.getVencimento()).isEqualTo(LocalDate.of(2026, 12, 10)); // vencimento da fatura
    assertThat(conta.getParcelaAtual()).isEqualTo(1);
    assertThat(conta.getOrigem()).isEqualTo("CARTAO");
  }

  @Test
  @DisplayName("item desmarcado (incluir=false) → IGNORADO, nada gravado")
  void itemDesmarcadoIgnorado() {
    ImportacaoItem despesa = item(12L, "DESPESA_PAGA", "Alimentação", "45.00", "EXTRATO");
    prepararDocumento(100L, List.of(despesa));

    ImportacaoService.ResultadoConfirmacao resultado =
        service.confirmar(
            100L,
            new ConfirmarRequest(List.of(new ConfirmarRequest.ConfirmarItem(12L, false, null))));

    assertThat(resultado.importados()).isZero();
    assertThat(resultado.ignorados()).isEqualTo(1);
    assertThat(despesa.getStatus()).isEqualTo("IGNORADO");
    verify(lancamentoRepository, never()).save(any());
    verify(contaAPagarRepository, never()).save(any());
  }

  @Test
  @DisplayName("item ausente da seleção → IGNORADO (sem lançamento)")
  void itemAusenteDaSelecaoIgnorado() {
    ImportacaoItem despesa = item(12L, "DESPESA_PAGA", "Alimentação", "45.00", "EXTRATO");
    prepararDocumento(100L, List.of(despesa));

    ImportacaoService.ResultadoConfirmacao resultado =
        service.confirmar(100L, new ConfirmarRequest(List.of()));

    assertThat(resultado.ignorados()).isEqualTo(1);
    assertThat(resultado.importados()).isZero();
    verify(lancamentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("segunda confirmação: itens IMPORTADO são pulados (idempotência, D-07)")
  void segundaConfirmacaoNaoDuplica() {
    ImportacaoItem receita = item(11L, "RECEITA", "Salário", "1000.00", "EXTRATO");
    receita.setStatus("IMPORTADO");
    receita.setLancamentoId(900L);
    ImportacaoItem despesa = item(12L, "DESPESA_PAGA", "Alimentação", "45.00", "EXTRATO");
    despesa.setStatus("IMPORTADO");
    despesa.setLancamentoId(901L);
    prepararDocumento(100L, List.of(receita, despesa));

    ImportacaoService.ResultadoConfirmacao resultado =
        service.confirmar(
            100L,
            new ConfirmarRequest(
                List.of(
                    new ConfirmarRequest.ConfirmarItem(11L, true, null),
                    new ConfirmarRequest.ConfirmarItem(12L, true, null))));

    assertThat(resultado.importados()).isZero();
    assertThat(resultado.duplicados()).isEqualTo(2);
    verify(lancamentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("chave de dedupe já gravada → contado como duplicado, não grava (D-08)")
  void chaveDedupeExistenteContadoComoDuplicado() {
    ImportacaoItem despesa = item(12L, "DESPESA_PAGA", "Alimentação", "45.00", "EXTRATO");
    prepararDocumento(100L, List.of(despesa));

    String chave =
        Deduplicador.gerarChaveDedupe(
            "SAIDA", despesa.getData(), despesa.getValor(), "Alimentação");
    when(lancamentoRepository.findChavesDedupeByEmpresaIdAndUsuarioId(EMPRESA_ID, 7L))
        .thenReturn(List.of(chave));

    ImportacaoService.ResultadoConfirmacao resultado =
        service.confirmar(
            100L,
            new ConfirmarRequest(List.of(new ConfirmarRequest.ConfirmarItem(12L, true, null))));

    assertThat(resultado.importados()).isZero();
    assertThat(resultado.duplicados()).isEqualTo(1);
    verify(lancamentoRepository, never()).save(any());
    assertThat(despesa.getStatus()).isEqualTo("IMPORTADO"); // marcado para não reaparecer
  }

  @Test
  @DisplayName("categoriaIdOverride substitui a categoria mapeada pela IA")
  void categoriaOverrideAplicado() {
    ImportacaoItem despesa = item(12L, "DESPESA_PAGA", "Alimentação", "45.00", "EXTRATO");
    despesa.setCategoriaId(1L);
    prepararDocumento(100L, List.of(despesa));

    service.confirmar(
        100L, new ConfirmarRequest(List.of(new ConfirmarRequest.ConfirmarItem(12L, true, 1L))));

    ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
    verify(lancamentoRepository).save(captor.capture());
    assertThat(captor.getValue().getCategoria().getId()).isEqualTo(1L);
  }

  @Test
  @DisplayName(
      "override de tipo incompatível → LinhaInvalidaException com índice da linha (LOT-02)")
  void categoriaTipoIncompativelLancaComIndice() {
    ImportacaoItem receita = item(11L, "RECEITA", "Salário", "1000.00", "EXTRATO");
    ImportacaoItem despesa = item(12L, "DESPESA_PAGA", "Alimentação", "45.00", "EXTRATO");
    prepararDocumento(100L, List.of(receita, despesa));

    // índice 1 (base 0): despesa com categoria de ENTRADA
    assertThatThrownBy(
            () ->
                service.confirmar(
                    100L,
                    new ConfirmarRequest(
                        List.of(
                            new ConfirmarRequest.ConfirmarItem(11L, true, null),
                            new ConfirmarRequest.ConfirmarItem(12L, true, 10L)))))
        .isInstanceOf(LinhaInvalidaException.class)
        .hasMessageContaining("Linha 1")
        .hasMessageContaining("incompatível");
  }

  @Test
  @DisplayName("categoria não resolvida (nula) → LinhaInvalidaException")
  void categoriaNaoResolvidaLanca() {
    ImportacaoItem semCategoria = item(14L, "DESPESA_PAGA", null, "45.00", "EXTRATO");
    prepararDocumento(100L, List.of(semCategoria));

    assertThatThrownBy(
            () ->
                service.confirmar(
                    100L,
                    new ConfirmarRequest(
                        List.of(new ConfirmarRequest.ConfirmarItem(14L, true, null)))))
        .isInstanceOf(LinhaInvalidaException.class)
        .hasMessageContaining("categoria");
    verify(lancamentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("confirmar documento de outra empresa → 404 (R3)")
  void confirmarOutraEmpresaLanca404() {
    when(documentoRepository.findByIdAndEmpresaIdAndUsuarioId(999L, EMPRESA_ID, 7L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirmar(999L, new ConfirmarRequest(List.of())))
        .isInstanceOf(ImportacaoNaoEncontradaException.class);
  }

  @Test
  @DisplayName("confirmar documento DESCARTADO → 409")
  void confirmarDescartadoLanca409() {
    ImportacaoDocumento doc = documento(100L, "DESCARTADO");
    when(documentoRepository.findByIdAndEmpresaIdAndUsuarioId(100L, EMPRESA_ID, 7L))
        .thenReturn(Optional.of(doc));

    assertThatThrownBy(() -> service.confirmar(100L, new ConfirmarRequest(List.of())))
        .isInstanceOf(ImportacaoEstadoInvalidoException.class);
  }

  /* ============================ fixtures ============================ */

  /** Contador de status consultado após as ações (o doc é fixture interna do teste). */
  private String statusDocumento;

  private String consultarStatusDocumento() {
    return statusDocumento;
  }

  private void prepararDocumento(Long id, List<ImportacaoItem> itens) {
    ImportacaoDocumento doc = documento(id, "ANALISADO");
    when(documentoRepository.findByIdAndEmpresaIdAndUsuarioId(id, EMPRESA_ID, 7L))
        .thenReturn(Optional.of(doc));
    when(itemRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(id, EMPRESA_ID, 7L))
        .thenReturn(itens);
    when(documentoRepository.save(any(ImportacaoDocumento.class)))
        .thenAnswer(
            inv -> {
              ImportacaoDocumento salvo = inv.getArgument(0);
              statusDocumento = salvo.getStatus();
              return salvo;
            });
  }

  private void prepararFaturaStaging(String total) {
    ImportacaoFatura fatura = new ImportacaoFatura();
    fatura.setTotal(new BigDecimal(total));
    fatura.setDataVencimento(LocalDate.of(2026, 12, 10));
    fatura.setMesReferencia(LocalDate.of(2026, 11, 1));
    when(faturaRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(100L, EMPRESA_ID, 7L))
        .thenReturn(fatura);
  }

  private static ImportacaoItem item(
      Long id, String tipo, String categoriaNome, String valor, String origem) {
    ImportacaoItem item = new ImportacaoItem();
    item.setId(id);
    item.setTipo(tipo);
    item.setCategoriaNome(categoriaNome);
    item.setValor(new BigDecimal(valor));
    item.setOrigem(origem);
    item.setData(LocalDate.of(2026, 11, 3));
    item.setDescricao("Item " + id);
    item.setStatus("PENDENTE");
    return item;
  }

  private ImportacaoDocumento documento(Long id, String status) {
    ImportacaoDocumento doc = new ImportacaoDocumento();
    doc.setId(id);
    doc.setEmpresa(empresa);
    doc.setUsuario(usuario);
    doc.setNomeArquivo("fatura.pdf");
    doc.setTipoDocumento("EXTRATO");
    doc.setHashSha256("hash-" + id);
    doc.setTamanhoBytes(1000);
    doc.setStatus(status);
    return doc;
  }

  private static Categoria categoria(Long id, String nome, String tipo) {
    Categoria c = new Categoria();
    c.setId(id);
    c.setNome(nome);
    c.setTipo(tipo);
    c.setAtivo(true);
    return c;
  }
}
