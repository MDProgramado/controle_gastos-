package com.controlefinanceiro.importacao;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sanitização de dados pessoais antes de qualquer envio à OpenRouter.
 *
 * <p>Máscaras aplicadas: CPF, CNPJ e números de cartão (13–19 dígitos, mantendo apenas os 4
 * últimos). Nunca falha por conteúdo estranho.
 */
public final class SanitizadorTexto {

  private SanitizadorTexto() {}

  private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b");
  private static final Pattern CNPJ =
      Pattern.compile("\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b");
  // Sequências de 13 a 19 dígitos (possivelmente com traços/espaços entre blocos),
  // típicas de números de cartão de crédito e similares.
  private static final Pattern CARTAO = Pattern.compile("\\b(?:[\\d\\- ]{13,19})\\b");

  /**
   * Retorna o texto com dados sensíveis substituídos por máscara.
   *
   * <p>Qualquer problema de padrão devolve o texto original — a sanitização nunca derruba o fluxo
   * de importação.
   */
  public static String sanitizar(String texto) {
    if (texto == null) {
      return null;
    }
    try {
      String resultado = CPF.matcher(texto).replaceAll("***.***.***-**");
      resultado = CNPJ.matcher(resultado).replaceAll("**.***.***/****-**");
      resultado = mascararCartoes(resultado);
      return resultado;
    } catch (Exception e) {
      return texto;
    }
  }

  private static String mascararCartoes(String texto) {
    Matcher matcher = CARTAO.matcher(texto);
    StringBuilder sb = new StringBuilder();
    int fimAnterior = 0;
    while (matcher.find()) {
      String bloco = matcher.group();
      String soDigitos = bloco.replaceAll("[^\\d]", "");
      // Ignora sequências curtas demais (datas isoladas, CEP, etc.) que
      // acasalmente batam no intervalo 13-19 com separadores.
      if (soDigitos.length() < 13 || soDigitos.length() > 19) {
        continue;
      }
      String ultimos = soDigitos.substring(Math.max(0, soDigitos.length() - 4));
      String mascarado = "****".repeat(Math.max(0, (soDigitos.length() - 4) / 4)) + ultimos;
      // Ajusta o tamanho da máscara para bater com o bloco original (aproximado).
      sb.append(texto, fimAnterior, matcher.start());
      sb.append(mascarado);
      fimAnterior = matcher.end();
    }
    sb.append(texto, fimAnterior, texto.length());
    return sb.toString();
  }
}
