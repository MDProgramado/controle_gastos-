package com.controlefinanceiro.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.ai.rag.RagSearchService;
import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.categoria.grupo.CategoriaGrupo503020;
import com.controlefinanceiro.categoria.grupo.CategoriaGrupo503020Repository;
import com.controlefinanceiro.dashboard.DashboardRepository;
import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.controlefinanceiro.lancamento.LancamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FerramentasFinanceirasTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private DashboardRepository dashboardRepository;
  private CategoriaRepository categoriaRepository;
  private LancamentoRepository lancamentoRepository;
  private CategoriaGrupo503020Repository grupoRepository;
  private RagSearchService ragSearchService;
  private FerramentasFinanceiras ferramentas;
  private ContextoFerramenta contexto;

  @BeforeEach
  void setup() {
    dashboardRepository = mock(DashboardRepository.class);
    categoriaRepository = mock(CategoriaRepository.class);
    lancamentoRepository = mock(LancamentoRepository.class);
    grupoRepository = mock(CategoriaGrupo503020Repository.class);
    ragSearchService = mock(RagSearchService.class);
    ferramentas =
        new FerramentasFinanceiras(
            dashboardRepository,
            categoriaRepository,
            lancamentoRepository,
            grupoRepository,
            ragSearchService);
    contexto = new ContextoFerramenta(1L, 2L);
  }

  @Test
  @DisplayName("simulação de juros usa BigDecimal e devolve parcela com duas casas")
  void simulacaoDeJurosUsaBigDecimal() throws Exception {
    JsonNode args = MAPPER.readTree("{\"valor\":1000,\"taxa_mensal\":2.5,\"parcelas\":12}");

    ResultadoFerramenta resultado = ferramentas.simularQuitacaoOuJuros(args, contexto);

    JsonNode json = MAPPER.readTree(resultado.conteudo());
    BigDecimal parcela = new BigDecimal(json.path("parcela").asText().replace(',', '.'));
    assertThat(parcela).isGreaterThan(BigDecimal.valueOf(97));
    assertThat(parcela).isLessThan(BigDecimal.valueOf(99));
    assertThat(parcela.scale()).isEqualTo(2);
    assertThat(json.path("valor_total").asText()).contains(",");
  }

  @Test
  @DisplayName("diagnóstico 50/30/20 sinaliza categoria que caiu no fallback")
  void diagnosticoSinalizaFallback() throws Exception {
    Categoria moradia = new Categoria();
    moradia.setId(10L);
    moradia.setNome("Moradia");
    Categoria nova = new Categoria();
    nova.setId(11L);
    nova.setNome("Categoria Nova");
    when(categoriaRepository.findByEmpresaId(1L)).thenReturn(List.of(moradia, nova));

    CategoriaGrupo503020 mapeado = new CategoriaGrupo503020();
    mapeado.setCategoriaId(10L);
    mapeado.setGrupo("NECESSIDADE");
    when(grupoRepository.findByEmpresaIdAndCategoriaIdIn(eq(1L), any()))
        .thenReturn(List.of(mapeado));

    when(dashboardRepository.totalEntradas(
            eq(1L), eq(2L), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(new BigDecimal("3000.00"));
    when(dashboardRepository.breakdownSaida(
            eq(1L), eq(2L), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(
            List.of(
                new CategoryExpense(10L, "Moradia", new BigDecimal("600.00")),
                new CategoryExpense(11L, "Categoria Nova", new BigDecimal("100.00"))));

    JsonNode args = MAPPER.readTree("{\"mes\":11,\"ano\":2026}");

    ResultadoFerramenta resultado = ferramentas.diagnosticarOrcamento503020(args, contexto);

    JsonNode json = MAPPER.readTree(resultado.conteudo());
    assertThat(json.path("usou_fallback").asBoolean()).isTrue();
    assertThat(resultado.conteudo()).contains("Categoria Nova");
    assertThat(resultado.conteudo()).contains("Considerei Categoria Nova como Desejo por padrão");
    assertThat(json.path("categorias").get(1).path("confianca").asText()).isEqualTo("media");
  }

  @Test
  @DisplayName("consulta à base de conhecimento preserva citações")
  void consultaBasePreservaCitacoes() {
    when(ragSearchService.buscar("teto de juros", 3))
        .thenReturn(
            List.of(
                new com.controlefinanceiro.ai.rag.RagTrecho(
                    "02-bcb-res4549::art-2c",
                    "Resolução CMN 4.549",
                    "Banco Central do Brasil",
                    "2023-12-21",
                    "art. 2-C",
                    "Trecho")));

    ResultadoFerramenta resultado =
        ferramentas.consultarBaseConhecimento(
            MAPPER.createObjectNode().put("query", "teto de juros").put("topK", 3), contexto);

    assertThat(resultado.citacoes()).hasSize(1);
    assertThat(resultado.citacoes().get(0).sourceId()).isEqualTo("02-bcb-res4549");
    assertThat(resultado.conteudo()).contains("art. 2-C");
  }
}
