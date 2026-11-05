# Plan Summary — 01-04 (Phase 1 — Fundação e Auth em Produção)

**Plan:** Deploy (Render + Vercel) + smoke + verificação humana
**Wave:** 4
**Status:** COMPLETE — human checkpoint approved (2026-11-05)

---

## Tasks Completed

| Task | Name | Commit | Files | Verified |
|------|------|--------|-------|----------|
| 1 | Provisionamento humano (Render + Vercel) — ação de dashboard | (dashboard) | — | **APROVADO com desvio D-08** (1 ambiente único — ver Deviations) |
| 2 | `render.yaml` (blueprint Docker) + smoke de produção | `bcf759b` | render.yaml | smoke itens 1–5 verdes (tabela abaixo) |
| 3 | Checkpoint humano — critério de aceite 1 (login na web publicada) | — | — | **APROVADO** (2026-11-05) |

Commits da wave: `922e131` (trabalho próprio do usuário — tela de login → dashboard), `f7308c3` (Dockerfile + .dockerignore, usuário), `bcf759b` (render.yaml, agente).

---

## Ambiente em produção (resultado final)

| | |
|---|---|
| **API (Render)** | `https://controle-gastos-7a6m.onrender.com` — runtime **Docker** (`api/Dockerfile`, multi-stage Maven + JRE 21), health `/api/health` |
| **Web (Vercel)** | `https://controle-gastos-three-blond.vercel.app` — Root Directory `web/`, env `API_URL` apontando para a API |
| **Banco (Render Postgres)** | `controle_gastos_db` — Free, Oregon, PostgreSQL 18, expira **2026-12-05** |
| **Env vars da API** | `JWT_SECRET`, `SEED_ADMIN_PASSWORD`, `SPRING_DATASOURCE_URL` (formato `jdbc:`), `SPRING_DATASOURCE_USERNAME/PASSWORD` — todas `sync: false` no blueprint |

---

## Incidents resolvidos durante o deploy

| # | Erro | Causa | Correção |
|---|------|-------|----------|
| 1 | `JAVA_HOME environment is not defined correctly` | Serviço Render criado com runtime **Node** — a Render **não tem runtime Java nativo** (só Node/Python/Ruby/Go/Rust/Elixir) | `api/Dockerfile` + `.dockerignore` (commit `f7308c3`); serviço recriado com runtime **Docker** |
| 2 | `'url' must start with "jdbc"` | `SPRING_DATASOURCE_URL` no formato `postgres://` (psql) | convertido para `jdbc:postgresql://…` (Pitfall 2) |
| 3 | `JDBC URL invalid port number` / `Driver claims to not accept jdbcUrl` | usuário + senha embutidos na URL JDBC (`user:senha@host`) — o driver JDBC não aceita | credenciais removidas da URL; ficam só em `SPRING_DATASOURCE_USERNAME/PASSWORD` + host com `:5432` explícito |

---

## Smoke (Task 2)

| # | Item | Resultado |
|---|------|-----------|
| 1 | `GET /api/health` → 200 | ✅ `{"status":"OK"}` (antes e depois do redeploy do `bcf759b`) |
| 2 | Login via API → 200 (token nunca impresso) | ✅ |
| 3 | Login via BFF → 200 + cookie `sess` | ✅ `HttpOnly; Secure; SameSite=lax; Max-Age=28800` (8h = D-01) |
| 4 | HTTP → HTTPS | ✅ Render `301`, Vercel `308` (AUTH-06) |
| 5 | Greps anti-pattern | ✅ zero `double/float` · zero `localStorage`/`NEXT_PUBLIC_` · `proxy.ts` presente / `middleware.ts` ausente · `port: ${PORT:8080}` · `ddl-auto: none` (Flyway é dono do schema) |
| 6 | Log `Successfully applied 2 migrations` | ⚠️ não confirmado textualmente pelo usuário; **inferido funcional** — banco novo criado do zero + login OK prova que V1+V1_1 aplicaram no boot |

---

## Deviations (aprovadas pelo usuário no checkpoint)

1. **D-08 — ambientes:** o plano previa **staging + produção** (2 serviços Render + 2 Postgres pagos com backup); foi provisionado **1 ambiente único** com **banco Free**. Aprovado no questionário de checkpoint ("Registrar desvio e seguir").
   - Consequências registradas: isolamento preview→staging (Pitfall 8/T-4-03) **não está valendo** — previews da Vercel apontam para a API de produção; banco **expira em 2026-12-05** (migrar para pago com backup = débito futuro).
2. **Runtime Docker** em vez de build Maven nativo — obrigatório (a Render não suporta JVM nativa); `render.yaml` reflete isso.
3. **`render.yaml` inerte:** blueprint documentado mas **não vinculado** a um Blueprint no dashboard (se vincular, conferir o `name` do serviço antes — ver nota no arquivo).

## Débitos pendentes (não bloquearam o checkpoint)

- **Rotação de segredos (D-04/T-4-01/T-4-02):** `JWT_SECRET`, senha do banco e `SEED_ADMIN_PASSWORD` foram expostos em conversa — rotacionar nos dashboards.
- Upgrade do banco para pago com backup (antes de 2026-12-05).
- Provisionar staging (2º serviço + 2º banco + `API_URL` de Preview na Vercel) quando fizer sentido.

---

## Verification Checklist (Task 3 — Human, 7 passos)

| # | Passo | Status |
|---|-------|--------|
| 1 | Aba anônima abre `/login` na Vercel | ✅ |
| 2 | Login `admin@controle.com` → shell autenticado | ✅ (usuário confirmou: "conseguir fazer o login do projeto") |
| 3 | Senha errada → mensagem genérica | ✅ (provado por E2E Playwright #4 na wave 3 + verificação manual) |
| 4 | "Sair" → volta ao `/login` | ✅ (E2E Playwright #9 na wave 3) |
| 5 | Cadeado HTTPS + cookie `sess` HttpOnly/Secure, sem token em `localStorage` | ✅ (smoke itens 3–4 + greps) |
| 6 | Preview → staging | ⛔ N/A (desvio D-08 aprovado) |
| 7 | Rotação `SEED_ADMIN_PASSWORD` pós-login | ⏳ **débito pendente** (registrado acima) |

## Critérios de sucesso da fase (ROADMAP)

1. Login com e-mail/senha funciona na web publicada → ✅ (checkpoint)
2. `GET /api/health` OK em produção → ✅ (smoke 1)
3. `empresa_id` exclusivamente do token → ✅ (`mvn verify` 16/16, AuthTenantIntegrationTest)
4. Seed exclusivamente por migration Flyway → ✅ (smoke 6 — inferido; SeedMigrationTest)

---

## Resume Signal

**APROVADO pelo usuário em 2026-11-05** — fase 1 encerrada (4/4 plans). Seguindo para Verify Phase 1 (VERIFICATION.md) e phase.complete.
