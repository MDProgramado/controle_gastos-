# Walking Skeleton — Controle Financeiro

**Phase:** 1
**Generated:** 2026-10-30

## Capability Proven End-to-End

> Um usuário logado com o seed `admin@controle.com` acessa a web publicada (Vercel), autentica via BFF (cookie httpOnly) contra a API (Render) com JWT de 8h, vê o shell autenticado com `empresa_id` vindo exclusivamente do token, e consegue sair — com `GET /api/health` respondendo OK nos ambientes de staging e produção.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Monorepo | `api/` + `web/` na raiz, sem CI custom | Deploy por push nativo (D-07); build local `./mvnw verify` + lint `web/` |
| API framework | Spring Boot 3.5.16, Java 21 (`maven.compiler.release=21`), Maven | Stack travada no AGENTS.md; Maven é padrão Spring (sem curva); `release=21` evita bytecode incompatível com o runtime da Render |
| Web framework | Next.js 16.4.3 App Router + React 19.3.1 + TypeScript | LTS ativo; App Router dá route handlers (BFF) e `proxy.ts` |
| Auth | JWT HS256 de 8h (D-01) via Spring `oauth2-resource-server` (Nimbus); sem lib JWT extra, sem filtro manual | Framework valida `exp`/`alg`/clock-skew — hand-roll é origem de CVE |
| Sessão | Cookie httpOnly `sess` gerido pelo BFF Next (D-02); `SameSite=Lax`; `Secure` em produção; sem refresh token (expirou → re-login) | JWT fora do alcance de JS (anti-XSS); BFF same-origin elimina CORS/CSRF |
| Senhas | `BCryptPasswordEncoder(12)`; hash só no banco; senha real só em env var `SEED_ADMIN_PASSWORD` (D-04) | R6/AUTH-02; nada de segredo em repo/docs/commits |
| Seed | Flyway `V1__schema.sql` (schema) + `V1_1__seed_empresa_admin` Java migration lendo env var (fail-fast) (D-03) | SQL não lê env vars; seed de empresa/admin reproducível em qualquer ambiente sem segredo versionado |
| Schema/tenant | Flyway dono do schema (`ddl-auto=validate`, `ddl-auto=update` proibido); `empresa_id` em TODAS as tabelas; `TenantContext` lê só do JWT (R3) | INF-01; single-tenant hoje com trilho multiempresa pronto (§3.4) |
| Dinheiro (schema já nasce pronto) | `NUMERIC(14,2)` + `CHECK (valor > 0)`; zero `double`/`float` (R1/R7) | A Fase 2 usa o DDL sem rework; gate de grep desde o dia 1 |
| Erros | RFC 7807 `ProblemDetail` via `@RestControllerAdvice`; mensagens de auth genéricas (D-05) | Padrão único herdado pelas fases 2–4 (Fase 2 estende `{ linha, erro }`) |
| Health | Controller custom `GET /api/health` com `SELECT 1` | Caminho exigido é `/api/health`; Render recomenda checar o DB |
| UI | shadcn (new-york/slate + tokens do UI-SPEC), pt-BR, tema claro; login minimalista (D-05) | Contrato do UI-SPEC é a base de design reutilizada nas fases 2–4 |
| Testes | JUnit 5 + AssertJ + Testcontainers `@ServiceConnection` (Postgres real, Flyway roda a cada teste) | Padrão P6 — TODAS as fases herdam |
| Deploy/ambientes | 3 ambientes (D-06): local, staging, produção. Render (API + Postgres) ×2, Vercel (web) com `API_URL` por ambiente; push → deploy (D-07) | Preço do staging pago com isolamento (ver D-08) |
| **D-08 (decisão do planner)** | **Staging com banco Postgres SEPARADO** (2ª instância dedicada no Render), espelhando produção: 1 serviço + 1 banco por ambiente | Custo +US$13–25/mês aceito vs. risco de deploy de teste tocar dados reais; env vars por serviço; modelo reproduzível quando houver mais clientes/empresas (motivo do D-06). Banco único rejeitado: exporia dados reais a previews e exigiria limpeza manual frágil |

## Stack Touched in Phase 1

- [x] Project scaffold (Spring Boot Maven + Next.js com shadcn/ESLint/Prettier/Spotless)
- [x] Routing — `/login` (pública), `/` (protegida via `proxy.ts`), rotas BFF `/api/auth/login|logout`, `/api/me`
- [x] Database — Flyway V1 aplica do zero (Testcontainers) + V1_1 escreve seed; `SELECT 1`/`/api/auth/me` leem
- [x] UI — `LoginForm` (client controlado) com estados do UI-SPEC, wireado ao BFF
- [x] Deployment — Render (API, 2 ambientes) + Vercel (web) por push; smoke automatizado + verificação humana

## Out of Scope (Deferred to Later Slices)

> Nada abaixo deve ser re-aberto nas fases 2–4 sem nova decisão do usuário.

- "Esqueci a senha"/redefinição; verificação de e-mail; cadastro de empresas (GROW-01) — deferred do CONTEXT
- Refresh token (expiração 8h → re-login, D-02)
- Multiempresa real (schema já preparado; app é single-tenant)
- CI custom (premissa: push → deploy nativo); domínio próprio
- Dark mode; react-hook-form/zod (entra na Fase 2 com os formularios de lançamento)
- Telas de categorias/lançamentos/dashboard (Fases 2–3); IA (Fase 4)
- Rate limiting/`SameSite=None`/CORS cross-domain (BFF elimina a necessidade)

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2: seed `V2__seed_categorias.sql` + CRUD de categorias (ADMIN) e de lançamentos com validação monetária estrita (R1/R2) e lote tudo-ou-nada (R4)
- Phase 3: dashboard com entradas/saídas/saldo por mês, gasto por categoria e comparação — números idênticos ao SQL de conferência
- Phase 4: IA via OpenRouter (function calling sobre as mesmas queries do dashboard; sugestão de categoria só grava com confirmação explícita)
