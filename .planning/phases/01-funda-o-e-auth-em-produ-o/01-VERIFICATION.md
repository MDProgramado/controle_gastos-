---
phase: 01-funda-o-e-auth-em-produ-o
verified: 2026-11-05T16:34:30Z
status: passed
score: 12/13 must-haves verified (1 partially deferred to Phase 2 — INF-01 clause V2)
overrides_applied: 2
overrides:
  - must_have: "Ambiente de staging com banco Postgres separado; três ambientes mantidos (local, staging, produção); GET /api/health respondendo também em staging (plan 01-04 must_haves)"
    reason: "Desvio D-08 aprovado pelo usuário no checkpoint de 2026-11-05 — provisionado  único ambiente de produção com banco Render Free (restrição de custo); staging adiado como débito registrado. Todos os 4 Success Criteria da ROADMAP são de produção e foram cumpridos."
    accepted_by: "user (checkpoint 01-04, 2026-11-05)"
    accepted_at: "2026-11-05"
  - must_have: "render.yaml blueprint com build Maven nativo e start java -jar ... server.port=$PORT (plan 01-04 artifact contains: server.port=$PORT)"
    reason: "A Render não oferece runtime Java nativo (plataforma) — serviço usa runtime Docker via api/Dockerfile; PORT é consumido por application.yml (${PORT:8080}). Desvio registrado e aprovado no checkpoint; render.yaml reflete o runtime Docker."
    accepted_by: "user (checkpoint 01-04, 2026-11-05)"
    accepted_at: "2026-11-05"
deferred:
  - truth: "INF-01: seed de categorias (Flyway V2); migrations imutáveis — cláusula V2"
    addressed_in: "Phase 2 — Categorias e Lançamentos"
    evidence: "ROADMAP Phase 2 goal: 'Seed V2 de categorias, CRUD de lançamentos…'; INF-01 cláusula V1 (schema imutável) já cumprida na Fase 1 (V1__schema.sql + comentário de imutabilidade; V2 livre para categorias — V1_1 javadoc)"
---

# Phase 01: Fundação e Auth em Produção — Verification Report

**Phase Goal (ROADMAP):** Monorepo (`api/` + `web/`), Flyway V1, autenticação JWT, seed de empresa/usuário, deploy da API na Render e da web na Vercel — "login funciona em produção, health check OK".

**Verified:** 2026-11-05T16:34:30Z
**Status:** passed
**Re-verification:** No — initial verification (no previous VERIFICATION.md)
**Mode:** mvp — observação: a meta da ROADMAP não está em formato user-story, mas os PLANs 01-02/01-03/01-04 carregam a goal em formato user-story ("As a usuário da empresa cliente, I want to me autenticar com e-mail/senha na web publicada, so that eu acesse o sistema em produção com credencial segura"). A seção User Flow Coverage usa essa história. Discrepância de formato da meta da ROADMAP registrada como warning de bookkeeping (ver "Bookkeeping" abaixo) — recomenda-se `/gsd mvp-phase 1` apenas se o processo exigir a goal da ROADMAP reformatada.

---

## User Flow Coverage (MVP mode)

User story (PLANs): «As a usuário da empresa cliente, I want to me autenticar com e-mail/senha na web publicada, so that eu acesse o sistema em produção com credencial segura (HTTPS, sessão protegida, sem senha em texto puro).»

| Step | Expected | Evidence | Status |
|------|----------|----------|--------|
| Abrir a web publicada | `/` redireciona para `/login` sem sessão | `web/proxy.ts:25-26` (cookie gate) + **live**: `GET https://controle-gastos-three-blond.vercel.app/` → redirect → `/login` 200 (re-executado hoje) | ✓ |
| Ver a tela de login | Tela pt-BR com "E-mail"/"Senha"/"Entrar" | `web/app/login/page.tsx` + `web/components/LoginForm.tsx`; **live**: HTML de `/login` contém "Controle Financeiro", "E-mail", "Senha", "Entrar", "seu@email.com" (re-executado hoje) | ✓ |
| Submeter credencial errada | Mensagem genérica pt-BR, sem enumeração | `web/app/api/auth/login/route.ts:31-34` (mesma msg p/ 401) + API `AuthService.java:30-35` (3 casos → mesma exceção) + E2E #4 e teste `senhaErradaEEmailInexistenteDevolvemAMesmaMensagemGenerica` | ✓ |
| Submeter credencial correta | Cookie httpOnly `sess` setado; redirect ao shell | **Smoke produção 2026-11-05:** BFF POST → 200 + `Set-Cookie: sess=…; Path=/; Max-Age=28800; Secure; HttpOnly; SameSite=lax`; código `login/route.ts:50-56`; **checkpoint humano APROVADO** (01-04, passo 2: "conseguir fazer o login do projeto") | ✓ |
| Ver shell autenticado | Página `/` mostra "Você está autenticado." + "Sair" | `web/app/page.tsx:15-35` (Server Component valida sessão via `getMe()` → API) + E2E #5/#8 + checkpoint humano wave 3 (10/10) | ✓ |
| Sair | Limpa cookie e volta ao `/login` | `web/app/api/auth/logout/route.ts:6` (`cookieStore.delete("sess")`) + E2E #9 + checkpoint | ✓ |
| Outcome — "acesso em produção com credencial segura" | Login funciona na Vercel→Render; HTTPS; senha nunca em texto puro | SC1+SC2 abaixo + `Secure; HttpOnly` no cookie live + redirect http→https 301/308 live + BCrypt(12) no seed | ✓ |

---

## Success Criteria (ROADMAP) — Verdict Table

| # | Criterion (verbatim) | Verdict | Evidence |
|---|----------------------|---------|----------|
| 1 | Login com e-mail/senha funciona na web publicada (Vercel → API Render) | **ACHIEVED** | Smoke produção 2026-11-05: BFF `POST /api/auth/login` → 200 + cookie `sess` `HttpOnly; Secure; SameSite=lax; Max-Age=28800`; checkpoint humano **APROVADO** 2026-11-05 (01-04 Task 3, 7 passos, "conseguir fazer o login do projeto"). Re-executado hoje: web no ar servindo `/login` com copy UI-SPEC, `GET /` → redirect `/login`, rota BFF implantada (`POST /api/auth/login` corpo vazio → **400** validação, não 404), `GET /api/me` sem cookie → **401**. E2E local 10/10 (web/e2e/auth.spec.ts). Código: `login/route.ts` → `fetch(process.env.API_URL)/api/auth/login` → cookie httpOnly; `lib/api.ts` → `Authorization: Bearer` → `/api/auth/me`. |
| 2 | `GET /api/health` responde OK em produção | **ACHIEVED** | **Re-executado pelo verificador hoje:** `curl https://controle-gastos-7a6m.onrender.com/api/health` → **200 `{"status":"OK"}`** (82s — cold start do plano Free). Código: `HealthController.java:24-27` (`SELECT 1` + `{"status":"OK"}`), `permitAll` em `SecurityConfig.java:38-39`, `healthCheckPath: /api/health` no `render.yaml`. Teste: `healthRetornaOkQuandoBancoAcessivel`. |
| 3 | `empresa_id` é extraído do token em todas as rotas autenticadas (teste de integração prova que o body não define tenant) | **ACHIEVED** | `AuthTenantIntegrationTest.corpoHostilComEmpresaIdNaoDefineTenant_TokenManda` (linha 51): corpo hostil `{"empresa_id": 999}` → eco `empresa_id: 1` (tenant do token). `TenantContext.java` é o **único** leitor do tenant (grep em `api/src/main/java`: nenhuma leitura de `empresa_id` de body/header/query — só `SecurityContext`/`UsuarioAuth`/claims JWT). Conversor `SecurityConfig.jwtAuthenticationConverter:57-68` (claim → principal). Gates estruturais: `LoginRequestEstruturaTest` (2 testes por reflexão — nenhum campo "empresa"). Rotas autenticadas da Fase 1 = `/api/auth/me` (+ probe de teste); `/api/categorias/**` já amarrada a `hasRole(ADMIN)` para a Fase 2 com o mesmo `TenantContext`. |
| 4 | Seed de empresa e usuário ADMIN criado exclusivamente por migration Flyway V1 | **ACHIEVED** (nota de nomenclatura) | `V1_1__seed_empresa_admin.java` (Flyway Java migration da linha V1): 1 empresa + `admin@controle.com` role ADMIN, BCrypt(12) de `SEED_ADMIN_PASSWORD` (fail-fast se ausente). Grep em `api/src/main`: **apenas** a migration contém `INSERT INTO empresa/usuario` — zero `CommandLineRunner`/`@PostConstruct`/endpoint de cadastro. Testes: `seedCriaExatamenteUmaEmpresaEUmUsuarioAdmin`, `senhaDoSeedEhHashBcryptENuncaTextoPuro`, `migrationFalhaRapidoQuandoSenhaDoAmbienteNaoExiste` (3/3). Produção: login funcional prova que o seed roda no boot (registro log "Successfully applied 2 migrations" não confirmado textualmente — inferência funcional: banco novo do zero + admin logável ⇒ V1+V1_1 aplicadas). Nota: o seed está em **V1_1** (não em `V1__schema.sql`) — "V1" no sentido da série V1; o próprio must_have do PLAN 01-02 especifica V1_1. |

**Score:** 4/4 success criteria ACHIEVED.

---

## Requirement Verdicts

| Req | Description (resumo) | Verdict | Evidence |
|-----|----------------------|---------|----------|
| AUTH-01 | Login e-mail/senha → JWT usado em todas as chamadas autenticadas | **ACHIEVED** | `POST /api/auth/login` (`AuthController`/`AuthService`/`JwtService`, HS256 8h) → BFF guarda em cookie → chamadas autenticadas usam `Authorization: Bearer` (`lib/api.ts:29`, `me/route.ts:18`); API stateless `oauth2ResourceServer` (`SecurityConfig:45-47`); rota protegida sem/inválido token → 401 (teste `rotaProtegidaSemTokenOuComTokenAleatorioRetorna401`; **live:** `/api/auth/me` sem token → 401). |
| AUTH-02 | Senhas apenas como hash BCrypt | **ACHIEVED** | Seed gera `BCryptPasswordEncoder(12)` (`V1_1:33`); login checa via `passwordEncoder.matches` (`AuthService:33`); `senha_hash` anotado `@JsonIgnore` (`Usuario.java:38`); teste `senhaDoSeedEhHashBcryptENuncaTextoPuro` (prefixo `$2`, nunca texto puro); senha só de env `SEED_ADMIN_PASSWORD` (nenhum segredo em repo — grep). |
| AUTH-03 | `empresa_id` exclusivamente do token; nenhum body define tenant | **ACHIEVED** | Ver SC3 — teste hostil + `TenantContext` único leitor + gates estruturais do `LoginRequest`. |
| AUTH-04 | Seed empresa + ADMIN criado por migration Flyway (V1) | **ACHIEVED** | Ver SC4 — `V1_1__seed_empresa_admin.java` é o único caminho de INSERT; testes de seed verdes. |
| AUTH-05 | Roles ADMIN/USER desde a fase 1; JWT carrega a role | **ACHIEVED** | Coluna `role VARCHAR(16) DEFAULT 'USER'` (`V1__schema.sql:19`); seed insere `ADMIN`; `JwtService:69` claim `role`; conversor `ROLE_{role}` (`SecurityConfig:65`); teste `meComTokenValidoDevolveSessaoCorreta` assera `role: ADMIN`. |
| AUTH-06 | Comunicação em produção somente HTTPS | **ACHIEVED** | **Re-executado hoje:** HTTP→HTTPS **301** (Render) e **308** (Vercel); cookie de sessão com `Secure` em produção (`login/route.ts:54`, smoke item 3 confirmou `Secure` no live). |
| INF-01 | Flyway gerencia schema (V1) e seed de categorias (V2); migrations imutáveis | **PARTIAL → V2 DEFERRED (Phase 2)** | Cláusula V1: `V1__schema.sql` imutável (comentário INF-01/T-1-01), `ddl-auto: none`/`validate`, Flyway dono do schema, teste `flywayV1AplicadaDoZeroComSucesso` + gate R1 (NUMERIC(14,2)/CHECK) verdes. Cláusula V2 (seed de categorias) pertence à Fase 2 — ver `deferred` no frontmatter. |
| INF-03 | API publicada na Render com health check OK em produção | **ACHIEVED** | Ver SC2 — 200 ao vivo hoje + `render.yaml healthCheckPath` + `api/Dockerfile` (multi-stage Maven + JRE 21, `ENTRYPOINT java -jar`, `${PORT:8080}` no `application.yml`). |
| INF-04 | Web publicada na Vercel consumindo a API publicada (login funciona em produção) | **ACHIEVED** | Ver SC1 — web live, BFF consome `process.env.API_URL` (server-only), checkpoint humano de login em produção aprovado. **Live:** rota BFF e gate `/api/me` respondendo no domínio Vercel. |

**Requirements:** 8/9 ACHIEVED; INF-01 parcialmente adiado (cláusula V2 — Phase 2, `deferred`).

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `api/` + `web/` monorepo | dois apps buildando | ✓ VERIFIED | `api/pom.xml`, `api/mvnw`, `web/package.json`; builds verdes documentados (16/16 Maven; `npm run lint`+`build`), commits existem no git (`cat-file` → commit). |
| `api/src/main/resources/db/migration/V1__schema.sql` | DDL 4 tabelas, NUMERIC(14,2), empresa_id em todas | ✓ VERIFIED | 4 tabelas, `CHECK (valor > 0)`, `empresa_id NOT NULL` nas 3 tabelas de dados, 3 índices; teste gate R1. |
| `api/.../db/migration/V1_1__seed_empresa_admin.java` | Seed empresa+ADMIN exclusivo | ✓ VERIFIED | Substantivo (INSERTs + BCrypt + fail-fast); único caminho de INSERT; wired via Flyway no boot. |
| `api/.../auth/*` (AuthController, AuthService, JwtService, LoginRequest/Response) | Login JWT | ✓ VERIFIED | 5 arquivos reais com lógica; testes AuthIntegrationTest (4). |
| `api/.../common/TenantContext.java` | Leitura única do tenant | ✓ VERIFIED | Único leitor (grep); usado por `AuthMeController:25`; wired no SecurityContext. |
| `api/.../config/SecurityConfig.java` | Cadeia completa resource-server | ✓ VERIFIED | Extendida (não duplicada): STATELESS, permitAll login/health, `oauth2ResourceServer` + converter, `hasRole ADMIN` seed fase 2. |
| `api/.../health/HealthController.java` | GET /api/health com SELECT 1 | ✓ VERIFIED | `@GetMapping("/api/health")`, `SELECT 1`, retorno `{"status":"OK"}`; erro → 5xx genérico. |
| `web/app/api/auth/{login,logout}/route.ts`, `web/app/api/me/route.ts` | BFF cookie httpOnly | ✓ VERIFIED | 3 rotas reais; cookie `sess` httpOnly/lax/secure-prod/maxAge 8h; logout deleta cookie; me faz proxy Bearer + limpa cookie em 401. |
| `web/proxy.ts` | Gate por cookie (Next 16) | ✓ VERIFIED | `export function proxy` + matcher; **live:** redirect `/`→`/login` funcionando em produção; `middleware.ts` ausente. |
| `web/app/login/page.tsx` + `LoginForm.tsx` + `web/app/page.tsx` | Tela login UI-SPEC + shell | ✓ VERIFIED | Copy exata confirmada no HTML live; shell real com `getMe()` (não placeholder — dashboard é escopo Fase 3 e diz isso ao usuário). |
| `render.yaml` + `api/Dockerfile` | Blueprint + runtime | ✓ VERIFIED (com overrides) | Ambos existem e são substantivos; `render.yaml` **inerte** (não vinculado ao dashboard — serviço provisionado manualmente) e diverge do plano nativo-Maven → overrides frontmatter. |
| Testes: `AuthTenantIntegrationTest`, `SeedMigrationTest`, `HealthIntegrationTest`, `AuthIntegrationTest`, `LoginRequestEstruturaTest`, `web/e2e/auth.spec.ts` | Cobertura | ✓ VERIFIED | Contagem de `@Test` no código = **16** (bate com 16/16 documentado); `test(` no E2E = **10** (bate com 10/10). |

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `SecurityConfig.jwtAuthenticationConverter` | `TenantContext.getEmpresaId` | claim `empresa_id` → `UsuarioAuth` → SecurityContext | WIRED | `SecurityConfig:59-66` → `UsuarioAuthAuthenticationToken` → `TenantContext:18-25`; teste hostil prova ponta a ponta. |
| `web/app/api/auth/login/route.ts` | API `POST /api/auth/login` | `fetch(process.env.API_URL…)` server-side | WIRED | `route.ts:20` + tratamento de status/token + set do cookie. |
| `web/app/api/me/route.ts` / `lib/api.ts` | API `GET /api/auth/me` | cookie `sess` → `Authorization: Bearer` | WIRED | `route.ts:17-18`, `api.ts:29-30`; **live** `/api/me` → 401 sem cookie. |
| `web/proxy.ts` | cookie `sess` | `request.cookies.has("sess")` decide `/` ↔ `/login` | WIRED | Nota: o pattern do plano previa `cookies()` — em proxy.ts do Next 16 o acesso correto é `request.cookies` (comentário no próprio arquivo). Implementação correta; **live** confirmado. |
| `LoginForm.tsx` | BFF login | `fetch("/api/auth/login")` → `router.push("/")` | WIRED | `LoginForm:79,86`; valida sessão via `/api/me` (`:42`). |
| `V1_1__seed_empresa_admin` | `usuario.senha_hash` | `BCryptPasswordEncoder(12).encode(SEED_ADMIN_PASSWORD)` | WIRED | `V1_1:31-33,55` + testes de seed. |
| Render boot | Flyway V1 + V1_1 | migrations no startup com env do serviço | WIRED (funcional) | Padrão textual "Successfully applied" não capturado, mas: banco novo do zero + login em produção funcionando ⇒ seed aplicado no boot; `flyway.enabled` + `ddl-auto: none` em `application.yml`. |
| Vercel (web) | Render (api) | env `API_URL` server-only | WIRED (live) | BFF respondendo no domínio Vercel consome a API (checkpoint login aprovado + rota 400 ao vivo); grep: zero `NEXT_PUBLIC_`. |

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `web/app/page.tsx` (shell) | `usuario` via `getMe()` | cookie `sess` → API `/api/auth/me` → DB (`UsuarioRepository.findByEmail` + seed) | Sim — email/empresa_id/role vindos do banco via JWT; sem sessão → redirect | ✓ FLOWING |
| `web/app/login/page.tsx` + `LoginForm` | erro/sucesso do login | BFF → API `AuthService` → DB (`passwordEncoder.matches`) | Sim — 401 genérico vs 200+token reais | ✓ FLOWING |
| `AuthMeController` | `MeResponse` | `TenantContext.getEmpresaId()` (JWT) + principal | Sim — asserido no teste `/me` com valores do seed | ✓ FLOWING |
| `HealthController` | `SELECT 1` | conexão JDBC real com o Postgres | Sim — 500 se DB inacessível (design para restart do Render) | ✓ FLOWING |
| Seed `V1_1` | 1 empresa + 1 admin | `SEED_ADMIN_PASSWORD` env + BCrypt | Sim — único INSERT do app; prova indireta: login em produção | ✓ FLOWING |

Nenhum artefato HOLLOW/DISCONNECTED: não há props vazias hardcoded nem retornos estáticos nos caminhos verificados.

## Behavioral Spot-Checks (re-executados pelo verificador — 2026-11-05)

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Health OK em produção | `curl.exe https://controle-gastos-7a6m.onrender.com/api/health` | `{"status":"OK"}` HTTP 200 (82,5s cold start) | ✓ PASS |
| HTTP→HTTPS (Render) | `curl.exe http://…onrender.com/api/health` | 301 → https://… | ✓ PASS |
| HTTP→HTTPS (Vercel) | `curl.exe http://controle-gastos-three-blond.vercel.app/` | 308 → https://… | ✓ PASS |
| Web publicada redireciona p/ login | `curl.exe -L https://…vercel.app/` | 200 final em `/login` | ✓ PASS |
| Tela de login com copy UI-SPEC | fetch `/login` + contains checks | "Controle Financeiro" / "E-mail" / "Senha" / "Entrar" / "seu@email.com" = True | ✓ PASS |
| Rota BFF de login implantada | `POST /api/auth/login` corpo vazio | 400 (validação — não 404) | ✓ PASS |
| Rota autenticada sem sessão → 401 | `GET /api/me` sem cookie (Vercel) e `GET /api/auth/me` sem token (API) | 401 / 401 | ✓ PASS |
| Teste hostil do tenant | leitura do código `AuthTenantIntegrationTest:51-68` | corpo `{"empresa_id":999}` → tenant 1 do token | ✓ PASS (teste não re-executado — Maven instruído a não rodar; contagem `@Test`=16 bate com 16/16 documentado) |
| E2E Playwright | leitura `web/e2e/auth.spec.ts` | 10 `test(` presentes, cobrindo redirect/cookie/logout/expiração | ✓ PASS (não re-executado — requer serviços locais; 10/10 documentado + checkpoint humano) |

## Requirements Coverage

| Requirement | Plan(s) | Status | Evidence |
|-------------|---------|--------|----------|
| AUTH-01 | 01-02, 01-03 | ✓ SATISFIED | Ver tabela de requirements |
| AUTH-02 | 01-02 | ✓ SATISFIED | BCrypt 12 + `@JsonIgnore` + teste seed |
| AUTH-03 | 01-02 | ✓ SATISFIED | Teste hostil + `TenantContext` único |
| AUTH-04 | 01-02 | ✓ SATISFIED | V1_1 único INSERT |
| AUTH-05 | 01-02 | ✓ SATISFIED | Claims `role` + authorities + teste `/me` |
| AUTH-06 | 01-04 | ✓ SATISFIED | 301/308 live + cookie `Secure` |
| INF-01 | 01-01, 01-02 | △ PARCIAL (V2 → Phase 2) | V1 imutável ok; seed categorias = Phase 2 (`deferred`) |
| INF-03 | 01-04 | ✓ SATISFIED | Health 200 live |
| INF-04 | 01-04 | ✓ SATISFIED | Vercel live + BFF→API + checkpoint login |

**Orphaned requirements:** nenhuma — todas as 9 requirements da fase aparecem em pelo menos um PLAN (`requirements:` frontmatter: 01-01 INF-01; 01-02 AUTH-01..05+INF-01; 01-03 AUTH-01; 01-04 AUTH-06+INF-03+INF-04).

## Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| — | `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` em `api/src`, `web/app`, `web/components`, `web/lib`, `web/e2e` | — | **0 ocorrências** (nenhum debt marker → gate passa) |
| — | `double`/`float` em `api/src` | — | 0 ocorrências |
| — | `localStorage`/`sessionStorage`/`NEXT_PUBLIC_` em `web` | — | 0 ocorrências |
| — | `return null`/handler vazio em componentes de login/shell | — | Nenhum stub — `page.tsx` valida sessão real; shell sem dashboard é escopo explícito da Fase 3 (mensagem ao usuário, não placeholder silencioso) |
| `render.yaml:21-24` | Nota "blueprint inerte" | ℹ️ Info | Blueprint documentado mas não vinculado no dashboard — não bloqueia (serviço real provisionado e funcionando); risco de drift infra-as-code |
| `.planning/ROADMAP.md:22,39` | "Plans: 3/4 plans executed" + checkbox `01-04` desmarcado | ⚠️ Warning (bookkeeping) | ROADMAP desatualizada: 01-04 está COMPLETE+aprovado. Corrigir no phase.complete. |
| `01-04-SUMMARY.md` untracked (`git status ??`) | SUMMARY não commitada | ⚠️ Warning (bookkeeping) | Commitar junto com este VERIFICATION.md no bundle do orquestrador. |

Nenhum 🛑 Blocker.

## Deviations & Residual Risks

**Deviations (registradas, aprovadas — overrides no frontmatter):**

1. **D-08 — ambiente único** (1 serviço Render + 1 Postgres Free, sem staging, sem bancos pagos). Override aplicado aos must_haves de staging do PLAN 01-04. Consequências: isolamento preview→staging **não vale** (previews Vercel apontam p/ API de produção); banco **expira 2026-12-05**.
2. **Runtime Docker em vez de Maven nativo** (plataforma: Render sem JVM nativo). Override aplicado ao artifact `render.yaml` do PLAN 01-04.

**Residual risks (não bloqueiam a meta da fase):**

| # | Risk | Severity | Note |
|---|------|----------|------|
| R1 | **Segredos expostos em chat** (`JWT_SECRET`, senha do DB, `SEED_ADMIN_PASSWORD`) — rotação pendente | 🔴 Alto (segurança) | Débito registrado no SUMMARY 01-04; rodar rotação nos dashboards. Nenhum valor aqui — verificação não imprime segredos. |
| R2 | **Banco Free expira 2026-12-05** | 🔴 Alto (continuidade) | Upgrade para pago com backup antes da data. |
| R3 | Cold start do Render Free: health levou **82s** na primeira chamada | ⚠️ Médio | Criterion 2 cumprido (200), mas espera perceptível; ok para fase 1. |
| R4 | Sem staging / previews apontam p/ produção (D-08) | ⚠️ Médio | Débito registrado; provisionar staging quando escalar. |
| R5 | `render.yaml` inerte (não vinculado como Blueprint) | ⚠️ Baixo | Se vincular, conferir `name` antes (aviso no próprio arquivo). |
| R6 | Smoke "Successfully applied 2 migrations" não capturado textualmente | ℹ️ Baixo | Inferência funcional sólida (banco novo + login OK); se quiser prova literal, ler `flyway_schema_history` no dashboard da Render. |
| R7 | E2E/Maven não re-executados pelo verificador (instrução do orquestrador) | ℹ️ Baixo | Contagens no código batem exatamente com os resultados documentados (16 `@Test` = 16/16; 10 `test(` = 10/10). |

## Human Verification Required

**Nenhum item pendente.** Todos os checks humanos da fase foram executados e aprovados antes desta verificação:

- **Wave 3 (01-03):** checklist manual de login local — **APROVADO** 2026-11-05, 10/10 passos (redirect, credencial errada, login ok, teclado, aba anônima, logout, mobile 375px).
- **Wave 4 (01-04):** critério de aceite 1 — **APROVADO** 2026-11-05: login na web publicada (Vercel→Render), mensagem genérica, logout, cadeado HTTPS + cookie HttpOnly/Secure.
- Itens marcados no checkpoint como pendentes são **débitos acima** (rotação de senha pós-login — R1), não verificações de aceite da fase.

Nenhum `<verify><human-check>` adiado existe nos PLANs (grep = 0).

---

## Gaps Summary

**Nenhum gap bloqueante.** Os 4 Success Criteria da ROADMAP estão ACHIEVED com evidência de código + testes + produção ao vivo (health, redirects, páginas e rotas re-executados pelo verificador em 2026-11-05) + checkpoints humanos aprovados. 8/9 requirements SATISFIED; INF-01 parcialmente adiado (cláusula V2 de categorias é escopo declarado da Phase 2 — `deferred`). Dois overrides documentados no frontmatter cobrem as deviations aprovadas (D-08 ambiente único; runtime Docker).

**Phase VERDICT: ACHIEVED** — "login funciona em produção, health check OK" comprovado; pronto para a Fase 2, acompanhando os débitos R1 (rotação de segredos) e R2 (banco pago antes de 2026-12-05).

---

_Verified: 2026-11-05T16:34:30Z_
_Verifier: the agent (gsd-verifier)_
