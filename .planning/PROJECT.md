# Controle Financeiro

## What This Is

Aplicação de controle financeiro (entradas e saídas) para uma empresa cliente — no início single-tenant, com `empresa_id` em todas as tabelas para suportar multiusuário/multiempresa no futuro. Web em Next.js (App Router, TypeScript) consumindo uma API Spring Boot (Java 21), publicada na Vercel (web) e Render (API + PostgreSQL).

## Core Value

Saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, que o cliente confere contra SQL direto e compara com sua contabilidade sem surpresas.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

- [x] Login JWT em produção com seed de empresa/usuário (Flyway V1) — validado na Fase 1
- [x] Categorias com tipo ENTRADA/SAÍDA (seed de 12 saídas e 4 entradas, Flyway V2) e flag `pessoal` para as marcadas [P] — validado na Fase 2
- [x] Lançamentos (criar/editar/excluir) com validação monetária estrita (máx. 2 casas, HALF_EVEN, sem double/float) — validado na Fase 2
- [x] Lote de lançamentos em transação tudo-ou-nada, erro informa o índice da linha — validado na Fase 2
- [x] Integridade: categoria ENTRADA não aceita lançamento de SAÍDA e vice-versa — validado na Fase 2
- [x] `empresa_id` sempre extraído do token, nunca do body — validado nas Fases 1–2

### Active

<!-- Current scope. Building toward these. -->

- [ ] Dashboard: entradas, saídas, saldo do mês, gasto por categoria e comparação mensal
- [ ] Testes de integração com Testcontainers; números do dashboard batem com SQL de conferência
- [ ] IA via OpenRouter na última fase: function calling sobre consultas fixas + categorização sugerida com confirmação

### Out of Scope

<!-- Explicit boundaries. Includes reasoning to prevent re-adding. -->

- IA/machine learning nas fases 1–3 — decisão explícita do cliente; ela vem apenas na fase 4
- Multiempresa real (cadastro de novas empresas) — fase 1 só prepara o schema com `empresa_id`; quem entra é o multiusuário, não o cadastro de empresas
- Importação de extratos (OFX/CSV) e lançamentos recorrentes — não mencionados no brief; adiar para v2
- Documentos legais LGPD (política de privacidade/termos de uso) — texto e contrato tratados fora do fluxo GSD; o app entrega os pré-requisitos técnicos (BCrypt, HTTPS, isolamento por empresa)
- App mobile nativo — web-first (Vercel)
- Planilhas ilimitadas de categorias — teto de ~16 categorias seed por decisão consciente do cliente ("não virar planilha")

## Context

- **Cliente:** empresa única no início; app de uso real, precisa de números confiáveis
- **Timeline:** 5 semanas — fases 1 e 2 nas semanas 1–2, fase 3 nas semanas 3–4, fase 4 na semana 5
- **Categorias seed (lista do cliente):** 12 saídas (Alimentação, Saúde/farmácia e Lazer marcadas [P] = pessoais) e 4 entradas (Vendas de produtos, Prestação de serviços, Rendimentos e reembolsos [P], Outras receitas)
- **Papéis do cliente no fluxo GSD:** escreve o brief/respostas do discuss-phase; revisa o `PLAN.md` de cada fase procurando lacunas de regra de negócio; revisa código nos pontos de valor e permissão (service de lançamento, lote, filtro por `empresa_id`, consultas do dashboard); desenha as consultas da IA da fase 4 junto
- **Dois pontos de controle não negociáveis do cliente:**
  1. No plano: verificar se toca em valor monetário ou permissão e se as regras aparecem como tarefas
  2. No verify: rodar o teste de lote com uma linha inválida e confirmar que nada foi gravado
- **Ambiente atual:** Fases 1 e 2 completas (2026-11-09). API de categorias e lançamentos (CRUD + lote tudo-ou-nada) com 40 testes de integração Testcontainers passando. UI web de categorias/lançamentos ficou para a Fase 3 (Dashboard), conforme decisão em `02-HUMAN-UAT.md`; categoria desativada continua aceitando novos lançamentos por decisão explícita.
- **Estado Phase 2 (2026-11-09):** Phase 2 complete — 4/4 plans, 40 integration tests green, human verification aprovada. Non-blocking findings em `02-VERIFICATION.md` (paginação fixa de 50, pré-checagem de duplicidade em `editar`, testes de scaffolding).

## Constraints

- **Stack** (definida, não negociável): Java 21 + Spring Boot 3, Spring Security + JWT, Spring Data JPA, PostgreSQL, Flyway, Testcontainers; Next.js (App Router, TypeScript)
- **Dinheiro**: valores em `BigDecimal` com `NUMERIC(14,2)` no banco, `scale = 2`, `RoundingMode.HALF_EVEN` no service; entrada com mais de 2 casas é rejeitada; nenhum `double`/`float` no código
- **Tenant**: `empresa_id` vem exclusivamente do token JWT, nunca do body
- **Lote**: transação tudo-ou-nada; erro informa o índice da linha
- **Integridade**: categoria do tipo ENTRADA não aceita lançamento de SAÍDA e vice-versa
- **Segurança**: senha com BCrypt, dados em trânsito via HTTPS, política de privacidade e LGPD previstas antes do uso real
- **Deploy**: API no Render (Postgres pago com backup), web na Vercel
- **Escopo de IA**: restrito à fase 4 (OpenRouter)

## Key Decisions

<!-- Decisions that constrain future work. Add throughout project lifecycle. -->

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Stack Spring Boot 3 + Next.js em Render/Vercel | Definida pelo cliente no brief | — Pending |
| Monorepo `api/` + `web/` em um repo | Deploy separado, revisão em um lugar só | — Pending |
| Pesquisa de ecossistema ignorada no init | Stack e domínio decididos; cliente pediu plano direto | ✓ Decided |
| Roles ADMIN/USER modeladas desde a fase 1 | Multiusuário futuro é objetivo explícito; barato agora, caro depois | — Pending (default) |
| Lançamentos com CRUD completo (criar/editar/excluir) | Ciclo completo é o que mais se usa em sistema financeiro | — Pending (default) |
| Lote limitado a 500 linhas | Limite prático de transação e latência HTTP | — Pending (default) |
| Categorias com boolean `pessoal` | Marcador [P] vira campo filtrável, não anotação morta | — Pending (default) |
| Dashboard: mês corrente + seletor de mês + comparação com mês anterior | Atende "comparação mensal" do brief | — Pending (default) |
| Deploy por push (Render/Vercel nativo), sem CI custom na fase 1 | Simplicidade; testes rodam localmente antes do push | — Pending (default) |
| Estrutura Vertical MVP (fases entregam capacidade ponta a ponta) | Marcos verificáveis do cliente são verticais (login em produção, lote testado, dashboard confere) | — Pending (default) |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-11-09 after Phase 2 completion*
