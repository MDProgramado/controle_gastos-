package com.controlefinanceiro.usuario;

import com.controlefinanceiro.auth.JwtService;
import com.controlefinanceiro.auth.TokenResponse;
import com.controlefinanceiro.common.CadastroInvalidoException;
import com.controlefinanceiro.common.ConviteInvalidoException;
import com.controlefinanceiro.common.NaoAutorizadoException;
import com.controlefinanceiro.common.Role;
import com.controlefinanceiro.empresa.EmpresaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConviteService {

  private static final Duration VALIDADE = Duration.ofDays(7);

  private final ConviteRepository conviteRepository;
  private final UsuarioRepository usuarioRepository;
  private final EmpresaRepository empresaRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final SecureRandom aleatorio = new SecureRandom();

  public ConviteService(
      ConviteRepository conviteRepository,
      UsuarioRepository usuarioRepository,
      EmpresaRepository empresaRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService) {
    this.conviteRepository = conviteRepository;
    this.usuarioRepository = usuarioRepository;
    this.empresaRepository = empresaRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public ConviteResponse criar(Long empresaId, String email, String role, String criadoPorEmail) {
    Role papel = Role.from(role);
    Usuario criadoPor =
        usuarioRepository
            .findByEmpresaIdAndEmail(empresaId, normalizar(criadoPorEmail))
            .orElseThrow(NaoAutorizadoException::new);

    byte[] bytes = new byte[32];
    aleatorio.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    Convite convite = new Convite();
    convite.setEmpresa(empresaRepository.getReferenceById(empresaId));
    convite.setEmail(normalizar(email));
    convite.setRole(papel.valor());
    convite.setTokenHash(hash(token));
    convite.setExpiraEm(OffsetDateTime.now().plus(VALIDADE));
    convite.setCriadoPor(criadoPor);
    conviteRepository.save(convite);

    return new ConviteResponse(token, convite.getExpiraEm());
  }

  @Transactional
  public TokenResponse aceitar(String token, String nome, String senha) {
    return aceitarInterno(token, null, nome, senha, null);
  }

  @Transactional
  public TokenResponse aceitarComGoogle(
      String token, String emailVerificado, String nome, String googleSub) {
    return aceitarInterno(token, emailVerificado, nome, null, googleSub);
  }

  private TokenResponse aceitarInterno(
      String token, String emailSolicitado, String nome, String senha, String googleSub) {
    Convite convite =
        conviteRepository.findByTokenHash(hash(token)).orElseThrow(ConviteInvalidoException::new);
    OffsetDateTime agora = OffsetDateTime.now();
    if (convite.getUsadoEm() != null || convite.getExpiraEm().isBefore(agora)) {
      throw new ConviteInvalidoException();
    }
    if (emailSolicitado != null && !normalizar(emailSolicitado).equals(convite.getEmail())) {
      throw new ConviteInvalidoException();
    }
    if (usuarioRepository.findByEmail(convite.getEmail()).isPresent()) {
      throw new CadastroInvalidoException();
    }

    Usuario usuario = new Usuario();
    usuario.setEmpresa(convite.getEmpresa());
    usuario.setEmail(convite.getEmail());
    usuario.setNome(nome == null || nome.isBlank() ? convite.getEmail() : nome.trim());
    String senhaBase = senha == null || senha.isBlank() ? UUID.randomUUID().toString() : senha;
    usuario.setSenhaHash(passwordEncoder.encode(senhaBase));
    usuario.setRole(convite.getRole());
    usuario.setAtivo(true);
    usuario.setEmailVerificado(googleSub != null);
    usuario.setGoogleSub(googleSub);
    usuario.setTokenVersion(0);
    usuarioRepository.save(usuario);

    convite.setUsadoEm(agora);
    return new TokenResponse(jwtService.gerarToken(usuario));
  }

  private String normalizar(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String hash(String token) {
    try {
      byte[] resumo =
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(resumo);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }
}
