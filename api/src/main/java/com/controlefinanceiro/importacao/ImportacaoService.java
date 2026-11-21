package com.controlefinanceiro.importacao;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioContext;
import com.controlefinanceiro.conta.ContaAPagar;
import com.controlefinanceiro.conta.ContaAPagarRepository;
import com.controlefinanceiro.conta.FaturaCartao;
import com.controlefinanceiro.conta.FaturaCartaoRepository;
import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
import com.controlefinanceiro.lancamento.Lancamento;
import com.controlefinanceiro.lancamento.LancamentoRepository;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orquestração da análise de PDFs (fase 5): valida o arquivo, extrai texto (PDFBox), sanitiza,
 * classifica, chama a IA (1 chamada por documento), mapeia categorias, detecta duplicidades e grava
 * <strong>apenas o staging</strong> com status {@code ANALISADO}.
 *
 * <p>D-02: nada é gravado em {@code lancamento} ou {@code conta_a_pagar} aqui — a confirmação
 * (05-05) é uma etapa separada, explícita e transacional. R3: {@code empresa_id} e o usuário vêm
 * exclusivamente do token ({@link TenantContext} e {@link UsuarioContext}), nunca do body. D-14: o
 * PDF bruto nunca é persistido — só hash SHA-256, metadados e o resultado estruturado.
 *
 * <p>Uma falha em um arquivo não derruba os demais (try/catch por arquivo). Quando a IA está
 * indisponível e <em>nenhum</em> documento foi analisado, propaga {@link
 * ServicoIaIndisponivelException} (503 amigável, D-23) — sem staging parcial órfão.
 */
@Service
public class ImportacaoService {

  private static final Logger LOG = LoggerFactory.getLogger(ImportacaoService.class);

  /** Content-Type esperado (browsers enviam application/pdf). */
  private static final String CONTENT_TYPE_PDF = "application/pdf";

  /** Magic bytes de um PDF real (proteção contra arquivo disfarçado, D-04). */
  private static final byte[] MAGIC_PDF = {'%', 'P', 'D', 'F'};

  /** Limite por arquivo, igual ao multipart do application.yml. */
  private static final long TAMANHO_MAXIMO_BYTES = 8 * 1024 * 1024;

  /** Limite de arquivos por lote (protege a cota de 50 req/dia dos modelos :free, D-19). */
  private static final int MAXIMO_ARQUIVOS = 10;

  private final PdfTextoExtractor pdfTextoExtractor;
  private final IaExtracaoService iaExtracaoService;
  private final MapeadorCategoria mapeadorCategoria;
  private final AnalisadorConsolidado analisadorConsolidado;
  private final ImportacaoDocumentoRepository documentoRepository;
  private final ImportacaoItemRepository itemRepository;
  private final ImportacaoFaturaRepository faturaRepository;
  private final FaturaCartaoRepository faturaCartaoRepository;
  private final ContaAPagarRepository contaAPagarRepository;
  private final CategoriaRepository categoriaRepository;
  private final LancamentoRepository lancamentoRepository;
  private final UsuarioRepository usuarioRepository;
  private final TransactionTemplate transactionTemplate;

  public ImportacaoService(
      PdfTextoExtractor pdfTextoExtractor,
      IaExtracaoService iaExtracaoService,
      MapeadorCategoria mapeadorCategoria,
      AnalisadorConsolidado analisadorConsolidado,
      ImportacaoDocumentoRepository documentoRepository,
      ImportacaoItemRepository itemRepository,
      ImportacaoFaturaRepository faturaRepository,
      FaturaCartaoRepository faturaCartaoRepository,
      ContaAPagarRepository contaAPagarRepository,
      CategoriaRepository categoriaRepository,
      LancamentoRepository lancamentoRepository,
      UsuarioRepository usuarioRepository,
      TransactionTemplate transactionTemplate) {
    this.pdfTextoExtractor = pdfTextoExtractor;
    this.iaExtracaoService = iaExtracaoService;
    this.mapeadorCategoria = mapeadorCategoria;
    this.analisadorConsolidado = analisadorConsolidado;
    this.documentoRepository = documentoRepository;
    this.itemRepository = itemRepository;
    this.faturaRepository = faturaRepository;
    this.faturaCartaoRepository = faturaCartaoRepository;
    this.contaAPagarRepository = contaAPagarRepository;
    this.categoriaRepository = categoriaRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.usuarioRepository = usuarioRepository;
    this.transactionTemplate = transactionTemplate;
  }

  /** Resultado da análise do lote (DTO de resposta, Jackson serializa direto). */
  public record ResultadoAnalise(
      List<DocumentoAnalise> documentos, AnalisadorConsolidado.ResumoAnalitico resumo) {}

  /** Um documento do lote: status, itens de staging e rejeitados da IA. */
  public record DocumentoAnalise(
      String nomeArquivo,
      String status, // ANALISADO | DUPLICADO | SEM_TEXTO | ERRO
      String mensagem,
      Long documentoId,
      String tipoDocumento,
      String modeloUsado,
      Boolean degradado,
      List<ItemAnalise> itens,
      List<Rejeitado> rejeitados) {}

  /** Item de staging devolvido para a tela de revisão. */
  public record ItemAnalise(
      Long id,
      LocalDate data,
      String descricao,
      BigDecimal valor,
      String tipo,
      String categoriaNome,
      Long categoriaId,
      String origem,
      Integer parcelaAtual,
      Integer parcelaTotal,
      String alerta) {}

  /** Item que a IA extraiu de forma inválida (conteúdo truncado, para auditoria). */
  public record Rejeitado(String motivo, String conteudoBruto) {}

  /**
   * Analisa um lote de PDFs e grava o staging. O tenant vem do token — parâmetro apenas para
   * clareza interna dos testes de unidade.
   *
   * @param arquivos PDFs (1..10)
   * @return documentos analisados, resumo do mês e alertas
   */
  public ResultadoAnalise analisar(MultipartFile[] arquivos) {
    return analisar(TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), arquivos);
  }

  public ResultadoAnalise analisar(Long empresaId, Long usuarioId, MultipartFile[] arquivos) {
    if (arquivos == null || arquivos.length == 0) {
      throw new ArquivoInvalidoException("Envie pelo menos um arquivo PDF.");
    }
    if (arquivos.length > MAXIMO_ARQUIVOS) {
      throw new ArquivoInvalidoException(
          ("Envie no máximo %d arquivos por vez (limite de cota da IA).")
              .formatted(MAXIMO_ARQUIVOS));
    }

    Usuario usuario = usuario(empresaId, usuarioId);
    List<String> categoriasDoTenant = nomesDeCategorias(empresaId);

    List<DocumentoAnalise> documentos = new ArrayList<>();
    List<ImportacaoItem> itensDoLote = new ArrayList<>();
    List<ImportacaoFatura> faturasDoLote = new ArrayList<>();
    int errosDeIa = 0;

    for (MultipartFile arquivo : arquivos) {
      String nome = nomeDoArquivo(arquivo);
      try {
        DocumentoAnalise resultado =
            analisarArquivo(arquivo, empresaId, usuarioId, usuario, categoriasDoTenant);
        documentos.add(resultado);
        if ("ANALISADO".equals(resultado.status())) {
          itensDoLote.addAll(
              itemRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(
                  resultado.documentoId(), empresaId, usuarioId));
          ImportacaoFatura fatura =
              faturaRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(
                  resultado.documentoId(), empresaId, usuarioId);
          if (fatura != null) {
            faturasDoLote.add(fatura);
          }
        }
      } catch (ServicoIaIndisponivelException e) {
        errosDeIa++;
        LOG.warn("IA indisponível ao analisar {}: {}", nome, e.getMessage());
        documentos.add(
            new DocumentoAnalise(
                nome,
                "ERRO",
                "IA indisponível no momento. Tente novamente em instantes.",
                null,
                null,
                null,
                null,
                List.of(),
                List.of()));
      } catch (ArquivoInvalidoException e) {
        documentos.add(
            new DocumentoAnalise(
                nome, "ERRO", e.getMessage(), null, null, null, null, List.of(), List.of()));
      } catch (PdfDemasiadoGrandeException e) {
        LOG.warn("PDF demasiado grande {}: {}", nome, e.getMessage());
        documentos.add(
            new DocumentoAnalise(
                nome, "ERRO", e.getMessage(), null, null, null, null, List.of(), List.of()));
      } catch (IOException e) {
        LOG.warn("Falha ao ler {}: {}", nome, e.getMessage());
        documentos.add(
            new DocumentoAnalise(
                nome,
                "ERRO",
                "Falha ao ler o arquivo.",
                null,
                null,
                null,
                null,
                List.of(),
                List.of()));
      }
    }

    // D-23: quando NADA foi analisado e a IA está fora, 503 amigável (sem staging órfão).
    boolean nenhumAnalisado = documentos.stream().noneMatch(d -> "ANALISADO".equals(d.status()));
    if (nenhumAnalisado && errosDeIa > 0) {
      throw new ServicoIaIndisponivelException(
          "O serviço de IA está indisponível no momento. Tente novamente em instantes.");
    }

    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisadorConsolidado.montarResumo(itensDoLote, faturaPrincipal(faturasDoLote), null, null);
    return new ResultadoAnalise(documentos, resumo);
  }

  /* ============================ consulta e descarte ============================ */

  /** Linha do histórico de importações (tela de listagem). */
  public record DocumentoResumo(
      Long id,
      String nomeArquivo,
      String tipoDocumento,
      String status,
      Integer tamanhoBytes,
      OffsetDateTime criadoEm) {}

  /** Cabeçalho de fatura devolvido no detalhe. */
  public record FaturaView(BigDecimal total, LocalDate dataVencimento, LocalDate mesReferencia) {}

  /** Detalhe completo de uma importação: documento + itens + fatura + resumo. */
  public record DocumentoDetalhe(
      Long id,
      String nomeArquivo,
      String tipoDocumento,
      String status,
      OffsetDateTime criadoEm,
      List<ItemAnalise> itens,
      FaturaView fatura,
      AnalisadorConsolidado.ResumoAnalitico resumo) {}

  /** Histórico da empresa/usuário do token, paginado. */
  public Page<DocumentoResumo> listar(Pageable pageable) {
    return listar(TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), pageable);
  }

  public Page<DocumentoResumo> listar(Long empresaId, Long usuarioId, Pageable pageable) {
    return documentoRepository
        .findByEmpresaIdAndUsuarioIdOrderByIdDesc(empresaId, usuarioId, pageable)
        .map(this::toResumo);
  }

  /** Detalhe de uma importação do tenant/usuário (itens + fatura + resumo recalculado). */
  public DocumentoDetalhe detalhar(Long id) {
    return detalhar(TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), id);
  }

  public DocumentoDetalhe detalhar(Long empresaId, Long usuarioId, Long id) {
    ImportacaoDocumento documento =
        documentoRepository
            .findByIdAndEmpresaIdAndUsuarioId(id, empresaId, usuarioId)
            .orElseThrow(
                () ->
                    new ImportacaoNaoEncontradaException(
                        "Importação %d não encontrada.".formatted(id)));

    List<ImportacaoItem> itens =
        itemRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(
            documento.getId(), empresaId, usuarioId);
    ImportacaoFatura fatura =
        faturaRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(
            documento.getId(), empresaId, usuarioId);
    AnalisadorConsolidado.ResumoAnalitico resumo =
        analisadorConsolidado.montarResumo(itens, fatura, null, null);

    FaturaView faturaView =
        fatura == null
            ? null
            : new FaturaView(
                fatura.getTotal(), fatura.getDataVencimento(), fatura.getMesReferencia());

    return new DocumentoDetalhe(
        documento.getId(),
        documento.getNomeArquivo(),
        documento.getTipoDocumento(),
        documento.getStatus(),
        documento.getCriadoEm(),
        itens.stream().map(this::toItemAnalise).toList(),
        faturaView,
        resumo);
  }

  /**
   * Descarta uma importação pendente (status → DESCARTADO). Importação já confirmada não pode ser
   * descartada (os lançamentos já existem) → 409.
   */
  public void descartar(Long id) {
    descartar(TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), id);
  }

  public void descartar(Long empresaId, Long usuarioId, Long id) {
    transactionTemplate.executeWithoutResult(
        status -> {
          ImportacaoDocumento documento =
              documentoRepository
                  .findByIdAndEmpresaIdAndUsuarioId(id, empresaId, usuarioId)
                  .orElseThrow(
                      () ->
                          new ImportacaoNaoEncontradaException(
                              "Importação %d não encontrada.".formatted(id)));
          if ("CONFIRMADO".equals(documento.getStatus())) {
            throw new ImportacaoEstadoInvalidoException(
                "Importação já confirmada — descarte os lançamentos individualmente, não o"
                    + " documento.");
          }
          documento.setStatus("DESCARTADO");
          documentoRepository.save(documento);
        });
  }

  private DocumentoResumo toResumo(ImportacaoDocumento documento) {
    return new DocumentoResumo(
        documento.getId(),
        documento.getNomeArquivo(),
        documento.getTipoDocumento(),
        documento.getStatus(),
        documento.getTamanhoBytes(),
        documento.getCriadoEm());
  }

  /* ============================ confirmação (05-05) ============================ */

  /** Resultado da confirmação (D-07): contagens e ids gravados. */
  public record ResultadoConfirmacao(
      int importados,
      int ignorados,
      int duplicados,
      List<Long> lancamentoIds,
      List<Long> contaAPagarIds) {}

  /**
   * Confirma a gravação dos itens selecionados — transação tudo-ou-nada (D-07).
   *
   * <p>Receita → {@code lancamento} tipo ENTRADA; DESPESA_PAGA → {@code lancamento} tipo SAIDA;
   * A_PAGAR → {@code conta_a_pagar} (categoria SAIDA, NÃO entra no saldo do dashboard, D-11) +
   * {@code fatura_cartao} uma única vez quando o documento tem cabeçalho de fatura. Itens já
   * IMPORTADO são pulados (idempotência) e itens com chave de dedupe já existente contam como
   * duplicados (D-08). Falha em qualquer linha → {@link LinhaInvalidaException} com o índice da
   * linha e rollback completo.
   *
   * @param importacaoId documento a confirmar (do tenant do token, R3)
   * @param request seleção do usuário (itens a incluir, overrides de categoria)
   */
  public ResultadoConfirmacao confirmar(Long importacaoId, ConfirmarRequest request) {
    return confirmar(
        TenantContext.getEmpresaId(), UsuarioContext.getUsuarioId(), importacaoId, request);
  }

  public ResultadoConfirmacao confirmar(
      Long empresaId, Long usuarioId, Long importacaoId, ConfirmarRequest request) {
    return transactionTemplate.execute(
        status -> {
          ImportacaoDocumento documento =
              documentoRepository
                  .findByIdAndEmpresaIdAndUsuarioId(importacaoId, empresaId, usuarioId)
                  .orElseThrow(
                      () ->
                          new ImportacaoNaoEncontradaException(
                              "Importação %d não encontrada.".formatted(importacaoId)));
          if ("DESCARTADO".equals(documento.getStatus())
              || "SEM_TEXTO".equals(documento.getStatus())) {
            throw new ImportacaoEstadoInvalidoException(
                "Importação %d está %s — não há itens a confirmar."
                    .formatted(importacaoId, documento.getStatus()));
          }

          List<ImportacaoItem> itensStaging =
              itemRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(
                  documento.getId(), empresaId, usuarioId);
          Map<Long, ConfirmarRequest.ConfirmarItem> selecao =
              selecaoPorItem(request == null ? null : request.itens());

          List<Long> lancamentoIds = new ArrayList<>();
          List<Long> contaAPagarIds = new ArrayList<>();
          int importados = 0;
          int ignorados = 0;
          int duplicados = 0;

          // D-08: chaves de dedupe já gravadas; chaves novas entram aqui dentro para pegar
          // duplicidade dentro do próprio lote.
          Set<String> chavesGravadas =
              new HashSet<>(
                  lancamentoRepository.findChavesDedupeByEmpresaIdAndUsuarioId(
                      empresaId, usuarioId));

          Usuario usuario = usuario(empresaId, usuarioId);
          FaturaCartao faturaCartao = null;
          ImportacaoFatura faturaStaging =
              faturaRepository.findByDocumentoIdAndEmpresaIdAndUsuarioId(
                  documento.getId(), empresaId, usuarioId);

          for (int i = 0; i < itensStaging.size(); i++) {
            ImportacaoItem item = itensStaging.get(i);
            ConfirmarRequest.ConfirmarItem confirmarItem = selecao.get(item.getId());

            if (item.getStatus() != null
                && ("IMPORTADO".equals(item.getStatus()) || "IGNORADO".equals(item.getStatus()))) {
              // Idempotência: segunda confirmação não regrava (D-07).
              duplicados++;
              continue;
            }
            if (confirmarItem == null || Boolean.FALSE.equals(confirmarItem.incluir())) {
              // Item desmarcado: usuário decide não gravar; registra a decisão.
              item.setStatus("IGNORADO");
              itemRepository.save(item);
              ignorados++;
              continue;
            }

            try {
              Gravado gravado =
                  gravarItem(
                      documento, usuario, item, confirmarItem, chavesGravadas, faturaStaging);
              if (gravado.duplicado()) {
                duplicados++;
                continue;
              }
              if (gravado.lancamentoId() != null) {
                lancamentoIds.add(gravado.lancamentoId());
              } else {
                contaAPagarIds.add(gravado.contaAPagarId());
                if (gravado.faturaId() != null && faturaCartao == null) {
                  long faturaId = gravado.faturaId();
                  faturaCartao =
                      faturaCartaoRepository
                          .findByIdAndEmpresaIdAndUsuarioId(faturaId, empresaId, usuarioId)
                          .orElse(null);
                }
              }
              importados++;
            } catch (RuntimeException ex) {
              // LOT-02: índice base 0 da linha que falhou + rollback total da transação.
              throw new LinhaInvalidaException(i, ex.getMessage());
            }
          }

          documento.setStatus("CONFIRMADO");
          documentoRepository.save(documento);

          return new ResultadoConfirmacao(
              importados, ignorados, duplicados, lancamentoIds, contaAPagarIds);
        });
  }

  /** Item gravado: id do lançamento ou da conta a pagar (e da fatura quando criada). */
  private record Gravado(Long lancamentoId, Long contaAPagarId, Long faturaId, boolean duplicado) {}

  /**
   * Grava um item de staging como lançamento (ENTRADA/SAIDA) ou conta a pagar (A_PAGAR). Valida
   * categoria×tipo (CAT-04), valor (LAN-04/07) e dedupe (D-08).
   */
  private Gravado gravarItem(
      ImportacaoDocumento documento,
      Usuario usuario,
      ImportacaoItem item,
      ConfirmarRequest.ConfirmarItem confirmarItem,
      Set<String> chavesGravadas,
      ImportacaoFatura faturaStaging) {

    if (item.getValor() == null || item.getValor().signum() <= 0) {
      throw new IllegalArgumentException("valor do item deve ser positivo");
    }
    if (item.getData() == null) {
      throw new IllegalArgumentException("data do item é obrigatória");
    }

    String tipoCategoria = mapeadorCategoria.tipoCategoria(item.getTipo());

    // Categoria: override do usuário > mapeada na análise > re-mapeamento por nome (fallback).
    Long categoriaId =
        confirmarItem.categoriaIdOverride() != null
            ? confirmarItem.categoriaIdOverride()
            : item.getCategoriaId();
    if (categoriaId == null && item.getCategoriaNome() != null) {
      MapeadorCategoria.ResultadoMapeamento remapeado =
          mapeadorCategoria.mapear(
              item.getCategoriaNome(), item.getTipo(), documento.getEmpresa().getId());
      categoriaId = remapeado.categoriaId();
    }
    if (categoriaId == null) {
      throw new IllegalArgumentException(
          "categoria não resolvida para '%s' — escolha uma categoria na confirmação"
              .formatted(item.getCategoriaNome()));
    }

    final Long categoriaIdResolvida = categoriaId;
    Categoria categoria =
        categoriaRepository
            .findByIdAndEmpresaId(categoriaIdResolvida, documento.getEmpresa().getId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "categoria %d não encontrada na empresa".formatted(categoriaIdResolvida)));
    if (!tipoCategoria.equals(categoria.getTipo())) {
      throw new IllegalArgumentException(
          "categoria '%s' é do tipo %s, incompatível com item do tipo %s"
              .formatted(categoria.getNome(), categoria.getTipo(), tipoCategoria));
    }

    String chaveDedupe =
        Deduplicador.gerarChaveDedupe(
            tipoCategoria, item.getData(), item.getValor(), categoria.getNome());
    if (chavesGravadas.contains(chaveDedupe)) {
      // D-08: mesmo tipo+data+valor+categoria já gravado — não duplica, conta como duplicado.
      item.setStatus("IMPORTADO"); // marca para não reaparecer na próxima confirmação
      itemRepository.save(item);
      return new Gravado(null, null, null, true);
    }
    chavesGravadas.add(chaveDedupe);

    if ("A_PAGAR".equals(item.getTipo())) {
      Long contaAPagarId =
          gravarContaAPagar(documento, usuario, item, categoria, chaveDedupe, faturaStaging);
      return new Gravado(null, contaAPagarId, null, false);
    }

    Long lancamentoId =
        gravarLancamento(documento, usuario, item, categoria, tipoCategoria, chaveDedupe);
    return new Gravado(lancamentoId, null, null, false);
  }

  /** Receita/Despesa paga → lancamento (tipo ENTRADA|SAIDA; nunca muda o contrato D-05/D-11). */
  private Long gravarLancamento(
      ImportacaoDocumento documento,
      Usuario usuario,
      ImportacaoItem item,
      Categoria categoria,
      String tipoCategoria,
      String chaveDedupe) {
    Lancamento lancamento = new Lancamento();
    lancamento.setEmpresa(documento.getEmpresa());
    lancamento.setUsuario(usuario);
    lancamento.setCategoria(categoria);
    lancamento.setTipo(tipoCategoria);
    lancamento.setValor(item.getValor());
    lancamento.setData(item.getData());
    lancamento.setDescricao(item.getDescricao());
    lancamento.setOrigem(item.getOrigem());
    lancamento.setDocumentoId(documento.getId());
    lancamento.setChaveDedupe(chaveDedupe);

    Lancamento salvo = lancamentoRepository.save(lancamento);
    item.setStatus("IMPORTADO");
    item.setLancamentoId(salvo.getId());
    itemRepository.save(item);
    return salvo.getId();
  }

  /**
   * A_PAGAR → conta_a_pagar (categoria SAIDA) + fatura_cartao uma única vez por documento. Não
   * grava lancamento: compromisso futuro não entra no saldo (D-11).
   */
  private Long gravarContaAPagar(
      ImportacaoDocumento documento,
      Usuario usuario,
      ImportacaoItem item,
      Categoria categoria,
      String chaveDedupe,
      ImportacaoFatura faturaStaging) {
    Long faturaId = null;
    if (faturaStaging != null) {
      FaturaCartao faturaCartao =
          faturaCartaoRepository
              .findByDocumentoIdAndEmpresaIdAndUsuarioId(
                  documento.getId(), documento.getEmpresa().getId(), usuario.getId())
              .orElseGet(
                  () -> {
                    FaturaCartao nova = new FaturaCartao();
                    nova.setEmpresa(documento.getEmpresa());
                    nova.setUsuario(usuario);
                    nova.setDocumento(documento);
                    nova.setTotal(faturaStaging.getTotal());
                    nova.setDataVencimento(faturaStaging.getDataVencimento());
                    nova.setMesReferencia(faturaStaging.getMesReferencia());
                    nova.setDataFechamento(faturaStaging.getDataFechamento());
                    return faturaCartaoRepository.save(nova);
                  });
      faturaId = faturaCartao.getId();
    }

    ContaAPagar conta = new ContaAPagar();
    conta.setEmpresa(documento.getEmpresa());
    conta.setUsuario(usuario);
    conta.setFaturaId(faturaId);
    conta.setCategoria(categoria);
    conta.setDescricao(item.getDescricao());
    conta.setValor(item.getValor());
    conta.setData(item.getData());
    conta.setParcelaAtual(item.getParcelaAtual());
    conta.setParcelaTotal(item.getParcelaTotal());
    conta.setVencimento(faturaStaging != null ? faturaStaging.getDataVencimento() : item.getData());
    conta.setOrigem(item.getOrigem());

    ContaAPagar salvo = contaAPagarRepository.save(conta);
    item.setStatus("IMPORTADO");
    item.setContaAPagarId(salvo.getId());
    itemRepository.save(item);
    return salvo.getId();
  }

  /** Índice itemId → confirmação (nulos defensivos). */
  private Map<Long, ConfirmarRequest.ConfirmarItem> selecaoPorItem(
      List<ConfirmarRequest.ConfirmarItem> itens) {
    Map<Long, ConfirmarRequest.ConfirmarItem> mapa = new HashMap<>();
    if (itens != null) {
      for (ConfirmarRequest.ConfirmarItem item : itens) {
        if (item != null && item.itemId() != null) {
          mapa.put(item.itemId(), item);
        }
      }
    }
    return mapa;
  }

  /* ============================ fluxo por arquivo ============================ */

  private DocumentoAnalise analisarArquivo(
      MultipartFile arquivo,
      Long empresaId,
      Long usuarioId,
      Usuario usuario,
      List<String> categoriasDoTenant)
      throws IOException {
    validar(arquivo);

    byte[] bytes = arquivo.getBytes();
    String nome = nomeDoArquivo(arquivo);
    validarMagicBytes(bytes, nome); // D-04: %PDF no início — extensão .pdf não basta
    String hash = sha256(bytes);

    // D-09: mesmo arquivo + mesmo tenant já importado → DUPLICADO, sem novo staging.
    if (documentoRepository.findByEmpresaIdAndUsuarioIdAndHashSha256(empresaId, usuarioId, hash)
        != null) {
      return new DocumentoAnalise(
          nome,
          "DUPLICADO",
          "Este arquivo já foi importado antes.",
          null,
          null,
          null,
          null,
          List.of(),
          List.of());
    }

    String texto;
    try {
      texto = pdfTextoExtractor.extrair(bytes);
    } catch (SemTextoExtraivelException e) {
      // D-12: documento fica no histórico com SEM_TEXTO; o lote continua.
      ImportacaoDocumento documento =
          salvarDocumento(empresaId, usuario, nome, null, hash, bytes.length, null, "SEM_TEXTO");
      return new DocumentoAnalise(
          nome,
          "SEM_TEXTO",
          "PDF sem texto extraível (provavelmente digitalizado). Não é possível"
              + " analisar por IA.",
          documento.getId(),
          null,
          null,
          null,
          List.of(),
          List.of());
    }

    String sanitizado = SanitizadorTexto.sanitizar(texto); // D-13: antes de qualquer envio à IA
    String tipoDocumento = ClassificadorDocumento.classificar(nome, texto);

    IaExtracaoService.ResultadoExtracao extracao =
        iaExtracaoService.extrair(sanitizado, categoriasDoTenant, String.valueOf(empresaId));

    List<ImportacaoItem> itens = construirItens(empresaId, usuario, extracao, tipoDocumento);
    ImportacaoFatura fatura = construirFatura(empresaId, usuario, extracao, tipoDocumento);

    // D-09: dedupe contra lancamento (chave_dedupe) e itens de staging IMPORTADO.
    marcarDuplicadosExternos(empresaId, usuarioId, itens);

    // D-26: alertas por item (duplicidade, atípico, fatura no extrato) + resumo.
    AnalisadorConsolidado.ResumoAnalitico resumoDoDocumento =
        analisadorConsolidado.montarResumo(itens, fatura, null, null);

    // Staging atômico por documento (documento + itens + fatura): a chamada de IA (rede) fica
    // FORA da transação — só a gravação é transacional.
    ImportacaoDocumento documento =
        transactionTemplate.execute(
            status -> {
              ImportacaoDocumento doc =
                  salvarDocumento(
                      empresaId,
                      usuario,
                      nome,
                      tipoDocumento,
                      hash,
                      bytes.length,
                      extracao.modeloUsado(),
                      "ANALISADO");
              vincularESalvarItens(doc, itens, extracao);
              if (fatura != null) {
                // Tenant vem do documento (mesmo padrão dos itens: empresa via entidade, R3).
                fatura.setEmpresa(doc.getEmpresa());
                fatura.setUsuario(doc.getUsuario());
                fatura.setDocumento(doc);
                faturaRepository.save(fatura);
              }
              return doc;
            });

    if (documento == null) {
      throw new IllegalStateException("A transação não retornou o documento salvo.");
    }

    return new DocumentoAnalise(
        nome,
        "ANALISADO",
        resumoDoDocumento == null ? null : "Documento analisado com IA.",
        documento.getId(),
        tipoDocumento,
        extracao.modeloUsado(),
        extracao.degradado(),
        itens.stream().map(this::toItemAnalise).toList(),
        extracao.rejeitados().stream()
            .map(r -> new Rejeitado(r.motivo(), truncar(r.conteudoBruto(), 200)))
            .toList());
  }

  /* ============================ construção de entidades ============================ */

  private List<ImportacaoItem> construirItens(
      Long empresaId,
      Usuario usuario,
      IaExtracaoService.ResultadoExtracao extracao,
      String tipoDocumento) {
    List<ImportacaoItem> itens = new ArrayList<>();
    for (IaExtracaoService.ItemExtraido extraido : extracao.itens()) {
      // Origem da IA vs classificação do documento: quando divergem, vale a classificação local
      // (o documento como um todo é fatura ou extrato).
      String origem = tipoDocumento.equals("CARTAO") ? "CARTAO" : "EXTRATO";

      MapeadorCategoria.ResultadoMapeamento mapeamento =
          mapeadorCategoria.mapear(extraido.categoria(), tipoDoItem(extraido.tipo()), empresaId);

      ImportacaoItem item = new ImportacaoItem();
      item.setUsuario(usuario);
      item.setData(extraido.data());
      item.setDescricao(extraido.descricao());
      item.setValor(extraido.valor().setScale(2, java.math.RoundingMode.HALF_EVEN));
      item.setTipo(extraido.tipo());
      item.setCategoriaNome(extraido.categoria());
      item.setCategoriaId(mapeamento.categoriaId());
      item.setOrigem(origem);
      item.setParcelaAtual(extraido.parcelaAtual());
      item.setParcelaTotal(extraido.parcelaTotal());
      item.setStatus("PENDENTE");
      if (mapeamento.necessitaRevisao()) {
        item.setAlerta(
            Deduplicador.acrescentarAlerta(
                item.getAlerta(),
                "REVISAR_CATEGORIA: '%s' não existe — sugerido: %s"
                    .formatted(extraido.categoria(), mapeamento.categoriaNome())));
      }
      itens.add(item);
    }
    return itens;
  }

  private ImportacaoFatura construirFatura(
      Long empresaId,
      Usuario usuario,
      IaExtracaoService.ResultadoExtracao extracao,
      String tipoDocumento) {
    IaExtracaoService.FaturaExtraida extraida = extracao.fatura();
    if (extraida == null || !"CARTAO".equals(tipoDocumento)) {
      return null;
    }
    ImportacaoFatura fatura = new ImportacaoFatura();
    fatura.setUsuario(usuario);
    fatura.setTotal(extraida.total().setScale(2, java.math.RoundingMode.HALF_EVEN));
    if (extraida.vencimento() != null) {
      fatura.setDataVencimento(extraida.vencimento());
    } else {
      // Fatura sem vencimento claro: não é possível confirmar de forma confiável — descarta.
      LOG.warn("Fatura sem vencimento; cabeçalho ignorado");
      return null;
    }
    fatura.setMesReferencia(extraida.mesReferencia());
    return fatura;
  }

  private void vincularESalvarItens(
      ImportacaoDocumento doc,
      List<ImportacaoItem> itens,
      IaExtracaoService.ResultadoExtracao extracao) {
    for (int i = 0; i < itens.size(); i++) {
      ImportacaoItem item = itens.get(i);
      item.setEmpresa(doc.getEmpresa());
      item.setUsuario(doc.getUsuario());
      item.setDocumento(doc);
      itemRepository.save(item);
    }
  }

  private ImportacaoDocumento salvarDocumento(
      Long empresaId,
      Usuario usuario,
      String nome,
      String tipoDocumento,
      String hash,
      Integer tamanho,
      String modeloUsado,
      String status) {
    ImportacaoDocumento documento = new ImportacaoDocumento();
    documento.setEmpresa(usuario.getEmpresa());
    documento.setUsuario(usuario);
    documento.setNomeArquivo(nome);
    documento.setTipoDocumento(tipoDocumento != null ? tipoDocumento : "EXTRATO");
    documento.setHashSha256(hash);
    documento.setTamanhoBytes(tamanho);
    documento.setStatus(status);
    // modeloUsado: não há coluna — fica no log (D-22 expõe na resposta)
    LOG.info("Documento {} salvo com status {} (modelo IA: {})", nome, status, modeloUsado);
    return documentoRepository.save(documento);
  }

  private void marcarDuplicadosExternos(
      Long empresaId, Long usuarioId, List<ImportacaoItem> itens) {
    if (itens.isEmpty()) {
      return;
    }
    Set<String> chavesExistentes =
        new HashSet<>(
            lancamentoRepository.findChavesDedupeByEmpresaIdAndUsuarioId(empresaId, usuarioId));
    itemRepository.findByEmpresaIdAndUsuarioIdAndStatus(empresaId, usuarioId, "IMPORTADO").stream()
        .map(
            item ->
                Deduplicador.gerarChaveDedupe(
                    item.getTipo(), item.getData(), item.getValor(), item.getCategoriaNome()))
        .forEach(chavesExistentes::add);

    for (ImportacaoItem item : itens) {
      if (Deduplicador.ehDuplicado(item, chavesExistentes)) {
        item.setAlerta(
            Deduplicador.acrescentarAlerta(
                item.getAlerta(), "DUPLICADO_EXTERNO: já existe lançamento idêntico"));
      }
    }
  }

  private ItemAnalise toItemAnalise(ImportacaoItem item) {
    return new ItemAnalise(
        item.getId(),
        item.getData(),
        item.getDescricao(),
        item.getValor(),
        item.getTipo(),
        item.getCategoriaNome(),
        item.getCategoriaId(),
        item.getOrigem(),
        item.getParcelaAtual(),
        item.getParcelaTotal(),
        item.getAlerta());
  }

  /* ============================ helpers ============================ */

  private void validar(MultipartFile arquivo) {
    if (arquivo == null || arquivo.isEmpty()) {
      throw new ArquivoInvalidoException("Há um arquivo vazio no lote.");
    }
    if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
      throw new ArquivoInvalidoException("Arquivo excede o limite de 8 MB.");
    }
    String contentType = arquivo.getContentType();
    if (contentType != null && !contentType.toLowerCase().contains(CONTENT_TYPE_PDF)) {
      throw new ArquivoInvalidoException("Arquivo não é um PDF (%s).".formatted(contentType));
    }
  }

  /** Magic bytes %PDF no início do arquivo (D-04 — extensão .pdf não basta). */
  void validarMagicBytes(byte[] bytes, String nome) {
    if (bytes == null
        || bytes.length < 4
        || bytes[0] != MAGIC_PDF[0]
        || bytes[1] != MAGIC_PDF[1]
        || bytes[2] != MAGIC_PDF[2]
        || bytes[3] != MAGIC_PDF[3]) {
      throw new ArquivoInvalidoException("Arquivo %s não é um PDF válido.".formatted(nome));
    }
  }

  private ImportacaoFatura faturaPrincipal(List<ImportacaoFatura> faturas) {
    return faturas.isEmpty() ? null : faturas.get(0);
  }

  private String tipoDoItem(String tipoIA) {
    return tipoIA; // RECEITA/DESPESA_PAGA/A_PAGAR — MapeadorCategoria converte
  }

  private List<String> nomesDeCategorias(Long empresaId) {
    return categoriaRepository.findByEmpresaId(empresaId).stream()
        .filter(categoria -> categoria.isAtivo())
        .map(categoria -> categoria.getNome())
        .toList();
  }

  private String nomeDoArquivo(MultipartFile arquivo) {
    String original = arquivo.getOriginalFilename();
    return original != null && !original.isBlank() ? original : "documento.pdf";
  }

  private Usuario usuario(Long empresaId, Long usuarioId) {
    return usuarioRepository
        .findByIdAndEmpresaId(
            Objects.requireNonNull(usuarioId, "usuarioId"),
            Objects.requireNonNull(empresaId, "empresaId"))
        .orElseThrow(() -> new IllegalStateException("Usuário do token não encontrado."));
  }

  private String truncar(String texto, int limite) {
    if (texto == null) {
      return "";
    }
    return texto.length() <= limite ? texto : texto.substring(0, limite) + "...";
  }

  private String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(bytes);
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 não disponível", e);
    }
  }
}
