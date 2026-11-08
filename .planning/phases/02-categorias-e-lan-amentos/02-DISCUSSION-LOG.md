# Phase 2: Categorias e Lançamentos - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-11-08
**Phase:** 02-Categorias e Lançamentos
**Areas discussed:** Seed V2 de categorias

---

## Seed V2 de categorias

| Option | Description | Selected |
|--------|-------------|----------|
| A - SQL (V2__seed_categorias.sql) | INSERTs diretos, mais curto e idiomático para seed estático | |
| B - Java (V2__seed_categorias.java) | Segue padrão V1_1 da Fase 1, mais verboso | ✓ |

**User's choice:** B - Java migration `V2__seed_categorias.java` seguindo o padrão V1_1.

**Notes / clarifications:**
- `empresa_id` via subquery `(SELECT id FROM empresa WHERE nome='Controle Financeiro')`, nunca hardcoded.
- Idempotência: checar existência da categoria antes de inserir.
- Manter a ordem da lista do cliente (12 saídas + 4 entradas; 4 pessoais [P]).

Áreas cinzas listadas mas **não discutidas** (usuário selecionou apenas esta área): formato do lote tudo-ou-nada, local da validação monetária >2 casas / HALF_EVEN, integridade categoria×tipo e categorias desativadas. A serem resolvidas em /gsd-plan-phase.

---

## the agent's Discretion

Nenhuma área delegada ao agente nesta sessão.

## Deferred Ideas

- Importação OFX/CSV, lançamentos recorrentes, app mobile, multiempresa real, IA/OpenRouter — já em Out of Scope, fora da fase 2.
