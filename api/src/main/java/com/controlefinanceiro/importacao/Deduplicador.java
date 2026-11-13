package com.controlefinanceiro.importacao;

import com.controlefinanceiro.importacao.model.ImportacaoFatura;
import com.controlefinanceiro.importacao.model.ImportacaoItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detecção de duplicidades da importação (fase 5, D-09).
 *
 * <p>A chave de dedupe é SHA-256 de {@code tipo|data|valor|categoria} (valor com 2 casas e ponto
 * decimal) — o mesmo contrato da coluna {@code lancamento.chave_dedupe} (migration V3, índice único
 * parcial por empresa). Dinheiro só em {@link BigDecimal}: nenhuma comparação com double.
 */
public final class Deduplicador {

  /** Tolerância para "valor ≈ total da fatura" (centavos). */
  static final BigDecimal TOLERANCIA_CENTAVOS = new BigDecimal("0.05");

  private Deduplicador() {}

  /**
   * Chave estável do lançamento: mesmo tipo+data+valor+categoria → mesma chave.
   *
   * @param tipo tipo do lançamento (RECEITA | DESPESA_PAGA | A_PAGAR)
   * @param data data do lançamento
   * @param valor valor monetário (2 casas, HALF_EVEN antes de hash)
   * @param categoria nome da categoria (normalizado — sem acento/caixa)
   */
  public static String gerarChaveDedupe(
      String tipo, LocalDate data, BigDecimal valor, String categoria) {
    String base =
        (tipo == null ? "-" : tipo.trim())
            + "|"
            + (data == null ? "-" : data.toString())
            + "|"
            + (valor == null ? "-" : valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString())
            + "|"
            + (categoria == null ? "-" : NormalizadorTexto.normalizar(categoria));
    return sha256(base);
  }

  /** Recomputa as chaves de dedupe de itens (staging IMPORTADO ou novos do lote). */
  public static Set<String> chavesDe(List<ImportacaoItem> itens) {
    Set<String> chaves = new HashSet<>();
    if (itens == null) {
      return chaves;
    }
    for (ImportacaoItem item : itens) {
      chaves.add(
          gerarChaveDedupe(
              item.getTipo(), item.getData(), item.getValor(), item.getCategoriaNome()));
    }
    return chaves;
  }

  /**
   * @param item item candidato à gravação
   * @param chavesExistentes chaves já gravadas em {@code lancamento} ou em staging IMPORTADO
   * @return true se o mesmo lançamento já existe para o tenant
   */
  public static boolean ehDuplicado(ImportacaoItem item, Set<String> chavesExistentes) {
    if (item == null || chavesExistentes == null || chavesExistentes.isEmpty()) {
      return false;
    }
    String chave =
        gerarChaveDedupe(item.getTipo(), item.getData(), item.getValor(), item.getCategoriaNome());
    return chavesExistentes.contains(chave);
  }

  /**
   * Detecta, entre itens de EXTRATO, o pagamento da própria fatura de cartão (D-26): descrição
   * menciona "fatura"/"cartão" e valor ≈ total da fatura (±0,05). Marca o item com alerta {@code
   * FATURA_NO_EXTRATO} (acrescentando ao alerta existente, se houver).
   *
   * @return quantos itens foram marcados
   */
  public static int detetarPagamentoPropriaFatura(
      List<ImportacaoItem> itensExtrato, ImportacaoFatura fatura) {
    if (itensExtrato == null || fatura == null) {
      return 0;
    }
    int marcados = 0;
    for (ImportacaoItem item : itensExtrato) {
      if (!"EXTRATO".equals(item.getOrigem())) {
        continue;
      }
      if (ehPagamentoDeFatura(item.getDescricao())
          && valorProximo(item.getValor(), fatura.getTotal())) {
        item.setAlerta(
            acrescentarAlerta(
                item.getAlerta(),
                "FATURA_NO_EXTRATO: parece o pagamento da própria fatura — não lance de novo"));
        marcados++;
      }
    }
    return marcados;
  }

  /** Descrição menciona fatura ou cartão (sem acento/caixa). */
  static boolean ehPagamentoDeFatura(String descricao) {
    String d = NormalizadorTexto.normalizar(descricao);
    return d.contains("fatura") || d.contains("cartao");
  }

  /** |a − b| ≤ 0,05 (BigDecimal, nunca double). */
  static boolean valorProximo(BigDecimal a, BigDecimal b) {
    if (a == null || b == null) {
      return false;
    }
    return a.subtract(b).abs().compareTo(TOLERANCIA_CENTAVOS) <= 0;
  }

  /** Concatena alertas do item com separador legível. */
  static String acrescentarAlerta(String existente, String novo) {
    if (existente == null || existente.isBlank()) {
      return novo;
    }
    return existente + " | " + novo;
  }

  private static String sha256(String base) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 é obrigatório no JDK — impossível na prática
      throw new IllegalStateException("SHA-256 não disponível", e);
    }
  }
}
