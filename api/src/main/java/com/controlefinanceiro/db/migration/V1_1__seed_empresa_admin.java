package com.controlefinanceiro.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Seed D-03/D-04 (AUTH-04): 1 empresa "Controle Financeiro" + usuário {@code admin@controle.com}
 * com role ADMIN e ativo=true.
 *
 * <p>A senha NUNCA está no repo: vem exclusivamente da env var {@code SEED_ADMIN_PASSWORD} (falha
 * rápido se ausente/vazia — cada ambiente tem a sua; D-04). O hash BCrypt (custo 12) é gerado
 * dentro da própria migration; a senha em claro não é logada nem impressa.
 *
 * <p>Sem DI Spring (anti-pattern 5): Flyway instancia esta classe — a env var é lida via {@link
 * System#getenv} direto.
 *
 * <p><strong>INF-01 — imutabilidade por convenção:</strong> migrations Java não têm checksum no
 * Flyway (não participam do change detection); por isso este arquivo não pode ser alterado depois
 * de aplicado em nenhum ambiente. Correção só via nova versão V<n>__. O {@code V2} fica livre para
 * o seed de categorias da Fase 2.
 */
public class V1_1__seed_empresa_admin extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    String senha = senhaObrigatoria(System.getenv("SEED_ADMIN_PASSWORD"));
    // custo 12 (R6/AUTH-02); a senha em claro nunca é logada/impressa
    String hash = new BCryptPasswordEncoder(12).encode(senha);

    Connection connection = context.getConnection();

    long empresaId;
    try (PreparedStatement ps =
        connection.prepareStatement(
            "INSERT INTO empresa (nome) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, "Controle Financeiro");
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        empresaId = rs.getLong(1);
      }
    }

    try (PreparedStatement ps =
        connection.prepareStatement(
            "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo)"
                + " VALUES (?, ?, ?, ?, ?, true)")) {
      ps.setLong(1, empresaId);
      ps.setString(2, "admin@controle.com");
      ps.setString(3, hash);
      ps.setString(4, "Administrador");
      ps.setString(5, "ADMIN");
      ps.executeUpdate();
    }
    // NÃO fechar a connection do Context (regra Flyway)
  }

  /**
   * Fail-fast (Pitfall 4): sem a env var o app não sobe — mensagem explícita indicando o que falta.
   * Exercitável nos testes removendo a variável do surefire; a lógica fica isolada aqui para
   * cobertura automatizada.
   */
  public static String senhaObrigatoria(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException(
          "SEED_ADMIN_PASSWORD não definida ou vazia — necessária para a migration de seed"
              + " V1_1__seed_empresa_admin (defina a env var por ambiente)");
    }
    return valor;
  }
}
