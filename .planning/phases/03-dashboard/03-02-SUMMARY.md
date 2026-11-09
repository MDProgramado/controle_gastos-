---
phase: 03-dashboard
plan: 02
subsystem: ui
tags: [nextjs, react, typescript, dashboard, kpi, api-client, server-component]

# Dependency graph
requires:
  - phase: 03-dashboard plan 01
    provides: GET /api/dashboard?ano=&mes= backend aggregation endpoint (DashboardController/Service/Repository/DTOs)
provides:
  - Frontend dashboard page / (Server Component) com KPIs de entradas/saidas/saldo
  - web/components/MonthSelector.tsx (input type=month + setas < >, URL ?mes=YYYY-MM)
  - web/components/DashboardKPIs.tsx (3 cards com variacao absoluta + percentual)
  - web/lib/api.ts getDashboard(ano, mes) funcao de cliente API
affects:
  - 03-03 (frontend breakdown por categoria consome GET /api/dashboard)
  - 04-ia (funcao calling reuso de queries do dashboard)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Server Component com export const instant = false e validacao getMe() cookie auth
    - getDashboard segue padrao exato de getMe() (cookie "sess" + Authorization: Bearer + cache: "no-store")
    - Percentuais como String para suportar "—" quando base do mes anterior e zero (herdado do backend 03-01)
    - Intl.NumberFormat('pt-BR', BRL) para formatacao monetária
    - Cor do Saldo varia pelo sinal (verde/vermelho/muted); Entradas verde, Saidas vermelho

key-files:
  created:
    - web/components/MonthSelector.tsx
    - web/components/DashboardKPIs.tsx
  modified:
    - web/app/page.tsx
    - web/lib/api.ts

key-decisions:
  - "Percentuais modelados como String para suportar '—' (divisão por zero evitada no backend, DSH-03/D-06)"
  - "getDashboard reutiliza padrao de getMe() para autenticacao via cookie sess + Bearer token (D-02/P4)"
  - "Valores dos KPIs coloridos: Entradas verde, Saidas vermelho, Saldo verde/vermelho/muted pelo sinal"
  - "Sem atributos min/max no input type=month (meses futuros e passados permitidos, D-14/D-15)"

patterns-established:
  - "DashboardData tipado espelha o DTO do backend (totais, variacoes, percentuais, categorias)"
  - "Sem sessão → redirect /login?expirada=1 tanto no getMe() quanto quando getDashboard retorna null"

requirements-completed: [DSH-01, DSH-03, DSH-04, DSH-05]

# Metrics
duration: 7min
completed: 2026-11-09T11:01:34Z
---

# Phase 3 Plan 02: Frontend dashboard page with KPIs, MonthSelector, comparison vs previous month

**Dashboard web funcional consumindo GET /api/dashboard, com seletor de mês persistido na URL (?mes=YYYY-MM), 3 KPIs (entradas/saídas/saldo) e comparação mensal com variação absoluta + percentual, tratamento de base zero ("—") e formatação monetária pt-BR.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-11-09T10:54:05Z
- **Completed:** 2026-11-09T11:01:34Z
- **Tasks:** 2/2 completed
- **Files modified:** 4

## Accomplishments

- web/lib/api.ts estendido com tipo DashboardData e funcao getDashboard(ano, mes) seguindo padrao de getMe()
- web/components/MonthSelector.tsx: input type="month" + setas < > com router.push (?mes=YYYY-MM), sem min/max
- web/app/page.tsx: Server Component instant=false, validacao getMe() com redirect /login?expirada=1, le searchParams mes, renderiza MonthSelector + DashboardKPIs
- web/components/DashboardKPIs.tsx: 3 cards (Entradas verde, Saidas vermelho, Saldo cor por sinal) com formatarVariacao e corSaldo, Intl.NumberFormat pt-BR
- Comparacao mensal implementada: variacao absoluta + percentual vs mes anterior, base zero = "—", sinais "+"/"-"

## Task Commits

Each task was committed atomically:

1. **Task 1: MonthSelector + getDashboard API + pagina / com KPIs basicos** - `a915ac0` (feat)
2. **Task 2: Comparacao mensal com tratamento de base zero** - `5b15722` (feat)

## Files Created/Modified

- `web/lib/api.ts` - tipo DashboardData + funcao getDashboard(ano, mes) (cookie sess + Bearer + no-store)
- `web/components/MonthSelector.tsx` - input type=month, setas < >, router.push com ?mes=YYYY-MM, sem min/max
- `web/components/DashboardKPIs.tsx` - 3 cards KPI com variacao absoluta/percentual, cores por sinal, formatacao monetaria
- `web/app/page.tsx` - Server Component dashboard, getMe() validacao, getDashboard chamada, passa props para DashboardKPIs

## Decisions Made

- Percentuais como String para suportar "—" quando base do mes anterior e zero (consistente com backend 03-01)
- getDashboard segue padrao exato de getMe() (mesma autenticacao, cache no-store)
- Cor do Saldo: verde positivo, vermelho negativo, muted zero; Entradas sempre verde, Saidas sempre vermelho

## Deviations from Plan

None - plan executed exactly as written. Task 2's formatarVariacao/corSaldo and value coloring were implemented alongside Task 1 because Task 1's own acceptance criteria already required variacao absoluta + percentual, base zero = "—", e cor do saldo por sinal; the Task 2 commit captured the final value-coloring refinement (Entradas/Saidas valores coloridos).

## Issues Encountered

None - TypeScript compiles cleanly (npx tsc --noEmit), all grep-based acceptance criteria pass.

## User Setup Required

None - no external service configuration required. Consume existing GET /api/dashboard backend endpoint from plan 03-01.

## Next Phase Readiness

- Frontend dashboard pronto, consumindo endpoint de agregacao backend
- Proximo: 03-03 (breakdown por categoria com Recharts) — checkpoint:human-verify para npm install recharts
- Numeros do dashboard prontos para conferencia contra SQL direto no banco

---
*Phase: 03-dashboard*
*Completed: 2026-11-09*
