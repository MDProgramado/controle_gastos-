package com.controlefinanceiro.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.ConnectionFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.webtoken.JsonWebSignature;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

class GoogleTokenValidatorTest {

  private static final String ID_TOKEN = "id-token-fixture";
  private static final String SUB = "google-sub-123";
  private static final String EMAIL = "usuario@teste.com";
  private static final String NOME = "Nome do Usuario";

  private GoogleIdTokenVerifier verifier;
  private GoogleTokenValidator validator;

  @BeforeEach
  void setup() {
    verifier = mock(GoogleIdTokenVerifier.class);
    validator = new GoogleTokenValidator(verifier);
  }

  @Test
  void tokenValidoExtraiSubEmailENome() throws Exception {
    when(verifier.verify(ID_TOKEN)).thenReturn(token(SUB, EMAIL, true, NOME));

    GoogleTokenValidator.DadosGoogle dados = validator.validar(ID_TOKEN);

    assertThat(dados.googleSub()).isEqualTo(SUB);
    assertThat(dados.email()).isEqualTo(EMAIL);
    assertThat(dados.nome()).isEqualTo(NOME);
    assertThat(GoogleTokenValidator.DadosGoogle.class.getRecordComponents())
        .extracting(componente -> componente.getName())
        .containsExactly("googleSub", "email", "nome");
  }

  @Test
  void tokenMalformadoQueLancaIllegalArgumentExceptionERejeitado() throws Exception {
    when(verifier.verify(ID_TOKEN)).thenThrow(new IllegalArgumentException("token malformado"));

    assertInvalido();
  }

  @Test
  void falhaDeIoNaBuscaDasChavesPublicasEReportadaComoIndisponivel() throws Exception {
    when(verifier.verify(ID_TOKEN)).thenThrow(new IOException("jwks indisponível"));

    assertThatThrownBy(() -> validator.validar(ID_TOKEN))
        .isInstanceOf(GoogleVerificacaoIndisponivelException.class)
        .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(ID_TOKEN, EMAIL));
  }

  @Test
  void tokenExpiradoDetectadoPelaBibliotecaERejeitado() throws Exception {
    when(verifier.verify(ID_TOKEN)).thenThrow(new GeneralSecurityException("token expirado"));

    assertInvalido();
  }

  @Test
  void assinaturaInvalidaDetectadaPelaBibliotecaERejeitada() throws Exception {
    when(verifier.verify(ID_TOKEN)).thenThrow(new GeneralSecurityException("assinatura inválida"));

    assertInvalido();
  }

  @Test
  void verificadorQueNaoRetornaTokenERejeitado() throws Exception {
    when(verifier.verify(ID_TOKEN)).thenReturn(null);

    assertInvalido();
  }

  @Test
  void tokenEmBrancoERejeitadoSemChamarVerificador() {
    assertThatThrownBy(() -> validator.validar(" "))
        .isInstanceOf(GoogleTokenInvalidoException.class);
    verifyNoInteractions(verifier);
  }

  @Test
  void emailNaoVerificadoERejeitadoMesmoComTokenVerificado() throws Exception {
    when(verifier.verify(ID_TOKEN)).thenReturn(token(SUB, EMAIL, false, NOME));

    assertInvalido();
  }

  @Test
  void transporteDoValidadorAplicaTimeoutsConfigurados() throws Exception {
    GoogleTokenValidator configurado = new GoogleTokenValidator("fixture-client-id", 1234, 5678);

    HttpURLConnection connection =
        connectionFactory(configurado)
            .openConnection(URI.create("https://accounts.google.com").toURL());

    assertThat(connection.getConnectTimeout()).isEqualTo(1234);
    assertThat(connection.getReadTimeout()).isEqualTo(5678);
  }

  @Test
  void timeoutsTemValorPadraoDe3000MsNasPropriedades() throws Exception {
    Constructor<GoogleTokenValidator> construtor =
        GoogleTokenValidator.class.getConstructor(String.class, int.class, int.class);
    Annotation[][] anotacoes = construtor.getParameterAnnotations();

    assertThat(((Value) anotacoes[1][0]).value()).isEqualTo("${google.connect-timeout-ms:3000}");
    assertThat(((Value) anotacoes[2][0]).value()).isEqualTo("${google.read-timeout-ms:3000}");
  }

  private ConnectionFactory connectionFactory(GoogleTokenValidator alvo) throws Exception {
    Field campoVerificador = GoogleTokenValidator.class.getDeclaredField("verifier");
    campoVerificador.setAccessible(true);
    GoogleIdTokenVerifier verificador = (GoogleIdTokenVerifier) campoVerificador.get(alvo);
    Field campoFabricante = NetHttpTransport.class.getDeclaredField("connectionFactory");
    campoFabricante.setAccessible(true);
    return (ConnectionFactory) campoFabricante.get(verificador.getTransport());
  }

  private void assertInvalido() {
    assertThatThrownBy(() -> validator.validar(ID_TOKEN))
        .isInstanceOf(GoogleTokenInvalidoException.class)
        .hasMessage("Token Google inválido");
  }

  private GoogleIdToken token(String sub, String email, boolean emailVerificado, String nome) {
    GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
    payload.setSubject(sub);
    payload.setEmail(email);
    payload.setEmailVerified(emailVerificado);
    payload.set("name", nome);
    return new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
  }
}
