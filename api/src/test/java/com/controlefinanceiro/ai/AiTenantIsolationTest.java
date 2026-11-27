package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.UsuarioAuth;
import com.controlefinanceiro.common.UsuarioAuthAuthenticationToken;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** Regressões de isolamento entre empresas (F-001 e F-002 do SECURITY.md). */
class AiTenantIsolationTest {

  private AiClient aiClient;
  private AiService aiService;

  @BeforeEach
  void setup() {
    aiClient = mock(AiClient.class);
    AiContextoService contextoService = mock(AiContextoService.class);
    when(contextoService.montarContexto(any(), any())).thenReturn("");
    aiService =
        new AiService(aiClient, new AiMemoryStore(), new AiKnowledgeStore(), contextoService);
    when(aiClient.completar(any(), anyLong())).thenReturn("CONSULTA|Resposta");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("usuario@teste.com", 1L, 7L, 0, "USER"),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
  }

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("historico de outra empresa com mesmo sessionId nao entra no prompt")
  void historicoDeOutraEmpresaComMesmoSessionIdNaoEntraNoPrompt() {
    aiService.processar("sessao-comum", "empresa-1", "segredo confidencial da empresa um");
    aiService.processar("sessao-comum", "empresa-2", "mensagem da empresa dois");

    assertThat(promptDaChamada(1, 2)).doesNotContain("segredo confidencial da empresa um");
  }

  @Test
  @DisplayName("conhecimento de outra empresa nao entra no prompt")
  void conhecimentoDeOutraEmpresaNaoEntraNoPrompt() {
    aiService.definirConhecimento("empresa-1", "REGRA SECRETA DA EMPRESA UM");

    aiService.processar("sessao", "empresa-2", "oi");

    assertThat(promptDaChamada(0, 1)).doesNotContain("REGRA SECRETA DA EMPRESA UM");
  }

  private String promptDaChamada(int indice, int totalChamadas) {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
    verify(aiClient, times(totalChamadas)).completar(captor.capture());
    return captor.getAllValues().get(indice).stream()
        .map(mensagem -> String.valueOf(mensagem.get("content")))
        .collect(Collectors.joining("\n"));
  }
}
