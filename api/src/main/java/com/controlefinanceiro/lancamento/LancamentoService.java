package com.controlefinanceiro.lancamento;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.CategoriaNaoEncontradaException;
import com.controlefinanceiro.common.CategoriaTipoIncompativelException;
import com.controlefinanceiro.common.LinhaInvalidaException;
import com.controlefinanceiro.common.LoteDemasiadoGrandeException;
import com.controlefinanceiro.common.MonetarioException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de lançamentos. empresa_id e usuario_id vêm exclusivamente do token (TenantContext e
 * UsuarioContext), nunca do body. Toda consulta financeira é escalonada por ambos. LAN-05: apenas
 * BigDecimal, scale=2, HALF_EVEN.
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

  @Transactional
  public LancamentoResponse criar(Long empresaId, Long usuarioId, LancamentoRequest request) {
    validarValor(request.valor());
    Categoria categoria = categoria(empresaId, request.categoriaId());
    validarIntegridadeCategoriaTipo(categoria, request.tipo());

    Lancamento lancamento = novoLancamento(empresaId, usuarioId, categoria, request);
    lancamento.setOrigem("MANUAL");
    return new LancamentoResponse(lancamentoRepository.save(lancamento));
  }

  @Transactional
  public LancamentoResponse criarComCanal(
      Long empresaId,
      Long usuarioId,
      LancamentoRequest request,
      String canal,
      String messageIdOrigem) {
    validarValor(request.valor());
    Categoria categoria = categoria(empresaId, request.categoriaId());
    validarIntegridadeCategoriaTipo(categoria, request.tipo());

    Lancamento lancamento = novoLancamento(empresaId, usuarioId, categoria, request);
    lancamento.setCanal(canal);
    lancamento.setMessageIdOrigem(messageIdOrigem);
    return new LancamentoResponse(lancamentoRepository.save(lancamento));
  }

  @Transactional
  public LancamentoResponse editar(
      Long id, Long empresaId, Long usuarioId, LancamentoRequest request) {
    validarValor(request.valor());

    Lancamento lancamento =
        lancamentoRepository
            .findByIdAndEmpresaIdAndUsuarioId(id, empresaId, usuarioId)
            .orElseThrow(() -> naoEncontrado(id, empresaId));

    Categoria categoria = categoria(empresaId, request.categoriaId());
    validarIntegridadeCategoriaTipo(categoria, request.tipo());

    lancamento.setCategoria(categoria);
    lancamento.setTipo(request.tipo());
    lancamento.setValor(request.valor());
    lancamento.setData(request.data());
    lancamento.setDescricao(request.descricao());

    return new LancamentoResponse(lancamentoRepository.save(lancamento));
  }

  @Transactional
  public void excluir(Long id, Long empresaId, Long usuarioId) {
    Lancamento lancamento =
        lancamentoRepository
            .findByIdAndEmpresaIdAndUsuarioId(id, empresaId, usuarioId)
            .orElseThrow(() -> naoEncontrado(id, empresaId));
    lancamentoRepository.delete(lancamento);
  }

  @Transactional(readOnly = true)
  public LancamentoResponse buscarPorId(Long id, Long empresaId, Long usuarioId) {
    return new LancamentoResponse(
        lancamentoRepository
            .findByIdAndEmpresaIdAndUsuarioId(
                Objects.requireNonNull(id, "id"), empresaId, usuarioId)
            .orElseThrow(() -> naoEncontrado(id, empresaId)));
  }

  @Transactional(readOnly = true)
  public LancamentoPage listar(
      Long empresaId, Long usuarioId, Integer ano, Integer mes, String tipo, Long categoriaId) {
    Page<Lancamento> page =
        lancamentoRepository.findByEmpresaIdAndUsuarioIdAndFiltros(
            empresaId, usuarioId, ano, mes, tipo, categoriaId, PageRequest.of(0, 50));
    return new LancamentoPage(
        page.getContent().stream().map(LancamentoResponse::new).toList(), page.getTotalElements());
  }

  @Transactional
  public List<LancamentoResponse> processarLote(
      Long empresaId, Long usuarioId, LancamentoLoteRequest lote) {
    List<LancamentoLoteRequest.LancamentoLinha> linhas =
        lote.linhas() == null ? List.of() : lote.linhas();

    if (linhas.size() > 500) {
      throw new LoteDemasiadoGrandeException(
          "Limite de 500 linhas excedido para lote de lançamentos");
    }

    List<LancamentoResponse> criados = new ArrayList<>(linhas.size());
    for (int i = 0; i < linhas.size(); i++) {
      try {
        criados.add(criar(empresaId, usuarioId, linhas.get(i).toRequest()));
      } catch (RuntimeException ex) {
        throw new LinhaInvalidaException(i, ex.getMessage());
      }
    }
    return criados;
  }

  private Lancamento novoLancamento(
      Long empresaId, Long usuarioId, Categoria categoria, LancamentoRequest request) {
    Usuario usuario =
        usuarioRepository
            .findByIdAndEmpresaId(Objects.requireNonNull(usuarioId, "usuarioId"), empresaId)
            .orElseThrow(() -> new NaoAutorizadoException("Usuário não encontrado"));

    Lancamento lancamento = new Lancamento();
    lancamento.setEmpresa(entityManager.getReference(Empresa.class, empresaId));
    lancamento.setUsuario(usuario);
    lancamento.setCategoria(categoria);
    lancamento.setTipo(request.tipo());
    lancamento.setValor(request.valor());
    lancamento.setData(request.data());
    lancamento.setDescricao(request.descricao());
    return lancamento;
  }

  private Categoria categoria(Long empresaId, Long categoriaId) {
    return categoriaRepository
        .findByIdAndEmpresaId(Objects.requireNonNull(categoriaId, "categoriaId"), empresaId)
        .orElseThrow(
            () ->
                new CategoriaNaoEncontradaException(
                    "Categoria não encontrada: id=%d, empresa=%d"
                        .formatted(categoriaId, empresaId)));
  }

  private CategoriaNaoEncontradaException naoEncontrado(Long id, Long empresaId) {
    return new CategoriaNaoEncontradaException(
        "Lançamento não encontrado: id=%d, empresa=%d".formatted(id, empresaId));
  }

  /** Valida valor monetário: escala <= 2 e positivo (LAN-04, LAN-07). */
  private void validarValor(BigDecimal valor) {
    if (valor.scale() > 2) {
      throw new MonetarioException(
          "Valor monetário deve ter no máximo 2 casas decimais. Valor informado tem %d casas."
              .formatted(valor.scale()));
    }
    if (valor.signum() <= 0) {
      throw new MonetarioException("Valor deve ser positivo. Valor informado: %s".formatted(valor));
    }
  }

  /** Valida integridade categoria×tipo (CAT-04). */
  private void validarIntegridadeCategoriaTipo(Categoria categoria, String tipoLancamento) {
    if (!categoria.getTipo().equals(tipoLancamento)) {
      throw new CategoriaTipoIncompativelException(
          "Categoria '%s' é do tipo '%s', incompatível com lançamento do tipo '%s'"
              .formatted(categoria.getNome(), categoria.getTipo(), tipoLancamento));
    }
  }
}
