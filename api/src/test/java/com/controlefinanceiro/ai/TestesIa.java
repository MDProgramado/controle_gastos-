package com.controlefinanceiro.ai;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Map;
import org.mockito.stubbing.Answer;

/**
 * Apoio a testes: o AiService chama {@link AiClient#completarComUso}, mas a maioria dos testes faz
 * stub de {@link AiClient#completar(List, long)} (só o texto). Este mock delega {@code
 * completarComUso} para o {@code completar} stubado (sem uso), mantendo {@code verify(completar)}.
 */
final class TestesIa {

  private TestesIa() {}

  static AiClient mockAiClient() {
    return mock(
        AiClient.class,
        withSettings()
            .defaultAnswer(
                invocacao -> {
                  if (invocacao.getMethod().getName().equals("completarComUso")) {
                    return delegarParaCompletar().answer(invocacao);
                  }
                  return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocacao);
                }));
  }

  @SuppressWarnings("unchecked")
  static Answer<AiClient.RespostaChat> delegarParaCompletar() {
    return invocacao -> {
      AiClient mock = (AiClient) invocacao.getMock();
      String conteudo =
          mock.completar(
              (List<Map<String, Object>>) invocacao.getArgument(0),
              (long) (Long) invocacao.getArgument(1));
      return new AiClient.RespostaChat(conteudo, List.of());
    };
  }
}
