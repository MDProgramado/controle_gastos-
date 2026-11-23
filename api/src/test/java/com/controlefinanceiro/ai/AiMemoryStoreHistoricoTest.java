package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiMemoryStoreHistoricoTest {

  private AiMemoriaMensagemRepository mensagens;
  private AiMemoryStore store;

  @BeforeEach
  void setup() {
    AiMemoriaSessaoRepository sessoes = mock(AiMemoriaSessaoRepository.class);
    mensagens = mock(AiMemoriaMensagemRepository.class);
    AiMemoriaSessao sessao = mock(AiMemoriaSessao.class);
    when(sessao.getId()).thenReturn(5L);
    when(sessoes.findByEmpresaIdAndUsuarioIdAndSessionId(1L, 7L, "s"))
        .thenReturn(Optional.of(sessao));

    when(mensagens.findTop20BySessaoIdOrderByIdDesc(5L))
        .thenReturn(
            List.of(
                new AiMemoriaMensagem(sessao, "user", "mais recente"),
                new AiMemoriaMensagem(sessao, "assistant", "meio"),
                new AiMemoriaMensagem(sessao, "user", "mais antiga")));

    store = new AiMemoryStore(sessoes, mensagens);
  }

  @Test
  @DisplayName("histórico carrega no máximo 20 mensagens em ordem cronológica")
  void historicoLimitadoEEmOrdem() {
    List<AiMessage> historico = store.obterHistorico("s", "1", "7");

    assertThat(historico)
        .extracting(AiMessage::content)
        .containsExactly("mais antiga", "meio", "mais recente");
    verify(mensagens, never()).findBySessaoIdOrderByIdAsc(any());
  }

  @Test
  @DisplayName("carregar sessão existente usa o mesmo histórico limitado")
  void carregarSessaoUsaHistoricoLimitado() {
    AiSession sessao = store.obterOuCriar("s", "1", "7");

    assertThat(sessao.mensagens())
        .extracting(AiMessage::content)
        .containsExactly("mais antiga", "meio", "mais recente");
  }
}
