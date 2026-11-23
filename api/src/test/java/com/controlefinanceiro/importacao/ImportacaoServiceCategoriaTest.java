package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.conta.ContaAPagarRepository;
import com.controlefinanceiro.conta.FaturaCartaoRepository;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

class ImportacaoServiceCategoriaTest {

  private static final Long EMPRESA_ID = 1L;

  private CategoriaRepository categorias;
  private ImportacaoService service;

  @BeforeEach
  void setup() {
    PdfTextoExtractor extrator = mock(PdfTextoExtractor.class);
    IaExtracaoService ia = mock(IaExtracaoService.class);
    ImportacaoDocumentoRepository documentos = mock(ImportacaoDocumentoRepository.class);
    ImportacaoItemRepository itens = mock(ImportacaoItemRepository.class);
    ImportacaoFaturaRepository faturas = mock(ImportacaoFaturaRepository.class);
    LancamentoRepository lancamentos = mock(LancamentoRepository.class);
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    categorias = mock(CategoriaRepository.class);

    Categoria alimentacao = new Categoria();
    alimentacao.setId(1L);
    alimentacao.setNome("Alimentação");
    alimentacao.setTipo("SAIDA");
    alimentacao.setAtivo(true);
    when(categorias.findByEmpresaId(EMPRESA_ID)).thenReturn(List.of(alimentacao));

    Usuario usuario = new Usuario();
    usuario.setId(7L);
    Empresa empresa = new Empresa();
    empresa.setId(EMPRESA_ID);
    usuario.setEmpresa(empresa);
    when(usuarios.findByIdAndEmpresaId(7L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
    when(documentos.save(any(ImportacaoDocumento.class)))
        .thenAnswer(
            inv -> {
              ImportacaoDocumento d = inv.getArgument(0);
              if (d.getId() == null) {
                d.setId(100L);
              }
              return d;
            });
    when(itens.save(any(ImportacaoItem.class)))
        .thenAnswer(
            inv -> {
              ImportacaoItem i = inv.getArgument(0);
              if (i.getId() == null) {
                i.setId(500L);
              }
              return i;
            });
    when(lancamentos.findChavesDedupeByEmpresaIdAndUsuarioId(EMPRESA_ID, 7L)).thenReturn(List.of());

    try {
      when(extrator.extrair(any())).thenReturn("extrato pix mercado");
      when(ia.extrair(anyString(), anyList(), anyString()))
          .thenReturn(
              new IaExtracaoService.ResultadoExtracao(
                  List.of(item("Mercado"), item("Restaurante"), item("Padaria"), item("Farmácia")),
                  null,
                  "modelo",
                  false,
                  List.of(),
                  String.valueOf(EMPRESA_ID)));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    service =
        new ImportacaoService(
            extrator,
            ia,
            new MapeadorCategoria(categorias),
            new AnalisadorConsolidado(),
            documentos,
            itens,
            faturas,
            mock(FaturaCartaoRepository.class),
            mock(ContaAPagarRepository.class),
            categorias,
            lancamentos,
            usuarios,
            new TransactionTemplate(mock(PlatformTransactionManager.class)));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UsuarioAuth("usuario@teste.com", EMPRESA_ID, 7L, 0, "USER"), null, List.of()));
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("lote com 2 PDFs e 4 itens consulta categorias uma única vez")
  void categoriasCarregadasUmaVezPorLote() {
    ImportacaoService.ResultadoAnalise resultado =
        service.analisar(new MultipartFile[] {pdf("a.pdf"), pdf("b.pdf")});

    assertThat(resultado.documentos()).hasSize(2);
    verify(categorias, times(1)).findByEmpresaId(EMPRESA_ID);
  }

  private static IaExtracaoService.ItemExtraido item(String descricao) {
    return new IaExtracaoService.ItemExtraido(
        LocalDate.of(2026, 11, 3),
        descricao,
        new BigDecimal("10.00"),
        "DESPESA_PAGA",
        "Alimentação",
        "EXTRATO",
        null,
        null);
  }

  private static MultipartFile pdf(String nome) {
    return new MockMultipartFile(
        "arquivos", nome, "application/pdf", "%PDF-1.4 teste".getBytes(StandardCharsets.UTF_8));
  }
}
