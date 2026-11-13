package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
import com.controlefinanceiro.lancamento.LancamentoRepository;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Testes de unidade de {@link ImportacaoService} (fase 5, 05-04-t4): orquestração da análise com
 * mocks (sem banco, sem rede, sem IA real).
 *
 * <p>SecurityContext populado com principal {@link UsuarioAuth} — é por aí que {@link
 * com.controlefinanceiro.common.TenantContext} lê o tenant (R3). A transação usa um {@link
 * PlatformTransactionManager} mockado: o {@link TransactionTemplate} executa o callback sem banco.
 */
class ImportacaoServiceTest {

  private static final Long EMPRESA_ID = 1L;
  private static final String EMAIL = "usuario@test.com";

  private PdfTextoExtractor pdfTextoExtractor;
  private IaExtracaoService iaExtracaoService;
  private MapeadorCategoria mapeadorCategoria;
  private ImportacaoDocumentoRepository documentoRepository;
  private ImportacaoItemRepository itemRepository;
  private ImportacaoFaturaRepository faturaRepository;
  private LancamentoRepository lancamentoRepository;
  private UsuarioRepository usuarioRepository;
  private ImportacaoService service;
  private Empresa empresa;

  @BeforeEach
  void setup() {
    pdfTextoExtractor = mock(PdfTextoExtractor.class);
    iaExtracaoService = mock(IaExtracaoService.class);
    mapeadorCategoria = mock(MapeadorCategoria.class);
    documentoRepository = mock(ImportacaoDocumentoRepository.class);
    itemRepository = mock(ImportacaoItemRepository.class);
    faturaRepository = mock(ImportacaoFaturaRepository.class);
    CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
    lancamentoRepository = mock(LancamentoRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);

    service =
        new ImportacaoService(
            pdfTextoExtractor,
            iaExtracaoService,
            mapeadorCategoria,
            new AnalisadorConsolidado(),
            documentoRepository,
            itemRepository,
            faturaRepository,
            categoriaRepository,
            lancamentoRepository,
            usuarioRepository,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    // R3: principal do token no SecurityContext — TenantContext lê daqui (autenticado: 3º arg).
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UsuarioAuth(EMAIL, EMPRESA_ID, "ADMIN"), null, List.of()));

    empresa = new Empresa();
    empresa.setId(EMPRESA_ID);
    empresa.setNome("Empresa X");
    Usuario usuario = new Usuario();
    usuario.setId(7L);
    usuario.setEmail(EMAIL);
    usuario.setEmpresa(empresa);
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

    // save devolve a própria entidade com id atribuído (simula IDENTITY).
    when(documentoRepository.save(any(ImportacaoDocumento.class)))
        .thenAnswer(
            inv -> {
              ImportacaoDocumento d = inv.getArgument(0);
              if (d.getId() == null) {
                d.setId(100L);
              }
              return d;
            });
    when(itemRepository.save(any(ImportacaoItem.class)))
        .thenAnswer(
            inv -> {
              ImportacaoItem i = inv.getArgument(0);
              if (i.getId() == null) {
                i.setId(500L);
              }
              return i;
            });
    when(faturaRepository.save(any(ImportacaoFatura.class))).thenAnswer(inv -> inv.getArgument(0));

    when(categoriaRepository.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of());
    when(mapeadorCategoria.mapear(any(), any(), any()))
        .thenReturn(new MapeadorCategoria.ResultadoMapeamento(9L, "Outros", false));
  }

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  /* ============================ validação do lote ============================ */

  @Test
  @DisplayName("lote vazio → ArquivoInvalidoException (400)")
  void loteVazioRejeitado() {
    assertThatThrownBy(() -> service.analisar(new MultipartFile[0]))
        .isInstanceOf(ArquivoInvalidoException.class)
        .hasMessageContaining("pelo menos um arquivo");
  }

  @Test
  @DisplayName("lote com mais de 10 arquivos → ArquivoInvalidoException (cota da IA)")
  void loteDemaisArquivosRejeitado() {
    MultipartFile[] arquivos = new MultipartFile[11];
    for (int i = 0; i < arquivos.length; i++) {
      arquivos[i] = pdfValido("arquivo-" + i + ".pdf");
    }

    assertThatThrownBy(() -> service.analisar(arquivos))
        .isInstanceOf(ArquivoInvalidoException.class)
        .hasMessageContaining("no máximo");
  }

  @Test
  @DisplayName("arquivo com content-type não-PDF vira status ERRO, sem derrubar o lote")
  void arquivoNaoPdfViraErro() {
    MockMultipartFile texto =
        new MockMultipartFile("arquivos", "nota.txt", "text/plain", "conteúdo".getBytes());

    ImportacaoService.ResultadoAnalise resultado = service.analisar(new MultipartFile[] {texto});

    assertThat(resultado.documentos()).hasSize(1);
    assertThat(resultado.documentos().get(0).status()).isEqualTo("ERRO");
    assertThat(resultado.documentos().get(0).mensagem()).contains("não é um PDF");
    verify(iaExtracaoService, never()).extrair(anyString(), anyList(), anyString());
  }

  @Test
  @DisplayName("arquivo sem magic bytes %PDF vira status ERRO (D-04)")
  void magicBytesInvalidosViraErro() {
    MockMultipartFile falso =
        new MockMultipartFile("arquivos", "falso.pdf", "application/pdf", "não é pdf".getBytes());

    ImportacaoService.ResultadoAnalise resultado = service.analisar(new MultipartFile[] {falso});

    assertThat(resultado.documentos()).hasSize(1);
    assertThat(resultado.documentos().get(0).status()).isEqualTo("ERRO");
    assertThat(resultado.documentos().get(0).mensagem()).contains("não é um PDF válido");
  }

  @Test
  @DisplayName("arquivo vazio no lote vira status ERRO")
  void arquivoVazioViraErro() {
    MockMultipartFile vazio =
        new MockMultipartFile("arquivos", "vazio.pdf", "application/pdf", new byte[0]);

    ImportacaoService.ResultadoAnalise resultado = service.analisar(new MultipartFile[] {vazio});

    assertThat(resultado.documentos()).hasSize(1);
    assertThat(resultado.documentos().get(0).status()).isEqualTo("ERRO");
    assertThat(resultado.documentos().get(0).mensagem()).contains("vazio");
  }

  /* ============================ caminho feliz ============================ */

  @Test
  @DisplayName("PDF válido: status ANALISADO, staging gravado, categoria mapeada")
  void pdfValidoEhAnalisado() throws IOException {
    when(pdfTextoExtractor.extrair(any())).thenReturn("fatura cartão vencimento 2026-12-10");
    when(iaExtracaoService.extrair(anyString(), anyList(), anyString()))
        .thenReturn(
            new IaExtracaoService.ResultadoExtracao(
                List.of(
                    new IaExtracaoService.ItemExtraido(
                        LocalDate.of(2026, 11, 3),
                        "Almoço no restaurante",
                        new BigDecimal("45.00"),
                        "DESPESA_PAGA",
                        "Alimentação",
                        "CARTAO",
                        1,
                        3)),
                new IaExtracaoService.FaturaExtraida(
                    new BigDecimal("1500.00"),
                    LocalDate.of(2026, 12, 10),
                    LocalDate.of(2026, 11, 1)),
                "google/gemma-4-31b-it:free",
                false,
                List.of(),
                String.valueOf(EMPRESA_ID)));

    ImportacaoService.ResultadoAnalise resultado =
        service.analisar(new MultipartFile[] {pdfValido("fatura.pdf")});

    assertThat(resultado.documentos()).hasSize(1);
    ImportacaoService.DocumentoAnalise doc = resultado.documentos().get(0);
    assertThat(doc.status()).isEqualTo("ANALISADO");
    assertThat(doc.documentoId()).isEqualTo(100L);
    assertThat(doc.tipoDocumento()).isEqualTo("CARTAO");
    assertThat(doc.modeloUsado()).isEqualTo("google/gemma-4-31b-it:free");
    assertThat(doc.degradado()).isFalse();

    // staging: documento + 1 item; fatura salva e vinculada
    verify(documentoRepository).save(any(ImportacaoDocumento.class));
    verify(itemRepository).save(any(ImportacaoItem.class));
    verify(faturaRepository).save(any(ImportacaoFatura.class));

    // item devolvido com id de staging, categoriaId resolvida e origem da classificação local
    assertThat(doc.itens()).hasSize(1);
    ImportacaoService.ItemAnalise item = doc.itens().get(0);
    assertThat(item.id()).isEqualTo(500L);
    assertThat(item.categoriaId()).isEqualTo(9L);
    assertThat(item.valor().scale()).isEqualTo(2); // NUMERIC(14,2)
    assertThat(item.origem()).isEqualTo("CARTAO");
  }

  @Test
  @DisplayName("staging não grava lancamento nem conta_a_pagar (D-02: confirmação é 05-05)")
  void analisarNaoGravaDefinitivo() throws IOException {
    when(pdfTextoExtractor.extrair(any())).thenReturn("extrato pix transferência");
    when(iaExtracaoService.extrair(anyString(), anyList(), anyString()))
        .thenReturn(
            new IaExtracaoService.ResultadoExtracao(
                List.of(), null, "m1", false, List.of(), String.valueOf(EMPRESA_ID)));

    service.analisar(new MultipartFile[] {pdfValido("extrato.pdf")});

    // nada em lancamento/conta_a_pagar: o repositório de lançamentos é usado só para ler chaves
    // dedupe; confirmação grava em 05-05, não aqui (D-02).
    verify(lancamentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("categoria sem correspondência → alerta REVISAR_CATEGORIA no item (D-25)")
  void categoriaDesconhecidaGeraAlerta() throws IOException {
    when(mapeadorCategoria.mapear(any(), any(), any()))
        .thenReturn(new MapeadorCategoria.ResultadoMapeamento(9L, "Outros", true));
    when(pdfTextoExtractor.extrair(any())).thenReturn("fatura cartão");
    when(iaExtracaoService.extrair(anyString(), anyList(), anyString()))
        .thenReturn(
            new IaExtracaoService.ResultadoExtracao(
                List.of(
                    new IaExtracaoService.ItemExtraido(
                        LocalDate.of(2026, 11, 3),
                        "Compra em loja exótica",
                        new BigDecimal("99.90"),
                        "DESPESA_PAGA",
                        "Loja Exótica",
                        "CARTAO",
                        null,
                        null)),
                null,
                "m1",
                false,
                List.of(),
                String.valueOf(EMPRESA_ID)));

    ImportacaoService.ResultadoAnalise resultado =
        service.analisar(new MultipartFile[] {pdfValido("fatura.pdf")});

    assertThat(resultado.documentos().get(0).itens().get(0).alerta())
        .contains("REVISAR_CATEGORIA")
        .contains("Loja Exótica");
  }

  /* ============================ duplicidade de arquivo ============================ */

  @Test
  @DisplayName("mesmo SHA-256 já importado → status DUPLICADO, IA não chamada")
  void arquivoDuplicado() {
    when(documentoRepository.findByEmpresaIdAndHashSha256(eq(EMPRESA_ID), anyString()))
        .thenReturn(new ImportacaoDocumento());

    ImportacaoService.ResultadoAnalise resultado =
        service.analisar(new MultipartFile[] {pdfValido("fatura.pdf")});

    assertThat(resultado.documentos()).hasSize(1);
    assertThat(resultado.documentos().get(0).status()).isEqualTo("DUPLICADO");
    verify(iaExtracaoService, never()).extrair(anyString(), anyList(), anyString());
    verify(itemRepository, never()).save(any());
  }

  /* ============================ PDF sem texto ============================ */

  @Test
  @DisplayName("PDF digitalizado → status SEM_TEXTO, documento no histórico, lote segue")
  void pdfSemTexto() throws IOException {
    when(pdfTextoExtractor.extrair(any())).thenThrow(new SemTextoExtraivelException("sem texto"));

    ImportacaoService.ResultadoAnalise resultado =
        service.analisar(new MultipartFile[] {pdfValido("digitalizado.pdf")});

    assertThat(resultado.documentos()).hasSize(1);
    assertThat(resultado.documentos().get(0).status()).isEqualTo("SEM_TEXTO");
    assertThat(resultado.documentos().get(0).mensagem()).contains("digitalizado");
    verify(documentoRepository).save(any(ImportacaoDocumento.class));
    verify(iaExtracaoService, never()).extrair(anyString(), anyList(), anyString());
  }

  /* ============================ IA indisponível ============================ */

  @Test
  @DisplayName("IA fora em todos os arquivos → 503 (nenhum documento analisado)")
  void iaForaEmTodosLanca503() throws IOException {
    when(pdfTextoExtractor.extrair(any())).thenReturn("fatura cartão");
    when(iaExtracaoService.extrair(anyString(), anyList(), anyString()))
        .thenThrow(new ServicoIaIndisponivelException("IA fora"));

    assertThatThrownBy(
            () -> service.analisar(new MultipartFile[] {pdfValido("a.pdf"), pdfValido("b.pdf")}))
        .isInstanceOf(ServicoIaIndisponivelException.class)
        .hasMessageContaining("indisponível");
  }

  @Test
  @DisplayName("IA fora em um arquivo, outro analisado → lote responde com ERRO no arquivo")
  void iaForaEmUmArquivoNaoDerrubaLote() throws IOException {
    when(pdfTextoExtractor.extrair(any())).thenReturn("extrato pix");
    when(iaExtracaoService.extrair(anyString(), anyList(), anyString()))
        .thenReturn(
            new IaExtracaoService.ResultadoExtracao(
                List.of(), null, "m1", false, List.of(), String.valueOf(EMPRESA_ID)))
        .thenThrow(new ServicoIaIndisponivelException("IA fora"));

    ImportacaoService.ResultadoAnalise resultado =
        service.analisar(new MultipartFile[] {pdfValido("ok.pdf"), pdfValido("falha.pdf")});

    assertThat(resultado.documentos()).hasSize(2);
    assertThat(resultado.documentos().stream().map(ImportacaoService.DocumentoAnalise::status))
        .containsExactly("ANALISADO", "ERRO");
  }

  /* ============================ consulta e descarte ============================ */

  @Test
  @DisplayName("listar devolve página do tenant com resumos")
  void listarPaginaDoTenant() {
    ImportacaoDocumento doc = documento(7L, "ANALISADO");
    when(documentoRepository.findByEmpresaIdOrderByIdDesc(eq(EMPRESA_ID), any()))
        .thenReturn(new PageImpl<>(List.of(doc), PageRequest.of(0, 20), 1));

    Page<ImportacaoService.DocumentoResumo> pagina = service.listar(PageRequest.of(0, 20));

    assertThat(pagina.getTotalElements()).isEqualTo(1);
    assertThat(pagina.getContent().get(0).id()).isEqualTo(7L);
    assertThat(pagina.getContent().get(0).status()).isEqualTo("ANALISADO");
  }

  @Test
  @DisplayName("detalhar devolve documento + itens + fatura + resumo")
  void detalharDocumento() {
    ImportacaoDocumento doc = documento(7L, "ANALISADO");
    when(documentoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(doc));

    ImportacaoItem item = new ImportacaoItem();
    item.setId(500L);
    item.setData(LocalDate.of(2026, 11, 3));
    item.setDescricao("Almoço");
    item.setValor(new BigDecimal("45.00"));
    item.setTipo("DESPESA_PAGA");
    item.setCategoriaNome("Alimentação");
    item.setCategoriaId(1L);
    item.setOrigem("CARTAO");
    when(itemRepository.findByDocumentoId(7L)).thenReturn(List.of(item));
    when(faturaRepository.findByDocumentoId(7L)).thenReturn(null);

    ImportacaoService.DocumentoDetalhe detalhe = service.detalhar(7L);

    assertThat(detalhe.id()).isEqualTo(7L);
    assertThat(detalhe.itens()).hasSize(1);
    assertThat(detalhe.fatura()).isNull();
    assertThat(detalhe.resumo().totalPago()).isEqualByComparingTo("45.00");
  }

  @Test
  @DisplayName("detalhar documento de outra empresa → 404 (isolamento R3)")
  void detalharDeOutraEmpresaLanca404() {
    when(documentoRepository.findByIdAndEmpresaId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.detalhar(99L))
        .isInstanceOf(ImportacaoNaoEncontradaException.class);
  }

  @Test
  @DisplayName("descartar importação pendente → status DESCARTADO")
  void descartarPendente() {
    ImportacaoDocumento doc = documento(7L, "ANALISADO");
    when(documentoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(doc));

    service.descartar(7L);

    assertThat(doc.getStatus()).isEqualTo("DESCARTADO");
    verify(documentoRepository).save(doc);
  }

  @Test
  @DisplayName("descartar importação já confirmada → 409 (estado inválido)")
  void descartarConfirmadaLanca409() {
    ImportacaoDocumento doc = documento(7L, "CONFIRMADO");
    when(documentoRepository.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(doc));

    assertThatThrownBy(() -> service.descartar(7L))
        .isInstanceOf(ImportacaoEstadoInvalidoException.class);
    verify(documentoRepository, never()).save(any());
  }

  @Test
  @DisplayName("descartar inexistente → 404")
  void descartarInexistenteLanca404() {
    when(documentoRepository.findByIdAndEmpresaId(42L, EMPRESA_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.descartar(42L))
        .isInstanceOf(ImportacaoNaoEncontradaException.class);
  }

  /* ============================ fixtures ============================ */

  /** PDF falso válido para o fluxo (bytes %PDF suficientes para o extrator mockado). */
  private static MockMultipartFile pdfValido(String nome) {
    byte[] bytes = "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.UTF_8);
    return new MockMultipartFile("arquivos", nome, "application/pdf", bytes);
  }

  private static ImportacaoDocumento documento(Long id, String status) {
    ImportacaoDocumento doc = new ImportacaoDocumento();
    doc.setId(id);
    doc.setNomeArquivo("fatura-" + id + ".pdf");
    doc.setTipoDocumento("CARTAO");
    doc.setHashSha256("hash-" + id);
    doc.setTamanhoBytes(1234);
    doc.setStatus(status);
    return doc;
  }
}
