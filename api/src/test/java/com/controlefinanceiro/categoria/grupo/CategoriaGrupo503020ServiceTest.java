package com.controlefinanceiro.categoria.grupo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.CategoriaNaoEncontradaException;
import com.controlefinanceiro.common.CategoriaTipoIncompativelException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoriaGrupo503020ServiceTest {

  private CategoriaRepository categoriaRepository;
  private CategoriaGrupo503020Repository grupoRepository;
  private CategoriaGrupo503020Service service;

  @BeforeEach
  void setup() {
    categoriaRepository = mock(CategoriaRepository.class);
    grupoRepository = mock(CategoriaGrupo503020Repository.class);
    service = new CategoriaGrupo503020Service(categoriaRepository, grupoRepository);
  }

  @Test
  void listarUsaDesejoComoPadraoParaCategoriaSemMapeamento() {
    when(categoriaRepository.findByEmpresaId(1L))
        .thenReturn(List.of(categoria(5L, "Lazer", "SAIDA")));
    when(grupoRepository.findByEmpresaId(1L)).thenReturn(List.of());

    List<CategoriaGrupo503020Response> resposta = service.listar(1L);

    assertThat(resposta).hasSize(1);
    assertThat(resposta.get(0).categoriaId()).isEqualTo(5L);
    assertThat(resposta.get(0).grupo()).isEqualTo("DESEJO");
    assertThat(resposta.get(0).padrao()).isTrue();
  }

  @Test
  void listarUsaMapeamentoSalvoQuandoExiste() {
    when(categoriaRepository.findByEmpresaId(1L))
        .thenReturn(List.of(categoria(5L, "Alimentação", "SAIDA")));
    when(grupoRepository.findByEmpresaId(1L))
        .thenReturn(List.of(mapeamento(1L, 5L, "NECESSIDADE")));

    List<CategoriaGrupo503020Response> resposta = service.listar(1L);

    assertThat(resposta).hasSize(1);
    assertThat(resposta.get(0).grupo()).isEqualTo("NECESSIDADE");
    assertThat(resposta.get(0).padrao()).isFalse();
  }

  @Test
  void salvarRejeitaCategoriaForaDoTenant() {
    when(categoriaRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.salvar(1L, 5L, "DESEJO"))
        .isInstanceOf(CategoriaNaoEncontradaException.class);
    verify(grupoRepository, never()).save(any());
  }

  @Test
  void salvarRejeitaCategoriaDeEntrada() {
    when(categoriaRepository.findByIdAndEmpresaId(5L, 1L))
        .thenReturn(Optional.of(categoria(5L, "Salário", "ENTRADA")));

    assertThatThrownBy(() -> service.salvar(1L, 5L, "NECESSIDADE"))
        .isInstanceOf(CategoriaTipoIncompativelException.class);
    verify(grupoRepository, never()).save(any());
  }

  @Test
  void salvarRejeitaGrupoDesconhecido() {
    assertThatThrownBy(() -> service.salvar(1L, 5L, "INVESTIMENTO"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("NECESSIDADE");
    verify(categoriaRepository, never()).findByIdAndEmpresaId(any(), any());
  }

  @Test
  void salvarAtualizaMapeamentoExistente() {
    Categoria categoria = categoria(5L, "Lazer", "SAIDA");
    CategoriaGrupo503020 existente = mapeamento(9L, 5L, "DESEJO");
    when(categoriaRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(categoria));
    when(grupoRepository.findByEmpresaIdAndCategoriaId(1L, 5L)).thenReturn(Optional.of(existente));

    CategoriaGrupo503020Response resposta = service.salvar(1L, 5L, "FUTURO");

    assertThat(existente.getGrupo()).isEqualTo("FUTURO");
    assertThat(resposta.grupo()).isEqualTo("FUTURO");
    assertThat(resposta.padrao()).isFalse();
    verify(grupoRepository).save(existente);
  }

  @Test
  void salvarCriaMapeamentoQuandoNaoExiste() {
    when(categoriaRepository.findByIdAndEmpresaId(5L, 1L))
        .thenReturn(Optional.of(categoria(5L, "Lazer", "SAIDA")));
    when(grupoRepository.findByEmpresaIdAndCategoriaId(1L, 5L)).thenReturn(Optional.empty());

    service.salvar(1L, 5L, "NECESSIDADE");

    var capturado = org.mockito.ArgumentCaptor.forClass(CategoriaGrupo503020.class);
    verify(grupoRepository).save(capturado.capture());
    assertThat(capturado.getValue().getEmpresaId()).isEqualTo(1L);
    assertThat(capturado.getValue().getCategoriaId()).isEqualTo(5L);
    assertThat(capturado.getValue().getGrupo()).isEqualTo("NECESSIDADE");
  }

  private Categoria categoria(Long id, String nome, String tipo) {
    Categoria categoria = new Categoria();
    categoria.setId(id);
    categoria.setNome(nome);
    categoria.setTipo(tipo);
    categoria.setAtivo(true);
    return categoria;
  }

  private CategoriaGrupo503020 mapeamento(Long id, Long categoriaId, String grupo) {
    CategoriaGrupo503020 mapeamento = new CategoriaGrupo503020();
    mapeamento.setId(id);
    mapeamento.setEmpresaId(1L);
    mapeamento.setCategoriaId(categoriaId);
    mapeamento.setGrupo(grupo);
    return mapeamento;
  }
}
