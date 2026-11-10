---
phase: 03-dashboard
plan: gap-03
subsystem: api
tags: [java, spring-boot, pagination, total-count, truncation, lancamentos]

# Dependency graph
requires:
  - phase: 03-04
    provides: Página CRUD /lancamentos com getLancamentos e aviso de truncamento "Mostrando 50 de {N}"
provides:
  - GET /api/lancamentos?ano=&mes= retorna objeto paginado com `lancamentos` (array) e `total` (long)
  - total obtido de page.getTotalElements() (contagem real do banco)
  - getLancamentos em web/lib/api.ts lê total do corpo da resposta JSON
affects:
  - 04-ia (IA reusa consultas do dashboard; total real disponível para paginação futura)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Page<Lancamento> já retornada por findByEmpresaIdAndFiltros; page.getTotalElements() expõe contagem real
    - LancamentoPage record (List<LancamentoResponse> lancamentos, long total) como resposta paginada
    - Frontend lê total do corpo da resposta (confia no servidor, não computa localmente)

key-files:
  created: []
  modified:
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java
    - web/lib/api.ts

key-decisions:
  - "LancamentoPage record adicionado dentro de LancamentoService.java (não classe separada) para manter coesão"
  - "BuscarPorId continua chamando listar() com filtros nulos e itera sobre pagina.lancamentos()"
  - "Nenhuma mudança em LancamentoRepository (findByEmpresaIdAndFiltros já retorna Page com getTotalElements)"
  - "Empresa_id continua vindo exclusivamente de TenantContext.getEmpresaId() (sem parâmetro do client)"

requirements-completed: [DSH-01, D-22]

# Metrics
duration: 15min
completed: 2026-11-09T14:35:00Z

# Phase 3 Gap 03: WR-03 contagem real de lançamentos

**Corrigido aviso de truncamento "Mostrando 50 de {N}" que nunca disparava porque total era o tamanho da página (WR-03).**

## Performance

- **Duration:** 15 min
- **Started:** 2026-11-09T14:20:00Z
- **Completed:** 2026-11-09T14:35:00Z
- **Tasks:** 2/2 completed
- **Files modified:** 3

## Accomplishments

- api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java: LancamentoPage record (lancamentos, total) adicionado; método listar retorna LancamentoPage com total = page.getTotalElements()
- api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java: GET /api/lancamentos retorna ResponseEntity<LancamentoPage> com corpo { lancamentos: [...], total: N }; buscarPorId atualizado para pagina.lancamentos()
- web/lib/api.ts: getLancamentos faz parse da resposta como { lancamentos, total } e lê total do corpo JSON (não de lista.length)
- ./mvnw compile -pl api passa sem erros
- cd web && npx tsc --noErrorTruncation --noEmit passa sem erros
- cd web && npx eslint . passa com 0 erros
- cd web && npx next build passa (todas as rotas geradas)
- total corresponde à contagem real de lançamentos no banco para o mês/empresa (não tamanho da página)
- Todas as queries continuam filtrando por TenantContext.getEmpresaId() (isolamento de tenant mantido)

## Task Commits

Each task was committed atomically:
1. **Task 1: Backend — GET /api/lancamentos retorna total real (count via Page)** - `d62e6e8` (feat)
2. **Task 2: Frontend — getLancamentos lê total do corpo da resposta JSON** - `d62e6e8` (feat)

## Files Created/Modified

- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` - LancamentoPage record; listar retorna total via page.getTotalElements()
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` - GET /api/lancamentos retorna objeto paginado com lancamentos + total
- `web/lib/api.ts` - getLancamentos lê total do corpo da resposta JSON

## Decisions Made

- Seguido o plano exatamente como escrito
- Nenhuma mudança em LancamentoRepository (query já retorna Page com getTotalElements)
- BuscarPorId mantido funcionando com a nova resposta paginada
- Empresa_id exclusivamente do token via TenantContext (R3/AUTH-03 respeitado)

## Deviations from Plan

None - plan executed exactly as written. All acceptance criteria passed (getTotalElements usado, controller retorna total, tenant do token, compilação OK, tsc exit 0).

## Issues Encountered

None - ambas as tarefas executadas sem bloqueios.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- WR-03 fechado: aviso de truncamento "Mostrando 50 de {N}" dispara quando total > 50
- Todos os 3 gaps da Fase 3 fechados (CR-01, CR-02, WR-03)
- Pronto para re-verify: /gsd-verify-work 3
- Após verify passar, prossiga para /gsd-plan-phase 4 (IA via OpenRouter)

---
*Phase: 03-dashboard*
*Completed: 2026-11-09*
