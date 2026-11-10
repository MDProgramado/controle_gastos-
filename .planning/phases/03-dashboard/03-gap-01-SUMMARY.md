---
phase: 03-dashboard
plan: gap-01
subsystem: ui
tags: [nextjs, typescript, react, frontend, enum, lancamento, categoria, saida, saida]

# Dependency graph
requires:
  - phase: 03-04
    provides: Páginas CRUD /lancamentos e /categorias com LancamentosClient.tsx e CategoriasClient.tsx (arquivos modificados neste gap)
provides:
  - TipoLancamento enum = "ENTRADA" | "SAIDA" (sem acento) em web/components/LancamentosClient.tsx
  - <option value="SAIDA">Saída</option> em LancamentosClient.tsx (label "Saída" inalterado)
  - cast setTipo(c.tipo as "ENTRADA" | "SAIDA") em web/components/CategoriasClient.tsx
  - <option value="SAIDA">Saída</option> em CategoriasClient.tsx (label "Saída" inalterado)
affects:
  - 03-gap-02 (CR-02 divisão por zero — mesmo arquivo frontend)
  - 03-gap-03 (WR-03 contagem total — LancamentosClient.tsx)
  - 04-ia (IA reusa consultas/dashboard; contrato SAIDA já consistente)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Valores de tipo enviados ao backend usam "SAIDA" (sem acento), exatamente como o backend armazena e consulta (l.tipo = 'SAIDA' no JPQL)
    - Label de exibição mantém acento ("Saída"); apenas o value do option muda — separação entre apresentação e contrato de dados

key-files:
  created: []
  modified:
    - web/components/LancamentosClient.tsx
    - web/components/CategoriasClient.tsx

key-decisions:
  - "Manter label de exibição 'Saída' com acento; alterar apenas o value do option e o enum/cast para 'SAIDA' sem acento, não alterando a UI vista pelo usuário"
  - "Nenhum pacote externo instalado; apenas alteração de strings existentes (ameaça T-03-SC aceita no threat model)"

requirements-completed: [DSH-01, DSH-02, LAN-01, LAN-02, LAN-03, LAN-04, LAN-05, LAN-06, LAN-07, CAT-01, CAT-02, CAT-03, CAT-04, CAT-05, CAT-06]

# Metrics
duration: 12min
completed: 2026-11-09T14:00:00Z

# Phase 3 Gap 01: CR-01 SAÍDA→SAIDA enum mismatch fix

**Frontend TipoLancamento enum e option values corrigidos de "SAÍDA" (com acento) para "SAIDA" (sem acento) em LancamentosClient.tsx e CategoriasClient.tsx, batendo com o contrato l.tipo = 'SAIDA' do LancamentoRepository e da validação categoria.getTipo().equals(tipoLancamento) — restaura criação e edição de lançamentos e categorias de tipo saída.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-11-09T13:48:00Z
- **Completed:** 2026-11-09T14:00:00Z
- **Tasks:** 2/2 completed
- **Files modified:** 2

## Accomplishments

- web/components/LancamentosClient.tsx: TipoLancamento = "ENTRADA" | "SAIDA" e <option value="SAIDA">Saída</option> (linhas 18 e 456)
- web/components/CategoriasClient.tsx: cast setTipo(c.tipo as "ENTRADA" | "SAIDA") e <option value="SAIDA">Saída</option> (linhas 77 e 407)
- Nenhuma ocorrência de "SAÍDA" (com acento) restante nos dois arquivos
- `npx tsc --noErrorTruncation --noEmit` passa sem erros (exit 0)
- Criar/editar lançamento de tipo saída envia "SAIDA" ao backend; validação categoria.getTipo().equals(tipoLancamento) passa

## Task Commits

Each task was committed atomically:

1. **Task 1: Corrigir TipoLancamento enum e option value em LancamentosClient.tsx (SAÍDA → SAIDA)** - `9f6deb2` (feat)
2. **Task 2: Corrigir cast de tipo e option value em CategoriasClient.tsx (SAÍDA → SAIDA)** - `1ce3d6c` (feat)

## Files Created/Modified

- `web/components/LancamentosClient.tsx` - enum TipoLancamento e option value do tipo de lançamento corrigidos para "SAIDA" (sem acento)
- `web/components/CategoriasClient.tsx` - cast de tipo e option value do tipo de categoria corrigidos para "SAIDA" (sem acento)

## Decisions Made

- Seguido o plano exatamente como escrito; nenhuma mudança arquitetural necessária
- Label de exibição "Saída" mantido com acento; apenas o valor enviado ao backend mudou para "SAIDA"

## Deviations from Plan

None - plan executed exactly as written. All acceptance criteria passed (grep SAÍDA = 0, grep SAIDA = 2 por arquivo, tsc exit 0).

## Issues Encountered

None - ambas as tarefas executadas sem bloqueios.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- CR-01 fechado: criação/edição de saídas e categorias de saída funciona no frontend
- Pronto para executar 03-gap-02 (CR-02 divisão por zero) e 03-gap-03 (WR-03 contagem total)
- Nenhum bloqueio para a próxima wave

---
*Phase: 03-dashboard*
*Completed: 2026-11-09*
