package com.controlefinanceiro.importacao;

import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Casamento da categoria sugerida pela IA com as categorias reais do tenant (fase 5, D-25).
 *
 * <p>Nunca deixa a IA criar categoria: normaliza (sem acento, minúsculas), casa por nome exato,
 * sinônimos e contenção; sem correspondência cai no fallback do tipo ({@code Outros} para SAÍDA,
 * {@code Outras receitas} para ENTRADA) e marca {@code necessitaRevisao} para o usuário revisar na
 * tela de confirmação.
 */
@Service
public class MapeadorCategoria {

  /** Sinônimos → nome normalizado da categoria do tenant (exhaustive-match pós-normalização). */
  private static final Map<String, String> SINONIMOS = sinonimos();

  private static final String FALLBACK_SAIDA = "Outros";
  private static final String FALLBACK_ENTRADA = "Outras receitas";

  private final CategoriaRepository categoriaRepository;

  public MapeadorCategoria(CategoriaRepository categoriaRepository) {
    this.categoriaRepository = categoriaRepository;
  }

  /**
   * Resultado do casamento.
   *
   * @param categoriaId id da categoria do tenant (nulo quando nem o fallback existe)
   * @param categoriaNome nome resolvido (ou sugerido para revisão)
   * @param necessitaRevisao true quando caiu no fallback (usuário deve revisar na confirmação)
   */
  public record ResultadoMapeamento(
      Long categoriaId, String categoriaNome, boolean necessitaRevisao) {}

  /**
   * @param categoriaSugeridaIA nome vindo da resposta da IA (livre)
   * @param tipoLancamento RECEITA/DESPESA_PAGA/A_PAGAR (IA) ou ENTRADA/SAIDA (domínio)
   * @param empresaId tenant do token (R3)
   */
  public ResultadoMapeamento mapear(
      String categoriaSugeridaIA, String tipoLancamento, Long empresaId) {
    return mapear(
        categoriaSugeridaIA, tipoLancamento, categoriaRepository.findByEmpresaId(empresaId));
  }

  /** Variante com o pool já carregado — evita uma consulta por item no lote. */
  public ResultadoMapeamento mapear(
      String categoriaSugeridaIA, String tipoLancamento, List<Categoria> doTenant) {
    String tipoCategoria = tipoCategoria(tipoLancamento);
    String sugerida = NormalizadorTexto.normalizar(categoriaSugeridaIA);
    List<Categoria> categorias = doTenant == null ? List.of() : doTenant;

    if (!sugerida.isEmpty()) {
      // 1. casamento exato (normalizado)
      Categoria exata =
          categorias.stream()
              .filter(c -> tipoCategoria.equals(c.getTipo()))
              .filter(c -> NormalizadorTexto.normalizar(c.getNome()).equals(sugerida))
              .findFirst()
              .orElse(null);
      if (exata != null) {
        return new ResultadoMapeamento(exata.getId(), exata.getNome(), false);
      }

      // 2. sinônimos (ex.: "alimentacao" → "alimentação")
      String alvo = SINONIMOS.get(sugerida);
      if (alvo != null) {
        String alvoFinal = alvo;
        Categoria sinonima =
            categorias.stream()
                .filter(c -> tipoCategoria.equals(c.getTipo()))
                .filter(c -> NormalizadorTexto.normalizar(c.getNome()).equals(alvoFinal))
                .findFirst()
                .orElse(null);
        if (sinonima != null) {
          return new ResultadoMapeamento(sinonima.getId(), sinonima.getNome(), false);
        }
      }

      // 3. contenção (ex.: "alimentação em casa" contém "alimentação"); mínimo 4 caracteres
      if (sugerida.length() >= 4) {
        Categoria contendo =
            categorias.stream()
                .filter(c -> tipoCategoria.equals(c.getTipo()))
                .filter(
                    c -> {
                      String nome = NormalizadorTexto.normalizar(c.getNome());
                      return nome.length() >= 4
                          && (nome.contains(sugerida) || sugerida.contains(nome));
                    })
                .findFirst()
                .orElse(null);
        if (contendo != null) {
          return new ResultadoMapeamento(contendo.getId(), contendo.getNome(), false);
        }
      }
    }

    // 4. fallback do tipo + revisão obrigatória (D-25)
    String nomeFallback = "SAIDA".equals(tipoCategoria) ? FALLBACK_SAIDA : FALLBACK_ENTRADA;
    Categoria fallback =
        categorias.stream()
            .filter(c -> tipoCategoria.equals(c.getTipo()))
            .filter(
                c ->
                    NormalizadorTexto.normalizar(c.getNome())
                        .equals(NormalizadorTexto.normalizar(nomeFallback)))
            .findFirst()
            .orElse(null);
    if (fallback != null) {
      return new ResultadoMapeamento(fallback.getId(), fallback.getNome(), true);
    }
    return new ResultadoMapeamento(null, nomeFallback, true);
  }

  /**
   * Converte o tipo do lançamento no tipo de categoria: RECEITA → ENTRADA; DESPESA_PAGA e A_PAGAR →
   * SAIDA. Aceita também ENTRADA/SAIDA diretos (idempotente). Desconhecido → SAIDA (conservador).
   */
  String tipoCategoria(String tipoLancamento) {
    if (tipoLancamento == null) {
      return "SAIDA";
    }
    return switch (tipoLancamento) {
      case "RECEITA", "ENTRADA" -> "ENTRADA";
      default -> "SAIDA"; // DESPESA_PAGA, A_PAGAR, SAIDA e desconhecidos
    };
  }

  private static Map<String, String> sinonimos() {
    Map<String, String> mapa = new HashMap<>();
    mapa.put("saude", "saude/farmacia");
    mapa.put("farmacia", "saude/farmacia");
    mapa.put("medicamento", "saude/farmacia");
    mapa.put("remedio", "saude/farmacia");
    mapa.put("alimentacao", "alimentacao");
    mapa.put("mercado", "supermercado");
    mapa.put("supermercado", "supermercado");
    mapa.put("salario", "rendimentos e reembolsos");
    mapa.put("rendimentos", "rendimentos e reembolsos");
    mapa.put("reembolso", "rendimentos e reembolsos");
    mapa.put("transporte", "transporte");
    mapa.put("combustivel", "transporte");
    mapa.put("gasolina", "transporte");
    mapa.put("uber", "transporte");
    mapa.put("moradia", "moradia");
    mapa.put("aluguel", "moradia");
    mapa.put("energia", "moradia");
    mapa.put("luz", "moradia");
    mapa.put("agua", "moradia");
    mapa.put("lazer", "lazer");
    mapa.put("servicos", "servicos");
    mapa.put("servico", "servicos");
    mapa.put("assinaturas", "assinaturas");
    mapa.put("assinatura", "assinaturas");
    mapa.put("outros", "outros");
    mapa.put("outras receitas", "outras receitas");
    return Map.copyOf(mapa);
  }
}
