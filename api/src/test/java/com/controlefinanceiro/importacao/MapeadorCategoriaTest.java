package com.controlefinanceiro.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de unidade de {@link MapeadorCategoria} (repositório mockado, sem banco).
 *
 * <p>Casos de aceite do plano 05-04-t1: casamento exato, sinônimo sem acento e fallback com
 * revisão.
 */
class MapeadorCategoriaTest {

  private CategoriaRepository repository;
  private MapeadorCategoria mapeador;

  /** Fixture de categorias do tenant (nomes reais do projeto). */
  private static final List<Categoria> CATEGORIAS =
      List.of(
          categoria(1L, "Alimentação", "SAIDA"),
          categoria(2L, "Saúde/farmácia", "SAIDA"),
          categoria(3L, "Transporte", "SAIDA"),
          categoria(4L, "Moradia", "SAIDA"),
          categoria(5L, "Lazer", "SAIDA"),
          categoria(6L, "Serviços", "SAIDA"),
          categoria(7L, "Assinaturas", "SAIDA"),
          categoria(8L, "Supermercado", "SAIDA"),
          categoria(9L, "Outros", "SAIDA"),
          categoria(10L, "Rendimentos e reembolsos", "ENTRADA"),
          categoria(11L, "Outras receitas", "ENTRADA"));

  private static Categoria categoria(Long id, String nome, String tipo) {
    Categoria c = new Categoria();
    c.setId(id);
    c.setNome(nome);
    c.setTipo(tipo);
    c.setAtivo(true);
    return c;
  }

  @BeforeEach
  void setup() {
    repository = mock(CategoriaRepository.class);
    when(repository.findByEmpresaId(1L)).thenReturn(CATEGORIAS);
    mapeador = new MapeadorCategoria(repository);
  }

  @Test
  @DisplayName("casamento exato normalizado: saúde/farmácia → id da categoria")
  void casamentoExato() {
    MapeadorCategoria.ResultadoMapeamento resultado =
        mapeador.mapear("saúde/farmácia", "SAIDA", 1L);

    assertThat(resultado.categoriaId()).isEqualTo(2L);
    assertThat(resultado.categoriaNome()).isEqualTo("Saúde/farmácia");
    assertThat(resultado.necessitaRevisao()).isFalse();
  }

  @Test
  @DisplayName("sinônimo sem acento: alimentacao → Alimentação")
  void sinonimoSemAcento() {
    MapeadorCategoria.ResultadoMapeamento resultado = mapeador.mapear("alimentacao", "SAIDA", 1L);

    assertThat(resultado.categoriaId()).isEqualTo(1L);
    assertThat(resultado.categoriaNome()).isEqualTo("Alimentação");
    assertThat(resultado.necessitaRevisao()).isFalse();
  }

  @Test
  @DisplayName("sinônimo: salario (tipo RECEITA) → Rendimentos e reembolsos")
  void sinonimoSalario() {
    MapeadorCategoria.ResultadoMapeamento resultado = mapeador.mapear("salario", "RECEITA", 1L);

    assertThat(resultado.categoriaId()).isEqualTo(10L);
    assertThat(resultado.necessitaRevisao()).isFalse();
  }

  @Test
  @DisplayName("categoria inexistente → fallback Outros com necessitaRevisao")
  void fallbackComRevisao() {
    MapeadorCategoria.ResultadoMapeamento resultado =
        mapeador.mapear("qualquer coisa inexistente", "SAIDA", 1L);

    assertThat(resultado.categoriaId()).isEqualTo(9L); // Outros
    assertThat(resultado.categoriaNome()).isEqualTo("Outros");
    assertThat(resultado.necessitaRevisao()).isTrue();
  }

  @Test
  @DisplayName("fallback de ENTRADA → Outras receitas")
  void fallbackEntrada() {
    MapeadorCategoria.ResultadoMapeamento resultado =
        mapeador.mapear("valor misterioso", "RECEITA", 1L);

    assertThat(resultado.categoriaId()).isEqualTo(11L);
    assertThat(resultado.necessitaRevisao()).isTrue();
  }

  @Test
  @DisplayName("fallback inexistente no tenant → id nulo com revisão")
  void fallbackInexistenteNoTenant() {
    when(repository.findByEmpresaId(1L)).thenReturn(List.of(categoria(1L, "Alimentação", "SAIDA")));

    MapeadorCategoria.ResultadoMapeamento resultado = mapeador.mapear("algo estranho", "SAIDA", 1L);

    assertThat(resultado.categoriaId()).isNull();
    assertThat(resultado.necessitaRevisao()).isTrue();
  }

  @Test
  @DisplayName("sugestão vazia/nula → fallback com revisão")
  void sugestaoVazia() {
    MapeadorCategoria.ResultadoMapeamento nula = mapeador.mapear(null, "SAIDA", 1L);
    assertThat(nula.necessitaRevisao()).isTrue();

    MapeadorCategoria.ResultadoMapeamento vazia = mapeador.mapear("   ", "SAIDA", 1L);
    assertThat(vazia.necessitaRevisao()).isTrue();
  }

  @Test
  @DisplayName("pool respeita o tipo: sugestão de SAIDA não casa categoria ENTRADA")
  void poolRespeitaTipo() {
    // "salario" mapeia para "rendimentos e reembolsos" (ENTRADA) — pedindo SAIDA não casa:
    MapeadorCategoria.ResultadoMapeamento resultado = mapeador.mapear("salario", "SAIDA", 1L);

    // "rendimentos e reembolsos" é ENTRADA; no pool SAIDA não existe → contenção também não pega
    // (nenhuma categoria SAIDA contém "salario") → fallback
    assertThat(resultado.categoriaId()).isEqualTo(9L);
    assertThat(resultado.necessitaRevisao()).isTrue();
  }

  @Test
  @DisplayName("sobrecarga com lista reutiliza o pool e não consulta o repositório")
  void mapearComListaNaoConsultaRepositorio() {
    MapeadorCategoria.ResultadoMapeamento resultado =
        mapeador.mapear("alimentacao", "SAIDA", CATEGORIAS);

    assertThat(resultado.categoriaId()).isEqualTo(1L);
    verify(repository, never()).findByEmpresaId(any());
  }

  @Test
  @DisplayName("tipo de lançamento da IA mapeia para tipo de categoria")
  void tipoLancamentoParaTipoCategoria() {
    assertThat(mapeador.tipoCategoria("RECEITA")).isEqualTo("ENTRADA");
    assertThat(mapeador.tipoCategoria("DESPESA_PAGA")).isEqualTo("SAIDA");
    assertThat(mapeador.tipoCategoria("A_PAGAR")).isEqualTo("SAIDA");
    assertThat(mapeador.tipoCategoria("ENTRADA")).isEqualTo("ENTRADA");
    assertThat(mapeador.tipoCategoria("SAIDA")).isEqualTo("SAIDA");
  }
}
