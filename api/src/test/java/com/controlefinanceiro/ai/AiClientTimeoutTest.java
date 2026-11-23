package com.controlefinanceiro.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

class AiClientTimeoutTest {

  @Test
  @DisplayName("OpenRouter usa timeouts configuráveis com padrões 3s/30s")
  void timeoutsConfiguraveisComPadroes() throws Exception {
    Constructor<AiClient> construtor =
        AiClient.class.getDeclaredConstructor(
            String.class, String.class, String.class, String.class, int.class, int.class);
    Annotation[][] anotacoes = construtor.getParameterAnnotations();
    assertThat(valor(anotacoes[4])).isEqualTo("${ai.openrouter.connect-timeout-ms:3000}");
    assertThat(valor(anotacoes[5])).isEqualTo("${ai.openrouter.read-timeout-ms:30000}");

    AiClient client = construtor.newInstance("chave", "modelo", "", "", 3000, 15000);
    Object fabrica = campo(client, "restClient", "clientRequestFactory");

    assertThat(fabrica).isInstanceOf(SimpleClientHttpRequestFactory.class);
    assertThat(ReflectionTestUtils.getField(fabrica, "connectTimeout")).isEqualTo(3000);
    assertThat(ReflectionTestUtils.getField(fabrica, "readTimeout")).isEqualTo(15000);
  }

  private static String valor(Annotation[] anotacoes) {
    for (Annotation anotacao : anotacoes) {
      if (anotacao instanceof org.springframework.beans.factory.annotation.Value value) {
        return value.value();
      }
    }
    return null;
  }

  private static Object campo(Object alvo, String... nomes) throws Exception {
    Object atual = alvo;
    for (String nome : nomes) {
      Class<?> tipo = atual.getClass();
      Field campo = null;
      while (campo == null && tipo != null) {
        try {
          campo = tipo.getDeclaredField(nome);
        } catch (NoSuchFieldException e) {
          tipo = tipo.getSuperclass();
        }
      }
      assertThat(campo).as("campo %s em %s", nome, alvo.getClass()).isNotNull();
      campo.setAccessible(true);
      atual = campo.get(atual);
    }
    return atual;
  }
}
