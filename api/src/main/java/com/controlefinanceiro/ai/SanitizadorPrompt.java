package com.controlefinanceiro.ai;

import java.util.regex.Pattern;

/**
 * Sanitiza texto vindo do usuário, do admin, do banco ou do RAG antes de inseri-lo nos
 * delimitadores de dados do prompt (defesa contra prompt injection, OWASP LLM01).
 *
 * <p>Remove tags que imitam os delimitadores ({@code <dados_financeiros>}, {@code
 * </resultado_ferramenta>}...) e neutraliza os prefixos do protocolo de saída ({@code LANCAMENTO|},
 * {@code CONSULTA|}, {@code ESCLARECIMENTO|}, {@code FERRAMENTA|}) trocando o {@code |} por {@code
 * :}, para que um dado nunca vire uma "linha de comando" do protocolo.
 */
public final class SanitizadorPrompt {

  public static final String TAG_DADOS = "dados_financeiros";
  public static final String TAG_CONHECIMENTO = "conhecimento_empresa";
  public static final String TAG_FERRAMENTA = "resultado_ferramenta";

  private static final Pattern TAGS =
      Pattern.compile(
          "<\\s*/?\\s*(" + TAG_DADOS + "|" + TAG_CONHECIMENTO + "|" + TAG_FERRAMENTA + ")\\s*>",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern PROTOCOLO =
      Pattern.compile(
          "(LANCAMENTO|CONSULTA|ESCLARECIMENTO|FERRAMENTA)\\s*\\|", Pattern.CASE_INSENSITIVE);

  private SanitizadorPrompt() {}

  /** Remove tags de delimitação e neutraliza prefixos do protocolo. Nulo vira vazio. */
  public static String sanitizar(String texto) {
    if (texto == null) {
      return "";
    }
    String limpo = texto;
    // Repete até estabilizar: "<dados_<dados_financeiros>financeiros>" não pode sobrar.
    String anterior;
    do {
      anterior = limpo;
      limpo = TAGS.matcher(limpo).replaceAll("");
    } while (!limpo.equals(anterior));
    return PROTOCOLO.matcher(limpo).replaceAll("$1:");
  }

  /** Envolve o conteúdo (já sanitizado) entre {@code <tag>} e {@code </tag>}. */
  public static String envolver(String tag, String conteudo) {
    return "<" + tag + ">\n" + sanitizar(conteudo) + "\n</" + tag + ">";
  }
}
