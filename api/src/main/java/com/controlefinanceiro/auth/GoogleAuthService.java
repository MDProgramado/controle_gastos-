package com.controlefinanceiro.auth;

import com.controlefinanceiro.common.CadastroInvalidoException;
import com.controlefinanceiro.common.ConviteInvalidoException;
import com.controlefinanceiro.common.CredenciaisInvalidasException;
import com.controlefinanceiro.usuario.ConviteService;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google Auth (D-05/D-06/D-07/D-12): a empresa NUNCA vem do request — ela é derivada do e-mail
 * global verificado pelo ID token ou do convite válido. Nenhum token do Google é devolvido; a
 * sessão é sempre o JWT próprio emitido por {@link JwtService}.
 */
@Service
public class GoogleAuthService {

  private static final String NOME_EMPRESA_PADRAO = "Minha Empresa";

  private final UsuarioRepository usuarioRepository;
  private final TenantOnboardingService tenantOnboardingService;
  private final ConviteService conviteService;
  private final JwtService jwtService;
  private final GoogleTokenValidator tokenValidator;

  public GoogleAuthService(
      UsuarioRepository usuarioRepository,
      TenantOnboardingService tenantOnboardingService,
      ConviteService conviteService,
      JwtService jwtService,
      GoogleTokenValidator tokenValidator) {
    this.usuarioRepository = usuarioRepository;
    this.tenantOnboardingService = tenantOnboardingService;
    this.conviteService = conviteService;
    this.jwtService = jwtService;
    this.tokenValidator = tokenValidator;
  }

  @Transactional
  public TokenResponse autenticarComToken(GoogleRequest request) {
    GoogleTokenValidator.DadosGoogle dados = tokenValidator.validar(request.idToken());
    return autenticar(dados, request.intencao(), request.conviteToken());
  }

  @Transactional
  public TokenResponse autenticar(
      GoogleTokenValidator.DadosGoogle dados, String intencao, String conviteToken) {
    IntencaoGoogle intencaoGoogle = IntencaoGoogle.from(intencao);
    String email = dados.email().trim().toLowerCase(Locale.ROOT);

    if (intencaoGoogle == IntencaoGoogle.CONVITE) {
      if (conviteToken == null || conviteToken.isBlank()) {
        throw new ConviteInvalidoException();
      }
      googleSubLivre(dados.googleSub(), null);
      return conviteService.aceitarComGoogle(conviteToken, email, dados.nome(), dados.googleSub());
    }

    Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);
    if (intencaoGoogle == IntencaoGoogle.CADASTRO && usuarioExistente.isEmpty()) {
      googleSubLivre(dados.googleSub(), null);
      return tenantOnboardingService.onboardingComGoogle(
          NOME_EMPRESA_PADRAO, dados.nome(), email, dados.googleSub());
    }

    Usuario usuario = usuarioExistente.orElseThrow(CredenciaisInvalidasException::new);
    if (!usuario.isAtivo()) {
      throw new CredenciaisInvalidasException();
    }
    googleSubLivre(dados.googleSub(), usuario);

    if (usuario.getGoogleSub() == null) {
      usuario.setGoogleSub(dados.googleSub());
      usuario.setEmailVerificado(true);
      try {
        usuarioRepository.saveAndFlush(usuario);
      } catch (DataIntegrityViolationException ex) {
        throw new CredenciaisInvalidasException();
      }
    } else if (!dados.googleSub().equals(usuario.getGoogleSub())) {
      throw new CredenciaisInvalidasException();
    }
    return new TokenResponse(jwtService.gerarToken(usuario));
  }

  /** Um google_sub já vinculado a outra conta não pode assumir identidade alheia. */
  private void googleSubLivre(String googleSub, Usuario usuarioAtual) {
    Optional<Usuario> dono = usuarioRepository.findByGoogleSub(googleSub);
    if (dono.isPresent()
        && (usuarioAtual == null || !Objects.equals(dono.get().getId(), usuarioAtual.getId()))) {
      throw new CredenciaisInvalidasException();
    }
  }

  private enum IntencaoGoogle {
    CADASTRO,
    LOGIN,
    CONVITE;

    static IntencaoGoogle from(String valor) {
      if (valor == null) {
        throw new CadastroInvalidoException();
      }
      try {
        return valueOf(valor.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        throw new CadastroInvalidoException();
      }
    }
  }
}
