package com.controlefinanceiro.lancamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.LancamentoNaoEncontradoException;
import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;

class LancamentoServiceConsultaTest {

  private LancamentoRepository lancamentos;
  private LancamentoService service;

  @BeforeEach
  void setup() {
    lancamentos = mock(LancamentoRepository.class);
    CategoriaRepository categorias = mock(CategoriaRepository.class);
    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    EntityManager entityManager = mock(EntityManager.class);

    Categoria categoria = new Categoria();
    categoria.setId(9L);
    categoria.setNome("Alimentação");
    categoria.setTipo("SAIDA");
    when(categorias.findByIdAndEmpresaId(9L, 1L)).thenReturn(Optional.of(categoria));

    Usuario usuario = new Usuario();
    usuario.setId(7L);
    when(usuarios.findByIdAndEmpresaId(7L, 1L)).thenReturn(Optional.of(usuario));
    when(entityManager.getReference(any(), any())).thenReturn(new Empresa());
    when(lancamentos.save(any(Lancamento.class))).thenAnswer(inv -> inv.getArgument(0));
    when(lancamentos.findByEmpresaIdAndUsuarioIdAndFiltros(
            any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(List.of()));

    service = new LancamentoService(lancamentos, categorias, usuarios, entityManager);
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("listar usa page/size e range inclusivo/exclusivo do mês")
  void listarComPaginacaoEIntervalo() {
    service.listar(1L, 7L, 2026, 11, null, null, 2, 10);

    verify(lancamentos)
        .findByEmpresaIdAndUsuarioIdAndFiltros(
            1L,
            7L,
            LocalDate.of(2026, 11, 1),
            LocalDate.of(2026, 12, 1),
            null,
            null,
            PageRequest.of(2, 10));
  }

  @Test
  @DisplayName("listar com ano sem mês usa intervalo do ano inteiro")
  void listarComAnoSemMes() {
    service.listar(1L, 7L, 2026, null, null, null, 0, 50);

    verify(lancamentos)
        .findByEmpresaIdAndUsuarioIdAndFiltros(
            1L,
            7L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2027, 1, 1),
            null,
            null,
            PageRequest.of(0, 50));
  }

  @Test
  @DisplayName("lançamento inexistente gera LancamentoNaoEncontradoException")
  void buscouInexistente() {
    when(lancamentos.findByIdAndEmpresaIdAndUsuarioId(99L, 1L, 7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscarPorId(99L, 1L, 7L))
        .isInstanceOf(LancamentoNaoEncontradoException.class)
        .hasMessageContaining("99");
  }

  @Test
  @DisplayName("criar define origem MANUAL; criarComCanal define canal e messageId")
  void criarECriarComCanalUsamMesmoHelper() {
    LancamentoRequest request =
        new LancamentoRequest(9L, "SAIDA", new BigDecimal("12.34"), LocalDate.of(2026, 11, 3), "x");

    service.criar(1L, 7L, request);
    service.criarComCanal(1L, 7L, request, "WHATSAPP", "msg-1");

    ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
    verify(lancamentos, org.mockito.Mockito.times(2)).save(captor.capture());
    assertThat(captor.getAllValues().get(0).getOrigem()).isEqualTo("MANUAL");
    assertThat(captor.getAllValues().get(0).getCanal()).isNull();
    assertThat(captor.getAllValues().get(1).getCanal()).isEqualTo("WHATSAPP");
    assertThat(captor.getAllValues().get(1).getMessageIdOrigem()).isEqualTo("msg-1");
    assertThat(captor.getAllValues().get(1).getOrigem()).isNull();
  }

  @Test
  @DisplayName("valor com uma casa é normalizado para NUMERIC(14,2) com HALF_EVEN")
  void valorNormalizadoParaDuasCasas() {
    service.criar(
        1L,
        7L,
        new LancamentoRequest(9L, "SAIDA", new BigDecimal("12.3"), LocalDate.of(2026, 11, 3), "x"));

    ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
    verify(lancamentos).save(captor.capture());
    assertThat(captor.getValue().getValor()).isEqualByComparingTo("12.30");
    assertThat(captor.getValue().getValor().scale()).isEqualTo(2);
  }

  @Test
  @DisplayName("controller usa page/size padrão 0/50 e limita tamanho a 100")
  void controllerPaginacao() {
    autenticar();
    LancamentoService servicoMock = mock(LancamentoService.class);
    when(servicoMock.listar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
        .thenReturn(new LancamentoPage(List.of(), 0));

    LancamentoController controller = new LancamentoController(servicoMock);
    controller.listar(null, null, null, null, 0, 50);
    controller.listar(2026, 11, "SAIDA", 9L, 5, 500);

    ArgumentCaptor<Integer> paginas = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> tamanhos = ArgumentCaptor.forClass(Integer.class);
    verify(servicoMock, org.mockito.Mockito.times(2))
        .listar(any(), any(), any(), any(), any(), any(), paginas.capture(), tamanhos.capture());
    assertThat(paginas.getAllValues()).containsExactly(0, 5);
    assertThat(tamanhos.getAllValues()).containsExactly(50, 100);

    assertThatThrownBy(() -> controller.listar(2026, 11, null, null, -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
    verify(servicoMock, org.mockito.Mockito.times(2))
        .listar(any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
  }

  private static void autenticar() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("usuario@teste.com", 1L, 7L, 0, "USER"), List.of()));
  }
}
