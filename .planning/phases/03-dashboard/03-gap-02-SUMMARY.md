---
phase: 03-dashboard
plan: gap-02
subsystem: ui
tags: [nextjs, typescript, react, frontend, bigdecimal, money, division-by-zero, percentual]

# Dependency graph
requires:
  - phase: 03-04
    provides: Páginas CRUD /lancamentos e /categorias com getLancamentos em web/lib/api.ts
provides:
  - totalSaidas tipado como string em CategoryBreakdownProps e DashboardData
  - Guard de divisão por zero: Number(totalSaidas) || 0, if (!totalSaidasNum)
  - categoria.total parseado com Number() antes da divisão
  - Campos monetários de DashboardData tipados como string
affects:
  - 03-gap-03 (WR-03 contagem total — getLancamentos em web/lib/api.ts)
  - 04-ia (IA reusa consultas/dashboard; tipos monetários consistentes)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Campos monetários recebidos da API tipados como string (BigDecimal serializado via ToStringSerializer)
    - Parse numérico (Number()) apenas na exibição, nunca no tipo TypeScript
    - Guard de divisão por zero: totalSaidasNum = Number(totalSaidas) || 0; if (!totalSaidasNum) return "—"

key-files:
  created: []
  modified:
    - web/lib/api.ts
    - web/components/CategoryBreakdown.tsx
    - web/app/page.tsx
    - web/app/lancamentos/page.tsx
    - web/components/DashboardKPIs.tsx

key-decisions:
  - "totalSaidas: string em CategoryBreakdownProps, parse com Number(totalSaidas) || 0 antes do guard"
  - "categoria.total: string, parseado com Number(categoria.total) antes da divisão e formatação"
  - "DashboardData campos monetários (totalEntradas, totalSaidas, saldo, variações) como string"
  - "LancamentoData.valor e LancamentoRequest.valor como string; conversão para number nos componentes cliente"
  - "CardContent import não utilizado removido de DashboardKPIs.tsx (lint warning)"
  - "Variável 'total' não utilizada removida de CategoryBreakdown.tsx (lint warning)"

requirements-completed: [DSH-02, DSH-05, D-10]

# Metrics
duration: 20min
completed: 2026-11-09T14:20:00Z

# Phase 3 Gap 02: CR-02 divisão por zero + WR-02 tipos monetários

**Corrigido guard de divisão por zero quebrado (CR-02) e tipos TypeScript mentirosos para campos monetários (WR-02) em CategoryBreakdown.tsx e web/lib/api.ts.**

## Performance

- **Duration:** 20 min
- **Started:** 2026-11-09T14:00:00Z
- **Completed:** 2026-11-09T14:20:00Z
- **Tasks:** 2/2 completed
- **Files modified:** 5

## Accomplishments

- web/lib/api.ts: DashboardData (totalEntradas, totalSaidas, saldo, variações) tipados como string; categorias[].total como string; LancamentoData.valor e LancamentoRequest.valor como string
- web/components/CategoryBreakdown.tsx: totalSaidas tipado como string; guard usa Number(totalSaidas) || 0 com if (!totalSaidasNum); categoria.total parseado com Number() antes da divisão e MONETARY.format
- web/app/page.tsx: dados.categorias e dados.totalSaidas passados como string para CategoryBreakdown; parse Number() apenas nos componentes cliente
- web/app/lancamentos/page.tsx: lancamentos mapeados para number (valor: Number(l.valor)) no servidor; server actions convertem number→string antes de enviar para API
- web/components/DashboardKPIs.tsx: import CardContent não utilizado removido (lint)
- `npx tsc --noErrorTruncation --noEmit` passa sem erros (exit 0)
- `npx eslint .` passa com 0 erros
- `npx next build` passa (todas as rotas geradas)
- totalSaidas = "0.00" resulta em percentual "—" (não "NaN%")

## Task Commits

Each task was committed atomically:
1. **Task 1: Corrigir tipos monetários em web/lib/api.ts (number → string)** - `69cb33c` (feat)
2. **Task 2: Corrigir guard de divisão por zero e parse numérico em CategoryBreakdown.tsx** - `69cb33c` (feat)

## Files Created/Modified

- `web/lib/api.ts` - todos os campos monetários recebidos da API são tipados como string
- `web/components/CategoryBreakdown.tsx` - totalSaidas como string, guard de zero corrigido, parse numérico antes da divisão
- `web/app/page.tsx` - dados.string passados para componentes cliente com parse Number()
- `web/app/lancamentos/page.tsx` - conversão number↔string entre servidor e API
- `web/components/DashboardKPIs.tsx` - import não utilizado removido (lint warning)

## Decisions Made

- Seguido o plano exatamente como escrito
- Campos monetários como string no tipo TypeScript (refletem o contrato backend BigDecimal → string JSON)
- Parse numérico (Number()) apenas na camada de exibição, nunca no tipo
- Label de exibição "—" quando totalSaidas é zero (D-10, sem divisão por zero)

## Deviations from Plan

None - plan executed exactly as written. All acceptance criteria passed (tipos string, guard Number(), tsc exit 0, eslint 0, next build OK).

## Issues Encountered

None - ambas as tarefas executadas sem bloqueios.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- CR-02 fechado: divisão por zero tratada, percentual "—" quando total=0
- WR-02 fechado: tipos TypeScript refletem contrato real do backend
- Pronto para executar 03-gap-03 (WR-03 contagem total para aviso de truncamento)

---
*Phase: 03-dashboard*
*Completed: 2026-11-09*
