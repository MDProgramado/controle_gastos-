package com.controlefinanceiro.ai.tools;

import com.controlefinanceiro.ai.AiResponse;
import com.controlefinanceiro.ai.rag.RagSearchService;
import com.controlefinanceiro.ai.rag.RagTrecho;
import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.categoria.grupo.CategoriaGrupo503020;
import com.controlefinanceiro.categoria.grupo.CategoriaGrupo503020Repository;
import com.controlefinanceiro.dashboard.DashboardRepository;
import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.controlefinanceiro.lancamento.Lancamento;
import com.controlefinanceiro.lancamento.LancamentoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FerramentasFinanceiras {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final BigDecimal CEM = BigDecimal.valueOf(100);
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
  private static final BigDecimal ALVO_NECESSIDADE = new BigDecimal("50");
  private static final BigDecimal ALVO_DESEJO = new BigDecimal("30");
  private static final BigDecimal ALVO_FUTURO = new BigDecimal("20");

  private final DashboardRepository dashboardRepository;
  private final CategoriaRepository categoriaRepository;
  private final LancamentoRepository lancamentoRepository;
  private final CategoriaGrupo503020Repository grupoRepository;
  private final RagSearchService ragSearchService;

  public FerramentasFinanceiras(
      DashboardRepository dashboardRepository,
      CategoriaRepository categoriaRepository,
      LancamentoRepository lancamentoRepository,
      CategoriaGrupo503020Repository grupoRepository,
      RagSearchService ragSearchService) {
    this.dashboardRepository = dashboardRepository;
    this.categoriaRepository = categoriaRepository;
    this.lancamentoRepository = lancamentoRepository;
    this.grupoRepository = grupoRepository;
    this.ragSearchService = ragSearchService;
  }

  public ResultadoFerramenta listarCategorias(JsonNode args, ContextoFerramenta contexto) {
    List<Categoria> categorias = categoriaRepository.findByEmpresaId(contexto.empresaId());
    ArrayNode array = MAPPER.createArrayNode();
    for (Categoria categoria : categorias) {
      ObjectNode item = array.addObject();
      item.put("id", categoria.getId());
      item.put("nome", categoria.getNome());
      item.put("tipo", categoria.getTipo());
      item.put("pessoal", categoria.isPessoal());
    }
    return new ResultadoFerramenta(MAPPER.createObjectNode().set("categorias", array).toString());
  }

  public ResultadoFerramenta consultarHistoricoEComparativo(
      JsonNode args, ContextoFerramenta contexto) {
    int meses = Math.max(1, Math.min(intDe(args, "meses", 1), 36));
    LocalDate hoje = LocalDate.now(FUSO);
    LocalDate fimAtual = hoje;
    LocalDate inicioAtual = YearMonth.from(hoje).minusMonths(meses - 1).atDay(1);
    LocalDate fimAnterior = inicioAtual.minusDays(1);
    LocalDate inicioAnterior = YearMonth.from(fimAnterior).minusMonths(meses - 1).atDay(1);

    ObjectNode json = MAPPER.createObjectNode();
    json.put("meses", meses);
    json.put("data_inicio", inicioAtual.toString());
    json.put("data_fim", fimAtual.toString());
    json.put("periodo_anterior_inicio", inicioAnterior.toString());
    json.put("periodo_anterior_fim", fimAnterior.toString());
    json.put("mesmo_numero_de_meses", true);

    BigDecimal entradasAtual =
        total(
            dashboardRepository.totalEntradas(
                contexto.empresaId(), contexto.usuarioId(), inicioAtual, fimAtual));
    BigDecimal entradasAnterior =
        total(
            dashboardRepository.totalEntradas(
                contexto.empresaId(), contexto.usuarioId(), inicioAnterior, fimAnterior));
    BigDecimal saidasAtual =
        total(
            dashboardRepository.totalSaidas(
                contexto.empresaId(), contexto.usuarioId(), inicioAtual, fimAtual));
    BigDecimal saidasAnterior =
        total(
            dashboardRepository.totalSaidas(
                contexto.empresaId(), contexto.usuarioId(), inicioAnterior, fimAnterior));

    json.put("entradas", textoMoeda(entradasAtual));
    json.put("saidas", textoMoeda(saidasAtual));
    json.put("saldo", textoMoeda(entradasAtual.subtract(saidasAtual)));
    json.put("entradas_anterior", textoMoeda(entradasAnterior));
    json.put("saidas_anterior", textoMoeda(saidasAnterior));
    json.put("variacao_entradas", textoPercentual(variacao(entradasAtual, entradasAnterior)));
    json.put("variacao_saidas", textoPercentual(variacao(saidasAtual, saidasAnterior)));

    ArrayNode entradas = json.putArray("detalhes_entradas");
    for (CategoryExpense item :
        dashboardRepository.breakdownEntrada(
            contexto.empresaId(), contexto.usuarioId(), inicioAtual, fimAtual)) {
      ObjectNode detalhe = entradas.addObject();
      detalhe.put("categoria", item.nome());
      detalhe.put("total", textoMoeda(item.total()));
    }
    ArrayNode saidas = json.putArray("detalhes_saidas");
    for (CategoryExpense item :
        dashboardRepository.breakdownSaida(
            contexto.empresaId(), contexto.usuarioId(), inicioAtual, fimAtual)) {
      ObjectNode detalhe = saidas.addObject();
      detalhe.put("categoria", item.nome());
      detalhe.put("total", textoMoeda(item.total()));
    }
    return new ResultadoFerramenta(json.toString());
  }

  public ResultadoFerramenta diagnosticarOrcamento503020(
      JsonNode args, ContextoFerramenta contexto) {
    LocalDate hoje = LocalDate.now(FUSO);
    int mes = Math.max(1, Math.min(intDe(args, "mes", hoje.getMonthValue()), 12));
    int ano = intDe(args, "ano", hoje.getYear());
    LocalDate inicio = LocalDate.of(ano, mes, 1);
    LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

    BigDecimal renda =
        total(
            dashboardRepository.totalEntradas(
                contexto.empresaId(), contexto.usuarioId(), inicio, fim));
    List<CategoryExpense> gastos =
        dashboardRepository.breakdownSaida(contexto.empresaId(), contexto.usuarioId(), inicio, fim);

    Map<Long, String> grupos = new LinkedHashMap<>();
    List<Long> ids = new ArrayList<>();
    for (CategoryExpense gasto : gastos) {
      if (gasto.id() != null && !ids.contains(gasto.id())) {
        ids.add(gasto.id());
      }
    }
    if (!ids.isEmpty()) {
      for (CategoriaGrupo503020 mapeado :
          grupoRepository.findByEmpresaIdAndCategoriaIdIn(contexto.empresaId(), ids)) {
        grupos.put(mapeado.getCategoriaId(), mapeado.getGrupo());
      }
    }

    Map<String, BigDecimal> totais = new LinkedHashMap<>();
    totais.put("NECESSIDADE", ZERO);
    totais.put("DESEJO", ZERO);
    totais.put("FUTURO", ZERO);

    ArrayNode categorias = MAPPER.createArrayNode();
    List<String> avisos = new ArrayList<>();
    for (CategoryExpense gasto : gastos) {
      String grupo = grupos.get(gasto.id());
      boolean fallback = grupo == null || grupo.isBlank();
      if (fallback) {
        grupo = "DESEJO";
      }
      totais.merge(grupo, total(gasto.total()), BigDecimal::add);
      ObjectNode item = categorias.addObject();
      item.put("categoria", gasto.nome());
      item.put("grupo", grupo);
      item.put("total", textoMoeda(gasto.total()));
      item.put("percentual_renda", textoPercentual(percentual(gasto.total(), renda)));
      item.put("confianca", fallback ? "media" : "alta");
      if (fallback && gasto.total().compareTo(BigDecimal.ZERO) > 0) {
        String aviso =
            "Considerei "
                + gasto.nome()
                + " como Desejo por padrão; me avise se ela for uma despesa essencial";
        item.put("aviso", aviso);
        avisos.add(aviso);
      }
    }

    ObjectNode json = MAPPER.createObjectNode();
    json.put("mes", mes);
    json.put("ano", ano);
    json.put("renda", textoMoeda(renda));
    json.put("gastos", textoMoeda(soma(totais.values())));
    json.put("usou_fallback", !avisos.isEmpty());
    json.put("avisos", String.join(" ", avisos));
    json.set("categorias", categorias);

    ObjectNode gruposJson = json.putObject("grupos");
    gruposJson.put("NECESSIDADE", grupoJson(totais.get("NECESSIDADE"), renda, ALVO_NECESSIDADE));
    gruposJson.put("DESEJO", grupoJson(totais.get("DESEJO"), renda, ALVO_DESEJO));
    gruposJson.put("FUTURO", grupoJson(totais.get("FUTURO"), renda, ALVO_FUTURO));
    return new ResultadoFerramenta(json.toString());
  }

  public ResultadoFerramenta detectarRalosERecorrencias(
      JsonNode args, ContextoFerramenta contexto) {
    List<Lancamento> saidas =
        lancamentoRepository.findByEmpresaIdAndUsuarioIdAndTipo(
            contexto.empresaId(), contexto.usuarioId(), "SAIDA");
    Map<String, List<Lancamento>> porDescricao = new LinkedHashMap<>();
    for (Lancamento lancamento : saidas) {
      String chave =
          normalizar(lancamento.getDescricao()) + "#" + categoriaSimplificada(lancamento);
      porDescricao.computeIfAbsent(chave, k -> new ArrayList<>()).add(lancamento);
    }

    ObjectNode json = MAPPER.createObjectNode();
    ArrayNode recorrencias = json.putArray("recorrencias");
    ArrayNode ralos = json.putArray("ralos");
    for (Map.Entry<String, List<Lancamento>> entry : porDescricao.entrySet()) {
      List<Lancamento> lista = entry.getValue();
      if (lista.size() < 2) {
        continue;
      }
      lista.sort(Comparator.comparing(Lancamento::getData));
      BigDecimal total = ZERO;
      for (Lancamento lancamento : lista) {
        total = total.add(total(lancamento.getValor()));
      }
      ObjectNode item = recorrencias.addObject();
      item.put("descricao", lista.get(0).getDescricao());
      item.put("quantidade", lista.size());
      item.put("total", textoMoeda(total));
      item.put(
          "ticket_medio",
          textoMoeda(total.divide(BigDecimal.valueOf(lista.size()), 2, RoundingMode.HALF_EVEN)));
      item.put("primeira_data", lista.get(0).getData().toString());
      item.put("ultima_data", lista.get(lista.size() - 1).getData().toString());
      if (lista.size() >= 3 && total.compareTo(new BigDecimal("300.00")) < 0) {
        ObjectNode ralo = ralos.addObject();
        ralo.put("descricao", lista.get(0).getDescricao());
        ralo.put("quantidade", lista.size());
        ralo.put("total", textoMoeda(total));
      }
    }
    return new ResultadoFerramenta(json.toString());
  }

  public ResultadoFerramenta simularQuitacaoOuJuros(JsonNode args, ContextoFerramenta contexto) {
    BigDecimal valor = decimalDe(args, "valor");
    BigDecimal taxaPercentual = decimalDe(args, "taxa_mensal");
    int parcelas = Math.max(1, Math.min(intDe(args, "parcelas", 12), 360));
    if (valor.signum() <= 0) {
      return new ResultadoFerramenta("{\"erro\":\"Valor deve ser positivo\"}");
    }
    BigDecimal taxa = taxaPercentual.divide(CEM, 20, RoundingMode.HALF_EVEN);
    BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas);
    BigDecimal desconto = BigDecimal.ONE.divide(fator, 20, RoundingMode.HALF_EVEN);
    BigDecimal denominador = BigDecimal.ONE.subtract(desconto);
    BigDecimal parcela;
    if (taxa.compareTo(BigDecimal.ZERO) == 0 || denominador.compareTo(BigDecimal.ZERO) == 0) {
      parcela = valor.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
    } else {
      parcela = valor.multiply(taxa).divide(denominador, 2, RoundingMode.HALF_EVEN);
    }
    BigDecimal valorTotal =
        parcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, RoundingMode.HALF_EVEN);
    BigDecimal juros = valorTotal.subtract(valor).setScale(2, RoundingMode.HALF_EVEN);

    ObjectNode json = MAPPER.createObjectNode();
    json.put("valor", textoMoeda(valor));
    json.put(
        "taxa_mensal_percentual", textoNumero(taxaPercentual.setScale(2, RoundingMode.HALF_EVEN)));
    json.put("parcelas", parcelas);
    json.put("parcela", textoNumero(parcela));
    json.put("valor_total", textoNumero(valorTotal));
    json.put("juros_totais", textoNumero(juros));
    return new ResultadoFerramenta(json.toString());
  }

  public ResultadoFerramenta consultarBaseConhecimento(JsonNode args, ContextoFerramenta contexto) {
    String consulta = args != null && args.hasNonNull("query") ? args.path("query").asText() : "";
    int topK = Math.max(1, Math.min(intDe(args, "topK", 5), 10));
    List<RagTrecho> trechos = ragSearchService.buscar(consulta, topK);

    ArrayNode array = MAPPER.createArrayNode();
    List<AiResponse.AiCitacao> citacoes = new ArrayList<>();
    for (RagTrecho trecho : trechos) {
      ObjectNode item = array.addObject();
      item.put("chunk_id", trecho.chunkId());
      item.put("source_id", trecho.sourceId());
      item.put("titulo", trecho.titulo());
      item.put("organizacao", trecho.organizacao());
      item.put("vigencia", trecho.vigencia());
      item.put("dispositivo", trecho.dispositivo());
      item.put("texto", trecho.texto());
      citacoes.add(
          new AiResponse.AiCitacao(
              trecho.sourceId(),
              trecho.titulo(),
              trecho.organizacao(),
              trecho.vigencia(),
              trecho.dispositivo()));
    }
    ObjectNode json = MAPPER.createObjectNode();
    json.put("consulta", consulta);
    json.set("trechos", array);
    return new ResultadoFerramenta(json.toString(), citacoes);
  }

  private ObjectNode grupoJson(BigDecimal total, BigDecimal renda, BigDecimal alvo) {
    ObjectNode json = MAPPER.createObjectNode();
    json.put("total", textoMoeda(total));
    json.put("percentual_renda", textoPercentual(percentual(total, renda)));
    json.put("alvo_percentual", textoPercentual(alvo));
    json.put("alvo_valor", textoMoeda(renda.multiply(alvo).divide(CEM, 2, RoundingMode.HALF_EVEN)));
    return json;
  }

  private BigDecimal percentual(BigDecimal valor, BigDecimal base) {
    if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
      return ZERO;
    }
    return valor.multiply(CEM).divide(base, 2, RoundingMode.HALF_EVEN);
  }

  private BigDecimal variacao(BigDecimal atual, BigDecimal anterior) {
    if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
      return ZERO;
    }
    return atual.subtract(anterior).multiply(CEM).divide(anterior, 2, RoundingMode.HALF_EVEN);
  }

  private BigDecimal soma(Iterable<BigDecimal> valores) {
    BigDecimal total = ZERO;
    for (BigDecimal valor : valores) {
      total = total.add(total(valor));
    }
    return total.setScale(2, RoundingMode.HALF_EVEN);
  }

  private BigDecimal total(BigDecimal valor) {
    return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_EVEN);
  }

  private int intDe(JsonNode args, String campo, int padrao) {
    if (args == null || !args.hasNonNull(campo) || !args.path(campo).isNumber()) {
      return padrao;
    }
    return args.path(campo).asInt(padrao);
  }

  private BigDecimal decimalDe(JsonNode args, String campo) {
    if (args == null || !args.hasNonNull(campo)) {
      return ZERO;
    }
    JsonNode valor = args.path(campo);
    if (valor.isNumber()) {
      return valor.decimalValue();
    }
    String texto = valor.asText("0").replace(',', '.').replaceAll("[^0-9.-]", "");
    try {
      return new BigDecimal(texto);
    } catch (NumberFormatException e) {
      return ZERO;
    }
  }

  private String normalizar(String texto) {
    if (texto == null || texto.isBlank()) {
      return "sem descricao";
    }
    return texto.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  private String categoriaSimplificada(Lancamento lancamento) {
    return lancamento.getCategoria() == null
        ? "sem categoria"
        : lancamento.getCategoria().getNome();
  }

  private String textoMoeda(BigDecimal valor) {
    return "R$ " + valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString().replace('.', ',');
  }

  private String textoNumero(BigDecimal valor) {
    return valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString().replace('.', ',');
  }

  private String textoPercentual(BigDecimal valor) {
    return textoNumero(valor) + "%";
  }
}
