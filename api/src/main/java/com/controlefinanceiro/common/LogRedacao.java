package com.controlefinanceiro.common;

import com.controlefinanceiro.importacao.SanitizadorTexto;
import java.util.regex.Pattern;

public final class LogRedacao {

  private static final String REDACTED = "[REDACTED]";
  private static final int LIMITE = 200;

  private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+");
  private static final Pattern CONVITE_TOKEN =
      Pattern.compile("(?i)(\\bconviteToken[\"']?\\s*[=:]\\s*)([\"']?)([^&\"'\\s,}]+)(\\2)");
  private static final Pattern EMAIL =
      Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
  private static final Pattern TELEFONE_E164 = Pattern.compile("\\+[1-9]\\d{7,14}");
  private static final Pattern JWT =
      Pattern.compile("\\beyJ[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");

  private LogRedacao() {}

  public static String paraLog(String texto) {
    if (texto == null) {
      return "";
    }
    String redacionado = BEARER.matcher(texto).replaceAll(REDACTED);
    redacionado = CONVITE_TOKEN.matcher(redacionado).replaceAll("$1$2" + REDACTED + "$2");
    redacionado = EMAIL.matcher(redacionado).replaceAll(REDACTED);
    redacionado = TELEFONE_E164.matcher(redacionado).replaceAll(REDACTED);
    redacionado = JWT.matcher(redacionado).replaceAll(REDACTED);
    return truncar(SanitizadorTexto.sanitizar(redacionado));
  }

  public static String mensagem(Throwable erro) {
    if (erro == null) {
      return "";
    }
    return paraLog(erro.getMessage());
  }

  private static String truncar(String texto) {
    if (texto.length() <= LIMITE) {
      return texto;
    }
    int fim = LIMITE;
    if (Character.isHighSurrogate(texto.charAt(fim - 1))
        && Character.isLowSurrogate(texto.charAt(fim))) {
      fim--;
    }
    return texto.substring(0, fim);
  }
}
