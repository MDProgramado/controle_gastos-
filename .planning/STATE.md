---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: ready_to_plan
last_updated: 2026-11-09T10:42:06.217Z
last_activity: 2026-11-09
progress:
  total_phases: 4
  completed_phases: 2
  total_plans: 8
  completed_plans: 8
  percent: 50
stopped_at: Phase 02 complete (4/4) — ready to discuss Phase 3
---

# STATE.md — Controle Financeiro

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-30)

**Core value:** Saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, conferidos contra SQL direto.
**Current focus:** Phase 3 — dashboard

## Current Position

Phase: 3
Plan: Not started
Next: Phase 3 (Dashboard) — plan 03-01
Status: Ready to plan
Total Plans in Phase: 4
Last activity: 2026-11-09
Last Activity Description: Plan 02-04 completed: POST /api/lancamentos/lote tudo-ou-nada (até 500 linhas) com rollback completo e índice da linha inválida no ProblemDetail 400; LancamentoLoteRequest, LinhaInvalidaException, LoteDemasiadoGrandeException; ./mvnw verify da API com 40 testes, 0 falhas (GREEN).

## Phase Status

| Phase | Status | Plans | Progress |
|-------|--------|-------|----------|
| 1 · Fundação e Auth em Produção | ✓ | 4/4 | 100% |
| 2 · Categorias e Lançamentos | ✓ | 4/4 | 100% |
| 3 · Dashboard | ○ | 0/2 | 0% |
| 4 · IA via OpenRouter | ○ | 0/1 | 0% |

## Current Phase

- **Phase 2 · Categorias e Lançamentos** — complete (4/4)
- Weeks 1–2 of 5 (timeline)
- Goal: seed V2 de categorias, CRUD de lançamentos com validação monetária estrita e lote tudo-ou-nada
- Plan 01 ✓: V2 seed migration (16 categorias) + GET /api/categorias read API
- Plan 02 ✓: Categoria write (POST/PUT/DELETE) + Security role split (USER lê, ADMIN escreve) — GREEN
- Plan 03 ✓: Lançamentos CRUD + validação monetária estrita (BigDecimal, max 2 casas, HALF_EVEN, sem double/float) — GREEN (8 testes passam)
- Plan 04 ✓: Lote tudo-ou-nada (POST /api/lancamentos/lote) com índice da linha inválida e rollback — GREEN (40 testes)
- Next: Phase 3 — Dashboard
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

## Key Decisions (from 02-04 execution)

- POST /api/lancamentos/lote responde 201 com o array de lançamentos criados (o teste RED de Task 1 exige corpo JSON não nulo; desvio do `ResponseEntity<Void>` do plano)
- Validação monetária fica no service por linha (não em @Digits no DTO do lote) para preservar o índice da linha no erro
- processarLote @Transactional: limite de 500 checado no service; cada linha delega a criar() na mesma transação (self-invocation) → rollback total
- ProblemDetail de LinhaInvalidaException inclui property `indiceLinha` além do detail textual
- empresa_id nunca no DTO do lote (R3/T-02-10) — sempre de TenantContext

## Active Blockers

- (none)

## Next Action

Phase 2 completa (4/4, GREEN). Executar `/gsd-transition` (Phase 2 → Phase 3 · Dashboard) ou `/gsd-plan-phase 3` para iniciar a Fase 3.

---
*Last updated: 2026-11-09 — Phase 2 complete (4/4 plans, 40 testes GREEN)*
