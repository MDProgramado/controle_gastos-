<!-- GSD:project-start source:PROJECT.md -->
## Project

**Controle Financeiro**

Aplicação de controle financeiro (entradas e saídas) para uma empresa cliente — no início single-tenant, com `empresa_id` em todas as tabelas para suportar multiusuário/multiempresa no futuro. Web em Next.js (App Router, TypeScript) consumindo uma API Spring Boot (Java 21), publicada na Vercel (web) e Render (API + PostgreSQL).

**Core Value:** Saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, que o cliente confere contra SQL direto e compara com sua contabilidade sem surpresas.

### Constraints

- **Stack** (definida, não negociável): Java 21 + Spring Boot 3, Spring Security + JWT, Spring Data JPA, PostgreSQL, Flyway, Testcontainers; Next.js (App Router, TypeScript)
- **Dinheiro**: valores em `BigDecimal` com `NUMERIC(14,2)` no banco, `scale = 2`, `RoundingMode.HALF_EVEN` no service; entrada com mais de 2 casas é rejeitada; nenhum `double`/`float` no código
- **Tenant**: `empresa_id` vem exclusivamente do token JWT, nunca do body
- **Lote**: transação tudo-ou-nada; erro informa o índice da linha
- **Integridade**: categoria do tipo ENTRADA não aceita lançamento de SAÍDA e vice-versa
- **Segurança**: senha com BCrypt, dados em trânsito via HTTPS, política de privacidade e LGPD previstas antes do uso real
- **Deploy**: API no Render (Postgres pago com backup), web na Vercel
- **Escopo de IA**: restrito à fase 4 (OpenRouter)
<!-- GSD:project-end -->

## Regras de Fluxo e Skills do Projeto

Sempre siga esta ordem ao desenvolver novas funcionalidades (dentro dos comandos `/gsd-quick`, `/gsd-debug` ou `/gsd-execute-phase`):

1. **Lógica e Backend (Java / Spring Boot):** Use as práticas da skill `superpowers` (planejamento e TDD rigoroso com JUnit/Testcontainers antes de implementar). Garanta precisão com `BigDecimal` e isolamento por `empresa_id`.
2. **Frontend e UI (Next.js / TypeScript):** Use a skill `design-taste-frontend` para qualquer alteração visual, mantendo o dashboard financeiro limpo, alinhado e validando no navegador.
3. **Revisão Final e Segurança:** Antes de concluir tarefas grandes ou deploy (Render/Vercel), verifique falhas de autenticação, JWT e exposição de dados com `security-audit`, e valide meta tags das páginas públicas com `seo-audit`.

<!-- GSD:stack-start source:STACK.md -->
## Technology Stack

Technology stack not yet documented. Will populate after codebase mapping or first phase.
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
