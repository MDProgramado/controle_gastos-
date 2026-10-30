# Phase 1: Fundação e Auth em Produção - Context

**Gathered:** 2026-10-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Entrega o alicerce do produto funcionando em produção: monorepo (`api/` Spring Boot + `web/` Next.js), schema inicial Flyway V1 (empresa, usuario, categoria, lancamento), login JWT com seed de empresa/usuário, deploy da API na Render e da web na Vercel, e health check respondendo OK. Escopo de UI: apenas a tela de login (as demais telas vêm nas fases 2–3).

**In scope:** scaffolding dos dois apps · Flyway V1 · auth JWT (login/logout básico) · roles ADMIN/USER no schema e no token · seed de empresa + admin · `empresa_id` exclusivamente do token · página de login minimalista · deploy local/staging/produção · `/api/health` · testes de integração de auth (Testcontainers).
**Out of scope:** "esqueci a senha" · verificação de e-mail · cadastro de novas empresas · telas de lançamento/categorias/dashboard (Fase 2–3) · CI custom · domínio próprio.

</domain>

<decisions>
## Implementation Decisions

### Sessão & Token
- **D-01:** JWT com expiração de **8h** (confirmado pelo usuário: "esse tempo mesmo").
- **D-02:** Sessão mantida em **cookie httpOnly** — token nunca em `localStorage` (proteção contra XSS). Sem refresh token na v1: expirou, refaz login.

### Seed & Acesso Inicial
- **D-03:** Seed cria 1 empresa + usuário **`admin@controle.com`** com role `ADMIN`, via migration Flyway V1.
- **D-04:** **Senha não versionada** (regra GSD: nada de senha em documentos/commits). A senha vem de variável de ambiente `SEED_ADMIN_PASSWORD` (Render: segredo do serviço; dev: valor local em `.env` gitignored). Recomendação registrada: **rotacionar a senha após o primeiro login em produção** — usuário foi avisado.

### Tela de Login
- **D-05:** Login **simples e minimalista**: apenas e-mail + senha, interface em **pt-BR**, mensagens de erro claras (credenciais inválidas genéricas — sem dizer qual campo falhou), sem extras (lembrar-me, social login, "esqueci a senha").

### Deploy & Ambientes
- **D-06:** **Três ambientes: local, staging e produção** — staging criado porque o usuário pretende **escalar** depois (mais clientes/empresas).
- **D-07:** Deploy por push: Vercel (web, com preview por branch) + Render (API), HTTPS nativo, migrations rodam no boot.
- **D-08 (pendente → plan-phase):** infraestrutura do staging — banco Postgres separado no Render (custo de 2º banco) vs. banco único com dados de teste. Usuário quer staging; o planner decide o modelo custo×segurança.

### the agent's Discretion
- Build tool com Maven (premissa do plano mestre, não questionada).
- Estrutura de pacotes, forma do `ProblemDetail`, formato dos erros 401/400 — decidir no plan (alinhado ao `.planning/PLAN.md` §3).
- Configuração do CORS, força do BCrypt, geração/assinatura do JWT.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Regras de negócio (obrigatório — criticidade: valor monetário e permissão)
- `.planning/PLAN.md` §2 — regras não negociáveis (R1–R7) e §6 pontos de controle do cliente
- `.planning/PLAN.md` §3.4 — modelo de dados rascunho (DDL com `NUMERIC(14,2)`, `empresa_id`)
- `.planning/PLAN.md` §5 — Fase 1 detalhada com tarefas e critérios de aceite

### Requisitos desta fase
- `.planning/REQUIREMENTS.md` — AUTH-01…AUTH-06, INF-01, INF-03, INF-04

### Contexto e escopo
- `.planning/PROJECT.md` — restrições de stack, segurança e deploy
- `.planning/ROADMAP.md` — Phase 1 (goal, success criteria, UI hint: yes)

Nenhuma spec/ADR externa — o plano mestre é a referência canônica.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Nenhum — repositório greenfield (apenas `.opencode/` do GSD e `.planning/`).

### Established Patterns
- Nenhum ainda — esta fase **cria** os padrões (estrutura de pacotes, error handling, formatação) que as fases 2–4 herdam.

### Integration Points
- Flyway V1 estabelece o schema que a Fase 2 (seed V2 + lançamentos) e a Fase 3 (dashboard) consomem.

</code_context>

<specifics>
## Specific Ideas

- "login e senha simples e minimalista" — referência explícita de visual do usuário.
- E-mail do seed: `admin@controle.com`.
- Staging pensando em **escala futura** (crescimento para múltiplas empresas/usuários).

</specifics>

<deferred>
## Deferred Ideas

- Fluxo "esqueci a senha" / redefinição — capacidade de fase futura.
- Verificação de e-mail no cadastro — fase futura.
- Cadastro de novas empresas (multiempresa real) — já é v2 no REQUIREMENTS.md (GROW-01).
- Domínio próprio na Vercel/Render — decidir quando escalar.
- Novas capacidades (tema de outra fase): comentários de lançamento, importação OFX — já fora de escopo v1.

None — discussion stayed within phase scope

</deferred>

---

*Phase: 1-Fundação e Auth em Produção*
*Context gathered: 2026-10-30*
