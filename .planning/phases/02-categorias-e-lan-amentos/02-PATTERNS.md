# Phase 2: Categorias e Lançamentos - Pattern Map

**Mapped:** 2026-11-08
**Files analyzed:** 22 (20 new, 2 modified)
**Analogs found:** 22 / 22

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `api/src/main/java/com/controlefinanceiro/db/migration/V2__seed_categorias.java` | migration | CRUD | `api/src/main/java/com/controlefinanceiro/db/migration/V1_1__seed_empresa_admin.java` | exact |
| `api/src/main/java/com/controlefinanceiro/categoria/Categoria.java` | model | CRUD | `api/src/main/java/com/controlefinanceiro/usuario/Usuario.java` | exact |
| `api/src/main/java/com/controlefinanceiro/categoria/CategoriaRepository.java` | repository | CRUD | `api/src/main/java/com/controlefinanceiro/usuario/UsuarioRepository.java` | exact |
| `api/src/main/java/com/controlefinanceiro/categoria/CategoriaService.java` | service | CRUD | `api/src/main/java/com/controlefinanceiro/auth/AuthService.java` | exact |
| `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java` | controller | request-response | `api/src/main/java/com/controlefinanceiro/auth/AuthController.java` | exact |
| `api/src/main/java/com/controlefinanceiro/categoria/CategoriaRequest.java` | DTO | request-response | `api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java` | exact |
| `api/src/main/java/com/controlefinanceiro/categoria/CategoriaResponse.java` | DTO | request-response | `api/src/main/java/com/controlefinanceiro/auth/LoginResponse.java` | role-match |
| `api/src/main/java/com/controlefinanceiro/lancamento/Lancamento.java` | model | CRUD | `api/src/main/java/com/controlefinanceiro/usuario/Usuario.java` | exact |
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java` | repository | CRUD | `api/src/main/java/com/controlefinanceiro/usuario/UsuarioRepository.java` | exact |
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` | service | CRUD + transactional batch | `api/src/main/java/com/controlefinanceiro/auth/AuthService.java` | exact |
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` | controller | request-response | `api/src/main/java/com/controlefinanceiro/auth/AuthController.java` | exact |
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRequest.java` | DTO | request-response | `api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java` | exact |
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoResponse.java` | DTO | request-response | `api/src/main/java/com/controlefinanceiro/auth/LoginResponse.java` | role-match |
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoLoteRequest.java` | DTO | batch | `api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java` | partial |
| `api/src/main/java/com/controlefinanceiro/common/MonetarioException.java` | utility (exception) | N/A | `api/src/main/java/com/controlefinanceiro/common/CredenciaisInvalidasException.java` | role-match |
| `api/src/main/java/com/controlefinanceiro/common/CategoriaTipoIncompativelException.java` | utility (exception) | N/A | `api/src/main/java/com/controlefinanceiro/common/CredenciaisInvalidasException.java` | role-match |
| `api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java` | test | request-response (integration) | `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` | exact |
| `api/src/test/java/com/controlefinanceiro/LancamentoIntegrationTest.java` | test | request-response (integration) | `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` | exact |
| `api/src/test/java/com/controlefinanceiro/LancamentoLoteIntegrationTest.java` | test | batch (integration) | `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` | role-match |
| `api/src/test/java/com/controlefinanceiro/V2SeedMigrationTest.java` | test | CRUD (integration) | `api/src/test/java/com/controlefinanceiro/SeedMigrationTest.java` | exact |
| `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` | middleware | request-response (modify) | itself — add handlers | N/A |
| `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java` | config/middleware | request-response (modify) | itself — add USER read rule | N/A |

## Pattern Assignments

### `api/src/main/java/com/controlefinanceiro/db/migration/V2__seed_categorias.java` (migration, CRUD)

**Analog:** `api/src/main/java/com/controlefinanceiro/db/migration/V1_1__seed_empresa_admin.java` (lines 1-76)

**Imports pattern** (lines 1-8):
```java
package com.controlefinanceiro.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
```

**Class declaration + migrate override** (lines 27-30):
```java
public class V1_1__seed_empresa_admin extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
```

**Connection from Flyway context** (line 35):
```java
    Connection connection = context.getConnection();
```

**Subquery for empresa_id (D-02)** — analog uses `RETURN_GENERATED_KEYS` to capture inserted empresa id (lines 38-47); V2 must instead use:
```java
    // V2 pattern: empresa já existe após V1_1, obter id via subquery, nunca hardcodear
    Long empresaId;
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT id FROM empresa WHERE nome = 'Controle Financeiro'")) {
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new IllegalStateException("Empresa 'Controle Financeiro' não encontrada");
        }
        empresaId = rs.getLong(1);
      }
    }
```

**Idempotency check (D-03)** — check before insert (no exact analog in V1_1; new pattern for V2):
```java
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT 1 FROM categoria WHERE empresa_id = ? AND nome = ? AND tipo = ?")) {
      ps.setLong(1, empresaId);
      ps.setString(2, nome);
      ps.setString(3, tipo);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          continue; // já existe, pular
        }
      }
    }
```

**Insert via raw JDBC** (V1_1 lines 49-58):
```java
    try (PreparedStatement ps =
        connection.prepareStatement(
            "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role, ativo)"
                + " VALUES (?, ?, ?, ?, ?, true)")) {
      ps.setLong(1, empresaId);
      ps.setString(2, "admin@controle.com");
      ...
      ps.executeUpdate();
    }
```

**Do NOT close the connection** (line 60):
```java
    // NÃO fechar a connection do Context (regra Flyway)
```

**Fail-fast guard** (lines 68-75):
```java
  public static String senhaObrigatoria(String valor) {
    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException(
          "SEED_ADMIN_PASSWORD não definida ou vazia — necessária para a migration de seed"
              + " V1_1__seed_empresa_admin (defina a env var por ambiente)");
    }
    return valor;
  }
```

---

### `api/src/main/java/com/controlefinanceiro/categoria/Categoria.java` (model, CRUD)

**Analog:** `api/src/main/java/com/controlefinanceiro/usuario/Usuario.java` (lines 1-124)

**Entity + table annotations** (lines 21-22):
```java
@Entity
@Table(name = "usuario")
public class Usuario {
```

**empresa_id via @ManyToOne** (lines 30-32 — R3: tenant never from body):
```java
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;
```

**Column patterns** (lines 34-49):
```java
  @Column(nullable = false, unique = true, length = 160)
  private String email;

  @Column(nullable = false, length = 16)
  private String role;

  @Column(nullable = false)
  private boolean ativo;
```

**PrePersist timestamp** (lines 51-59):
```java
  @Column(name = "criado_em", nullable = false, updatable = false)
  private OffsetDateTime criadoEm;

  @PrePersist
  void aoPersistir() {
    if (criadoEm == null) {
      criadoEm = OffsetDateTime.now();
    }
  }
```

**Getters/setters** (lines 61-123) — standard JavaBean accessors.

---

### `api/src/main/java/com/controlefinanceiro/categoria/CategoriaRepository.java` (repository, CRUD)

**Analog:** `api/src/main/java/com/controlefinanceiro/usuario/UsuarioRepository.java` (lines 1-17)

**JpaRepository extension** (line 8):
```java
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
```

**Custom @Query with @Param** (lines 15-16):
```java
  @Query("select u from Usuario u left join fetch u.empresa where u.email = :email")
  Optional<Usuario> findByEmail(@Param("email") String email);
```

---

### `api/src/main/java/com/controlefinanceiro/categoria/CategoriaService.java` (service, CRUD)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/AuthService.java` (lines 1-39)

**Service annotation + constructor injection** (lines 14-26):
```java
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
```

**Business logic with custom exception** (lines 28-38):
```java
  public LoginResponse login(LoginRequest request) {
    Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);

    if (usuario == null
        || !usuario.isAtivo()
        || !passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
      throw new CredenciaisInvalidasException();
    }

    return new LoginResponse(jwtService.gerarToken(usuario));
  }
```

**@Transactional on writes** — not present in AuthService (read-only login) but required for Categoria/Lancamento write methods. Pattern from RESEARCH.md:
```java
  @Transactional
  public LancamentoResponse criar(Long empresaId, LancamentoRequest request) {
    // ... business rules, then repository.save()
  }
```

---

### `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java` (controller, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/AuthController.java` (lines 1-28)

**Imports** (lines 1-8):
```java
package com.controlefinanceiro.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
```

**Controller declaration** (lines 14-16):
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
```

**Constructor injection** (lines 18-22):
```java
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }
```

**Thin handler with @Valid @RequestBody** (lines 24-27):
```java
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
```

---

### `api/src/main/java/com/controlefinanceiro/categoria/CategoriaRequest.java` (DTO, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java` (lines 1-15)

**Record DTO with Jakarta Validation** (lines 7-15):
```java
public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(max = 72) String senha) {}
```

**R3 gate note** (lines 10-11): DTO must NOT contain `empresa_id` field — tenant comes exclusively from token via `TenantContext`.

---

### `api/src/main/java/com/controlefinanceiro/categoria/CategoriaResponse.java` (DTO, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/LoginResponse.java` (not read directly; pattern from AuthController line 25 return type `ResponseEntity<LoginResponse>`)

**Response record pattern** — simple immutable record returned from service, serialized as JSON body.

---

### `api/src/main/java/com/controlefinanceiro/lancamento/Lancamento.java` (model, CRUD)

**Analog:** `api/src/main/java/com/controlefinanceiro/usuario/Usuario.java` (entity pattern) + `api/src/main/java/com/controlefinanceiro/empresa/Empresa.java` (simpler entity)

See `Categoria.java` analog above for entity annotations, `@ManyToOne` to empresa/categoria/usuario, `@Column` for `valor` (BigDecimal, NUMERIC(14,2)), `data` (DATE), `descricao`, `tipo`, `criado_em`, `atualizado_em`.

---

### `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java` (repository, CRUD)

**Analog:** `api/src/main/java/com/controlefinanceiro/usuario/UsuarioRepository.java` (lines 1-17)

See `CategoriaRepository.java` analog above — `JpaRepository<Lancamento, Long>` with custom `@Query` for filtering by empresa_id + month/type/category (uses existing indexes `idx_lanc_empresa_data`, `idx_lanc_empresa_cat`).

---

### `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` (service, CRUD + transactional batch)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/AuthService.java` (lines 1-39)

See `CategoriaService.java` analog above for service structure. Additional patterns:

**@Transactional write method** (from RESEARCH.md code example):
```java
  @Transactional
  public LancamentoResponse criar(Long empresaId, LancamentoRequest request) {
    // LAN-04: rejeita (não arredonda) se mais de 2 casas decimais
    validarCasasDecimais(request.valor());
    // LAN-07: valor sempre positivo
    if (request.valor().signum() <= 0) {
      throw new ValorInvalidoException("Valor deve ser positivo");
    }
    // CAT-04: integridade categoria × tipo
    validarIntegridadeCategoriaTipo(request.categoriaId(), request.tipo(), empresaId);
    // ... salvar via repositório
  }
```

**@Transactional batch with line index** (from RESEARCH.md code example):
```java
  @Transactional
  public void processarLote(Long empresaId, LancamentoLoteRequest lote) {
    // LOT-01/LOT-02: tudo-ou-nada — se qualquer linha falhar, rollback total
    IntStream.range(0, lote.linhas().size())
        .forEach(i -> processarLinha(empresaId, lote.linhas().get(i), i));
  }

  private void processarLinha(Long empresaId, LancamentoLinha linha, int indice) {
    try {
      criar(empresaId, linha.toRequest());
    } catch (Exception ex) {
      throw new LinhaInvalidaException(indice, ex.getMessage()); // carrega índice
    }
  }
```

---

### `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` (controller, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/AuthController.java` (lines 1-28)

See `CategoriaController.java` analog above — thin controller, `@RestController`, `@RequestMapping("/api/lancamentos")`, constructor injection, `@Valid @RequestBody`, delegates to service.

Batch endpoint adds `@PostMapping("/lote")` with `LancamentoLoteRequest`.

---

### `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRequest.java` (DTO, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java` (lines 1-15)

See `CategoriaRequest.java` analog above. BigDecimal field with validation:
```java
  // LAN-04: @Digits para rejeição precoce no controller; service faz defesa em profundidade
  @NotNull @Digits(integer = 12, fraction = 2) BigDecimal valor,
```

No `empresa_id` field (R3 gate).

---

### `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoResponse.java` (DTO, request-response)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/LoginResponse.java` (response record pattern)

Simple immutable record with id, tipo, valor (BigDecimal), data, descricao, categoria info.

---

### `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoLoteRequest.java` (DTO, batch)

**Analog:** `api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java` (DTO record pattern — partial match; no batch DTO exists yet)

Wrapper record containing `List<LancamentoLinha> linhas`. Limit of 500 enforced in service.

---

### `api/src/main/java/com/controlefinanceiro/common/MonetarioException.java` (utility/exception, N/A)

**Analog:** `api/src/main/java/com/controlefinanceiro/common/CredenciaisInvalidasException.java` (lines 1-12)

**Custom RuntimeException pattern** (lines 7-11):
```java
public class CredenciaisInvalidasException extends RuntimeException {

  public CredenciaisInvalidasException() {
    super("E-mail ou senha inválidos");
  }
}
```

MonetarioException follows same structure with message about >2 decimal places or non-positive value.

---

### `api/src/main/java/com/controlefinanceiro/common/CategoriaTipoIncompativelException.java` (utility/exception, N/A)

**Analog:** `api/src/main/java/com/controlefinanceiro/common/CredenciaisInvalidasException.java` (lines 1-12)

Same custom RuntimeException structure; message indicates category type does not match lancamento type.

---

### `api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java` (test, request-response integration)

**Analog:** `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` (lines 1-116)

**Test class annotations** (lines 28-33):
```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
```

**Autowired TestRestTemplate** (line 38):
```java
  @Autowired private TestRestTemplate rest;
```

**Test method pattern** (lines 42-60): status code assertion + body assertion via ObjectMapper.

---

### `api/src/test/java/com/controlefinanceiro/LancamentoIntegrationTest.java` (test, request-response integration)

**Analog:** `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` (lines 1-116)

Same Testcontainers pattern as `CategoriaIntegrationTest.java` above.

---

### `api/src/test/java/com/controlefinanceiro/LancamentoLoteIntegrationTest.java` (test, batch integration)

**Analog:** `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` (lines 1-116)

Same Testcontainers setup; tests batch all-or-nothing: send batch with one invalid line, assert 400 + line index in detail, assert zero rows persisted.

---

### `api/src/test/java/com/controlefinanceiro/V2SeedMigrationTest.java` (test, CRUD integration)

**Analog:** `api/src/test/java/com/controlefinanceiro/SeedMigrationTest.java` (lines 1-71)

**Test class annotations** (lines 22-23):
```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeedMigrationTest {
```

**JdbcTemplate autowired** (line 29):
```java
  @Autowired private JdbcTemplate jdbc;
```

**JDBC assertions** (lines 31-45): count rows, check values via `jdbc.queryForList` / `jdbc.queryForObject`.

**Migration unit test** (lines 59-70): call static guard method directly, assert exception.

---

### Modified: `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java`

**Analog:** itself — add new `@ExceptionHandler` methods following existing pattern.

**Existing handler structure** (lines 21-40):
```java
  @ExceptionHandler(CredenciaisInvalidasException.class)
  public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(NaoAutorizadoException.class)
  public ProblemDetail naoAutorizado(NaoAutorizadoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail argumentosInvalidos(MethodArgumentNotValidException ex) {
    String campos =
        ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, campos);
  }
```

**New handlers to add** (following same pattern):
```java
  @ExceptionHandler(MonetarioException.class)
  public ProblemDetail monetarioInvalido(MonetarioException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(CategoriaTipoIncompativelException.class)
  public ProblemDetail tipoIncompativel(CategoriaTipoIncompativelException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(LinhaInvalidaException.class)
  public ProblemDetail linhaInvalida(LinhaInvalidaException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }
```

---

### Modified: `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java`

**Analog:** itself — split `/api/categorias/**` rule so USER can read.

**Current rule** (lines 41-42):
```java
                    .requestMatchers("/api/categorias/**")
                    .hasRole("ADMIN")
```

**New pattern** (split read/write):
```java
                    .requestMatchers(HttpMethod.GET, "/api/categorias").hasAnyRole("ADMIN", "USER")
                    .requestMatchers("/api/categorias/**").hasRole("ADMIN")
```

---

## Shared Patterns

### Authentication / Tenant Resolution
**Source:** `api/src/main/java/com/controlefinanceiro/common/TenantContext.java` (lines 18-26)
**Apply to:** All service classes in `categoria` and `lancamento` packages
```java
  public static Long getEmpresaId() {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao != null
        && autenticacao.isAuthenticated()
        && autenticacao.getPrincipal() instanceof UsuarioAuth usuario) {
      return usuario.empresaId();
    }
    throw new NaoAutorizadoException();
  }
```
**Rule:** `empresa_id` comes exclusively from JWT via `TenantContext.getEmpresaId()`, never from DTO/body/query.

### Error Handling (RFC 7807 ProblemDetail)
**Source:** `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` (lines 17-49)
**Apply to:** All controller files; modified by adding handlers for `MonetarioException`, `CategoriaTipoIncompativelException`, `LinhaInvalidaException`
```java
@RestControllerAdvice
public class ProblemDetailExceptionHandler {
  @ExceptionHandler(CredenciaisInvalidasException.class)
  public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }
  // ... same pattern for each custom exception
}
```

### Validation (Jakarta Bean Validation)
**Source:** `api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java` (lines 7-15)
**Apply to:** All `*Request.java` DTOs
```java
public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(max = 72) String senha) {}
```
**Monetary:** `@NotNull @Digits(integer = 12, fraction = 2) BigDecimal valor` for early rejection; service-level `scale() > 2` check as defense-in-depth.

### Thin Controller + Service + Repository
**Source:** `api/src/main/java/com/controlefinanceiro/auth/AuthController.java` (lines 14-27) + `AuthService.java` (lines 14-38) + `UsuarioRepository.java` (line 8)
**Apply to:** `CategoriaController/Service/Repository`, `LancamentoController/Service/Repository`
```java
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {
  private final CategoriaService categoriaService;
  public CategoriaController(CategoriaService categoriaService) { this.categoriaService = categoriaService; }
  @PostMapping
  public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
    return ResponseEntity.ok(categoriaService.criar(TenantContext.getEmpresaId(), request));
  }
}
```

### Flyway Java Migration
**Source:** `api/src/main/java/com/controlefinanceiro/db/migration/V1_1__seed_empresa_admin.java` (lines 1-76)
**Apply to:** `V2__seed_categorias.java`
```java
public class V2__seed_categorias extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    // raw JDBC, sem DI Spring; empresa_id via subquery; idempotência: SELECT antes de INSERT
    // NÃO fechar a connection do Context (regra Flyway)
  }
}
```

### Testcontainers Integration Test
**Source:** `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` (lines 28-33) + `SeedMigrationTest.java` (lines 22-29)
**Apply to:** `CategoriaIntegrationTest`, `LancamentoIntegrationTest`, `LancamentoLoteIntegrationTest`, `V2SeedMigrationTest`
```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class XxxIntegrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
  @Autowired private TestRestTemplate rest; // or JdbcTemplate for migration tests
}
```

## No Analog Found

None — all 22 files have at least a role-match analog in the existing Phase 1 codebase. The closest analog for every file is identified above. `LancamentoLoteRequest.java` (batch DTO) and `LancamentoLoteIntegrationTest.java` (batch integration test) are partial matches only — no batch endpoint exists yet in Phase 1, so the planner should derive the batch DTO pattern from `LoginRequest.java` (record DTO) and the batch test pattern from `AuthIntegrationTest.java` (Testcontainers + TestRestTemplate).

## Metadata

**Analog search scope:** `api/src/main/java/com/controlefinanceiro/**`, `api/src/main/resources/db/migration/**`, `api/src/test/java/com/controlefinanceiro/**`
**Files scanned:** 23 source files + 6 test files
**Pattern extraction date:** 2026-11-08
**Key patterns identified:**
- All controllers: thin `@RestController` + constructor-injected service + `@Valid @RequestBody`
- All services: `@Service` + business rules + custom `RuntimeException` throws + `@Transactional` on writes
- All repositories: `JpaRepository<Entity, Long>` + optional `@Query` with `@Param`
- All entities: `@Entity` + `@Table` + `@ManyToOne` for `empresa_id` (tenant never from body)
- All DTOs: immutable Java `record` with Jakarta Validation annotations, no `empresa_id` field
- Error handling: `@RestControllerAdvice` with `@ExceptionHandler` per custom exception returning RFC 7807 `ProblemDetail`
- Tenant: `TenantContext.getEmpresaId()` reads from `SecurityContext`/`UsuarioAuth` principal only
- Migrations: `BaseJavaMigration` with raw JDBC, `context.getConnection()`, never close connection, idempotent checks
- Tests: `@Testcontainers` + `@ServiceConnection PostgreSQLContainer` + `TestRestTemplate` or `JdbcTemplate`