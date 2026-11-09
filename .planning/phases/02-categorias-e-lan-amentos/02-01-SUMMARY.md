---
phase: 02-categorias-e-lan-amentos
plan: 01
subsystem: api/database
tags: [flyway, spring-boot, jpa, postgresql, testcontainers, categorias]

# Dependency graph
requires:
  - phase: 01-fundacao-e-auth-em-producao
    provides: V1 schema (categoria table), V1_1 seed (empresa + admin user), TenantContext, SecurityConfig, AuthController pattern
provides:
  - V2 Flyway Java migration seeding 16 categorias (12 SAIDA + 4 ENTRADA, 5 pessoais [P])
  - Categoria entity, repository, service, controller, response DTO
  - GET /api/categorias endpoint (tenant-filtered, R3 compliant)
  - CategoriaIntegrationTest (Testcontainers, PostgreSQL real)
affects: [02-02, 02-03, 02-04] — categoria CRUD writes, lancamento CRUD, lote tudo-ou-nada

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Flyway Java migration (BaseJavaMigration) following V1_1__seed_empresa_admin.java pattern
    - Thin controller + service + JPA repository (AuthController/AuthService/UsuarioRepository pattern)
    - Tenant isolation via TenantContext.getEmpresaId() from JWT SecurityContext (R3)
    - DTO record without empresa_id field (R3 gate)
    - Testcontainers integration test with @ServiceConnection PostgreSQLContainer

key-files:
  created:
    - api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java
    - api/src/main/java/com/controlefinanceiro/db/migration/V2__seed_categorias.java
    - api/src/main/java/com/controlefinanceiro/categoria/Categoria.java
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaRepository.java
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaService.java
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaResponse.java
  modified: []

key-decisions:
  - "Categoria entity omits criado_em field — V1 schema's categoria table has no such column (Rule 1 bug fix during implementation)"
  - "SecurityConfig already restricts /api/categorias/** to ADMIN role — no change needed for read endpoint"
  - "V2 migration uses raw JDBC PreparedStatement with parameters (T-02-01 mitigated)"

patterns-established:
  - "V2 seed migration: context.getConnection(), subquery for empresa_id, SELECT-before-INSERT idempotency, never close connection"
  - "Categoria listar(): service calls TenantContext.getEmpresaId(), repository.findByEmpresaId, maps to CategoriaResponse record"

requirements-completed: [CAT-01, CAT-02, CAT-03, CAT-06]

# Metrics
duration: 12min
completed: 2026-11-08

---

# Phase 02 Plan 01: Categorias e Lançamentos — Seed V2 + Categoria Read API Summary

**Flyway V2 Java migration seeds 16 categorias (12 SAÍDA + 4 ENTRADA, 5 pessoais [P]) and GET /api/categorias returns them tenant-filtered via JWT.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-11-08T22:41:00Z
- **Completed:** 2026-11-08T22:53:29Z
- **Tasks:** 2/2 completed
- **Files modified:** 7 created, 0 modified

## Accomplishments

- V2__seed_categorias.java applies 16 categorias idempotently via Flyway Java migration (D-01 to D-04)
- GET /api/categorias returns 200 with 16 categories (12 SAIDA, 4 ENTRADA), pessoal=true for [P] names
- Tenant isolation: empresa_id from JWT via TenantContext, never from body (R3); CategoriaResponse has no empresa_id field
- CategoriaIntegrationTest passes (GREEN): Testcontainers + PostgreSQL 16 real, 2 tests, 0 failures

## Task Commits

Each task was committed atomically:

1. **Task 1: Escrever teste de integração falhando para GET /api/categorias (RED)** - `a870db5` (test)
2. **Task 2: Implementar V2 seed migration + categoria read API (GREEN)** - `47352da` (feat)

## Files Created/Modified

- `api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java` — TDD RED test, then GREEN verification (Testcontainers, GET /api/categorias)
- `api/src/main/java/com/controlefinanceiro/db/migration/V2__seed_categorias.java` — Flyway V2 Java migration, 16 categorias idempotent seed
- `api/src/main/java/com/controlefinanceiro/categoria/Categoria.java` — JPA entity mapped to categoria table
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaRepository.java` — JpaRepository with findByEmpresaId query
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaService.java` — @Service, listar(empresaId) via TenantContext
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java` — @RestController GET /api/categorias
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaResponse.java` — Record DTO id/nome/tipo/pessoal/ativo, no empresa_id (R3 gate)

## Decisions Made

- Followed plan exactly as specified; no architectural changes needed
- V2 migration follows V1_1__seed_empresa_admin.java pattern (raw JDBC, context.getConnection(), subquery for empresa_id, idempotent SELECT before INSERT)
- Categoria entity omits criado_em — V1__schema.sql categoria table has no such column (Rule 1 fix during Task 2 implementation)
- SecurityConfig already has /api/categorias/** → hasRole("ADMIN"); no change needed for read endpoint

## Deviations from Plan

**1. [Rule 1 - Bug] Removed criado_em field from Categoria entity**
- **Found during:** Task 2 (GREEN implementation)
- **Issue:** Categoria entity mapped @Column(name="criado_em") but V1__schema.sql's categoria table has no created_em column — caused SQL error "column c1_0.criado_em does not exist" (500 on GET /api/categorias)
- **Fix:** Removed criadoEm field, @PrePersist method, OffsetDateTime import, and getter/setter from Categoria.java
- **Files modified:** api/src/main/java/com/controlefinanceiro/categoria/Categoria.java
- **Verification:** ./mvnw test -Dtest=CategoriaIntegrationTest passes (2 tests, 0 failures)
- **Committed in:** 47352da (Task 2 commit)

**Total deviations:** 1 auto-fixed (1 bug fix)
**Impact on plan:** Auto-fix was necessary for correctness — entity must match existing schema. No scope creep.

## Issues Encountered

- None beyond the Rule 1 auto-fix above (criado_em column mismatch)

## User Setup Required

None - no external service configuration required. Tests use Testcontainers with Docker (already available in environment).

## Next Phase Readiness

- Category seed and read API complete — foundation for categoria CRUD writes (02-02) and lancamento CRUD (02-03)
- All 16 categorias seedadas and available via GET /api/categorias for lancamento category validation
- Tenant isolation pattern established for all future endpoints

---
*Phase: 02-categorias-e-lan-amentos*
*Completed: 2026-11-08*
