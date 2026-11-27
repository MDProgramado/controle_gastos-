package com.controlefinanceiro.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Rate limit por usuario para o chat de IA. Limite por instância (ok enquanto 1 instância na
 * Render).
 */
@Component
public class AiRateLimiter {

  private static final int LIMITE_MINUTO = 10;
  private static final int LIMITE_DIA = 100;
  private static final Duration JANELA_MINUTO = Duration.ofMinutes(1);
  private static final Duration JANELA_DIA = Duration.ofDays(1);

  private final Map<Long, Deque<Instant>> eventosPorUsuario = new ConcurrentHashMap<>();

  public void verificar(Long usuarioId) {
    if (usuarioId == null) return;
    Instant agora = Instant.now();
    Deque<Instant> eventos =
        eventosPorUsuario.computeIfAbsent(usuarioId, k -> new ArrayDeque<>());
    synchronized (eventos) {
      limparAntigos(eventos, agora);
      long noMinuto = contarDesde(eventos, agora.minus(JANELA_MINUTO));
      if (noMinuto >= LIMITE_MINUTO) {
        throw new com.controlefinanceiro.common.LimiteIaExcedidoException(
            "Muitas mensagens em pouco tempo. Aguarde um minuto e tente novamente.");
      }
      long noDia = eventos.size();
      if (noDia >= LIMITE_DIA) {
        throw new com.controlefinanceiro.common.LimiteIaExcedidoException(
            "Você atingiu o limite diário de mensagens da IA. Tente novamente amanhã.");
      }
      eventos.addLast(agora);
    }
  }

  private void limparAntigos(Deque<Instant> eventos, Instant agora) {
    Instant limite = agora.minus(JANELA_DIA);
    while (!eventos.isEmpty() && eventos.peekFirst().isBefore(limite)) {
      eventos.removeFirst();
    }
  }

  private long contarDesde(Deque<Instant> eventos, Instant inicio) {
    long total = 0;
    for (Instant evento : eventos) {
      if (!evento.isBefore(inicio)) total++;
    }
    return total;
  }
}
