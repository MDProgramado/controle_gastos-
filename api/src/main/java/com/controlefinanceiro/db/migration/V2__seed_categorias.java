package com.controlefinanceiro.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Seed V2 (CAT-02, CAT-03, D-01 a D-04): 16 categorias (12 SAÍDA + 4 ENTRADA) com flag pessoal para
 * as marcadas [P] da lista do cliente.
 *
 * <p>Segue o padrão de {@code V1_1__seed_empresa_admin}: migration Java via {@link
 * BaseJavaMigration}, JDBC cru via {@link Context#getConnection()}, {@code empresa_id} obtido por
 * subquery na tabela empresa (nunca hardcoded), idempotência via {@code SELECT} antes de {@code
 * INSERT} (D-03).
 *
 * <p>Sem DI Spring (anti-pattern 5): Flyway instancia esta classe.
 *
 * <p>INF-01 — imutabilidade por convenção: este arquivo não pode ser alterado depois de aplicado.
 * Correção só via nova versão V{n}__.
 */
public class V2__seed_categorias extends BaseJavaMigration {

  /** Ordem da lista do cliente (D-04): nome, tipo, pessoal. */
  private static final Object[][] CATEGORIAS = {
    // SAÍDA — pessoais [P]
    new Object[] {"Alimentação", "SAIDA", true},
    new Object[] {"Saúde/farmácia", "SAIDA", true},
    new Object[] {"Lazer", "SAIDA", true},
    // SAÍDA — não pessoais
    new Object[] {"Transporte", "SAIDA", false},
    new Object[] {"Moradia", "SAIDA", false},
    new Object[] {"Educação", "SAIDA", false},
    new Object[] {"Vestuário", "SAIDA", false},
    new Object[] {"Tecnologia", "SAIDA", false},
    new Object[] {"Serviços", "SAIDA", false},
    new Object[] {"Assinaturas", "SAIDA", false},
    new Object[] {"Impostos", "SAIDA", false},
    new Object[] {"Outros", "SAIDA", false},
    // ENTRADA — não pessoais
    new Object[] {"Vendas de produtos", "ENTRADA", false},
    new Object[] {"Prestação de serviços", "ENTRADA", false},
    // ENTRADA — pessoal [P]
    new Object[] {"Rendimentos e reembolsos", "ENTRADA", true},
    new Object[] {"Outras receitas", "ENTRADA", false},
  };

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();

    Long empresaId;
    try (PreparedStatement ps =
        connection.prepareStatement("SELECT id FROM empresa WHERE nome = 'Controle Financeiro'")) {
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new IllegalStateException(
              "Empresa 'Controle Financeiro' não encontrada — a migration V1_1 deve ser aplicada"
                  + " antes da V2");
        }
        empresaId = rs.getLong(1);
      }
    }

    for (Object[] categoria : CATEGORIAS) {
      String nome = (String) categoria[0];
      String tipo = (String) categoria[1];
      boolean pessoal = (Boolean) categoria[2];

      // D-03: idempotência — pular se já existir
      try (PreparedStatement ps =
          connection.prepareStatement(
              "SELECT 1 FROM categoria WHERE empresa_id = ? AND nome = ? AND tipo = ?")) {
        ps.setLong(1, empresaId);
        ps.setString(2, nome);
        ps.setString(3, tipo);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            continue; // já existe, pular
          }
        }
      }

      try (PreparedStatement ps =
          connection.prepareStatement(
              "INSERT INTO categoria (empresa_id, nome, tipo, pessoal, ativo)"
                  + " VALUES (?, ?, ?, ?, true)")) {
        ps.setLong(1, empresaId);
        ps.setString(2, nome);
        ps.setString(3, tipo);
        ps.setBoolean(4, pessoal);
        ps.executeUpdate();
      }
    }
    // NÃO fechar a connection do Context (regra Flyway)
  }
}
