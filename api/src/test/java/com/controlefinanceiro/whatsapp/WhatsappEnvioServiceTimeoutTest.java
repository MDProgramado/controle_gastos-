package com.controlefinanceiro.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class WhatsappEnvioServiceTimeoutTest {

  private final WhatsappMensagemProcessadaRepository repositorio =
      mock(WhatsappMensagemProcessadaRepository.class);

  @Test
  void configuraTimeoutsInformadosNoRestClient() {
    RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_SELF);

    new WhatsappEnvioService(
        repositorio,
        builder,
        "https://graph.facebook.com/v21.0",
        "123456789",
        "token-fixture",
        "template-fixture",
        2500,
        9000);

    SimpleClientHttpRequestFactory fabrica = capturarFabrica(builder);
    assertThat(ReflectionTestUtils.getField(fabrica, "connectTimeout")).isEqualTo(2500);
    assertThat(ReflectionTestUtils.getField(fabrica, "readTimeout")).isEqualTo(9000);
  }

  @Test
  void construtorSemTimeoutsUsaPadroesSeguros() {
    RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_SELF);

    new WhatsappEnvioService(
        repositorio,
        builder,
        "https://graph.facebook.com/v21.0",
        "123456789",
        "token-fixture",
        "template-fixture");

    SimpleClientHttpRequestFactory fabrica = capturarFabrica(builder);
    assertThat(ReflectionTestUtils.getField(fabrica, "connectTimeout")).isEqualTo(3000);
    assertThat(ReflectionTestUtils.getField(fabrica, "readTimeout")).isEqualTo(15000);
  }

  private SimpleClientHttpRequestFactory capturarFabrica(RestClient.Builder builder) {
    ArgumentCaptor<ClientHttpRequestFactory> captor =
        ArgumentCaptor.forClass(ClientHttpRequestFactory.class);
    verify(builder).requestFactory(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(SimpleClientHttpRequestFactory.class);
    return (SimpleClientHttpRequestFactory) captor.getValue();
  }
}
