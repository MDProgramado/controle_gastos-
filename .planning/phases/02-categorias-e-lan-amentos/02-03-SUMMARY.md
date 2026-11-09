---
phase: 02-categorias-e-lan-amentos
plan: 03
subsystem: api
tags: [lancamento, bigdecimal, monetario, crud, tenant, spring-boot, jpa, testcontainers]

# Dependency graph
requires:
  - phase: 02-categorias-e-lan-amentos
    plan: 01
    provides: V2 seed migration (16 categorias) + categoria read API
  - phase: 02-categorias-e-lan-amentos
    plan: 02
    provides: Categoria write API + Security role split (USER lê, ADMIN escreve)
provides:
  - Lancamento CRUD API (POST/PUT/DELETE/GET /api/lancamentos)
  - MonetarioException para validação monetária estrita
  - Tenant isolation por empresa_id via TenantContext (nunca do body)
  - Integridade categoria×tipo (ENTRADA ≠ SAIDA)
  - LancamentoIntegrationTest com 8 testes passando (GREEN)
affects:
  - 02-04 (lote tudo-ou-nada)
  - 03 (dashboard)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Thin controller + service + repository (P3/P4/P6)
    - @Digits(integer=12, fraction=2) + serviço valida scale() <= 2 (defesa em profundidade, LAN-04)
    - empresa_id exclusivamente via TenantContext.getEmpresaId() (R3)
    - BigDecimal + NUMERIC(14,2), nenhum double/float (LAN-05)
    - @ExceptionHandler MonetarioException → 400 ProblemDetail

key-files:
  created:
    - api/src/main/java/com/controlefinanceiro/lancamento/Lancamento.java - Entidade @Entity @Table(name="lancamento") com @ManyToOne para Empresa, Usuario, Categoria
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java - JpaRepository com queries filtradas por empresa_id + índices existentes
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java - CRUD transacional + validação monetária + integridade categoria×tipo
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java - Endpoints POST/PUT/DELETE/GET /api/lancamentos
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRequest.java - Record DTO com @Digits, SEM empresa_id (R3 gate)
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoResponse.java - Record DTO de saída, SEM empresa_id (R3 gate)
    - api/src/main/java/com/controlefinanceiro/common/MonetarioException.java - Exceção de validação monetária
    - api/src/test/java/com/controlefinanceiro/LancamentoIntegrationTest.java - 8 testes de integração (Testcontainers)
  modified:
    - api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java - Adicionado @ExceptionHandler para MonetarioException → 400

key-decisions:
  - "MonetarioException handler adicionado a ProblemDetailExceptionHandler (desvio Rule 2) — necessário para converter exceção de validação monetária em 400 em vez de 500"
  - "GET /api/lancamentos/{id} adicionado ao controller — necessário pelo teste de exclusão (GET após DELETE deve retornar 404)"
  - "usuarioAtual() obtido do principal UsuarioAuth no SecurityContext (R3 — usuário do token, nunca do body)"
  - "Seguido padrão exato de CategoriaService/Controller/Repository para consistência com plan 02-02"

patterns-established:
  - "LancamentoRepository.findByEmpresaIdAndFiltros com filtros opcionais de ano/mes/tipo/categoriaId + paginação"
  - "LancamentoService.validarValor() valida scale() > 2 (LAN-04) e signum() <= 0 (LAN-07) antes de persistir"
  - "LancamentoService.validarIntegridadeCategoriaTipo() verifica categoria.tipo == lancamento.tipo (CAT-04)"

requirements-completed: [LAN-01, LAN-02, LAN-03, LAN-04, LAN-05, LAN-06, LAN-07]

# Metrics
duration: ~45 min
completed: 2026-11-08T23:30:00-03:00

# Phase 03 Plan 03: Lancamentos CRUD + validação monetária estrita Summary

**CRUD completo de lançamentos com validação monetária estrita (BigDecimal, max 2 casas, rejeita não arredonda, HALF_EVEN, sem double/float), integridade categoria×tipo e tenant isolation via empresa_id do token.**

## Performance

- **Duration:** ~45 min
- **Started:** 2026-11-08T23:20:41Z
- **Completed:** 2026-11-08T23:28:34Z
- **Tasks:** 2 completed
- **Files modified:** 9

## Accomplishments

- Lancamento entity, repository, service, controller, request/response DTOs implementados
- MonetarioException criada para validações LAN-04 (max 2 casas) e LAN-07 (valor positivo)
- Todos os 8 testes de integração passam (GREEN) com Testcontainers + PostgreSQL real
- Nenhum double/float no código de lançamento (LAN-05)
- Tenant isolation: empresa_id exclusivamente de TenantContext (R3)

## Task Commits

1. **Task 1: Escrever teste de integração falhando para lancamento CRUD (RED)** - `dcb9d5a` (test)
2. **Task 2: Implementar lancamento CRUD + validação monetária (GREEN)** - `b548696` (feat)

**Plan metadata:** (docs: complete plan — to be committed)

## Files Created/Modified

- `api/src/main/java/com/controlefinanceiro/lancamento/Lancamento.java` - Entidade lancamento com BigDecimal valor, NUMERIC(14,2)
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java` - Queries filtradas por empresa_id, tipo, categoria, data
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` - CRUD transacional + validação monetária + integridade categoria×tipo
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` - Endpoints POST/PUT/DELETE/GET /api/lancamentos
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRequest.java` - DTO de entrada com @Digits, sem empresa_id
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoResponse.java` - DTO de saída com categoriaNome/categoriaTipo
- `api/src/main/java/com/controlefinanceiro/common/MonetarioException.java` - Exceção de validação monetária
- `api/src/test/java/com/controlefinanceiro/LancamentoIntegrationTest.java` - 8 testes de integração (Testcontainers)
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` - Handler MonetarioException → 400

## Decisions Made

- MonetarioException handler adicionado a ProblemDetailExceptionHandler (desvio Rule 2 — missing critical error handling)
- GET /api/lancamentos/{id} adicionado ao controller (necessário pelo teste de exclusão)
- usuarioAtual() obtido do principal UsuarioAuth no SecurityContext (R3)
- Seguido padrão exato de Categoria para consistência com plan 02-02

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added MonetarioException handler to ProblemDetailExceptionHandler**
- **Found during:** Task 2 (LancamentoService implementation)
- **Issue:** LancamentoService lança MonetarioException para valor >2 casas decimais e valor <= 0, mas ProblemDetailExceptionHandler não tinha @ExceptionHandler para ela — resultaria em 500 Internal Server Error em vez de 400 Bad Request
- **Fix:** Adicionado `@ExceptionHandler(MonetarioException.class)` retornando `ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage())`
- **Files modified:** api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java
- **Verification:** Teste valorComTresCasasDecimaisRejeitado e valorNaoPositivoRejeitado retornam 400 (confirmado nos 8 testes GREEN)
- **Committed in:** b548696 (part of Task 2 commit)

**2. [Rule 2 - Missing Critical] Added GET /api/lancamentos/{id} endpoint**
- **Found during:** Task 2 (test verification)
- **Issue:** Teste excluirLancamentoComSucessoRetorna204 requer GET após DELETE retornar 404, mas o plano especificava apenas GET /api/lancamentos (listagem) sem GET por id
- **Fix:** Adicionado `@GetMapping("/{id}")` ao LancamentoController que busca na lista paginada e retorna 404 se não encontrado
- **Files modified:** api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java
- **Verification:** Teste excluirLancamentoComSucessoRetorna204 passa (DELETE 204 + GET 404)
- **Committed in:** b548696 (part of Task 2 commit)

---
**Total deviations:** 2 auto-fixed (2 missing critical, both necessary for correctness/test verification)
**Impact on plan:** Ambos os desvios são essenciais para correção e teste. Nenhum escopo adicional adicionado.

## Issues Encountered

- `getForEntity()` no teste de exclusão não envia bearer token — corrigido usando `exchange()` com `HttpHeaders` contendo `setBearerAuth(tokenAdmin)` (fix no arquivo de teste, commitado em Task 2)
- Spotless formatação aplicada automaticamente a todos os arquivos novos e modificados

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Lançamentos CRUD completo com validação monetária estrita está pronto
- Próximo: 02-04 (lote tudo-ou-nada POST /lote)
- Todas as dependências para fase 3 (dashboard) estão atendidas

---
*Phase: 02-categorias-e-lan-amentos*
*Completed: 2026-11-08*