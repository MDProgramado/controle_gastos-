package com.controlefinanceiro.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Custo da IA por empresa/usuário: acumula o uso de cada mensagem do chat e de cada extração de PDF
 * na tabela {@code ai_uso_diario}. Falha ao registrar nunca derruba o chat nem a importação.
 */
@Service
public class AiUsoService {

  private static final Logger LOG = LoggerFactory.getLogger(AiUsoService.class);
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final long MAX_DIAS_CONSULTA = 366;

  private final AiUsoRepository repository;

  public AiUsoService(AiUsoRepository repository) {
    this.repository = repository;
  }

  /** Consolida por (operação, modelo) e faz o upsert do dia atual (America/Sao_Paulo). */
  public void registrar(Long empresaId, Long usuarioId, List<UsoIa> usos) {
    if (empresaId == null || usuarioId == null || usos == null || usos.isEmpty()) {
      return;
    }
    LocalDate hoje = LocalDate.now(FUSO);
    for (UsoIa uso : UsoIa.consolidar(usos)) {
      try {
        repository.acumular(empresaId, usuarioId, hoje, uso);
      } catch (RuntimeException e) {
        LOG.warn(
            "Não foi possível registrar o uso da IA (operacao={}, modelo={}): {}",
            uso.operacao(),
            uso.modelo(),
            e.getClass().getSimpleName());
      }
    }
  }

  /** Relatório do período, sempre da empresa do JWT (o chamador passa o tenant do token). */
  public RelatorioUso relatorio(Long empresaId, LocalDate de, LocalDate ate) {
    if (de == null || ate == null || ate.isBefore(de)) {
      throw new IllegalArgumentException(
          "Período inválido: 'de' deve ser anterior ou igual a 'ate'");
    }
    if (ChronoUnit.DAYS.between(de, ate) > MAX_DIAS_CONSULTA) {
      throw new IllegalArgumentException("Período máximo de consulta: 366 dias");
    }
    List<AiUsoRepository.UsoPorDia> linhas = repository.porDiaEUsuario(empresaId, de, ate);
    long entrada = 0;
    long saida = 0;
    long chamadas = 0;
    BigDecimal custo = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_EVEN);
    for (AiUsoRepository.UsoPorDia l : linhas) {
      entrada += l.tokensEntrada();
      saida += l.tokensSaida();
      chamadas += l.chamadas();
      custo = custo.add(l.custoUsd());
    }
    return new RelatorioUso(de, ate, linhas, new Total(entrada, saida, custo, chamadas));
  }

  public record Total(long tokensEntrada, long tokensSaida, BigDecimal custoUsd, long chamadas) {}

  public record RelatorioUso(
      LocalDate de, LocalDate ate, List<AiUsoRepository.UsoPorDia> linhas, Total total) {}
}
