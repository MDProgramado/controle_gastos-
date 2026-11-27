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

/** Mesmo sessionId nunca pode cruzar histórico entre usuários ou empresas (D-03/D-16). */
class AiIsolamentoTest {

  private AiClient aiClient;
  private AiService aiService;

  @BeforeEach
  void setup() {
    aiClient = mock(AiClient.class);
    AiContextoService contextoService = mock(AiContextoService.class);
    when(contextoService.montarContexto(any())).thenReturn("");
    aiService =
        new AiService(aiClient, new AiMemoryStore(), new AiKnowledgeStore(), contextoService);
    when(aiClient.completar(any(), anyLong())).thenReturn("CONSULTA|Resposta");
  }

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("mesmo sessionId nao compartilha historico entre usuarios da mesma empresa")
  void mesmoSessionIdNaoCompartilhaHistoricoEntreUsuarios() {
    autenticar(1L, 7L);
    aiService.processar("sessao-comum", "1", "segredo confidencial do usuario sete");

    autenticar(1L, 8L);
    aiService.processar("sessao-comum", "1", "mensagem do usuario oito");

    assertThat(promptDaChamada(1, 2)).doesNotContain("segredo confidencial do usuario sete");
  }

  @Test
  @DisplayName("mesmo sessionId nao compartilha historico entre empresas diferentes")
  void mesmoSessionIdNaoCompartilhaHistoricoEntreEmpresas() {
    autenticar(1L, 7L);
    aiService.processar("sessao-comum", "1", "segredo confidencial da empresa um");

    autenticar(2L, 7L);
    aiService.processar("sessao-comum", "2", "mensagem da empresa dois");

    assertThat(promptDaChamada(1, 2)).doesNotContain("segredo confidencial da empresa um");
  }

  private void autenticar(Long empresaId, Long usuarioId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsuarioAuthAuthenticationToken(
                new UsuarioAuth("usuario@teste.com", empresaId, usuarioId, 0, "USER"),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
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
