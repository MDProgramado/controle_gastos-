---
phase: 03-dashboard
plan: 03
subsystem: ui
tags: [nextjs, react, typescript, dashboard, recharts, category-breakdown]

# Dependency graph
requires:
  - phase: 03-dashboard plan 01
    provides: GET /api/dashboard backend endpoint de agregacao (SUM/GROUP BY) com breakdown de categorias de SAIDA (DashboardController/Service/Repository/DTOs)
  - phase: 03-dashboard plan 02
    provides: Frontend pagina dashboard (Server Component) com getDashboard, DashboardKPIs, MonthSelector e web/lib/api.ts
provides:
  - web/components/CategoryBreakdown.tsx — tabela ordenada por valor decrescente + grafico de barras horizontais por categoria (exporta CategoryBreakdown)
  - web/app/page.tsx — integra CategoryBreakdown abaixo dos DashboardKPIs, passa dados do getDashboard como props
affects:
  - 03-04 (paginas CRUD /lancamentos e /categorias reusam contexto do dashboard)
  - 04-ia (queries de agregacao do dashboard reutilizadas por function calling)

# Tech tracking
tech-stack:
  added: [recharts ^2.15.4]
  patterns:
    - "use client" componente cliente recebendo props (categorias, totalSaidas) do Server Component page.tsx
    - Wrapper Chart (shadcn/ui Charts) envolvendo Recharts BarChart com layout="horizontal"
    - Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }) para valores monetarios
    - Intl.NumberFormat estilo percent (2 casas decimais, pt-BR) para percentuais
    - Tratamento de base zero: percentual = "—" quando totalSaidas = 0 (evita NaN/Infinity, T-03-08)
    - Token CSS var(--chart-1) do globals.css para cor da barra (tema claro/escuro)

key-files:
  created:
    - web/components/CategoryBreakdown.tsx
  modified:
    - web/app/page.tsx

key-decisions:
  - "Usar shadcn/ui Charts (wrapper Chart) + Recharts BarChart layout='horizontal' para grafico de barras horizontais (D-11, D-12)"
  - "Denominador do percentual = totalSaidas (prop); quando totalSaidas = 0, percentual = '—' sem divisao por zero (D-10)"
  - "Cor da barra = token CSS var(--chart-1) (#0F766E teal) do globals.css, reaproveitando tema claro/escuro"
  - "Valor formatado com Intl.NumberFormat pt-BR BRL (D-03)"
  - "Nao agrupar em 'Outros'; listar todas as categorias de SAIDA inclusive zeradas; ENTRADA filtrada no backend (D-08, D-09)"

# Metrics
duration: 12min
completed: 2026-11-09T14:45:00Z
tasks: 1/1
files: 2 (1 created, 1 modified)
---

# Phase 3 Plan 03: Category Breakdown (tabela + grafico de barras horizontais)

**Componente CategoryBreakdown integrado ao dashboard: tabela ordenada por valor decrescente (inclusive categorias zeradas de SAIDA) e grafico de barras horizontais via shadcn/ui Charts (Recharts), consumindo o breakdown de GET /api/dashboard.**

## Performance

- **Duration:** 12 min
- **Completed:** 2026-11-09
- **Tasks:** 1/1 completed
- **Files modified:** 2 (1 created, 1 modified)

## Accomplishments

- web/components/CategoryBreakdown.tsx: "use client", recebe props `categorias` (Array<{id, nome, total}>) e `totalSaidas` (number)
- Tabela: colunas Categoria | Valor | Percentual, ordenada por total decrescente, inclui categorias com total = 0
- Percentual: (categoria.total / totalSaidas) * 100 formatado "0,00%" (pt-BR) quando totalSaidas > 0; "—" quando totalSaidas = 0 (sem divisao por zero)
- Valor formatado: Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }) -> ex: R$ 100,00
- Grafico: BarChart do Recharts com layout="horizontal", margin esquerda 120px para nomes longos, YAxis categoria, XAxis numerico, Tooltip em BRL, barra com fill="var(--chart-1)" e radius [0,4,4,0]
- Empty state: "Nenhuma categoria de saida neste mes" quando categorias vazio
- web/app/page.tsx: importa CategoryBreakdown e renderiza abaixo dos DashboardKPIs passando dados.categorias e dados.totalSaidas

## Task Commits

Each task was committed atomically:

1. **Task 1: CategoryBreakdown componente (tabela + grafico barras horizontais) e integracao na pagina** - `35b6846` (feat)

## Files Created/Modified

- `web/components/CategoryBreakdown.tsx` - criado: componente cliente com tabela + grafico Recharts horizontal
- `web/app/page.tsx` - modificado: importa e renderiza CategoryBreakdown abaixo dos DashboardKPIs

## Decisions Made

- Wrapper Chart (shadcn/ui Charts) + Recharts BarChart layout="horizontal" (D-11, D-12)
- Percentual com totalSaidas como denominador; "—" quando base zero (D-10, T-03-08)
- Cor da barra var(--chart-1) token CSS do globals.css (teal #0F766E)
- Valores monetarios formatados com Intl.NumberFormat pt-BR BRL (D-03)
- Sem agrupamento "Outros"; todas as categorias de SAIDA listadas, ENTRADA filtrada no backend (D-08, D-09)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Type error] Removido prop `dashed` do `<CartesianGrid>`**
- **Found during:** `npx tsc --noEmit` apos criar CategoryBreakdown.tsx
- **Issue:** O plano especificou `<CartesianGrid horizontal dashed={false} />`, mas a versao instalada do recharts (^2.15.4) nao define `dashed` em `CartesianGridProps`, causando erro TS2322 e impedindo a compilacao.
- **Fix:** Removido `dashed={false}`; mantido `horizontal` (valido em `GridLineType`, que aceita `boolean`). Grafico de linhas horizontais de grade preservado.
- **Files modified:** web/components/CategoryBreakdown.tsx
- **Commit:** 35b6846

## User Setup Required

None - recharts ja instalado (plano 03-02, commit cb5b3e0) e verificado humano ("aprovado"). Consume endpoint GET /api/dashboard existente do plano 03-01.

## Next Phase Readiness

- Frontend breakdown por categoria completo, integrado ao dashboard
- Proximo: 03-04 (paginas CRUD /lancamentos e /categorias, TopNav, DeleteConfirmationDialog, role ADMIN-only)

---
*Phase: 03-dashboard | Plan: 03 | Completed: 2026-11-09*
