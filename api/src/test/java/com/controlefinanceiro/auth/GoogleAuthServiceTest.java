package com.controlefinanceiro.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.common.CadastroInvalidoException;
import com.controlefinanceiro.common.ConviteInvalidoException;
import com.controlefinanceiro.common.CredenciaisInvalidasException;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.usuario.ConviteService;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class GoogleAuthServiceTest {

  private static final String SUB = "google-sub-123";
  private static final String EMAIL = "usuario@teste.com";
  private static final String NOME = "Nome do Usuario";
  private static final String ID_TOKEN = "id-token-fixture";

  private UsuarioRepository usuarioRepository;
  private TenantOnboardingService onboarding;
  private ConviteService conviteService;
  private JwtService jwtService;
  private GoogleTokenValidator tokenValidator;
  private GoogleAuthService service;

  @BeforeEach
  void setup() {
    usuarioRepository = mock(UsuarioRepository.class);
    onboarding = mock(TenantOnboardingService.class);
    conviteService = mock(ConviteService.class);
    jwtService = mock(JwtService.class);
    tokenValidator = mock(GoogleTokenValidator.class);
    service =
        new GoogleAuthService(
            usuarioRepository, onboarding, conviteService, jwtService, tokenValidator);
  }

  @Test
  void autenticarComTokenValidaIdTokenEDelegar() {
    when(tokenValidator.validar(ID_TOKEN)).thenReturn(dados());
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
    when(onboarding.onboardingComGoogle(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new TokenResponse("jwt-facade"));

    TokenResponse resposta =
        service.autenticarComToken(new GoogleRequest(ID_TOKEN, "CADASTRO", null));

    assertThat(resposta.token()).isEqualTo("jwt-facade");
    verify(tokenValidator).validar(ID_TOKEN);
  }

  @Test
  void cadastroComEmailNovoCriaTenantComGoogleSub() {
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
    when(onboarding.onboardingComGoogle(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(new TokenResponse("jwt-cadastro"));

    TokenResponse resposta = service.autenticar(dados(), "CADASTRO", null);

    assertThat(resposta.token()).isEqualTo("jwt-cadastro");
    verify(onboarding).onboardingComGoogle("Minha Empresa", NOME, EMAIL, SUB);
    verify(jwtService, never()).gerarToken(any());
  }

  @Test
  void cadastroComEmailExistenteEntraNaEmpresaExistenteSemRevelarEnumeration() {
    Usuario usuario = usuario(55L, 77L, SUB, true);
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.of(usuario));
    when(jwtService.gerarToken(usuario)).thenReturn("jwt-empresa-existente");

    TokenResponse resposta = service.autenticar(dados(), "CADASTRO", null);

    assertThat(resposta.token()).isEqualTo("jwt-empresa-existente");
    verify(usuarioRepository, times(1)).findByEmail(EMAIL);
    verify(onboarding, never())
        .onboardingComGoogle(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void loginComEmailExistenteVinculaGoogleSubSemCriarEmpresa() {
    Usuario usuario = usuario(55L, 77L, null, false);
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
    when(jwtService.gerarToken(usuario)).thenReturn("jwt-login");

    TokenResponse resposta = service.autenticar(dados(), "LOGIN", null);

    assertThat(resposta.token()).isEqualTo("jwt-login");
    ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getGoogleSub()).isEqualTo(SUB);
    assertThat(captor.getValue().isEmailVerificado()).isTrue();
    assertThat(captor.getValue().getEmpresa().getId()).isEqualTo(77L);
    verify(onboarding, never())
        .onboardingComGoogle(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void loginComGoogleSubDeOutraContaERejeitado() {
    Usuario usuario = usuario(55L, 77L, null, false);
    Usuario outro = usuario(99L, 88L, SUB, true);
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.of(outro));

    assertThatThrownBy(() -> service.autenticar(dados(), "LOGIN", null))
        .isInstanceOf(CredenciaisInvalidasException.class);
    verify(usuarioRepository, never()).saveAndFlush(any());
    verify(jwtService, never()).gerarToken(any());
  }

  @Test
  void vinculacaoGoogleSubComConcorrenciaDuplicadaViraCredenciaisInvalidas() {
    Usuario usuario = usuario(55L, 77L, null, false);
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
    when(usuarioRepository.saveAndFlush(usuario))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> service.autenticar(dados(), "LOGIN", null))
        .isInstanceOf(CredenciaisInvalidasException.class);
    verify(usuarioRepository).saveAndFlush(usuario);
    verify(jwtService, never()).gerarToken(any());
  }

  @Test
  void loginDeEmailExistenteDeOutraEmpresaUsaEmpresaExistente() {
    Usuario usuario = usuario(55L, 77L, null, false);
    when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
    when(jwtService.gerarToken(usuario)).thenReturn("jwt-empresa-77");

    TokenResponse resposta = service.autenticar(dados(), "LOGIN", null);

    assertThat(resposta.token()).isEqualTo("jwt-empresa-77");
    verify(jwtService).gerarToken(usuario);
    verify(onboarding, never())
        .onboardingComGoogle(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void conviteValidoUsaEmailVerificadoDoTokenGoogle() {
    when(usuarioRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
    when(conviteService.aceitarComGoogle("convite-token", EMAIL, NOME, SUB))
        .thenReturn(new TokenResponse("jwt-convite"));

    TokenResponse resposta = service.autenticar(dados(), "CONVITE", "convite-token");

    assertThat(resposta.token()).isEqualTo("jwt-convite");
    verify(conviteService).aceitarComGoogle("convite-token", EMAIL, NOME, SUB);
  }

  @Test
  void conviteSemTokenERejeitado() {
    assertThatThrownBy(() -> service.autenticar(dados(), "CONVITE", " "))
        .isInstanceOf(ConviteInvalidoException.class);
    verify(conviteService, never())
        .aceitarComGoogle(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void intencaoDesconhecidaERejeitada() {
    assertThatThrownBy(() -> service.autenticar(dados(), "ROOT", null))
        .isInstanceOf(CadastroInvalidoException.class);
    verify(usuarioRepository, never()).findByEmail(anyString());
    verify(onboarding, never())
        .onboardingComGoogle(anyString(), anyString(), anyString(), anyString());
  }

  private GoogleTokenValidator.DadosGoogle dados() {
    return new GoogleTokenValidator.DadosGoogle(SUB, EMAIL, NOME);
  }

  private Usuario usuario(Long id, Long empresaId, String googleSub, boolean emailVerificado) {
    Empresa empresa = new Empresa();
    empresa.setId(empresaId);
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setEmpresa(empresa);
    usuario.setEmail(EMAIL);
    usuario.setNome(NOME);
    usuario.setRole("ADMIN");
    usuario.setAtivo(true);
    usuario.setGoogleSub(googleSub);
    usuario.setEmailVerificado(emailVerificado);
    return usuario;
  }
}
