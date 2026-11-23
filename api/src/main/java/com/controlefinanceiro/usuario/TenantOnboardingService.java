package com.controlefinanceiro.usuario;

import com.controlefinanceiro.auth.JwtService;
import com.controlefinanceiro.auth.TokenResponse;
import com.controlefinanceiro.categoria.CategoriaRepository;
import com.controlefinanceiro.common.CadastroInvalidoException;
import com.controlefinanceiro.empresa.Empresa;
import com.controlefinanceiro.empresa.EmpresaRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantOnboardingService {

  private final EmpresaRepository empresaRepository;
  private final UsuarioRepository usuarioRepository;
  private final CategoriaRepository categoriaRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public TenantOnboardingService(
      EmpresaRepository empresaRepository,
      UsuarioRepository usuarioRepository,
      CategoriaRepository categoriaRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.empresaRepository = empresaRepository;
    this.usuarioRepository = usuarioRepository;
    this.categoriaRepository = categoriaRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public TokenResponse onboarding(String empresaNome, String nome, String email, String senha) {
    String emailNormalizado = normalizar(email);
    if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
      throw new CadastroInvalidoException();
    }
    return criarTenant(
        empresaNome, nome, emailNormalizado, passwordEncoder.encode(senha), null, false);
  }

  /** D-07/D-09: senha BCrypt aleatória (login por senha impossível) + e-mail já verificado. */
  @Transactional
  public TokenResponse onboardingComGoogle(
      String empresaNome, String nome, String email, String googleSub) {
    String emailNormalizado = normalizar(email);
    if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
      throw new CadastroInvalidoException();
    }
    return criarTenant(
        empresaNome,
        nome,
        emailNormalizado,
        passwordEncoder.encode(UUID.randomUUID().toString()),
        googleSub,
        true);
  }

  private TokenResponse criarTenant(
      String empresaNome,
      String nome,
      String emailNormalizado,
      String senhaHash,
      String googleSub,
      boolean emailVerificado) {
    Empresa empresa = new Empresa();
    empresa.setNome(empresaNome.trim());
    empresaRepository.save(empresa);

    Usuario usuario = new Usuario();
    usuario.setEmpresa(empresa);
    usuario.setEmail(emailNormalizado);
    usuario.setNome(nome == null || nome.isBlank() ? emailNormalizado : nome.trim());
    usuario.setSenhaHash(senhaHash);
    usuario.setRole("ADMIN");
    usuario.setAtivo(true);
    usuario.setEmailVerificado(emailVerificado);
    usuario.setGoogleSub(googleSub);
    usuario.setTokenVersion(0);
    usuarioRepository.save(usuario);

    categoriaRepository.saveAll(CategoriasPadrao.para(empresa));

    return new TokenResponse(jwtService.gerarToken(usuario));
  }

  private String normalizar(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
