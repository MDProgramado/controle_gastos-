package com.controlefinanceiro.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guardrail de entrada do chat (OWASP LLM01 — prompt injection). Roda antes de qualquer chamada ao
 * LLM: se a mensagem casar com um padrão conhecido de injection, ou passar do tamanho máximo, o LLM
 * não é chamado e o usuário recebe um esclarecimento.
 */
public class GuardrailEntrada {

  private static final Logger LOG = LoggerFactory.getLogger(GuardrailEntrada.class);

  public static final int TAMANHO_MAXIMO = 2000;

  static final String RESPOSTA_BLOQUEIO =
      "Posso ajudar com seus lançamentos, consultas e dicas de educação financeira. Pode"
          + " reformular?";
  static final String RESPOSTA_LONGA =
      "Sua mensagem ficou muito longa. Pode resumir em até " + TAMANHO_MAXIMO + " caracteres?";

  private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

  /** Padrões nomeados (o nome vai para o log; a mensagem nunca). */
  private static final Map<String, Pattern> PADROES = new LinkedHashMap<>();

  static {
    PADROES.put(
        "ignore-instrucoes-pt",
        Pattern.compile(
            "ignor[ea]r?\\s+(as\\s+|todas\\s+as\\s+)(instru[cç][oõ]es|regras)\\s+anteriores",
            FLAGS));
    PADROES.put(
        "ignore-instructions-en",
        Pattern.compile("ignore\\s+(all\\s+|the\\s+|any\\s+)?previous\\s+instructions", FLAGS));
    PADROES.put("voce-agora-e", Pattern.compile("voc[eê]\\s+agora\\s+[eé](?!\\p{L})", FLAGS));
    PADROES.put("you-are-now", Pattern.compile("you\\s+are\\s+now\\b", FLAGS));
    PADROES.put(
        "revele-prompt",
        Pattern.compile("revel[ea]r?\\s+(o|seu)\\s+(prompt|system\\s+prompt)", FLAGS));
    PADROES.put("system-prompt", Pattern.compile("system\\s*prompt", FLAGS));
    PADROES.put("modo-desenvolvedor", Pattern.compile("modo\\s+desenvolvedor", FLAGS));
    PADROES.put("developer-mode", Pattern.compile("developer\\s+mode", FLAGS));
    PADROES.put("jailbreak", Pattern.compile("jail\\s*break", FLAGS));
  }

  /**
   * Avalia a mensagem do usuário.
   *
   * @return resposta de esclarecimento quando bloqueada; vazio quando pode seguir para o LLM
   */
  public Optional<AiResponse> avaliar(String mensagem, String usuarioId) {
    if (mensagem == null) {
      return Optional.empty();
    }
    if (mensagem.length() > TAMANHO_MAXIMO) {
      LOG.warn(
          "Guardrail de entrada: mensagem acima de {} caracteres (usuarioId={}, tamanho={})",
          TAMANHO_MAXIMO,
          usuarioId,
          mensagem.length());
      return Optional.of(esclarecimento(RESPOSTA_LONGA));
    }
    for (Map.Entry<String, Pattern> padrao : PADROES.entrySet()) {
      if (padrao.getValue().matcher(mensagem).find()) {
        LOG.warn(
            "Guardrail de entrada bloqueou possível prompt injection (usuarioId={}, padrao={})",
            usuarioId,
            padrao.getKey());
        return Optional.of(esclarecimento(RESPOSTA_BLOQUEIO));
      }
    }
    return Optional.empty();
  }

  private static AiResponse esclarecimento(String texto) {
    return new AiResponse("esclarecimento", texto, List.of(), null, List.of(), List.of());
  }
}
