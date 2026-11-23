package com.controlefinanceiro.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URI;
import org.junit.jupiter.api.Test;

class GoogleConnectionFactoryTest {

  @Test
  void openConnectionAplicaTimeoutsConfigurados() throws Exception {
    GoogleConnectionFactory factory = new GoogleConnectionFactory(1234, 5678);

    HttpURLConnection connection =
        factory.openConnection(URI.create("https://accounts.google.com").toURL());

    assertThat(connection.getConnectTimeout()).isEqualTo(1234);
    assertThat(connection.getReadTimeout()).isEqualTo(5678);
  }
}
