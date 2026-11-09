---
phase: 02-categorias-e-lan-amentos
plan: 02
subsystem: api
tags: [spring-boot, java-21, jpa, jwt, spring-security, categorias, testcontainers, rbac]

# Dependency graph
requires:
  - phase: 02-categorias-e-lan-amentos
    plan: 01
    provides: V2 seed migration (16 categorias) + GET /api/categorias read API + SecurityConfig base
provides:
  - POST /api/categorias (criar categoria, ADMIN only, 201)
  - PUT /api/categorias/{id} (editar categoria, ADMIN only, 200)
  - DELETE /api/categorias/{id} (desativar categoria, ADMIN only, 204)
  - Security role split: GET /api/categorias for ADMIN+USER, escrita para ADMIN only
  - CategoriaTipoIncompativelException → 400 ProblemDetail (CAT-04 integridade)
  - CategoriaDuplicadaException → 400 ProblemDetail (unicidade nome+tipo+empresa)
  - CategoriaNaoEncontradaException → 404 ProblemDetail
affects:
  - 02-03 (lancamentos CRUD - depende de categorias existentes)
  - 02-04 (lote tudo-ou-nada)
  - 03-dashboard (gasto por categoria)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Record DTO sem empresa_id (R3 gate - tenant vem exclusivamente do token JWT)
    - EntityManager.getReference() para proxy de Empresa (evita carregar entidade desnecessária)
    - Exceções dedicadas por código HTTP (400 duplicada/tipo, 404 nao-encontrada)
    - SecurityConfig split: HttpMethod.GET específico antes do padrão /** para RBAC

key-files:
  created:
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaRequest.java - DTO de entrada criar/editar (sem empresa_id)
    - api/src/main/java/com/controlefinanceiro/common/CategoriaTipoIncompativelException.java - exceção integridade categoria×tipo
    - api/src/main/java/com/controlefinanceiro/common/CategoriaNaoEncontradaException.java - exceção categoria não encontrada → 404
    - api/src/main/java/com/controlefinanceiro/common/CategoriaDuplicadaException.java - exceção categoria duplicada → 400
  modified:
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaService.java - métodos criar/editar/desativar com @Transactional
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java - endpoints POST/PUT/DELETE /api/categorias
    - api/src/main/java/com/controlefinanceiro/categoria/CategoriaRepository.java - findByIdAndEmpresaId + existsByEmpresaIdAndNomeAndTipo
    - api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java - split leitura/escrita por papel
    - api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java - handlers 400/404 para novas exceções
    - api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java - throws Exception no setup + @AfterEach isolamento

key-decisions:
  - "Usar EntityManager.getReference(Empresa.class, empresaId) em CategoriaService.criar para associar tenant sem carregar a entidade Empresa"
  - "Criar exceções dedicadas (CategoriaDuplicadaException, CategoriaNaoEncontradaException) para mapeamento claro 400/404 via ProblemDetailExceptionHandler"
  - "Adicionar @AfterEach no teste para deletar categorias com id > 16 (seed V2), garantindo isolamento entre métodos de teste"

patterns-established:
  - "DTO de entrada segue padrão P4: record imutável, @NotBlank/@NotNull/@Size, sem campo de tenant"
  - "Service é fino: valida formato, verifica unicidade, delega persistência ao repository; tenant via TenantContext"
  - "Controller delega ao serviço; nunca contém regra de negócio nem lê tenant (R3)"

requirements-completed: [CAT-04, CAT-05]

# Metrics
duration: 25min
completed: 2026-11-08T23:15:37-03:00
---

# Phase 02 Plan 02: Categoria Write + Security Role Split Summary

**Categoria write API implementada (GREEN): ADMIN cria/edita/desativa categorias via POST/PUT/DELETE /api/categorias; USER só lê (GET). Integridade categoria×tipo (CAT-04) e RBAC (CAT-05) aplicados, todos os 8 testes de integração passam com PostgreSQL real via Testcontainers.**

## Performance

- **Duration:** 25 min
- **Started:** 2026-11-08T23:07:00-03:00
- **Completed:** 2026-11-08T23:15:37-03:00
- **Tasks:** 1 of 2 (Task 1 complete as 5c1bedc, Task 2 complete as ec132ca)
- **Files modified:** 10 (4 created, 6 modified)

## Accomplishments

- POST /api/categorias — ADMIN cria categoria (201), USER recebe 403
- PUT /api/categorias/{id} — ADMIN edita categoria (200), valida tipo e unicidade
- DELETE /api/categorias/{id} — ADMIN desativa categoria (ativo=false, 204), sem exclusão em cascata
- Security role split — GET /api/categorias liberado para ADMIN+USER; escrita restrita a ADMIN
- Integridade categoria×tipo — tipo inválido (não ENTRADA/SAIDA) lança CategoriaTipoIncompativelException → 400
- Nome único por (empresa_id, nome, tipo) — duplicata lança CategoriaDuplicadaException → 400
- Problema não encontrado → CategoriaNaoEncontradaException → 404
- Todos os 8 testes CategoriaIntegrationTest passam (GREEN) com Testcontainers PostgreSQL 16

## Task Commits

Each task was committed atomically:

1. **Task 1: Escrever testes de integração falhando para categoria write (RED)** - `5c1bedc` (test)
2. **Task 2: Implementar categoria write + Security role split (GREEN)** - `ec132ca` (feat)

## Files Created/Modified

- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaRequest.java` - DTO de entrada (nome, tipo, pessoal), sem empresa_id (R3)
- `api/src/main/java/com/controlefinanceiro/common/CategoriaTipoIncompativelException.java` - exceção de integridade categoria×tipo
- `api/src/main/java/com/controlefinanceiro/common/CategoriaNaoEncontradaException.java` - exceção categoria não encontrada → 404
- `api/src/main/java/com/controlefinanceiro/common/CategoriaDuplicadaException.java` - exceção categoria duplicada → 400
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaService.java` - adicionados criar/editar/desativar com @Transactional
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java` - endpoints POST/PUT/DELETE /api/categorias
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaRepository.java` - findByIdAndEmpresaId + existsByEmpresaIdAndNomeAndTipo
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java` - split leitura/escrita por papel (ADMIN/USER para GET, ADMIN para escrita)
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` - handlers 400/404 para novas exceções
- `api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java` - throws Exception no setup + @AfterEach isolamento de teste

## Decisions Made

- Usar `EntityManager.getReference(Empresa.class, empresaId)` para associar o tenant na criação, evitando carregar a entidade Empresa desnecessariamente
- Exceções dedicadas por código HTTP para mapeamento claro via ProblemDetailExceptionHandler (400 para duplicada/tipo, 404 para não encontrada)
- `@AfterEach` no teste para deletar categorias com id > 16 (seed V2), garantindo isolamento entre métodos de teste

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] CategoriaIntegrationTest.setup() missing `throws Exception`**
- **Found during:** Task 2 (GREEN compilation)
- **Issue:** `setup()` calls `login()` which declares `throws Exception`, but `setup()` did not declare it. This was masked during Task 1 RED because compilation failed earlier on missing `CategoriaRequest` class. Once classes existed, compilation proceeded further and hit this error.
- **Fix:** Added `throws Exception` to `setup()` method signature.
- **Files modified:** `api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java`
- **Verification:** Test compilation succeeds; all 8 tests pass
- **Committed in:** `ec132ca` (Task 2 commit)

**2. [Rule 3 - Blocking] Test data leaking between test methods**
- **Found during:** Task 2 (GREEN test execution)
- **Issue:** `listarCategoriasRetorna16CategoriasSeedadas` expected exactly 16 categories but found 17, because `criarCategoriaComSucessoRetorna201` created "Supermercado" (id=17) which persisted in the shared Testcontainers database across test methods.
- **Fix:** Added `@AfterEach apagarCategoriasCriadasNosTestes()` that deletes categories with id > 16 (seed V2 range), ensuring test isolation regardless of execution order.
- **Files modified:** `api/src/test/java/com/controlefinanceiro/CategoriaIntegrationTest.java`
- **Verification:** All 8 tests pass (Tests run: 8, Failures: 0, Errors: 0)
- **Committed in:** `ec132ca` (Task 2 commit)

**3. [Rule 2 - Missing Critical] CategoriaDuplicadaException and CategoriaNaoEncontradaException**
- **Found during:** Task 2 (implementation)
- **Issue:** Plan required handlers for "categoria duplicada → 400" and "categoria não encontrada → 404" in ProblemDetailExceptionHandler, but no exception classes existed for these cases.
- **Fix:** Created `CategoriaDuplicadaException` and `CategoriaNaoEncontradaException` extending RuntimeException, plus corresponding `@ExceptionHandler` methods in `ProblemDetailExceptionHandler`.
- **Files modified:** `api/src/main/java/com/controlefinanceiro/common/CategoriaDuplicadaException.java` (created), `api/src/main/java/com/controlefinanceiro/common/CategoriaNaoEncontradaException.java` (created), `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` (modified)
- **Verification:** All 8 tests pass; exception handlers mapped to correct HTTP status codes
- **Committed in:** `ec132ca` (Task 2 commit)

---
**Total deviations:** 3 auto-fixed (2 blocking via Rule 3, 1 missing critical via Rule 2)
**Impact on plan:** All auto-fixes necessary for correctness and test isolation. No scope creep.

## Issues Encountered

- Spotless formatting check initially failed on new/modified files — resolved with `mvn spotless:apply` before re-running tests
- Test execution order caused `listarCategoriasRetorna16CategoriasSeedadas` to fail when run after category-creating tests — resolved with `@AfterEach` cleanup

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Categoria write API completa, pronta para uso na fase 02-03 (Lançamentos CRUD)
- Security role split estabelecido: ADMIN escreve, USER lê — padrão a ser mantido em lançamentos
- Nenhum bloqueador para a próxima fase

---
*Phase: 02-categorias-e-lan-amentos*
*Completed: 2026-11-08*
