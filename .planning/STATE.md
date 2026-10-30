# STATE.md — Controle Financeiro

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-30)

**Core value:** Saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, conferidos contra SQL direto.
**Current focus:** Phase 1 — Fundação e Auth em Produção

## Phase Status

| Phase | Status | Plans | Progress |
|-------|--------|-------|----------|
| 1 · Fundação e Auth em Produção | ○ | 0/1 | 0% |
| 2 · Categorias e Lançamentos | ○ | 0/2 | 0% |
| 3 · Dashboard | ○ | 0/2 | 0% |
| 4 · IA via OpenRouter | ○ | 0/1 | 0% |

## Current Phase

- **Phase 1 · Fundação e Auth em Produção**
- Weeks 1–2 of 5
- Goal: monorepo, Flyway V1, auth JWT, seed de empresa/usuário, deploy Render + Vercel
- Milestone: login funciona em produção, health check OK

## Key Artifacts

- `.planning/PLAN.md` — plano mestre (arquitetura, práticas, todas as fases)
- `.planning/PROJECT.md` — contexto, restrições, decisões
- `.planning/REQUIREMENTS.md` — 33 requisitos v1 com traceability
- `.planning/ROADMAP.md` — 4 fases, 100% de cobertura
- `.planning/config.json` — preferências do fluxo (interactive · coarse · sequential)

## Active Blockers

- (none)

## Next Action

`/gsd-discuss-phase 1` — colher contexto e revisar decisões da fase 1 (ex.: formato da sessão no front, expiração do JWT, escolha Maven/Gradle).

---
*Last updated: 2026-10-30 after initialization*
