package com.controlefinanceiro.categoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.CategoriaTipoIncompativelException;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoriaServiceTest {

  private CategoriaRepository repository;
  private CategoriaService service;
  private Categoria categoria;

  @BeforeEach
  void setup() {
    repository = mock(CategoriaRepository.class);
    service = new CategoriaService(repository, mock(EntityManager.class));
    categoria = new Categoria();
    categoria.setId(5L);
    categoria.setNome("Alimentação");
    categoria.setTipo("SAIDA");
    categoria.setAtivo(true);
    when(repository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(categoria));
  }

  @Test
  @DisplayName("editar não muda tipo de categoria com lançamentos (analytics preservados)")
  void editarTipoComLancamentosRejeitado() {
    when(repository.existeLancamento(5L, 1L)).thenReturn(true);

    assertThatThrownBy(
            () -> service.editar(5L, 1L, new CategoriaRequest("Alimentação", "ENTRADA", false)))
        .isInstanceOf(CategoriaTipoIncompativelException.class)
        .hasMessageContaining("lançamentos");

    assertThat(categoria.getTipo()).isEqualTo("SAIDA");
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("editar mantém tipo quando há lançamentos")
  void editarSemMudarTipoComLancamentosPermitido() {
    when(repository.existeLancamento(5L, 1L)).thenReturn(true);
    when(repository.save(categoria)).thenReturn(categoria);

    service.editar(5L, 1L, new CategoriaRequest("Alimentação e mercados", "SAIDA", false));

    assertThat(categoria.getNome()).isEqualTo("Alimentação e mercados");
    assertThat(categoria.getTipo()).isEqualTo("SAIDA");
  }

  @Test
  @DisplayName("editar muda tipo quando não há lançamentos")
  void editarTipoSemLancamentosPermitido() {
    when(repository.existeLancamento(5L, 1L)).thenReturn(false);
    when(repository.save(categoria)).thenReturn(categoria);

    service.editar(5L, 1L, new CategoriaRequest("Receitas várias", "ENTRADA", false));

    assertThat(categoria.getTipo()).isEqualTo("ENTRADA");
  }
}
