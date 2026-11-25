package com.controlefinanceiro.ai.rag;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RagIngestaoEstado {

  private static final int TAMANHO_MAXIMO_ERRO = 300;

  private RagIngestaoStatus atual = new RagIngestaoStatus("PARADO", 0, 0, null, null, null);

  public synchronized boolean reservar() {
    if ("EM_ANDAMENTO".equals(atual.situacao())) {
      return false;
    }
    atual = new RagIngestaoStatus("EM_ANDAMENTO", 0, 0, null, Instant.now(), null);
    return true;
  }

  public synchronized void iniciar(int total) {
    int quantidade = Math.max(total, 0);
    if ("EM_ANDAMENTO".equals(atual.situacao())) {
      atual =
          new RagIngestaoStatus(
              "EM_ANDAMENTO",
              quantidade,
              atual.processados(),
              atual.erro(),
              atual.iniciadoEm(),
              atual.finalizadoEm());
      return;
    }
    atual = new RagIngestaoStatus("EM_ANDAMENTO", quantidade, 0, null, Instant.now(), null);
  }

  public synchronized void registrarProgresso(int processados) {
    if (!"EM_ANDAMENTO".equals(atual.situacao())) {
      return;
    }
    int valor = Math.min(Math.max(processados, 0), atual.total());
    atual = comum(valor, null, atual.iniciadoEm(), null).comSituacao("EM_ANDAMENTO");
  }

  public synchronized void concluir() {
    if (!"EM_ANDAMENTO".equals(atual.situacao())) {
      return;
    }
    atual = comum(atual.total(), null, atual.iniciadoEm(), Instant.now()).comSituacao("SUCESSO");
  }

  public synchronized void falhar(String mensagem) {
    String erro = mensagem == null ? "Erro desconhecido" : mensagem.replaceAll("\\s+", " ").trim();
    if (erro.length() > TAMANHO_MAXIMO_ERRO) {
      erro = erro.substring(0, TAMANHO_MAXIMO_ERRO);
    }
    atual = comum(atual.processados(), erro, atual.iniciadoEm(), Instant.now()).comSituacao("ERRO");
  }

  public synchronized RagIngestaoStatus status() {
    return atual;
  }

  private RagIngestaoStatus comum(
      int processados, String erro, Instant iniciadoEm, Instant finalizadoEm) {
    return new RagIngestaoStatus(
        atual.situacao(), atual.total(), processados, erro, iniciadoEm, finalizadoEm);
  }

  public record RagIngestaoStatus(
      String situacao,
      int total,
      int processados,
      String erro,
      Instant iniciadoEm,
      Instant finalizadoEm) {

    private RagIngestaoStatus comSituacao(String situacao) {
      return new RagIngestaoStatus(situacao, total, processados, erro, iniciadoEm, finalizadoEm);
    }
  }
}
