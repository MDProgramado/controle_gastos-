---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_plan: 5
status: complete
stopped_at: Fase 5 completa (waves 05-01 a 05-07) — verificação aprovada
last_updated: "2026-11-14T00:00:00Z"
last_activity: 2026-11-14
progress:
  total_phases: 5
  completed_phases: 5
  total_plans: 22
  completed_plans: 22
  percent: 100
---

# STATE.md — Controle Financeiro

## Project Reference

See: .planning/PROJECT.md (updated 2026-10-30)

**Core value:** Saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, conferidos contra SQL direto.
**Current focus:** Fase 5 completa — importação de PDFs e análise por IA (pronta para produção)

## Current Position

Phase: 5 (completa)
Plan: 7 of 7 (05-01 a 05-07)
Status: Fase completa — verificação aprovada (VERIFICATION.md: passed)
Current Plan: 5
Total Plans in Phase: 7
**Progress:** [██████████] 100%
Last activity: 2026-11-14
Last Activity Description: Wave 7 concluída — testes unitários (57, 0 falhas), IT escrito e compilado (execução pendente sem Docker), security-audit aprovado (SECURITY.md), build+lint da web verdes, cobertura IMP-01..15/D-01..32 preenchida (05-COVERAGE.md). Fase 5 marcada como Complete.

## Phase Status (resumo executivo)

- **Phase 1 · Fundação e Auth em Produção** — complete (4/4)
- **Phase 2 · Categorias e Lançamentos** — complete (4/4)
- **Phase 3 · Dashboard** — complete (4/4 + 3 gaps fechados)
- **Phase 4 · IA via OpenRouter** — complete (1/1)
- **Phase 5 · Importação de PDFs e Análise por IA** — complete (7/7: 05-01 planejamento, 05-02 extração/sanitização, 05-03 IA/migração, 05-04 staging/dedupe, 05-05 confirmação/idempotência, 05-06 web+BFF+e2e, 05-07 testes/security-audit/verificação)

## Phase Status

| Phase | Status | Plans | Progress |
|-------|--------|-------|----------|
| 1 · Fundação e Auth em Produção | ✓ | 4/4 | 100% |
| 2 · Categorias e Lançamentos | ✓ | 4/4 | 100% |
| 3 · Dashboard | ✓ | 4/4 + 3 gaps | 100% |
| 4 · IA via OpenRouter | ✓ | 1/1 | 100% |
| 5 · Importação de PDFs e Análise por IA | ✓ | 7/7 (05-01..05-07) | 100% |

## Current Phase

- **Phase 2 · Categorias e Lançamentos** — complete (4/4)
- Weeks 1–2 of 5 (timeline)
- Goal: seed V2 de categorias, CRUD de lançamentos com validação monetária estrita e lote tudo-ou-nada
- Plan 01 ✓: V2 seed migration (16 categorias) + GET /api/categorias read API
- Plan 02 ✓: Categoria write (POST/PUT/DELETE) + Security role split (USER lê, ADMIN escreve) — GREEN
- Plan 03 ✓: Lançamentos CRUD + validação monetária estrita (BigDecimal, max 2 casas, HALF_EVEN, sem double/float) — GREEN (8 testes passam)
- Plan 04 ✓: Lote tudo-ou-nada (POST /api/lancamentos/lote) com índice da linha inválida e rollback — GREEN (40 testes)

- **Phase 3 · Dashboard** — executed (4/4 plans, 100%) — 3 gap plans pending closure
- Weeks 3–4 of 5 (timeline)
- Goal: Entradas, saídas, saldo por mês, gasto por categoria e comparação mensal na web
- Plan 03-01 (Wave 1): Backend endpoint de agregação GET /api/dashboard com SUM/GROUP BY — TDD RED→GREEN, 4 testes GREEN
- Plan 03-02 (Wave 2): Frontend página dashboard com KPIs, MonthSelector, comparação mensal — tsc/eslint/build OK
- Plan 03-03 (Wave 3): Frontend breakdown por categoria (tabela + gráfico barras horizontais Recharts) — completo (recharts 2.15.4, chart.tsx wrapper)
- Plan 03-04 (Wave 4): CRUD páginas /lancamentos e /categorias, TopNav, DeleteConfirmationDialog, role ADMIN-only — tsc/eslint/next build OK
- Gap Plan 03-gap-01 (Wave 1): Fix CR-01 — "SAÍDA" → "SAIDA" enum mismatch in LancamentosClient.tsx + CategoriasClient.tsx
- Gap Plan 03-gap-02 (Wave 2): Fix CR-02 — division-by-zero guard (totalSaidas string) + monetary TS types
- Gap Plan 03-gap-03 (Wave 3): Fix WR-03 — real total count for truncation warning "Mostrando 50 de {N}"
- Next: /gsd-execute-phase 3 --gaps-only (execute gap plans), then /gsd-verify-work 3 (re-verify)
- Milestone: números do dashboard batem com SQL de conferência direto no banco

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

Todas as fases entregues. Projeto pronto para deploy (Render API + PostgreSQL, Vercel web). Recomendado: rodar ITs Testcontainers em ambiente com Docker para validar `ImportacaoPdfIntegrationTest` e regressão completa antes do deploy.

