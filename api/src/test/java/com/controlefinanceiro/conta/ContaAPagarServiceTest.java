package com.controlefinanceiro.conta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.common.ContaAPagarJaPagaException;
import com.controlefinanceiro.common.ContaAPagarNaoEncontradoException;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.lancamento.LancamentoRequest;
import com.controlefinanceiro.lancamento.LancamentoResponse;
import com.controlefinanceiro.lancamento.LancamentoService;
import com.controlefinanceiro.usuario.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ContaAPagarServiceTest {

  private static final Long EMPRESA_ID = 1L;
  private static final Long USUARIO_ID = 2L;
  private static final Long CATEGORIA_ID = 3L;
  private static final LocalDate PAGAMENTO = LocalDate.of(2026, 11, 26);

  private ContaAPagarRepository repository;
  private LancamentoService lancamentoService;
  private ContaAPagarService service;

  @BeforeEach
  void setup() {
    repository = mock(ContaAPagarRepository.class);
    lancamentoService = mock(LancamentoService.class);
    service = new ContaAPagarService(repository, lancamentoService);
  }

  @Test
  void pagarCriaSaidaEMarcaContaComoPaga() {
    ContaAPagar conta = conta(false);
    when(repository.findByIdAndEmpresaIdAndUsuarioId(10L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.of(conta));
    when(lancamentoService.criar(eq(EMPRESA_ID), eq(USUARIO_ID), any()))
        .thenReturn(
            new LancamentoResponse(
                99L,
                "SAIDA",
                conta.getValor(),
                PAGAMENTO,
                "Pagamento: Boleto",
                "Despesas",
                "SAIDA"));

    ContaAPagarResponse resposta = service.pagar(10L, EMPRESA_ID, USUARIO_ID, PAGAMENTO);

    assertThat(resposta.pago()).isTrue();
    assertThat(resposta.status()).isEqualTo("PAGO");
    ArgumentCaptor<LancamentoRequest> captor = ArgumentCaptor.forClass(LancamentoRequest.class);
    verify(lancamentoService).criar(eq(EMPRESA_ID), eq(USUARIO_ID), captor.capture());
    assertThat(captor.getValue().tipo()).isEqualTo("SAIDA");
    assertThat(captor.getValue().categoriaId()).isEqualTo(CATEGORIA_ID);
    assertThat(captor.getValue().valor()).isEqualByComparingTo("123.45");
    assertThat(captor.getValue().data()).isEqualTo(PAGAMENTO);
    assertThat(captor.getValue().descricao()).isEqualTo("Pagamento: Boleto");
  }

  @Test
  void pagarTruncaDescricaoDoLancamentoParaLimiteDoBanco() {
    ContaAPagar conta = conta(false);
    conta.setDescricao("B".repeat(250));
    when(repository.findByIdAndEmpresaIdAndUsuarioId(10L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.of(conta));
    when(lancamentoService.criar(eq(EMPRESA_ID), eq(USUARIO_ID), any()))
        .thenReturn(
            new LancamentoResponse(
                99L, "SAIDA", conta.getValor(), PAGAMENTO, "Pagamento", "Despesas", "SAIDA"));

    service.pagar(10L, EMPRESA_ID, USUARIO_ID, PAGAMENTO);

    ArgumentCaptor<LancamentoRequest> captor = ArgumentCaptor.forClass(LancamentoRequest.class);
    verify(lancamentoService).criar(eq(EMPRESA_ID), eq(USUARIO_ID), captor.capture());
    assertThat(captor.getValue().descricao()).hasSize(200);
  }

  @Test
  void pagarContaJaPagaLancaConflito() {
    ContaAPagar conta = conta(true);
    when(repository.findByIdAndEmpresaIdAndUsuarioId(10L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.of(conta));

    assertThatThrownBy(() -> service.pagar(10L, EMPRESA_ID, USUARIO_ID, PAGAMENTO))
        .isInstanceOf(ContaAPagarJaPagaException.class);
    verify(lancamentoService, never()).criar(any(), any(), any());
  }

  @Test
  void pagarContaDeOutroTenantOuUsuarioRetorna404() {
    when(repository.findByIdAndEmpresaIdAndUsuarioId(10L, EMPRESA_ID, USUARIO_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.pagar(10L, EMPRESA_ID, USUARIO_ID, PAGAMENTO))
        .isInstanceOf(ContaAPagarNaoEncontradoException.class);
    verify(lancamentoService, never()).criar(any(), any(), any());
  }

  @Test
  void listarAplicaFiltrosEComputaStatus() {
    when(repository.findByEmpresaIdAndUsuarioIdAndFiltros(
            EMPRESA_ID, USUARIO_ID, false, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1)))
        .thenReturn(List.of(conta(false)));

    List<ContaAPagarResponse> respostas =
        service.listar(
            EMPRESA_ID, USUARIO_ID, false, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1));

    assertThat(respostas).hasSize(1);
    assertThat(respostas.get(0).status()).isEqualTo("PENDENTE");
    assertThat(respostas.get(0).categoriaNome()).isEqualTo("Despesas");
  }

  private ContaAPagar conta(boolean pago) {
    Empresa empresa = new Empresa();
    empresa.setId(EMPRESA_ID);
    Usuario usuario = new Usuario();
    usuario.setId(USUARIO_ID);
    Categoria categoria = new Categoria();
    categoria.setId(CATEGORIA_ID);
    categoria.setNome("Despesas");
    categoria.setTipo("SAIDA");

    ContaAPagar conta = new ContaAPagar();
    conta.setId(10L);
    conta.setEmpresa(empresa);
    conta.setUsuario(usuario);
    conta.setCategoria(categoria);
    conta.setDescricao("Boleto");
    conta.setValor(new BigDecimal("123.45"));
    conta.setData(LocalDate.of(2026, 11, 20));
    conta.setVencimento(LocalDate.of(2026, 12, 10));
    conta.setOrigem("EXTRATO");
    conta.setPago(pago);
    if (pago) {
      conta.setPagoEm(OffsetDateTime.now());
    }
    return conta;
  }
}
