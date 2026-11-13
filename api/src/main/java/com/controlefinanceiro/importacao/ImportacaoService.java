package com.controlefinanceiro.importacao;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.importacao.model.ImportacaoDocumento;
import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * exclusivamente do token ({@link TenantContext}), nunca do body. D-14: o PDF bruto nunca é
 * persistido — só hash SHA-256, metadados e o resultado estruturado.
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
    if (arquivos == null || arquivos.length == 0) {
      throw new ArquivoInvalidoException("Envie pelo menos um arquivo PDF.");
    }
    if (arquivos.length > MAXIMO_ARQUIVOS) {
      throw new ArquivoInvalidoException(
          ("Envie no máximo %d arquivos por vez (limite de cota da IA).")
              .formatted(MAXIMO_ARQUIVOS));
    }

    Long empresaId = TenantContext.getEmpresaId();
    Usuario usuario = usuarioDoToken();
    List<String> categoriasDoTenant = nomesDeCategorias(empresaId);

    List<DocumentoAnalise> documentos = new ArrayList<>();
    List<ImportacaoItem> itensDoLote = new ArrayList<>();
    List<ImportacaoFatura> faturasDoLote = new ArrayList<>();
    int errosDeIa = 0;

    for (MultipartFile arquivo : arquivos) {
      String nome = nomeDoArquivo(arquivo);
      try {
        DocumentoAnalise resultado =
            analisarArquivo(arquivo, empresaId, usuario, categoriasDoTenant);
        documentos.add(resultado);
        if ("ANALISADO".equals(resultado.status())) {
          itensDoLote.addAll(itemRepository.findByDocumentoId(resultado.documentoId()));
          ImportacaoFatura fatura = faturaRepository.findByDocumentoId(resultado.documentoId());
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

  /** Histórico da empresa, paginado (tenant do token — R3). */
  public Page<DocumentoResumo> listar(Pageable pageable) {
    Long empresaId = TenantContext.getEmpresaId();
    return documentoRepository
        .findByEmpresaIdOrderByIdDesc(empresaId, pageable)
        .map(this::toResumo);
  }

  /** Detalhe de uma importação do tenant (itens + fatura + resumo recalculado). */
  public DocumentoDetalhe detalhar(Long id) {
    Long empresaId = TenantContext.getEmpresaId();
    ImportacaoDocumento documento =
        documentoRepository
            .findByIdAndEmpresaId(id, empresaId)
            .orElseThrow(
                () ->
                    new ImportacaoNaoEncontradaException(
                        "Importação %d não encontrada.".formatted(id)));

    List<ImportacaoItem> itens = itemRepository.findByDocumentoId(documento.getId());
    ImportacaoFatura fatura = faturaRepository.findByDocumentoId(documento.getId());
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
    Long empresaId = TenantContext.getEmpresaId();
    transactionTemplate.executeWithoutResult(
        status -> {
          ImportacaoDocumento documento =
              documentoRepository
                  .findByIdAndEmpresaId(id, empresaId)
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

  /* ============================ fluxo por arquivo ============================ */

  private DocumentoAnalise analisarArquivo(
      MultipartFile arquivo, Long empresaId, Usuario usuario, List<String> categoriasDoTenant)
      throws IOException {
    validar(arquivo);

    byte[] bytes = arquivo.getBytes();
    String nome = nomeDoArquivo(arquivo);
    validarMagicBytes(bytes, nome); // D-04: %PDF no início — extensão .pdf não basta
    String hash = sha256(bytes);

    // D-09: mesmo arquivo + mesmo tenant já importado → DUPLICADO, sem novo staging.
    if (documentoRepository.findByEmpresaIdAndHashSha256(empresaId, hash) != null) {
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

    List<ImportacaoItem> itens = construirItens(empresaId, extracao, tipoDocumento);
    ImportacaoFatura fatura = construirFatura(empresaId, extracao, tipoDocumento);

    // D-09: dedupe contra lancamento (chave_dedupe) e itens de staging IMPORTADO.
    marcarDuplicadosExternos(empresaId, itens);

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
                fatura.setDocumento(doc);
                faturaRepository.save(fatura);
              }
              return doc;
            });

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
      Long empresaId, IaExtracaoService.ResultadoExtracao extracao, String tipoDocumento) {
    List<ImportacaoItem> itens = new ArrayList<>();
    for (IaExtracaoService.ItemExtraido extraido : extracao.itens()) {
      // Origem da IA vs classificação do documento: quando divergem, vale a classificação local
      // (o documento como um todo é fatura ou extrato).
      String origem = tipoDocumento.equals("CARTAO") ? "CARTAO" : "EXTRATO";

      MapeadorCategoria.ResultadoMapeamento mapeamento =
          mapeadorCategoria.mapear(extraido.categoria(), tipoDoItem(extraido.tipo()), empresaId);

      ImportacaoItem item = new ImportacaoItem();
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
      Long empresaId, IaExtracaoService.ResultadoExtracao extracao, String tipoDocumento) {
    IaExtracaoService.FaturaExtraida extraida = extracao.fatura();
    if (extraida == null || !"CARTAO".equals(tipoDocumento)) {
      return null;
    }
    ImportacaoFatura fatura = new ImportacaoFatura();
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

  private void marcarDuplicadosExternos(Long empresaId, List<ImportacaoItem> itens) {
    if (itens.isEmpty()) {
      return;
    }
    Set<String> chavesExistentes =
        new HashSet<>(lancamentoRepository.findChavesDedupeByEmpresaId(empresaId));
    itemRepository.findByEmpresaIdAndStatus(empresaId, "IMPORTADO").stream()
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
        .filter(Categoria::isAtivo)
        .map(Categoria::getNome)
        .toList();
  }

  private String nomeDoArquivo(MultipartFile arquivo) {
    String original = arquivo.getOriginalFilename();
    return original != null && !original.isBlank() ? original : "documento.pdf";
  }

  private Usuario usuarioDoToken() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao instanceof UsernamePasswordAuthenticationToken token
        && token.getPrincipal() instanceof UsuarioAuth auth) {
      return usuarioRepository
          .findByEmail(auth.subject())
          .orElseThrow(() -> new IllegalStateException("Usuário do token não encontrado."));
    }
    throw new IllegalStateException("Token sem principal de usuário válido.");
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
