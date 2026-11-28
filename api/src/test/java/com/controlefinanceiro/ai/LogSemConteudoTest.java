package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.controlefinanceiro.common.ServicoIaIndisponivelException;
import com.controlefinanceiro.importacao.IaExtracaoService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Item 3.4: nenhum log contém o conteúdo das mensagens do usuário nem das respostas da IA. */
class LogSemConteudoTest {

  private static final String SEGREDO_USUARIO = "SEGREDO-DO-USUARIO-123";
  private static final String SEGREDO_IA = "SEGREDO-DA-RESPOSTA-456";

  private ListAppender<ILoggingEvent> appender;
  private Logger raiz;

  @BeforeEach
  void capturar() {
    raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    appender = new ListAppender<>();
    appender.start();
    raiz.addAppender(appender);
  }

  @AfterEach
  void soltar() {
    raiz.detachAppender(appender);
  }

  private String tudoLogado() {
    StringBuilder sb = new StringBuilder();
    for (ILoggingEvent e : appender.list) {
      sb.append(e.getFormattedMessage()).append('\n');
      if (e.getThrowableProxy() != null) {
        sb.append(ThrowableProxyUtil.asString(e.getThrowableProxy())).append('\n');
      }
    }
    return sb.toString();
  }

  @Test
  @DisplayName("erro inesperado no chat não loga o texto do usuário (mesmo ecoado na exceção)")
  void erroInesperadoNaoLogaMensagem() {
    AiClient aiClient = TestesIa.mockAiClient();
    when(aiClient.completar(any(), org.mockito.ArgumentMatchers.anyLong()))
        .thenThrow(new IllegalStateException("eco do prompt: " + SEGREDO_USUARIO));
    AiContextoService contexto = mock(AiContextoService.class);
    when(contexto.montarContexto(any(), any())).thenReturn("");
    AiService service =
        new AiService(aiClient, new AiMemoryStore(), new AiKnowledgeStore(), contexto);

    service.processar("s", "1", "7", "gastei 10 " + SEGREDO_USUARIO);

    assertThat(appender.list).isNotEmpty();
    assertThat(tudoLogado()).doesNotContain(SEGREDO_USUARIO);
  }

  @Test
  @DisplayName("IA indisponível não loga a mensagem de erro do provedor sem redação")
  void indisponivelNaoLogaConteudo() {
    AiClient aiClient = TestesIa.mockAiClient();
    when(aiClient.completar(any(), org.mockito.ArgumentMatchers.anyLong()))
        .thenThrow(
            new ServicoIaIndisponivelException(
                "O serviço de IA retornou erro: contato " + "fulano@exemplo.com"));
    AiContextoService contexto = mock(AiContextoService.class);
    when(contexto.montarContexto(any(), any())).thenReturn("");
    AiService service =
        new AiService(aiClient, new AiMemoryStore(), new AiKnowledgeStore(), contexto);

    service.processar("s", "1", "7", "oi");

    assertThat(tudoLogado()).doesNotContain("fulano@exemplo.com");
  }

  @Test
  @DisplayName("extração de PDF com JSON inválido não loga a resposta da IA")
  void extracaoNaoLogaResposta() {
    AiClient aiClient = mock(AiClient.class);
    when(aiClient.completarComCascata(anyList(), any(AiClient.OpcoesCompletar.class)))
        .thenReturn(new AiClient.RespostaCompletar("isto não é json " + SEGREDO_IA, "m", false, 1));
    IaExtracaoService service = new IaExtracaoService(aiClient, "m", "m", 0.0, 100, 1, 1L);

    service.extrair("texto do extrato", List.of("Outros"), "1");

    assertThat(tudoLogado()).doesNotContain(SEGREDO_IA);
  }
}
