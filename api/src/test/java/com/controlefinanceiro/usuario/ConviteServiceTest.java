package com.controlefinanceiro.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.auth.JwtService;
import com.controlefinanceiro.auth.TokenResponse;
import com.controlefinanceiro.common.ConviteInvalidoException;
import com.controlefinanceiro.common.RoleInvalidoException;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.empresa.EmpresaRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class ConviteServiceTest {

  private ConviteRepository conviteRepository;
  private UsuarioRepository usuarioRepository;
  private EmpresaRepository empresaRepository;
  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;
  private ConviteService service;

  @BeforeEach
  void setup() {
    conviteRepository = mock(ConviteRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    empresaRepository = mock(EmpresaRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    jwtService = mock(JwtService.class);
    service =
        new ConviteService(
            conviteRepository, usuarioRepository, empresaRepository, passwordEncoder, jwtService);
  }

  @Test
  void criarGravaApenasHashDoToken() {
    Empresa empresa = empresa();
    when(empresaRepository.getReferenceById(10L)).thenReturn(empresa);
    when(usuarioRepository.findByEmpresaIdAndEmail(10L, "admin@teste.com"))
        .thenReturn(Optional.of(usuario()));

    ConviteResponse resposta = service.criar(10L, "convidado@teste.com", "USER", "admin@teste.com");

    ArgumentCaptor<Convite> captor = ArgumentCaptor.forClass(Convite.class);
    verify(conviteRepository).save(captor.capture());
    Convite convite = captor.getValue();
    assertThat(resposta.token()).isNotBlank();
    assertThat(convite.getTokenHash()).hasSize(64).isNotEqualTo(resposta.token());
    assertThat(convite.getEmail()).isEqualTo("convidado@teste.com");
    assertThat(convite.getRole()).isEqualTo("USER");
    assertThat(convite.getExpiraEm()).isAfter(OffsetDateTime.now());
  }

  @Test
  void criarRejeitaRoleForaDoEnum() {
    assertThatThrownBy(() -> service.criar(10L, "convidado@teste.com", "ROOT", "admin@teste.com"))
        .isInstanceOf(RoleInvalidoException.class);
    verify(conviteRepository, never()).save(any());
  }

  @Test
  void aceitarConviteExpiradoFalha() {
    Convite convite = convite(OffsetDateTime.now().minusMinutes(1), null);
    when(conviteRepository.findByTokenHash(any())).thenReturn(Optional.of(convite));

    assertThatThrownBy(() -> service.aceitar("token", "Nome", "senha"))
        .isInstanceOf(ConviteInvalidoException.class);
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void aceitarConviteJaUsadoFalha() {
    Convite convite = convite(OffsetDateTime.now().plusDays(1), OffsetDateTime.now());
    when(conviteRepository.findByTokenHash(any())).thenReturn(Optional.of(convite));

    assertThatThrownBy(() -> service.aceitar("token", "Nome", "senha"))
        .isInstanceOf(ConviteInvalidoException.class);
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void aceitarComEmailDivergenteFalha() {
    Convite convite = convite(OffsetDateTime.now().plusDays(1), null);
    when(conviteRepository.findByTokenHash(any())).thenReturn(Optional.of(convite));

    assertThatThrownBy(
            () -> service.aceitarComGoogle("token", "outro@teste.com", "Nome", "google-sub"))
        .isInstanceOf(ConviteInvalidoException.class);
    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void aceitarConviteValidoCriaUsuarioNaEmpresaDoConvite() {
    Convite convite = convite(OffsetDateTime.now().plusDays(1), null);
    when(conviteRepository.findByTokenHash(any())).thenReturn(Optional.of(convite));
    when(usuarioRepository.findByEmail("convidado@teste.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("senha")).thenReturn("$2a$12$hash");
    when(jwtService.gerarToken(any())).thenReturn("jwt");

    TokenResponse resposta = service.aceitar("token", "Nome", "senha");

    assertThat(resposta.token()).isEqualTo("jwt");
    ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(captor.capture());
    assertThat(captor.getValue().getEmpresa()).isSameAs(convite.getEmpresa());
    assertThat(captor.getValue().getRole()).isEqualTo("USER");
    assertThat(convite.getUsadoEm()).isNotNull();
  }

  private Empresa empresa() {
    Empresa empresa = new Empresa();
    empresa.setId(10L);
    empresa.setNome("Empresa");
    return empresa;
  }

  private Usuario usuario() {
    Usuario usuario = new Usuario();
    usuario.setEmpresa(empresa());
    usuario.setEmail("admin@teste.com");
    usuario.setRole("ADMIN");
    return usuario;
  }

  private Convite convite(OffsetDateTime expiraEm, OffsetDateTime usadoEm) {
    Convite convite = new Convite();
    convite.setEmpresa(empresa());
    convite.setEmail("convidado@teste.com");
    convite.setRole("USER");
    convite.setTokenHash("hash");
    convite.setExpiraEm(expiraEm);
    convite.setUsadoEm(usadoEm);
    return convite;
  }
}
