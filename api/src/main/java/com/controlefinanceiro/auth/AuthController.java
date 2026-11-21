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

  public AuthController(
      AuthService authService,
      TenantOnboardingService tenantOnboardingService,
      ConviteService conviteService) {
    this.authService = authService;
    this.tenantOnboardingService = tenantOnboardingService;
    this.conviteService = conviteService;
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

  @PostMapping("/convites/aceitar")
  public ResponseEntity<TokenResponse> aceitarConvite(
      @Valid @RequestBody AceitarConviteRequest request) {
    return ResponseEntity.ok(
        conviteService.aceitar(request.token(), request.nome(), request.senha()));
  }
}
