package com.controlefinanceiro.dashboard;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DashboardValidacaoTest {

  private final DashboardRepository repository = mock(DashboardRepository.class);
  private final DashboardService service = new DashboardService(repository);

  @ParameterizedTest
  @ValueSource(ints = {1899, 2101})
  @DisplayName("ano fora de 1900..2100 é rejeitado antes de consultar o banco")
  void anoForaDaFaixa(int ano) {
    assertThatThrownBy(() -> service.obterDashboard(1L, 7L, ano, 11))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ano");

    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 13})
  @DisplayName("mês fora de 1..12 é rejeitado antes de consultar o banco")
  void mesForaDaFaixa(int mes) {
    assertThatThrownBy(() -> service.obterDashboard(1L, 7L, 2026, mes))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mês");

    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("ano e mês obrigatórios")
  void anoEMesObrigatorios() {
    assertThatThrownBy(() -> service.obterDashboard(1L, 7L, null, 11))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ano");
    assertThatThrownBy(() -> service.obterDashboard(1L, 7L, 2026, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mês");

    verifyNoInteractions(repository);
  }
}
