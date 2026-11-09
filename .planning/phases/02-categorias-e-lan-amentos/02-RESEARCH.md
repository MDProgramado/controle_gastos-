# Phase 2: Categorias e Lançamentos - Research

**Researched:** 2026-11-08
**Domain:** Java 21 + Spring Boot 3 CRUD API with Flyway seed, strict monetary validation, transactional batch processing
**Confidence:** HIGH

## Summary

Phase 2 extends the existing Phase 1 API with two bounded contexts: (1) **Categorias** — Flyway V2 Java migration seeding 16 categories (12 SAÍDA + 4 ENTRADA, 4 marked pessoal), plus full CRUD with role-based access (ADMIN writes, USER read-only); (2) **Lançamentos** — CRUD with strict `BigDecimal` monetary validation (NUMERIC(14,2), max 2 decimal places, rejected not rounded, HALF_EVEN), category×type integrity (ENTRADA category rejects SAÍDA lancamento and vice-versa), filtering by month/type/category, and a transação tudo-ou-nada batch endpoint (up to 500 lines; on error returns the invalid line index and rolls back everything).

All dependencies are already declared in `api/pom.xml` (Spring Boot 3.5.16 parent, Spring Data JPA, Spring Validation, Flyway core + PostgreSQL module, Testcontainers, PostgreSQL driver). No new external packages need to be installed. The phase builds entirely on Phase 1's established patterns: thin controller + service + JPA repository, `TenantContext` for tenant-from-token, RFC 7807 `ProblemDetailExceptionHandler`, Flyway Java migrations in `com.controlefinanceiro.db.migration`, and Testcontainers integration tests with `@ServiceConnection PostgreSQLContainer`.

**Primary recommendation:** Implement Phase 2 by copying Phase 1's established patterns (controller/service/repository, Java migration, Testcontainers test) into new `categoria` and `lancamento` packages under `com.controlefinanceiro`, adding BigDecimal monetary validation and `@Transactional` batch rollback.

## <user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Implementar o seed como **Java migration** `V2__seed_categorias.java` seguindo o padrão de `V1_1__seed_empresa_admin.java` (Fase 1) — não SQL puro — por consistência com o que já existe no repo.
- **D-02:** `empresa_id` obtido via subquery `(SELECT id FROM empresa WHERE nome='Controle Financeiro')`, nunca hardcoded (a empresa já existe após V1_1).
- **D-03:** Idempotência: checar se a categoria já existe antes de inserir, protegendo re-aplicação do V2.
- **D-04:** Manter a ordem da lista do cliente na migration (12 saídas + 4 entradas; pessoais [P]: Alimentação, Saúde/farmácia, Lazer, Rendimentos e reembolsos).

### the agent's Discretion

- Formato do lote tudo-ou-nada (array JSON único vs JSONL), reporte do índice da linha inválida e limite de 500 linhas.
- Onde rejeitar valor com >2 casas decimais (DTO bean validation vs service em BigDecimal) e onde aplicar RoundingMode.HALF_EVEN.
- Integridade categoria×tipo (checagem no service vs banco) e comportamento de lançamentos históricos quando a categoria é desativada (ativo=false).

### Deferred Ideas (OUT OF SCOPE)

- Importação de extrato (OFX/CSV) e lançamentos recorrentes — v2.
- App mobile nativo — fora de escopo (web-first).
- Multiempresa real (cadastro de novas empresas) — fase 1 só prepara schema com `empresa_id`.
- IA/OpenRouter — restrita à fase 4.
</user_constraints>

## <phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CAT-01 | Categoria possui tipo `ENTRADA` ou `SAÍDA` | Schema V1 already has `tipo VARCHAR(10)` CHECK column on `categoria`; entity mapping needed |
| CAT-02 | Seed (Flyway V2) cria 12 categorias de saída e 4 de entrada conforme lista do cliente | V1_1 Java migration pattern verified; V2 follows D-01..D-04 |
| CAT-03 | Categorias marcadas [P] (Alimentação, Saúde/farmácia, Lazer, Rendimentos e reembolsos) têm `pessoal = true` | Schema already has `pessoal BOOLEAN NOT NULL DEFAULT false` |
| CAT-04 | Categoria do tipo ENTRADA rejeita lançamento de SAÍDA e vice-versa | Open area — service-level vs DB-level check to be decided (see Open Questions) |
| CAT-05 | Usuário ADMIN pode criar, editar e desativar categorias; USER apenas lista | SecurityConfig already has `/api/categorias/**` → `hasRole("ADMIN")`; USER read needs new rule |
| CAT-06 | Categorias pertencem à empresa do token e nome é único por (empresa, tipo) | Schema already has `UNIQUE (empresa_id, nome, tipo)`; TenantContext provides empresa_id |
| LAN-01 | Usuário pode criar lançamento com data, tipo, valor, categoria e descrição | Schema already has `lancamento` table; need controller+service+repository |
| LAN-02 | Usuário pode editar lançamento existente (mesmas validações da criação) | Same CRUD pattern; edit path reuses creation validation |
| LAN-03 | Usuário pode excluir lançamento | DELETE endpoint with ownership/tenant check |
| LAN-04 | Valor aceita no máximo 2 casas decimais; entrada com mais de 2 casas é **rejeitada** (não arredondada) | BigDecimal validation needed; PostgreSQL NUMERIC(14,2) rounds silently — app-level rejection required |
| LAN-05 | Todo cálculo monetário usa `BigDecimal` com `NUMERIC(14,2)`, `scale = 2` e `RoundingMode.HALF_EVEN`; nenhum `double`/`float` no código | Verified constraint from AGENTS.md; BigDecimal everywhere |
| LAN-06 | Usuário pode filtrar lançamentos por mês, tipo e categoria (sempre dentro da empresa do token) | Repository query methods with empresa_id + filters; existing index `idx_lanc_empresa_data` and `idx_lanc_empresa_cat` support this |
| LAN-07 | Valor aceito é sempre positivo — o sinal é dado pelo tipo (ENTRADA/SAÍDA) | Schema already has `CHECK (valor > 0)`; service enforces positive |
| LOT-01 | Endpoint aceita lote de até 500 lançamentos em **transação tudo-ou-nada** | `@Transactional` on service method; `Flux`/`stream` processing with rollback on first error |
| LOT-02 | Em erro, a resposta informa o **índice da linha** inválida e **nenhuma linha** do lote é gravada | Transaction rollback + exception carrying line index; ProblemDetail response |
| INF-02 | Testes de integração rodam com Testcontainers (PostgreSQL real) | Existing test pattern verified (AuthIntegrationTest, HealthIntegrationTest, SeedMigrationTest) |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Categoria seed (Flyway V2) | Database / Storage | — | Flyway migration runs at app startup, inserts into `categoria` table |
| Categoria CRUD (read) | API / Backend | Browser / Client | Service enforces tenant; controller exposes REST; web UI calls API |
| Categoria CRUD (write) | API / Backend | — | ADMIN-only; business rules (unique name per empresa+tipo) in service |
| Lançamento CRUD | API / Backend | Browser / Client | Service validates money, category×type integrity, tenant isolation |
| Lote tudo-ou-nada | API / Backend | — | `@Transactional` service method; all-or-nothing rollback |
| Monetary validation | API / Backend | — | BigDecimal validation in DTO and/or service; never in browser |
| Tenant resolution | API / Backend | — | `TenantContext.getEmpresaId()` from JWT SecurityContext |

## Project Constraints (from AGENTS.md)

- **Stack** (não negociável): Java 21 + Spring Boot 3, Spring Security + JWT, Spring Data JPA, PostgreSQL, Flyway, Testcontainers; Next.js (App Router, TypeScript)
- **Dinheiro**: valores em `BigDecimal` com `NUMERIC(14,2)` no banco, `scale = 2`, `RoundingMode.HALF_EVEN` no service; entrada com mais de 2 casas é rejeitada; nenhum `double`/`float` no código
- **Tenant**: `empresa_id` vem exclusivamente do token JWT, nunca do body
- **Lote**: transação tudo-ou-nada; erro informa o índice da linha
- **Integridade**: categoria do tipo ENTRADA não aceita lançamento de SAÍDA e vice-versa
- **Segurança**: senha com BCrypt, dados em trânsito via HTTPS, política de privacidade e LGPD previstas antes do uso real
- **Deploy**: API no Render (Postgres pago com backup), web na Vercel
- **Escopo de IA**: restrito à fase 4 (OpenRouter)

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `spring-boot-starter-parent` | 3.5.16 | BOM + plugin management | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `spring-boot-starter-web` | managed by BOM | REST controllers, Tomcat | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `spring-boot-starter-security` | managed by BOM | Security filter chain, stateless sessions | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `spring-boot-starter-oauth2-resource-server` | managed by BOM | JWT Bearer token validation (Nimbus) | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `spring-boot-starter-data-jpa` | managed by BOM | JPA entities, repositories, Hibernate | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `spring-boot-starter-validation` | managed by BOM | Jakarta Bean Validation (`@Valid`, `@Digits`) | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `flyway-core` | managed by BOM | Schema migrations | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `flyway-database-postgresql` | managed by BOM | PostgreSQL support (Flyway 10+ separate module) | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `postgresql` (JDBC driver) | managed by BOM | PostgreSQL connectivity (runtime scope) | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `spring-boot-starter-test` | managed by BOM | Test infrastructure (JUnit 5, AssertJ) | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `spring-boot-testcontainers` | managed by BOM | Testcontainers integration with Spring Boot | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `testcontainers-postgresql` | managed by BOM | PostgreSQL container for integration tests | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |
| `testcontainers-junit-jupiter` | managed by BOM | JUnit 5 extensions for Testcontainers | Verified in `api/pom.xml` [VERIFIED: codebase pom.xml] |

### Supporting

No supporting libraries needed beyond the core stack. All phase requirements are satisfied by existing dependencies.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Flyway Java migration | SQL migration (`.sql`) | D-01 locks to Java migration for consistency with V1_1; SQL would break the established pattern |
| `@Digits` bean validation | Custom BigDecimal scale validator | `@Digits` is standard Jakarta Validation; custom validator gives more control over error messages |
| Service-level category×type check | Database CHECK constraint / trigger | Service check gives better error messages (line index for batch); DB check is a secondary safety net |

**Installation:** No `npm install` needed — all dependencies already in `api/pom.xml`. Build with `./mvnw verify`.

**Version verification:** All library versions are managed by Spring Boot 3.5.16 BOM (verified in `api/pom.xml`). Spring Boot's BOM pins transitive versions; individual library versions do not need separate `npm view`/`pip` verification since they come from the project's own build file [VERIFIED: codebase pom.xml].

## Package Legitimacy Audit

**No new packages to install** — all dependencies already exist in `api/pom.xml` and the project builds successfully. No slopcheck run required. If the planner later decides to add a new dependency (e.g., a BigDecimal validation helper), each package must pass the Package Legitimacy Gate before being added to this section.

## Architecture Patterns

### System Architecture Diagram

```
Client (Next.js web on Vercel)
     │
     │ HTTP /api/categorias, /api/lancamentos (Bearer JWT)
     ▼
┌─────────────────────────────────────────┐
│  Spring Boot API (Render)               │
│  ┌─────────────┐  ┌─────────────────┐  │
│  │  Controller │  │  ProblemDetail  │  │
│  │  (thin)     │  │  ExceptionHandler │  │
│  └──────┬──────┘  │  (RFC 7807)     │  │
│         │         └─────────────────┘  │
│         ▼                              │
│  ┌─────────────┐                       │
│  │  Service    │◄── TenantContext    │  │
│  │  (@Valid,   │     (empresa_id     │  │
│  │  @Transactional)│  from JWT)      │  │
│  └──────┬──────┘                       │
│         │                              │
│         ▼                              │
│  ┌─────────────┐                       │
│  │  Repository │                       │
│  │  (JPA)      │                       │
│  └──────┬──────┘                       │
│         │                              │
│         ▼                              │
│  ┌─────────────┐                       │
│  │  Flyway     │                       │
│  │  (V1, V1_1, │                       │
│  │   V2 Java)  │                       │
│  └──────┬──────┘                       │
│         │                              │
│         ▼                              │
│  PostgreSQL (Render, Testcontainers in  │
│   tests)                                │
└─────────────────────────────────────────┘
```

Data flow for the primary use case (criar lançamento):
1. Client sends `POST /api/lancamentos` with JSON body + Bearer JWT
2. SecurityFilterChain validates JWT, extracts `empresa_id` and `role` claims → `UsuarioAuth` principal → `SecurityContext`
3. Controller receives `@Valid` DTO; Bean Validation runs (field-level constraints)
4. Service calls `TenantContext.getEmpresaId()` (from SecurityContext, never body)
5. Service validates: valor > 0, scale ≤ 2, category×type match, category exists & active & same empresa
6. Repository saves `Lancamento` entity within `@Transactional` boundary
7. Controller returns `201 Created` with ProblemDetail on error

### Recommended Project Structure

```
api/src/main/java/com/controlefinanceiro/
├── categoria/                 # Nova — Categoria entity, repository, service, controller, DTOs
│   ├── Categoria.java
│   ├── CategoriaRepository.java
│   ├── CategoriaService.java
│   ├── CategoriaController.java
│   ├── CategoriaRequest.java  (DTO de entrada)
│   └── CategoriaResponse.java (DTO de saída)
├── lancamento/                # Novo — Lancamento entity, repository, service, controller, DTOs
│   ├── Lancamento.java
│   ├── LancamentoRepository.java
│   ├── LancamentoService.java
│   ├── LancamentoController.java
│   ├── LancamentoRequest.java (DTO de entrada, lote e unitário)
│   ├── LancamentoResponse.java (DTO de saída)
│   └── LancamentoLoteRequest.java (DTO do lote com lista)
├── common/
│   ├── TenantContext.java     (reutilizado — empresa_id do token)
│   ├── NaoAutorizadoException.java (reutilizado)
│   ├── MonetarioException.java (NOVO — exceção de validação monetária com índice da linha)
│   └── CategoriaTipoIncompativelException.java (NOVO — integridade categoria×tipo)
├── config/
│   ├── ProblemDetailExceptionHandler.java (reutilizado — adicionar handlers para novas exceções)
│   └── SecurityConfig.java    (reutilizado — adicionar regras de leitura para USER)
└── db/migration/
    └── V2__seed_categorias.java (NOVO — Java migration seguindo padrão V1_1)

api/src/test/java/com/controlefinanceiro/
├── CategoriaIntegrationTest.java (NOVO — Testcontainers, CRUD + permissões)
├── LancamentoIntegrationTest.java (NOVO — Testcontainers, CRUD + validação monetária)
├ LancamentoLoteIntegrationTest.java (NOVO — Testcontainers, lote tudo-ou-nada, índice da linha)
└── V2SeedMigrationTest.java (NOVO — Testcontainers, seed V2 idempotente)
```

### Pattern 1: Thin Controller + Business Service + JPA Repository

**What:** The controller only handles HTTP concerns (deserialization, status codes, routing) and delegates all business logic to the service. The service handles transactions, tenant resolution, monetary validation, and business rules. The repository only performs data access.

**When to use:** All write and read operations for categorias and lançamentos — this is the established Phase 1 pattern (`AuthController`/`AuthService`/`UsuarioRepository`).

**Example:**
```java
// Source: api/src/main/java/com/controlefinanceiro/auth/AuthController.java (existing pattern)
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

  private final CategoriaService categoriaService;

  public CategoriaController(CategoriaService categoriaService) {
    this.categoriaService = categoriaService;
  }

  // ADMIN only — SecurityConfig already has /api/categorias/** → hasRole("ADMIN")
  @PostMapping
  public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
    var empresaId = TenantContext.getEmpresaId(); // do token, nunca do body
    return ResponseEntity.ok(categoriaService.criar(empresaId, request));
  }
}
```

```java
// Source: pattern from AuthService (existing) + new monetary validation
@Service
public class LancamentoService {

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

  @Transactional
  public void processarLote(Long empresaId, LancamentoLoteRequest lote) {
    // LOT-01/LOT-02: tudo-ou-nada — se qualquer linha falhar, rollback total
    // exceção carrega o índice da linha inválida
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
}
```

### Anti-Patterns to Avoid

- **Hardcoding `empresa_id`**: Never read tenant from body/header/query — always from `TenantContext.getEmpresaId()` (R3/AUTH-03). Phase 1 already enforces this via `UsuarioAuth` principal.
- **Using `double`/`float` for money**: Violates LAN-05. `BigDecimal` everywhere; `NUMERIC(14,2)` in DB.
- **Rounding instead of rejecting >2 decimals**: PostgreSQL `NUMERIC(14,2)` silently rounds on insert. Must reject at application layer BEFORE persistence (LAN-04).
- **Second `SecurityConfig` or manual JWT filter**: Phase 1 established one `SecurityConfig` with `oauth2ResourceServer` (Nimbus). Anti-pattern 2 blocks hand-rolled filters.
- **Mutable Flyway migrations**: Corrections only via new `V<n>__` version (INF-01). V2 seed is additive; V1/V1_1 never change.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Monetary validation (scale, rounding) | Custom arithmetic with `double`/`float` | `BigDecimal` + `@Digits` or custom constraint validator | Avoids floating-point precision errors; LAN-05 forbids `double`/`float` |
| JWT parsing/validation | Manual filter with `io.jsonwebtoken` | Spring OAuth2 Resource Server (Nimbus) via `SecurityConfig` | Phase 1 pattern; handles exp, alg, clock skew correctly |
| Error responses | Custom `ResponseEntity` error bodies | `ProblemDetailExceptionHandler` (RFC 7807) | Standardized error format; Phase 1 pattern |
| Transaction batch rollback | Manual `try/catch` with delete-compensation | `@Transactional` on service method | Automatic rollback on exception; simpler and correct |
| Test database | Embedded H2 or manual PostgreSQL install | Testcontainers `PostgreSQLContainer` | Phase 1 pattern; real PostgreSQL, same as production |

**Key insight:** Every deceptively complex problem in this phase (monetary precision, JWT validation, error formatting, batch rollback) already has a standard library solution in the existing stack. Hand-rolling introduces subtle bugs that directly violate the phase's core value (valores corretos ao centavo).

## Common Pitfalls

### Pitfall 1: PostgreSQL NUMERIC(14,2) silently rounds >2 decimal places
**What goes wrong:** Inserting `10.123` into `NUMERIC(14,2)` results in `10.12` stored — the value is rounded, not rejected. LAN-04 requires rejection.
**Why it happens:** PostgreSQL's numeric type rounds to scale on insert; it does not validate input precision at the SQL level.
**How to avoid:** Validate `BigDecimal.scale()` / use `@Digits(integer=12, fraction=2)` in the DTO *before* saving to the database. Reject (throw exception) rather than round.
**Warning signs:** A test inserts `10.125` and the DB returns `10.13` instead of a 400 error.

### Pitfall 2: Reading `empresa_id` from request body
**What goes wrong:** A malicious or buggy client could set `empresa_id` in the JSON body, causing cross-tenant data access.
**Why it happens:** Convenience — passing tenant through the body seems simpler than extracting from JWT.
**How to avoid:** `TenantContext.getEmpresaId()` is the single source of truth (reads from `SecurityContext`/`UsuarioAuth`). DTOs must NOT contain `empresa_id` fields (R3/AUTH-03). Phase 1's `LoginRequestEstruturaTest` enforces this for login DTOs; same gate should apply to `LancamentoRequest` and `CategoriaRequest`.
**Warning signs:** Any DTO field named `empresaId`, `empresa_id`, or `empresa`.

### Pitfall 3: Batch partial success instead of all-or-nothing
**What goes wrong:** Processing 10 lines where line 7 fails leaves lines 1–6 persisted, violating LOT-01/LOT-02.
**Why it happens:** Processing each line in its own transaction, or catching exceptions per-line without rolling back the outer transaction.
**How to avoid:** `@Transactional` on the batch service method. If any line throws, the entire transaction rolls back. The exception must carry the line index for the ProblemDetail response.
**Warning signs:** Integration test shows some rows inserted after a batch error.

### Pitfall 4: Flyway V2 seed not idempotent
**What goes wrong:** Re-applying `V2__seed_categorias.java` (e.g., after a failed deploy that partially ran) causes duplicate key errors on `UNIQUE (empresa_id, nome, tipo)`.
**Why it happens:** Naive `INSERT` without checking existence.
**How to avoid:** Follow D-03 — check if category exists before inserting. Flyway Java migrations don't have checksum-based change detection, so idempotency must be manual (note: migrations are immutable per INF-01).
**Warning signs:** `DuplicateKeyException` or `PSQLException` on re-run of Flyway migration.

### Pitfall 5: Missing `@Transactional` on write operations
**What goes wrong:** Partial persistence when an exception occurs mid-operation (e.g., after validating but before saving related entities).
**Why it happens:** Service methods without `@Transactional` rely on default JPA behavior which may not roll back cleanly.
**How to avoid:** Annotate all service write methods (criar, editar, excluir, processarLote) with `@Transactional`.
**Warning signs:** Data inconsistency after error; integration test fails on rollback assertion.

## Code Examples

### Existing: Flyway Java migration pattern (V1_1) — to copy for V2
```java
// Source: api/src/main/java/com/controlefinanceiro/db/migration/V1_1__seed_empresa_admin.java
package com.controlefinanceiro.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1_1__seed_empresa_admin extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    // raw JDBC, sem DI Spring; System.getenv para variáveis de ambiente
    // NÃO fechar a connection do Context (regra Flyway)
  }
}
```

### Existing: Testcontainers integration test pattern
```java
// Source: api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private TestRestTemplate rest;

  @Test
  void exemplo() {
    // Flyway roda automaticamente no startup; banco real do Testcontainers
  }
}
```

### Planned: BigDecimal validation rejecting >2 decimal places
```java
// Source: padrão Spring Boot Validation (jakarta.validation) — a confirmar onde aplicar (DTO vs service)
// Opção A: DTO bean validation (rejeita no controller com 400)
public record LancamentoRequest(
    @NotNull @Digits(integer = 12, fraction = 2) BigDecimal valor,
    // ... outros campos
) {}

// Opção B: Service-level validação explícita (controle total da mensagem)
public void validarCasasDecimais(BigDecimal valor) {
  if (valor.scale() > 2) {
    throw new ValorInvalidoException("Valor não pode ter mais de 2 casas decimais");
  }
  // HALF_EVEN aplicado APENAS após confirmação de ≤ 2 casas
  BigDecimal normalizado = valor.setScale(2, RoundingMode.HALF_EVEN);
}
```

### Planned: `@Transactional` batch with line index
```java
// Source: padrão Spring @Transactional — a confirmar implementação detalhada no planejamento
@Transactional
public void processarLote(Long empresaId, List<LancamentoLinha> linhas) {
  if (linhas.size() > 500) {
    throw new LoteDemasiadoGrandeException("Limite de 500 linhas excedido");
  }
  for (int i = 0; i < linhas.size(); i++) {
    try {
      criarLinha(empresaId, linhas.get(i));
    } catch (Exception ex) {
      throw new LinhaInvalidaException(i, ex.getMessage()); // índice da linha
    }
  }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `double`/`float` for money | `BigDecimal` + `NUMERIC(14,2)` | Project constraint from start | Prevents centavo-level errors |
| SQL-only Flyway migrations | Java migrations (`BaseJavaMigration`) | Phase 1 (V1_1) | Consistency; env var access via `System.getenv` |
| Manual JWT filter | Spring OAuth2 Resource Server (Nimbus) | Phase 1 | Correct exp/alg/clock skew handling |
| Ad-hoc error responses | RFC 7807 `ProblemDetail` | Phase 1 | Standardized, machine-readable errors |

**Deprecated/outdated:** None within this project's scope — all patterns are current for Spring Boot 3.5.x / Java 21.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `BigDecimal.scale() > 2` check is sufficient to detect >2 decimal places (vs. `@Digits`) | Standard Stack / Code Examples | Validation may let through values like `10.00` (scale 2) which is fine, but `10.100` (scale 3) correctly rejected — need to confirm if `10.10` (scale 2) should pass |
| A2 | `@Digits(integer=12, fraction=2)` on BigDecimal works in Hibernate Validator 8.x (Spring Boot 3.5) | Standard Stack | If `@Digits` doesn't work as expected on BigDecimal, validation layer fails silently |
| A3 | `NUMERIC(14,2)` CHECK (valor > 0) at DB level is sufficient for LAN-07 positivity | Architecture Patterns | DB check is a safety net; app-level check still needed for clear error messages |
| A4 | `RoundingMode.HALF_EVEN` applied with `setScale(2, HALF_EVEN)` after validation | Code Examples | HALF_EVEN is banker's rounding — client may expect different rounding behavior |

**These claims are tagged `[ASSUMED]` inline where they appear.** The planner should gate these behind `checkpoint:human-verify` tasks before implementation if the user needs confirmation.

## Open Questions (RESOLVED)

1. **Formato do lote tudo-ou-nada:** array JSON único vs JSONL? — **RESOLVED** (Plan 02-04): array JSON wrapper (`LancamentoLoteRequest` com `List<LancamentoLinha>`), índice da linha = posição no array.
   - What we know: LOT-01 says "lote de até 500 lançamentos", LOT-02 says "índice da linha inválida". CONTEXT.md marks this as the agent's discretion.
   - What's unclear: Whether the client sends `[{...}, {...}]` or JSONL lines; how the index maps to array position vs JSONL line number.
   - Recommendation: Use array JSON (`List<LancamentoLinha>` in a wrapper DTO) — simpler for web clients, index = array position. Confirm with user.

2. **Onde rejeitar valor >2 casas:** DTO bean validation vs service em BigDecimal? — **RESOLVED** (Plan 02-03): ambos — `@Digits(integer=12, fraction=2)` no DTO para rejeição precoce no controller + checagem explícita `scale() > 2` no service como defesa-em-profundidade.
   - What we know: LAN-04 requires rejection (not rounding). CONTEXT.md marks as open.
   - What's unclear: Whether `@Digits` on the DTO (400 from controller) or explicit `scale()` check in service (400 from ProblemDetail handler) is preferred.
   - Recommendation: Do both — `@Digits` for early rejection at controller layer, plus explicit service check as defense-in-depth. Confirm with user.

3. **Integridade categoria×tipo:** service check vs banco? — **RESOLVED** (Plans 02-02, 02-03): checagem no service com `CategoriaTipoIncompativelException`; banco como rede de segurança secundária opcional.
   - What we know: CAT-04 requires ENTRADA category to reject SAÍDA lancamento and vice-versa. CONTEXT.md marks as open.
   - What's unclear: Whether to enforce purely in Java service (clearer error messages, can include category name) or also add DB-level CHECK/trigger.
   - Recommendation: Service-level check (returns meaningful ProblemDetail); DB check as optional secondary safety net. Confirm with user.

4. **Comportamento de lançamentos históricos quando categoria é desativada (ativo=false):** — **RESOLVED** (Plan 02-02 Task 2): `ativo=false` bloqueia novos lançamentos na categoria; lançamentos históricos permanecem visíveis.
   - What we know: CAT-05 says ADMIN can "desativar" categories. Schema has `ativo BOOLEAN NOT NULL DEFAULT true`.
   - What's unclear: Should desativar a categoria bloquear novos lançamentos? Should historical lançamentos remain visible? Should desativar cascade?
   - Recommendation: Desativar blocks new lançamentos (service checks `ativo=true`); historical lançamentos remain readable. Confirm with user.

5. **USER role permissions for categorias:** — **RESOLVED** (Plan 02-02 Task 2): `GET /api/categorias` aberto a todos os usuários autenticados; escrita (POST/PUT/DELETE) restrita a ADMIN.
   - Recommendation: Split security rules — `GET /api/categorias` for all authenticated users, `POST/PUT/DELETE` for ADMIN only. Confirm with user.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 (JDK) | Build and run API | ✓ | (from `./mvnw` wrapper) | — |
| Maven (`./mvnw`) | Build, test (`verify`) | ✓ | (project wrapper) | — |
| Docker | Testcontainers PostgreSQL | Needs verification | — | Tests fail without Docker |
| PostgreSQL 16 | Testcontainers image | Pulled by Testcontainers | postgres:16-alpine | — |
| Docker daemon | `postgres:16-alpine` container | Needs verification | — | Integration tests cannot run |

**Missing dependencies with no fallback:**
- Docker (or Docker Desktop) — required for Testcontainers PostgreSQL; `./mvnw verify` will fail on integration tests without it

**Missing dependencies with fallback:**
- None identified

## Security Domain

> `security_enforcement` is not set to `false` in `.planning/config.json` (absent = enabled). Section included.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | JWT via Spring OAuth2 Resource Server (Nimbus), HS256, 32-byte+ secret |
| V3 Session Management | yes | Stateless JWT; no server-side sessions; 8h expiry (D-01) |
| V4 Access Control | yes | Role-based (`@PreAuthorize`/`hasRole("ADMIN")`), tenant isolation via `TenantContext` |
| V5 Input Validation | yes | Jakarta Bean Validation (`@Valid`, `@Digits`), `fail-on-unknown-properties: true` |
| V6 Cryptography | yes | BCrypt (custo 12) for password hashing; no custom crypto |

### Known Threat Patterns for Java/Spring Boot Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Mass assignment (extra fields in JSON body) | Tampering | `fail-on-unknown-properties: true` + DTOs with only allowed fields |
| SQL injection | Tampering | Spring Data JPA parameterized queries (no string concatenation) |
| Tenant escalation via body | Elevation of Privilege | `empresa_id` exclusively from JWT via `TenantContext`; DTOs forbid `empresa_id` field |
| Floating-point precision loss | Tampering | `BigDecimal` only; no `double`/`float` (LAN-05) |
| Batch partial persistence | Tampering | `@Transactional` all-or-nothing (LOT-01/LOT-02) |

## Sources

### Primary (HIGH confidence)
- `api/pom.xml` — dependency versions, Spring Boot 3.5.16 parent, all stack libraries [VERIFIED: codebase]
- `api/src/main/resources/db/migration/V1__schema.sql` — `categoria` and `lancamento` table DDL, `NUMERIC(14,2)`, `CHECK (valor > 0)`, `UNIQUE (empresa_id, nome, tipo)` [VERIFIED: codebase]
- `api/src/main/java/com/controlefinanceiro/db/migration/V1_1__seed_empresa_admin.java` — Flyway Java migration pattern (BaseJavaMigration, raw JDBC, System.getenv) [VERIFIED: codebase]
- `api/src/main/java/com/controlefinanceiro/common/TenantContext.java` — single tenant access point from SecurityContext [VERIFIED: codebase]
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` — RFC 7807 ProblemDetail error handling [VERIFIED: codebase]
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java` — stateless security, oauth2ResourceServer, `/api/categorias/**` → hasRole("ADMIN") [VERIFIED: codebase]
- `api/src/main/java/com/controlefinanceiro/auth/AuthController.java` + `AuthService.java` — thin controller + service pattern [VERIFIED: codebase]
- `api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java` — Testcontainers integration test pattern [VERIFIED: codebase]
- `AGENTS.md` — project constraints (BigDecimal, tenant, stack, no double/float) [VERIFIED: codebase]
- `.planning/REQUIREMENTS.md` — CAT-01..06, LAN-01..07, LOT-01..02, INF-02 requirement definitions [VERIFIED: codebase]
- `.planning/phases/02-categorias-e-lan-amentos/02-CONTEXT.md` — D-01..D-04 locked decisions, open areas, deferred ideas [VERIFIED: codebase]
- `.planning/ROADMAP.md` — Phase 2 success criteria [VERIFIED: codebase]

### Secondary (MEDIUM confidence)
- None — all critical claims verified against codebase

### Tertiary (LOW confidence)
- `@Digits(integer=12, fraction=2)` behavior on `BigDecimal` in Hibernate Validator 8.x — not verified against official docs this session [ASSUMED]
- `BigDecimal.scale() > 2` as the sole check for >2 decimal places — not verified against official docs this session [ASSUMED]

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all dependencies verified in `api/pom.xml` and existing codebase
- Architecture: HIGH — established Phase 1 patterns (controller/service/repository, TenantContext, ProblemDetail, Flyway Java migration, Testcontainers)
- Pitfalls: MEDIUM — based on known Spring/Java pitfalls and codebase analysis; some not yet observed in this project

**Research date:** 2026-11-08
**Valid until:** 2026-12-08 (30 days — stable stack, no fast-moving dependencies)