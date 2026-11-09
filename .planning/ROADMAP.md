# Roadmap: Controle Financeiro

**Created:** 2026-10-30
**Granularity:** coarse (4 phases · 5 weeks)
**Project mode:** mvp (vertical — cada fase entrega capacidade ponta a ponta)

## Phases

### Phase 1: Fundação e Auth em Produção

**Goal:** Monorepo (`api/` + `web/`), Flyway V1, autenticação JWT, seed de empresa/usuário, deploy da API na Render e da web na Vercel
**Mode:** mvp
**Success Criteria**:

1. Login com e-mail/senha funciona na web publicada (Vercel → API Render)
2. `GET /api/health` responde OK em produção
3. `empresa_id` é extraído do token em todas as rotas autenticadas (teste de integração prova que o body não define tenant)
4. Seed de empresa e usuário ADMIN criado exclusivamente por migration Flyway V1

**Requirements:** AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06, INF-01, INF-03, INF-04
**UI hint**: yes
**Plans:** 4/4 plans executed ✓ [phase complete 2026-11-05 — VERIFICATION: ACHIEVED]

Plans:
**Wave 1**

- [x] 01-01-PLAN.md — Monorepo api/+web/ + Flyway V1 (NUMERIC(14,2)) + GET /api/health com Testcontainers

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-02-PLAN.md — Seed V1_1 (BCrypt via env) + login JWT (resource-server Nimbus) + TenantContext só do token

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 01-03-PLAN.md — BFF cookie httpOnly + tela de login UI-SPEC (pt-BR) + shell autenticado [checkpoint aprovado 2026-11-05]

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 01-04-PLAN.md — Deploy Render+Vercel + smoke + verificação humana [aprovado 2026-11-05] · desvio D-08 aprovado: 1 ambiente único (banco Free expira 2026-12-05)

---

### Phase 2: Categorias e Lançamentos

**Goal:** Seed V2 de categorias, CRUD de lançamentos com validação monetária estrita e lote tudo-ou-nada
**Mode:** mvp
**Success Criteria**:

1. Testes de integração (Testcontainers) passam em `./mvnw verify`
2. Lote com uma linha inválida retorna o índice da linha e **não grava nenhuma linha**
3. Valor com mais de 2 casas decimais é rejeitado; nenhum `double`/`float` no código
4. Categoria de tipo ENTRADA rejeita lançamento de SAÍDA e vice-versa

**Requirements:** CAT-01, CAT-02, CAT-03, CAT-04, CAT-05, CAT-06, LAN-01, LAN-02, LAN-03, LAN-04, LAN-05, LAN-06, LAN-07, LOT-01, LOT-02, INF-02
**UI hint**: yes
**Plans:** 4/4 plans executed ✓ [phase complete 2026-11-09 — VERIFICATION: ACHIEVED, human UAT approved]

Plans:

**Wave 1**

- [x] 02-01-PLAN.md — V2 seed migration (16 categorias) + categoria read API GET /api/categorias

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 02-02-PLAN.md — Categoria write (POST/PUT/DELETE) + Security role split (USER lê, ADMIN escreve) [complete 2026-11-08 — VERIFICATION: ACHIEVED, GREEN]

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 02-03-PLAN.md — Lancamentos CRUD + validação monetária estrita (BigDecimal, max 2 casas, HALF_EVEN) [complete 2026-11-08 — GREEN, 8 testes passam]

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 02-04-PLAN.md — Lote tudo-ou-nada (POST /lote) + todos os testes de integração [complete 2026-11-09 — GREEN, 40 testes passam]

---

### Phase 3: Dashboard

**Goal:** Entradas, saídas, saldo por mês, gasto por categoria e comparação mensal na web
**Mode:** mvp
**Success Criteria**:

1. Todos os números do dashboard batem com SQL de conferência executado direto no banco (seed conhecida)
2. Seletor de mês atualiza entradas, saídas, saldo, categorias e comparação
3. Mês sem lançamentos mostra zeros sem erro (divisão por zero tratada)
4. Consultas filtram por `empresa_id` do token (usuário de outra empresa não vê dados)

**Requirements:** DSH-01, DSH-02, DSH-03, DSH-04, DSH-05
**UI hint**: yes
**Plans:** 4/4 execution plans complete · 3 gap plans pending closure (verification: gaps_found)

Plans:

**Wave 1**

- [x] 03-01-PLAN.md — Backend: endpoint de agregação GET /api/dashboard com SUM/GROUP BY (DSH-01, DSH-02, DSH-03, DSH-05)

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 03-02-PLAN.md — Frontend: página dashboard com KPIs, seletor de mês, comparação mensal (DSH-01, DSH-03, DSH-04, DSH-05)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 03-03-PLAN.md — Frontend: breakdown por categoria (tabela + gráfico barras horizontais com Recharts) (DSH-02, DSH-05) [complete 2026-11-09 — commit 35b6846]

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 03-04-PLAN.md — CRUD páginas: /lancamentos e /categorias com TopNav, DeleteConfirmationDialog, role ADMIN-only (CAT-01..06, LAN-01..07, LOT-01..02)

**Gap Closure (pending execution — /gsd-execute-phase 3 --gaps-only)**

**Gap Wave 1**

- [ ] 03-gap-01-PLAN.md — Fix CR-01: "SAÍDA" → "SAIDA" enum mismatch em LancamentosClient.tsx + CategoriasClient.tsx (cria/edita saídas quebrado)

**Gap Wave 2** *(blocked on Gap Wave 1 completion)*

- [ ] 03-gap-02-PLAN.md — Fix CR-02: guard divisão por zero (totalSaidas string) + tipos monetários TS (produz "NaN%" quando total=0)

**Gap Wave 3** *(blocked on Gap Wave 2 completion)*

- [ ] 03-gap-03-PLAN.md — Fix WR-03: contagem real de lançamentos para aviso "Mostrando 50 de {N}" (retorna tamanho da página, não o total)

---

### Phase 4: IA via OpenRouter

**Goal:** Function calling sobre consultas fixas e categorização sugerida com confirmação explícita
**Mode:** mvp
**Success Criteria**:

1. Consultas de teste retornam os mesmos números que o dashboard (tools reusam as mesmas queries)
2. Categorização sugerida só grava após confirmação explícita do usuário
3. Chave OpenRouter existe somente no servidor; sem chave/limite, o app segue funcionando (degradação graciosa)

**Requirements:** AI-01, AI-02, AI-03
**UI hint**: yes

---

## Timeline

| Weeks | Phase(s) | Milestone |
|-------|----------|-----------|
| 1–2 | Phase 1 + Phase 2 | Login em produção com health check OK · lote com erro não grava nada |
| 3–4 | Phase 3 | Números batem com SQL de conferência |
| 5 | Phase 4 | Consultas da IA = números do dashboard |

## Coverage

- v1 requirements: 33 total
- Mapped to phases: 33
- Unmapped: 0 ✓

---
*Roadmap created: 2026-10-30*
