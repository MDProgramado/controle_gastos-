package com.controlefinanceiro.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Guardrail de saída do chat: descarta respostas que vazam instruções/delimitadores do prompt,
 * sinaliza propostas de lançamento suspeitas e remove citações sem fonte recuperada.
 */
public class GuardrailSaida {

  static final String AVISO_CONFERIR = "Confira o valor e a data antes de confirmar.";
  static final String RESPOSTA_GENERICA =
      "Não consegui gerar uma resposta segura agora. Pode reformular a pergunta?";

  private static final BigDecimal VALOR_MAXIMO = new BigDecimal("1000000.00");
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  /** Trechos que só existem no system prompt ou nos delimitadores de dados. */
  private static final List<String> MARCADORES_VAZAMENTO =
      List.of(
          "formato obrigatório",
          "regras:",
          "<" + SanitizadorPrompt.TAG_DADOS,
          "</" + SanitizadorPrompt.TAG_DADOS,
          "<" + SanitizadorPrompt.TAG_CONHECIMENTO,
          "</" + SanitizadorPrompt.TAG_CONHECIMENTO,
          "<" + SanitizadorPrompt.TAG_FERRAMENTA,
          "</" + SanitizadorPrompt.TAG_FERRAMENTA);

  /** Citação no formato pedido pelo prompt: {@code [título — órgão, vigência, ... (source_id)]}. */
  private static final Pattern CITACAO = Pattern.compile("\\s*\\[[^\\[\\]]*—[^\\[\\]]*]");

  /** {@code true} quando a resposta contém trechos do system prompt ou das tags de dados. */
  public boolean vazaInstrucoes(String texto) {
    if (texto == null) {
      return false;
    }
    String t = texto.toLowerCase(Locale.ROOT);
    return MARCADORES_VAZAMENTO.stream().anyMatch(t::contains);
  }

  public AiResponse respostaGenerica() {
    return new AiResponse(
        "esclarecimento",
        RESPOSTA_GENERICA,
        List.of(),
        null,
        List.of(),
        List.of("Tentar novamente", "Falar com atendente"));
  }

  /**
   * Mantém as propostas, mas adiciona aviso quando algum valor passa de R$ 1.000.000,00 ou a data
   * está a mais de 1 ano no passado/futuro.
   */
  public AiResponse revisarPropostas(AiResponse resposta) {
    if (resposta == null || resposta.propostas() == null || resposta.propostas().isEmpty()) {
      return resposta;
    }
    boolean suspeita = resposta.propostas().stream().anyMatch(this::suspeita);
    if (!suspeita || resposta.resposta().contains(AVISO_CONFERIR)) {
      return resposta;
    }
    return new AiResponse(
        resposta.intent(),
        resposta.resposta().stripTrailing() + " " + AVISO_CONFERIR,
        resposta.dados(),
        resposta.proposta(),
        resposta.propostas(),
        resposta.opcoes(),
        resposta.citacoes());
  }

  /** Sem fonte recuperada pelo RAG, remove citações no formato da base (seriam inventadas). */
  public String removerCitacoesSemFonte(String texto, List<AiResponse.AiCitacao> citacoes) {
    if (texto == null || (citacoes != null && !citacoes.isEmpty())) {
      return texto;
    }
    return CITACAO.matcher(texto).replaceAll("");
  }

  private boolean suspeita(AiResponse.AiPropostaLancamento p) {
    if (p.valor() != null && p.valor().compareTo(VALOR_MAXIMO) > 0) {
      return true;
    }
    try {
      LocalDate data = LocalDate.parse(p.data());
      LocalDate hoje = LocalDate.now(FUSO);
      return data.isBefore(hoje.minusYears(1)) || data.isAfter(hoje.plusYears(1));
    } catch (DateTimeParseException | NullPointerException e) {
      return true;
    }
  }
}
