package com.controlefinanceiro.auth;

import com.controlefinanceiro.common.CredenciaisInvalidasException;
import com.controlefinanceiro.usuario.Usuario;
import com.controlefinanceiro.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Login (AUTH-01/02): BCrypt check + emissão do JWT. Os TRÊS casos de falha — e-mail inexistente,
 * senha não casa, usuário desativado — lançam a MESMA {@link CredenciaisInvalidasException} com
 * mensagem genérica (D-05 anti-enumeração, T-2-01).
 */
@Service
public class AuthService {

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.usuarioRepository = usuarioRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public LoginResponse login(LoginRequest request) {
    Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);

    if (usuario == null
        || !usuario.isAtivo()
        || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
      throw new CredenciaisInvalidasException();
    }

    return new LoginResponse(jwtService.gerarToken(usuario));
  }
}
