package com.controlefinanceiro.health;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check público da API (INF-03). Controller custom em vez de actuator porque o caminho
 * exigido é /api/health (RESEARCH: remapear actuator produziria /api/actuator/health).
 *
 * <p>Render recomenda checar o banco: SELECT 1 falha com exceção → 500 genérico → Render restarta
 * (T-1-02: resposta só com {"status":"OK"}, sem detalhe de erro).
 */
@RestController
public class HealthController {

  private final JdbcTemplate jdbc;

  public HealthController(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/api/health")
  public Map<String, String> health() {
    jdbc.queryForObject("SELECT 1", Integer.class);
    return Map.of("status", "OK");
  }
}
