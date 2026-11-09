---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
last_updated: "2026-11-08T23:30:00-03:00"
last_activity: 2026-11-08
progress:
  total_phases: 4
  completed_phases: 1
  total_plans: 8
  completed_plans: 7
  percent: 25
---

# STATE.md — Controle Financeiro

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-30)

**Core value:** Saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, conferidos contra SQL direto.
**Current focus:** Phase 02 — categorias-e-lan-amentos

## Current Position

Phase: 02 (categorias-e-lan-amentos) — EXECUTING
Plan: 3 of 4
Next: Phase 2 plan 4 (Lote tudo-ou-nada + todos os testes de integração)
Status: Plan 01 complete, Plan 02 complete, Plan 03 complete — ready for plan 04
Total Plans in Phase: 4
Last activity: 2026-11-08 — 02-03 plan complete (Lançamentos CRUD + validação monetária estrita GREEN)
Last Activity Description: Plan 02-03 completed: Lancamentos CRUD (POST/PUT/DELETE/GET /api/lancamentos) com validação monetária estrita (BigDecimal, max 2 casas, rejeita não arredonda, HALF_EVEN, sem double/float), integridade categoria×tipo, tenant isolation via empresa_id do token. Todos os 8 testes de integração passam (GREEN).

## Phase Status

| Phase | Status | Plans | Progress |
|-------|--------|-------|----------|
| 1 · Fundação e Auth em Produção | ✓ | 4/4 | 100% |
| 2 · Categorias e Lançamentos | ○ | 3/4 | 75% |
| 3 · Dashboard | ○ | 0/2 | 0% |
| 4 · IA via OpenRouter | ○ | 0/1 | 0% |

## Current Phase

- **Phase 2 · Categorias e Lançamentos** — in progress
- Weeks 1–2 of 5 (timeline)
- Goal: seed V2 de categorias, CRUD de lançamentos com validação monetária estrita e lote tudo-ou-nada
- Plan 01 ✓: V2 seed migration (16 categorias) + GET /api/categorias read API
- Plan 02 ✓: Categoria write (POST/PUT/DELETE) + Security role split (USER lê, ADMIN escreve) — GREEN
- Plan 03 ✓: Lançamentos CRUD + validação monetária estrita (BigDecimal, max 2 casas, HALF_EVEN, sem double/float) — GREEN (8 testes passam)
- Next: plan 04 — Lote tudo-ou-nada (POST /lote) + todos os testes de integração
- Milestone: lote com erro informa índice e não grava nada; valor >2 casas rejeitado

## Pending Debts (não bloqueiam — registrados no 01-04-SUMMARY)

- Rotação de segredos (JWT_SECRET, senha do banco, SEED_ADMIN_PASSWORD) — expostos em conversa
- Banco Free Render expira 2026-12-05 → upgrade pago com backup
- Staging inexistente (desvio D-08 aprovado) — previews da Vercel tocam a API de produção

## Key Artifacts

- `.planning/PLAN.md` — plano mestre (arquitetura, práticas, todas as fases)
- `.planning/PROJECT.md` — contexto, restrições, decisões
- `.planning/REQUIREMENTS.md` — 33 requisitos v1 com traceability
- `.planning/ROADMAP.md` — 4 fases, 100% de cobertura
- `.planning/config.json` — preferências do fluxo (interactive · coarse · sequential)

## Key Decisions (from 02-01 execution)

- V2 seed migration follows V1_1__seed_empresa_admin.java pattern (raw JDBC, context.getConnection(), subquery for empresa_id, idempotent SELECT-before-INSERT, never close connection)
- Categoria entity omits criado_em — V1__schema.sql's categoria table has no such column (Rule 1 bug fix during Task 2 implementation)
- SecurityConfig already restricts /api/categorias/** to ADMIN role — no change needed for read endpoint

## Key Decisions (from 02-02 execution)

- EntityManager.getReference(Empresa.class, empresaId) usado em CategoriaService.criar para associar tenant sem carregar entidade Empresa
- Exceções dedicadas (CategoriaDuplicadaException, CategoriaNaoEncontradaException) para mapeamento claro 400/404 via ProblemDetailExceptionHandler
- @AfterEach no teste deleta categorias com id > 16 (seed V2), garantindo isolamento entre métodos de teste

## Key Decisions (from 02-03 execution)

- MonetarioException handler adicionado a ProblemDetailExceptionHandler (Rule 2 desvio — necessário para converter exceção de validação monetária em 400 em vez de 500)
- GET /api/lancamentos/{id} adicionado ao controller (necessário pelo teste de exclusão: GET após DELETE deve retornar 404)
- usuarioAtual() obtido do principal UsuarioAuth no SecurityContext (R3 — usuário do token, nunca do body)
- Seguido padrão exato de CategoriaService/Controller/Repository para consistência com plan 02-02

## Active Blockers

- (none)

## Next Action

`/gsd-execute-phase 2 04` — executar plan 02-04: lote tudo-ou-nada (POST /lote) + todos os testes de integração

---
*Last updated: 2026-11-05 — Phase 1 complete (verification ACHIEVED)*
