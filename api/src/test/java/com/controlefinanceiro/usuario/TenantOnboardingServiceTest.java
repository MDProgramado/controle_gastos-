package com.controlefinanceiro.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.controlefinanceiro.auth.JwtService;
import com.controlefinanceiro.auth.TokenResponse;
import com.controlefinanceiro.categoria.Categoria;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.CadastroInvalidoException;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.empresa.EmpresaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class TenantOnboardingServiceTest {

  private EmpresaRepository empresaRepository;
  private UsuarioRepository usuarioRepository;
  private CategoriaRepository categoriaRepository;
  private PasswordEncoder passwordEncoder;
  private JwtService jwtService;
  private TenantOnboardingService service;

  @BeforeEach
  void setup() {
    empresaRepository = mock(EmpresaRepository.class);
    usuarioRepository = mock(UsuarioRepository.class);
    categoriaRepository = mock(CategoriaRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    jwtService = mock(JwtService.class);
    service =
        new TenantOnboardingService(
            empresaRepository, usuarioRepository, categoriaRepository, passwordEncoder, jwtService);
  }

  @Test
  void onboardingCriaEmpresaAdminCategoriasEToken() {
    when(empresaRepository.save(any()))
        .thenAnswer(
            invocacao -> {
              Empresa empresa = invocacao.getArgument(0);
              empresa.setId(42L);
              return empresa;
            });
    when(usuarioRepository.findByEmail("novo@teste.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode("senha")).thenReturn("$2a$12$hash");
    when(jwtService.gerarToken(any())).thenReturn("jwt");

    TokenResponse resposta = service.onboarding("Nova Empresa", "Nome", "novo@teste.com", "senha");

    assertThat(resposta.token()).isEqualTo("jwt");
    ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(usuarioCaptor.capture());
    assertThat(usuarioCaptor.getValue().getRole()).isEqualTo("ADMIN");
    assertThat(usuarioCaptor.getValue().isAtivo()).isTrue();
    assertThat(usuarioCaptor.getValue().getSenhaHash()).isEqualTo("$2a$12$hash");

    ArgumentCaptor<Iterable<Categoria>> categoriasCaptor = ArgumentCaptor.forClass(Iterable.class);
    verify(categoriaRepository).saveAll(categoriasCaptor.capture());
    assertThat(categoriasCaptor.getValue()).hasSize(16);
  }

  @Test
  void onboardingComGoogleGravaSubEmailVerificadoESenhaAleatoria() {
    when(empresaRepository.save(any()))
        .thenAnswer(
            invocacao -> {
              Empresa empresa = invocacao.getArgument(0);
              empresa.setId(42L);
              return empresa;
            });
    when(usuarioRepository.findByEmail("google@teste.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash-aleatorio");
    when(jwtService.gerarToken(any())).thenReturn("jwt");

    TokenResponse resposta =
        service.onboardingComGoogle("Nova Empresa", "Nome", "google@teste.com", "sub-google");

    assertThat(resposta.token()).isEqualTo("jwt");
    ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(usuarioCaptor.capture());
    assertThat(usuarioCaptor.getValue().getGoogleSub()).isEqualTo("sub-google");
    assertThat(usuarioCaptor.getValue().isEmailVerificado()).isTrue();
    assertThat(usuarioCaptor.getValue().getSenhaHash()).startsWith("$2");

    ArgumentCaptor<String> senhaCaptor = ArgumentCaptor.forClass(String.class);
    verify(passwordEncoder).encode(senhaCaptor.capture());
    assertThat(senhaCaptor.getValue()).isNotBlank().hasSizeGreaterThan(16);
  }

  @Test
  void onboardingComEmailExistenteNaoCriaEmpresa() {
    when(usuarioRepository.findByEmail("existe@teste.com")).thenReturn(Optional.of(new Usuario()));

    assertThatThrownBy(
            () -> service.onboarding("Nova Empresa", "Nome", "existe@teste.com", "senha"))
        .isInstanceOf(CadastroInvalidoException.class)
        .hasMessage("Não foi possível concluir o cadastro");
    verify(empresaRepository, never()).save(any());
    verify(categoriaRepository, never()).saveAll(any());
  }
}
