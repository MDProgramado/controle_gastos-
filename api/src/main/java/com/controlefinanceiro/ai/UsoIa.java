package com.controlefinanceiro.ai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uso da OpenRouter (objeto {@code usage}) de uma ou mais chamadas a um modelo.
 *
 * @param operacao {@code chat}, {@code extracao} ou {@code embedding}
 * @param modelo modelo solicitado
 * @param custoUsd campo {@code usage.cost} da OpenRouter (zero quando não informado)
 */
public record UsoIa(
    String operacao,
    String modelo,
    long tokensEntrada,
    long tokensSaida,
    BigDecimal custoUsd,
    int chamadas) {

  public UsoIa {
    custoUsd = custoUsd == null ? BigDecimal.ZERO : custoUsd;
  }

  /** Chamada que falhou (sem resposta útil): conta a chamada, sem tokens. */
  public static UsoIa falha(String operacao, String modelo) {
    return new UsoIa(operacao, modelo, 0, 0, BigDecimal.ZERO, 1);
  }

  public UsoIa somar(UsoIa outro) {
    return new UsoIa(
        operacao,
        modelo,
        tokensEntrada + outro.tokensEntrada,
        tokensSaida + outro.tokensSaida,
        custoUsd.add(outro.custoUsd),
        chamadas + outro.chamadas);
  }

  /** Soma os usos por (operação, modelo), preservando a ordem da primeira ocorrência. */
  public static List<UsoIa> consolidar(List<UsoIa> usos) {
    Map<String, UsoIa> porChave = new LinkedHashMap<>();
    if (usos != null) {
      for (UsoIa uso : usos) {
        porChave.merge(uso.operacao() + "\u0000" + uso.modelo(), uso, UsoIa::somar);
      }
    }
    return new ArrayList<>(porChave.values());
  }
}
