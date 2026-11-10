package com.controlefinanceiro.lancamento;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.common.LoteDemasiadoGrandeException;
import com.controlefinanceiro.common.MonetarioException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.TenantContext;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de lançamentos (segue o padrão de {@link
 * com.controlefinanceiro.categoria.CategoriaService}). R3: empresa_id vem exclusivamente de {@link
 * TenantContext#getEmpresaId()}, nunca do body. LAN-05: todo cálculo monetário usa BigDecimal,
 * scale=2, HALF_EVEN; nenhum double/float.
 */
@Service
public class LancamentoService {

  private final LancamentoRepository lancamentoRepository;
  private final CategoriaRepository categoriaRepository;
  private final UsuarioRepository usuarioRepository;
  private final EntityManager entityManager;

  public LancamentoService(
      LancamentoRepository lancamentoRepository,
      CategoriaRepository categoriaRepository,
      UsuarioRepository usuarioRepository,
      EntityManager entityManager) {
    this.lancamentoRepository = lancamentoRepository;
    this.categoriaRepository = categoriaRepository;
    this.usuarioRepository = usuarioRepository;
    this.entityManager = entityManager;
  }

  /**
   * Cria um lançamento para a empresa autenticada.
   *
   * @param empresaId tenant extraído do token via TenantContext
   * @param request dados do lançamento a criar
   * @return resposta do lançamento criado
   */
  @Transactional
  public LancamentoResponse criar(Long empresaId, LancamentoRequest request) {
    validarValor(request.valor());

    Categoria categoria =
        categoriaRepository
            .findByIdAndEmpresaId(request.categoriaId(), empresaId)
            .orElseThrow(
                () ->
                    new com.controlefinanceiro.common.CategoriaNaoEncontradaException(
                        "Categoria não encontrada: id=%d, empresa=%d"
                            .formatted(request.categoriaId(), empresaId)));

    validarIntegridadeCategoriaTipo(categoria, request.tipo());

    Lancamento lancamento = new Lancamento();
    lancamento.setEmpresa(entityManager.getReference(Empresa.class, empresaId));
    lancamento.setUsuario(usuarioAtual());
    lancamento.setCategoria(categoria);
    lancamento.setTipo(request.tipo());
    lancamento.setValor(request.valor());
    lancamento.setData(request.data());
    lancamento.setDescricao(request.descricao());

    Lancamento salvo = lancamentoRepository.save(lancamento);
    return new LancamentoResponse(salvo);
  }

  /**
   * Edita um lançamento existente da empresa autenticada.
   *
   * @param id id do lançamento
   * @param empresaId tenant extraído do token via TenantContext
   * @param request novos dados do lançamento
   * @return resposta do lançamento atualizado
   */
  @Transactional
  public LancamentoResponse editar(Long id, Long empresaId, LancamentoRequest request) {
    validarValor(request.valor());

    Lancamento lancamento =
        lancamentoRepository
            .findById(id)
            .filter(l -> l.getEmpresa().getId().equals(empresaId))
            .orElseThrow(
                () ->
                    new com.controlefinanceiro.common.CategoriaNaoEncontradaException(
                        "Lançamento não encontrado: id=%d, empresa=%d".formatted(id, empresaId)));

    Categoria categoria =
        categoriaRepository
            .findByIdAndEmpresaId(request.categoriaId(), empresaId)
            .orElseThrow(
                () ->
                    new com.controlefinanceiro.common.CategoriaNaoEncontradaException(
                        "Categoria não encontrada: id=%d, empresa=%d"
                            .formatted(request.categoriaId(), empresaId)));

    validarIntegridadeCategoriaTipo(categoria, request.tipo());

    lancamento.setCategoria(categoria);
    lancamento.setTipo(request.tipo());
    lancamento.setValor(request.valor());
    lancamento.setData(request.data());
    lancamento.setDescricao(request.descricao());

    Lancamento salvo = lancamentoRepository.save(lancamento);
    return new LancamentoResponse(salvo);
  }

  /**
   * Exclui um lançamento existente da empresa autenticada.
   *
   * @param id id do lançamento
   * @param empresaId tenant extraído do token via TenantContext
   */
  @Transactional
  public void excluir(Long id, Long empresaId) {
    Lancamento lancamento =
        lancamentoRepository
            .findById(id)
            .filter(l -> l.getEmpresa().getId().equals(empresaId))
            .orElseThrow(
                () ->
                    new com.controlefinanceiro.common.CategoriaNaoEncontradaException(
                        "Lançamento não encontrado: id=%d, empresa=%d".formatted(id, empresaId)));

    lancamentoRepository.delete(lancamento);
  }

  /**
   * Lista lançamentos da empresa autenticada com filtros opcionais de mês, ano, tipo e categoria.
   *
   * @param empresaId tenant extraído do token via TenantContext
   * @param ano filtro opcional por ano
   * @param mes filtro opcional por mês
   * @param tipo filtro opcional por tipo (ENTRADA | SAIDA)
   * @param categoriaId filtro opcional por id da categoria
   * @param pageable paginação
   * @return página de respostas de lançamento
   */
  @Transactional(readOnly = true)
  public LancamentoPage listar(
      Long empresaId, Integer ano, Integer mes, String tipo, Long categoriaId) {
    Page<Lancamento> page =
        lancamentoRepository.findByEmpresaIdAndFiltros(
            empresaId,
            ano,
            mes,
            tipo,
            categoriaId,
            org.springframework.data.domain.PageRequest.of(0, 50));
    List<LancamentoResponse> lista =
        page.getContent().stream().map(LancamentoResponse::new).toList();
    return new LancamentoPage(lista, page.getTotalElements());
  }

  /**
   * Processa um lote de lançamentos em transação tudo-ou-nada (LOT-01, LOT-02, T-02-08).
   *
   * <p>Limite de 500 linhas (T-02-09). Cada linha é validada pelo {@link #criar(Long,
   * LancamentoRequest)}; se qualquer linha falhar, a exceção é convertida em {@link
   * LinhaInvalidaException} carregando o índice base 0 da linha, e o {@code @Transactional} garante
   * rollback completo — nenhuma linha é persistida.
   *
   * @param empresaId tenant extraído do token via TenantContext (T-02-10)
   * @param lote lote de linhas a criar
   * @return lançamentos criados, na ordem do lote
   */
  @Transactional
  public List<LancamentoResponse> processarLote(Long empresaId, LancamentoLoteRequest lote) {
    List<LancamentoLoteRequest.LancamentoLinha> linhas =
        lote.linhas() == null ? List.of() : lote.linhas();

    if (linhas.size() > 500) {
      throw new LoteDemasiadoGrandeException(
          "Limite de 500 linhas excedido para lote de lançamentos");
    }

    List<LancamentoResponse> criados = new ArrayList<>(linhas.size());
    for (int i = 0; i < linhas.size(); i++) {
      try {
        criados.add(criar(empresaId, linhas.get(i).toRequest()));
      } catch (RuntimeException ex) {
        // LOT-02: reporta o índice base 0 da linha inválida e aborta o lote inteiro
        throw new LinhaInvalidaException(i, ex.getMessage());
      }
    }
    return criados;
  }

  /** Valida valor monetário: escala <= 2 e positivo (LAN-04, LAN-07). */
  private void validarValor(BigDecimal valor) {
    // LAN-04: rejeita (não arredonda) se mais de 2 casas decimais
    if (valor.scale() > 2) {
      throw new MonetarioException(
          "Valor monetário deve ter no máximo 2 casas decimais. Valor informado tem %d casas."
              .formatted(valor.scale()));
    }
    // LAN-07: valor deve ser positivo
    if (valor.signum() <= 0) {
      throw new MonetarioException("Valor deve ser positivo. Valor informado: %s".formatted(valor));
    }
  }

  /**
   * Valida integridade categoria×tipo: o tipo da categoria deve bater com o tipo do lançamento
   * (CAT-04).
   */
  private void validarIntegridadeCategoriaTipo(Categoria categoria, String tipoLancamento) {
    if (!categoria.getTipo().equals(tipoLancamento)) {
      throw new com.controlefinanceiro.common.CategoriaTipoIncompativelException(
          "Categoria '%s' é do tipo '%s', incompatível com lançamento do tipo '%s'"
              .formatted(categoria.getNome(), categoria.getTipo(), tipoLancamento));
    }
  }

  /** Retorna o usuário autenticado a partir do principal no SecurityContext (R3). */
  private Usuario usuarioAtual() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UsuarioAuth usuarioAuth) {
      return usuarioRepository
          .findByEmail(usuarioAuth.subject())
          .orElseThrow(() -> new NaoAutorizadoException("Usuário não encontrado"));
    }
    throw new NaoAutorizadoException();
  }
}
