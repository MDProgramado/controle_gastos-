package com.controlefinanceiro.conta;

import com.controlefinanceiro.common.ContaAPagarJaPagaException;
import com.controlefinanceiro.common.ContaAPagarNaoEncontradoException;
import com.controlefinanceiro.lancamento.LancamentoRequest;
import com.controlefinanceiro.lancamento.LancamentoResponse;
import com.controlefinanceiro.lancamento.LancamentoService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaAPagarService {

  private static final int DESCRICAO_MAXIMA = 200;
  private static final String PREFIXO_DESCRICAO = "Pagamento: ";

  private final ContaAPagarRepository repository;
  private final LancamentoService lancamentoService;

  public ContaAPagarService(ContaAPagarRepository repository, LancamentoService lancamentoService) {
    this.repository = repository;
    this.lancamentoService = lancamentoService;
  }

  @Transactional(readOnly = true)
  public List<ContaAPagarResponse> listar(
      Long empresaId,
      Long usuarioId,
      Boolean pago,
      LocalDate vencimentoDe,
      LocalDate vencimentoAte) {
    return repository
        .findByEmpresaIdAndUsuarioIdAndFiltros(
            empresaId, usuarioId, pago, vencimentoDe, vencimentoAte)
        .stream()
        .map(ContaAPagarResponse::new)
        .toList();
  }

  @Transactional
  public ContaAPagarResponse pagar(
      Long id, Long empresaId, Long usuarioId, LocalDate dataPagamento) {
    ContaAPagar conta =
        repository
            .findByIdAndEmpresaIdAndUsuarioId(id, empresaId, usuarioId)
            .orElseThrow(
                () ->
                    new ContaAPagarNaoEncontradoException(
                        "Conta a pagar não encontrada: id=%d, empresa=%d"
                            .formatted(id, empresaId)));

    if (conta.isPago()) {
      throw new ContaAPagarJaPagaException("Conta a pagar já foi baixada: id=%d".formatted(id));
    }

    LocalDate data = dataPagamento == null ? LocalDate.now() : dataPagamento;
    LancamentoResponse lancamento =
        lancamentoService.criar(
            empresaId,
            usuarioId,
            new LancamentoRequest(
                conta.getCategoria().getId(),
                "SAIDA",
                conta.getValor(),
                data,
                descricaoPagamento(conta.getDescricao())));

    conta.setPago(true);
    conta.setPagoEm(java.time.OffsetDateTime.now());
    conta.setLancamentoId(lancamento.id());
    return new ContaAPagarResponse(conta);
  }

  private String descricaoPagamento(String descricao) {
    String valor = descricao == null ? "" : descricao;
    int limite = DESCRICAO_MAXIMA - PREFIXO_DESCRICAO.length();
    if (valor.length() > limite) {
      valor = valor.substring(0, limite);
    }
    return PREFIXO_DESCRICAO + valor;
  }
}
