package com.controlefinanceiro.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validação do ID token Google pela biblioteca oficial (D-01) — nenhum parse manual de JWT.
 *
 * <p>Audience é o {@code GOOGLE_CLIENT_ID} (valor público) e issuer é fixo em {@code
 * https://accounts.google.com}. A verificação cobre assinatura, expiração, audience e issuer antes
 * de qualquer leitura de claims; {@code email_verified=false} é recusado explicitamente.
 */
@Component
public class GoogleTokenValidator {

  private static final String ISSUER_GOOGLE = "https://accounts.google.com";
  private static final Logger log = LoggerFactory.getLogger(GoogleTokenValidator.class);

  private final GoogleIdTokenVerifier verifier;

  public GoogleTokenValidator(
      @Value("${google.client-id:}") String clientId,
      @Value("${google.connect-timeout-ms:3000}") int connectTimeoutMs,
      @Value("${google.read-timeout-ms:3000}") int readTimeoutMs) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalStateException(
          "google.client-id (env var GOOGLE_CLIENT_ID) ausente — configure o OAuth Client Web");
    }
    NetHttpTransport transport =
        new NetHttpTransport.Builder()
            .setConnectionFactory(new GoogleConnectionFactory(connectTimeoutMs, readTimeoutMs))
            .build();
    this.verifier =
        new GoogleIdTokenVerifier.Builder(transport, GsonFactory.getDefaultInstance())
            .setAudience(List.of(clientId.trim()))
            .setIssuer(ISSUER_GOOGLE)
            .build();
  }

  GoogleTokenValidator(GoogleIdTokenVerifier verifier) {
    this.verifier = verifier;
  }

  /** Dados verificados do ID token — nunca devolvemos tokens do Google ao cliente. */
  public record DadosGoogle(String googleSub, String email, String nome) {}

  public DadosGoogle validar(String idToken) {
    if (idToken == null || idToken.isBlank()) {
      throw new GoogleTokenInvalidoException();
    }

    GoogleIdToken token;
    try {
      token = verifier.verify(idToken);
    } catch (GeneralSecurityException | IllegalArgumentException ex) {
      throw new GoogleTokenInvalidoException();
    } catch (IOException ex) {
      log.warn("Falha de I/O ao consultar as chaves públicas do Google");
      throw new GoogleVerificacaoIndisponivelException();
    }
    if (token == null || token.getPayload() == null) {
      throw new GoogleTokenInvalidoException();
    }

    GoogleIdToken.Payload payload = token.getPayload();
    if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
      throw new GoogleTokenInvalidoException();
    }

    String googleSub = payload.getSubject();
    String email = payload.getEmail();
    if (googleSub == null || googleSub.isBlank() || email == null || email.isBlank()) {
      throw new GoogleTokenInvalidoException();
    }

    Object nome = payload.get("name");
    return new DadosGoogle(
        googleSub.trim(),
        email.trim().toLowerCase(Locale.ROOT),
        nome instanceof String texto ? texto.trim() : null);
  }
}
