package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import com.controlefinanceiro.dashboard.DashboardRepository;
import com.controlefinanceiro.dashboard.dto.CategoryExpense;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Item 1.4: minimização dos dados enviados ao LLM. */
class AiContextoServiceTest {

  private static final String EMAIL = "maria.cliente@exemplo.com.br";
  private static final String TELEFONE = "+5511988887777";

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  private DashboardRepository dashboard() {
    DashboardRepository dashboard = mock(DashboardRepository.class);
    when(dashboard.totalEntradas(anyLong(), anyLong(), any(), any()))
        .thenReturn(new BigDecimal("5000.00"));
    when(dashboard.totalSaidas(anyLong(), anyLong(), any(), any()))
        .thenReturn(new BigDecimal("1200.50"));
    when(dashboard.breakdownSaida(anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(new CategoryExpense(1L, "Mercado", new BigDecimal("700.00"))));
    when(dashboard.breakdownEntrada(anyLong(), anyLong(), any(), any()))
        .thenReturn(List.of(new CategoryExpense(2L, "Salário", new BigDecimal("5000.00"))));
    return dashboard;
  }

  @Test
  @DisplayName("contexto tem só totais do mês atual e anterior e categorias do mês atual")
  void contextoMinimo() {
    DashboardRepository dashboard = dashboard();

    String contexto = new AiContextoService(dashboard).montarContexto(1L, 7L);

    verify(dashboard, times(2)).totalEntradas(eq(1L), eq(7L), any(), any());
    verify(dashboard, times(2)).totalSaidas(eq(1L), eq(7L), any(), any());
    assertThat(contexto).contains("Mercado").contains("Salário").contains("R$ 5.000,00");
    assertThat(contexto.lines().filter(l -> l.contains("entradas R$")).count()).isEqualTo(2);
    assertThat(contexto).doesNotContainIgnoringCase("últimos").doesNotContain("lançamentos:");
  }

  @Test
  @DisplayName("prompt não contém descrições de lançamentos nem e-mail/telefone do usuário")
  void promptSemDadosPessoais() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth(EMAIL, 1L, 7L, 0, "USER"),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    AiClient aiClient = mock(AiClient.class);
    when(aiClient.completar(any(), anyLong())).thenReturn("CONSULTA|ok");
    AiMemoryStore memoria = new AiMemoryStore();
    AiService service =
        new AiService(
            aiClient, memoria, new AiKnowledgeStore(), new AiContextoService(dashboard()));

    service.processar("s1", "1", "7", "quanto gastei esse mês?");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
    verify(aiClient).completar(captor.capture(), anyLong());
    List<String> conteudos = new ArrayList<>();
    for (Map<String, Object> m : captor.getValue()) {
      conteudos.add(String.valueOf(m.get("content")));
    }
    String tudo = String.join("\n", conteudos);
    assertThat(tudo).contains("Mercado");
    assertThat(tudo).doesNotContain(EMAIL).doesNotContain(TELEFONE).doesNotContain("@");
    assertThat(tudo).doesNotContain("Últimos");
  }
}
