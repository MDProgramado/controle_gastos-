package com.controlefinanceiro.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryExpenseSerializacaoTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("total por categoria é serializado como string para preservar BigDecimal")
  void totalComoString() throws Exception {
    String json =
        mapper.writeValueAsString(new CategoryExpense(1L, "Mercado", new BigDecimal("1234.56")));

    assertThat(json).contains("\"total\":\"1234.56\"");
  }
}
