---
status: partial
phase: 02-categorias-e-lan-amentos
source: [02-VERIFICATION.md]
started: 2026-11-09T11:00:00Z
updated: 2026-11-09T11:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Reformatar o goal da fase como User Story (modo MVP)
expected: Rodar `/gsd mvp-phase 2` e reexecutar a verificação, OU aceitar explicitamente a verificação goal-backward padrão para um goal que não está no formato 'As a ..., I want to ..., so that ...'
result: [pending]

### 2. Comportamento de categoria desativada em novos lançamentos
expected: Segundo 02-RESEARCH.md:449, `ativo=false` deve bloquear novos lançamentos (400). O código atual NÃO verifica `ativo` em LancamentoService.criar/editar — a categoria desativada continua aceita (201).
result: [pending]

### 3. Escopo de UI da Fase 2 (ROADMAP `UI hint: yes`)
expected: Confirmar se a Fase 2 deveria entregar UI web de categorias/lançamentos ou se a entrega API-only é aceitável, com a UI ficando para a Fase 3 (Dashboard).
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
