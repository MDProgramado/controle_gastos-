package com.controlefinanceiro.auth;

import com.controlefinanceiro.usuario.ConviteService;
import com.controlefinanceiro.usuario.TenantOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/auth/login — público (SecurityConfig). Controller só valida formato e delega ao {@link
 * AuthService} (PLAN §3.3); nunca contém regra de negócio nem lê tenant (R3).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final TenantOnboardingService tenantOnboardingService;
  private final ConviteService conviteService;
  private final GoogleAuthService googleAuthService;

  public AuthController(
      AuthService authService,
      TenantOnboardingService tenantOnboardingService,
      ConviteService conviteService,
      GoogleAuthService googleAuthService) {
    this.authService = authService;
    this.tenantOnboardingService = tenantOnboardingService;
    this.conviteService = conviteService;
    this.googleAuthService = googleAuthService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/registro")
  public ResponseEntity<TokenResponse> registro(@Valid @RequestBody RegistroRequest request) {
    return ResponseEntity.ok(
        tenantOnboardingService.onboarding(
            request.empresaNome(), request.nome(), request.email(), request.senha()));
  }

  /** Fase 4 (D-01): delega validação e autenticação ao serviço; devolve somente o JWT próprio. */
  @PostMapping("/google")
  public ResponseEntity<TokenResponse> google(@Valid @RequestBody GoogleRequest request) {
    return ResponseEntity.ok(googleAuthService.autenticarComToken(request));
  }

  @PostMapping("/convites/aceitar")
  public ResponseEntity<TokenResponse> aceitarConvite(
      @Valid @RequestBody AceitarConviteRequest request) {
    return ResponseEntity.ok(
        conviteService.aceitar(request.token(), request.nome(), request.senha()));
  }
}
