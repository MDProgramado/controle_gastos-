# Phase 6: Multi-tenant, Usuários e Google — Plano Mestre

**Phase:** 06-multi-tenant
**Source:** `MULT_TENANT.md` (aprovado 2026-11-21)
**Granularity:** 7 planos (um por Fase 0–6) · 2–3 tasks por plano
**Status:** Planned
**Criado:** 2026-11-21

<objective>
Transformar o Controle Financeiro de single-tenant em plataforma multi-tenant com cadastro self-service, gestão de usuários, login Google e isolamento financeiro por usuário (inclusive ADMIN sem acesso a dados alheios), preservando saldo exato ao centavo, tenant pelo JWT e migrations imutáveis.
</objective>

<wave_structure>
## Waves (ordem de execução — espelha as Fases 0–6 de MULT_TENANT.md)

| Wave | Plano | Fase | Entrega | Depende de | Autônomo |
|------|-------|------|---------|-----------|----------|
| 1 | `06-01-PLAN.md` | F0 | Estabilização: F-001/002/003, spotless, Testcontainers, baseline verde | — | sim |
| 2 | `06-02-PLAN.md` | F1 | Migration V9, entidades/repositórios, enum `Role` | 06-01 | sim |
| 3 | `06-03-PLAN.md` | F2 | Onboarding transacional, convites, usuários, endpoints + SecurityConfig | 06-02 | sim |
| 4 | `06-04-PLAN.md` | F3 | Isolamento por usuário em todas as queries + stores IA + ArchUnit | 06-02 | sim |
| 5 | `06-05-PLAN.md` | F4 | Validação ID token Google + BFF PKCE + rate limit | 06-03, 06-04 | sim (checkpoint de credenciais Google via `user_setup`) |
| 6 | `06-06-PLAN.md` | F5 | Páginas, BFFs, allowlist, sidebar por role, e2e Playwright | 06-03, 06-04, 06-05 | sim |
| 7 | `06-07-PLAN.md` | F6 | Segurança final, deploy Render/Vercel, LGPD, auditorias | 06-01–06-06 | sim |

**Nota de paralelismo:** 06-03 e 06-04 são da mesma wave lógica (ambos dependem só de 06-02) e podem rodar em paralelo **se** executados com worktrees separados; sem worktrees, executar 06-03 antes de 06-04 (ordem conservadora). Demais waves são estritamente sequenciais.
</wave_structure>

<coverage>
## Cobertura de requisitos e decisões

| Req (REQUIREMENTS.md / novo) | Fase | Plano(s) |
|------------------------------|------|----------|
| GROW-01 (multiempresa real) | F1–F4 | 06-02, 06-03, 06-04, 06-05 |
| GROW-02 (convite de usuários) | F2 | 06-03, 06-06 |
| LGPD-01 (pré-requisitos técnicos) | F6 | 06-07 |
| AUTH-01/03/05 (JWT, tenant do token, role) | F1–F3 | 06-02, 06-03, 06-04 |
| INF-01/02 (Flyway imutável, Testcontainers) | F0–F1 | 06-01, 06-02 |
| MT-01 (V9 + domínio multi-tenant) | F1 | 06-02 |
| MT-02 (onboarding + gestão de contas) | F2 | 06-03 |
| MT-03 (isolamento empresa+usuário) | F3 | 06-04 |
| MT-04 (Google OIDC + PKCE) | F4 | 06-05 |
| MT-05 (frontend + e2e) | F5 | 06-06 |
| MT-06 (segurança/deploy/LGPD) | F6 | 06-07 |

Decisões D-01..D-24 (ver `06-CONTEXT.md`) mapeadas em cada plano, seção "Decisions covered". Nenhuma decisão travada foi simplificada ou adiada.
</coverage>

<verification>
## Verificação global da fase

```powershell
.\mvnw.cmd "-Dspotless.check.skip=true" "-Dtest=!*IntegrationTest,!*MigrationTest" test
.\mvnw.cmd test
npm --prefix C:\Users\11\controle-gastos\web run lint
npx tsc --noEmit
npm --prefix C:\Users\11\controle-gastos\web run test:e2e
```

**Aceite final (MULT_TENANT.md §6):** tree limpo; 106 unitários + 13 integrações; rollback do onboarding; convite expirado/reutilizado rejeitado; cross-user/cross-tenant inclusive ADMIN→404/403; `aud`/`exp`/`email_verified` inválidos → 401; e2e de cadastro/convite/Google/gestão verde; nenhuma prévia aponta para produção; nenhum segredo em `NEXT_PUBLIC_*`.
</verification>

<risk_register>
## Registro de riscos (resumo — detalhes em `06-CONTEXT.md` §risks)

| Risco | Plano | Mitigação |
|-------|-------|-----------|
| Docker indisponível trava integração | 06-01 | Instalar/subir Docker antes de rodar `.\mvnw.cmd test` |
| Migration V9 sobre schema existente com dados | 06-02 | Testar em banco limpo **e** em cópia do schema atual; nunca editar V1..V8 |
| E-mail duplicado vazando enumeração | 06-03/06-05 | Erro genérico no cadastro; Google entra na empresa existente |
| ADMIN lendo dados alheios | 06-04 | Filtro `empresa_id + usuario_id` em toda query financeira + ArchUnit + teste 404/403 |
| Role tampering no JWT | 06-02/06-04 | Enum `Role` na emissão/conversão + `token_version`/`ativo` no filtro |
| ID token Google aceito com `aud` errado | 06-05 | Validar `iss`/`aud`/`exp`/`email_verified` com a lib Google; teste negativo 401 |
| ID/access token Google vazando para o browser | 06-05/06-06 | Sessão é só o JWT próprio no cookie `sess`; callback server-side |
| Multi-replica IA cruzando histórico | 06-04 | Chave `empresa_id::usuario_id::sessionId` + persistência em tabela |
| Segredo em `NEXT_PUBLIC_*` / preview→produção | 06-07 | `security-audit` + `seo-audit`; `GOOGLE_*` com `sync:false`; bloquear onboarding em preview |
</risk_register>

<next>
Executar `06-01-PLAN.md` (Wave 1). Cada plano traz tasks com `<verify>` automatizado, critérios de aceite e notas de risco.
</next>

---
*Phase: 06-multi-tenant · Plano mestre gerado em 2026-11-21 a partir de MULT_TENANT.md*
