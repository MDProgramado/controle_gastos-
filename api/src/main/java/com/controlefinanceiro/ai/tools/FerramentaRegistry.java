package com.controlefinanceiro.ai.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class FerramentaRegistry {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Map<String, Ferramenta> ferramentas;

  public FerramentaRegistry(FerramentasFinanceiras financeiras) {
    Map<String, Ferramenta> mapa = new LinkedHashMap<>();
    mapa.put("listar_categorias", adapter("listar_categorias", financeiras::listarCategorias));
    mapa.put(
        "consultar_historico_e_comparativo",
        adapter("consultar_historico_e_comparativo", financeiras::consultarHistoricoEComparativo));
    mapa.put(
        "diagnosticar_orcamento_50_30_20",
        adapter("diagnosticar_orcamento_50_30_20", financeiras::diagnosticarOrcamento503020));
    mapa.put(
        "detectar_ralos_e_recorrencias",
        adapter("detectar_ralos_e_recorrencias", financeiras::detectarRalosERecorrencias));
    mapa.put(
        "simular_quitacao_ou_juros",
        adapter("simular_quitacao_ou_juros", financeiras::simularQuitacaoOuJuros));
    mapa.put(
        "consultar_base_conhecimento",
        adapter("consultar_base_conhecimento", financeiras::consultarBaseConhecimento));
    this.ferramentas = Map.copyOf(mapa);
  }

  public ResultadoFerramenta executar(FerramentaChamada chamada, Long empresaId, Long usuarioId) {
    Ferramenta ferramenta = ferramentas.get(chamada.nome());
    if (ferramenta == null) {
      return new ResultadoFerramenta(
          "{\"erro\":\"Ferramenta desconhecida: " + chamada.nome() + "\"}");
    }
    JsonNode args;
    try {
      args = MAPPER.readTree(chamada.argsJson());
    } catch (Exception e) {
      return new ResultadoFerramenta("{\"erro\":\"Argumentos da ferramenta não são JSON válido\"}");
    }
    if (args == null || !args.isObject()) {
      return new ResultadoFerramenta(
          "{\"erro\":\"Argumentos da ferramenta devem ser um objeto JSON\"}");
    }
    try {
      return ferramenta.executar(args, new ContextoFerramenta(empresaId, usuarioId));
    } catch (RuntimeException e) {
      return new ResultadoFerramenta(
          "{\"erro\":\"Falha ao executar " + chamada.nome() + ": " + e.getMessage() + "\"}");
    }
  }

  private static Ferramenta adapter(
      String nome, BiFunction<JsonNode, ContextoFerramenta, ResultadoFerramenta> funcao) {
    return new FerramentaAdapter(nome, funcao);
  }

  private record FerramentaAdapter(
      String nome, BiFunction<JsonNode, ContextoFerramenta, ResultadoFerramenta> funcao)
      implements Ferramenta {

    @Override
    public ResultadoFerramenta executar(JsonNode args, ContextoFerramenta contexto) {
      return funcao.apply(args, contexto);
    }
  }
}
