package com.controlefinanceiro.auth;

import com.controlefinanceiro.usuario.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Emissão (e decode) do JWT HS256 via Nimbus — RESEARCH Pattern 2 (sem lib extra, sem filtro manual
 * — anti-pattern 2 bloqueante).
 *
 * <p>Segredo: propriedade {@code jwt.secret} mapeada para a env var {@code JWT_SECRET} (≥ 32 bytes,
 * Pitfall 6 — gere com {@code openssl rand -base64 48}); ausente/fraco → fail-fast com mensagem
 * explícita. Em teste, a env var é um fixture óbvio no surefire (D-04 — nunca valor real).
 *
 * <p>Claims: {@code sub}=e-mail, {@code empresa_id} (R3), {@code role} (AUTH-05), {@code
 * iat}=agora, {@code exp}=+8h (D-01 — sem refresh: expirou, refaz login).
 */
@Service
public class JwtService {

  /** D-01: validade de 8 horas. */
  static final Duration VALIDADE = Duration.ofHours(8);

  private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

  private final SecretKey chave;
  private final JwtEncoder encoder;

  public JwtService(@Value("${jwt.secret:}") String segredo) {
    byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < TAMANHO_MINIMO_SEGREDO_BYTES) {
      throw new IllegalStateException(
          "jwt.secret (env var JWT_SECRET) ausente ou com menos de 32 bytes —"
              + " gere um segredo com: openssl rand -base64 48");
    }
    this.chave = new SecretKeySpec(bytes, "HmacSHA256");
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
  }

  /** Decoder HS256 do resource-server — usa o MESMO segredo da emissão (assinatura + exp/clock). */
  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /** Emite o token do usuário autenticado (AUTH-01/05). */
  public String gerarToken(Usuario usuario) {
    Instant agora = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(usuario.getEmail())
            .claim("empresa_id", usuario.getEmpresa().getId())
            .claim("role", usuario.getRole())
            .issuedAt(agora)
            .expiresAt(agora.plus(VALIDADE.toMillis(), ChronoUnit.MILLIS))
            .build();
    // O header JWS é OBRIGATÓRIO: sem o algoritmo o NimbusJwtEncoder não consegue selecionar a
    // chave de assinatura ("Failed to select a JWK signing key") — HS256 fixo (T-2-02).
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
