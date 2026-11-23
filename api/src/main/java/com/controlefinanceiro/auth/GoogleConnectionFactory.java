package com.controlefinanceiro.auth;

import com.google.api.client.http.javanet.ConnectionFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

final class GoogleConnectionFactory implements ConnectionFactory {

  private final int connectTimeoutMs;
  private final int readTimeoutMs;

  GoogleConnectionFactory(int connectTimeoutMs, int readTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
    this.readTimeoutMs = readTimeoutMs;
  }

  @Override
  public HttpURLConnection openConnection(URL url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setConnectTimeout(connectTimeoutMs);
    connection.setReadTimeout(readTimeoutMs);
    return connection;
  }
}
