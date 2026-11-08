# Phase 2: Categorias e Lançamentos - Context

**Gathered:** 2026-11-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Fase 2 entrega: seed V2 de categorias (Flyway), CRUD de lançamentos com validação monetária estrita e lote tudo-ou-nada. O que está dentro do escopo vem de REQUIREMENTS.md (CAT-01 a CAT-06, LAN-01 a LAN-07, LOT-01, LOT-02, INF-02) e do ROADMAP.md (sucess criteria: testes Testcontainers passam em ./mvnw verify; lote com linha inválida retorna índice e não grava nada; valor >2 casas rejeitado, nenhum double/float; categoria ENTRADA rejeita lançamento SAÍDA e vice-versa).

Novas capacidades (importação OFX/CSV, lançamentos recorrentes, app mobile, multiempresa real, IA) são OUT OF SCOPE — pertencem a outras fases ou ao v2, não a esta discussão.

</domain>

<decisions>
## Implementation Decisions

### Seed V2 de categorias (Flyway V2)
- **D-01:** Implementar o seed como **Java migration** `V2__seed_categorias.java` seguindo o padrão de `V1_1__seed_empresa_admin.java` (Fase 1) — não SQL puro — por consistência com o que já existe no repo.
- **D-02:** `empresa_id` obtido via subquery `(SELECT id FROM empresa WHERE nome='Controle Financeiro')`, nunca hardcoded (a empresa já existe após V1_1).
- **D-03:** Idempotência: checar se a categoria já existe antes de inserir, protegendo re-aplicação do V2.
- **D-04:** Manter a ordem da lista do cliente na migration (12 saídas + 4 entradas; pessoais [P]: Alimentação, Saúde/farmácia, Lazer, Rendimentos e reembolsos).

### Áreas em aberto (decisão deixada para o planejamento / /gsd-plan-phase)
As seguintes áreas cinzas foram listadas mas **não foram discutidas** nesta sessão (usuário selecionou apenas a área Seed V2). O planner deve resolvê-las antes da execução:
- Formato do lote tudo-ou-nada (array JSON único vs JSONL), reporte do índice da linha inválida e limite de 500 linhas.
- Onde rejeitar valor com >2 casas decimais (DTO bean validation vs service em BigDecimal) e onde aplicar RoundingMode.HALF_EVEN.
- Integridade categoria×tipo (checagem no service vs banco) e comportamento de lançamentos históricos quando a categoria é desativada (ativo=false).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisitos e escopo (locked)
- `.planning/ROADMAP.md` — goal e success criteria da Fase 2 (lote tudo-ou-nada, valor >2 casas rejeitado, sem double/float, integridade categoria×tipo)
- `.planning/REQUIREMENTS.md` — CAT-01..06, LAN-01..07, LOT-01..02, INF-02 (regras de negócio da fase)
- `.planning/PROJECT.md` — constraints não negociáveis (BigDecimal NUMERIC(14,2), escala 2, HALF_EVEN; empresa_id exclusivamente do token; lote tudo-ou-nada com índice da linha; integridade categoria×tipo; segurança BCrypt/HTTPS)

### Código existente (padrões e assets reutilizáveis)
- `api/src/main/resources/db/migration/V1__schema.sql` — schema existente: tabelas categoria e lancamento com NUMERIC(14,2), CHECK(valor>0), empresa_id em todas as tabelas
- `api/src/main/java/com/controlefinanceiro/db/migration/V1_1__seed_empresa_admin.java` — padrão de Java migration a seguir para V2 (sem DI Spring, System.getenv, fail-fast)
- `api/src/main/java/com/controlefinanceiro/common/TenantContext.java` — único ponto de acesso ao tenant (empresa_id só do SecurityContext/JWT, nunca do body)
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` — padrão RFC 7807 ProblemDetail para erros (400/401)
- `api/src/main/resources/application.yml` — Flyway locations (db/migration + pacote db/migration Java), fail-on-unknown-properties, JWT secret por env

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `TenantContext` — único ponto de acesso ao `empresa_id` (do token), a ser usado por todo novo código de categoria/lancamento
- `ProblemDetailExceptionHandler` — tratamento padronizado de erros RFC 7807 a ser reutilizado pelos novos controllers
- `V1_1__seed_empresa_admin.java` — modelo de Java migration (Flyway) para o V2 de categorias
- `V1__schema.sql` — tabelas `categoria` e `lancamento` já existem (não recriar; apenas inserir seed e construir CRUD sobre elas)
- `application.yml` — config de Flyway (descobre migrations SQL e Java) e Jackson (rejeita campos desconhecidos)

### Established Patterns
- Controller fina + service com regras de negócio (padrão `AuthController`/`AuthService` da Fase 1)
- Exceções customizadas lançadas no service e tratadas globalmente por `ProblemDetailExceptionHandler`
- Migrations imutáveis: correção só via nova versão `V<n>__` (INF-01)
- `empresa_id` em todas as tabelas; tenant vem exclusivamente do token JWT (AUTH-03)

### Integration Points
- Novo pacote `categoria` e `lancamento` em `api/src/main/java/com/controlefinanceiro/` seguindo pacotes existentes (`auth`, `empresa`, `usuario`)
- Endpoints REST sob `/api/categorias` e `/api/lancamentos` (incluindo lote)
- Web consome a API via `web/lib/api.ts` (server-side, cookie `sess`)

</code_context>

<specifics>
## Specific Ideas

Nenhuma referência específica além das listadas acima — aberto a abordagens padrão para CRUD e lote, desde que respeitem as constraints de dinheiro, tenant e integridade já definidas.

</specifics>

<deferred>
## Deferred Ideas

### Áreas cinzas não discutidas nesta sessão (para o planejamento)
- Formato do lote tudo-ou-nada (array JSON vs JSONL), índice da linha inválida, limite de 500 linhas.
- Local da validação monetária >2 casas (DTO vs service) e aplicação de HALF_EVEN.
- Integridade categoria×tipo (service vs banco) e categorias desativadas.

### Escopo futuro (já em Out of Scope do PROJECT.md/REQUIREMENTS.md)
- Importação de extrato (OFX/CSV) e lançamentos recorrentes — v2.
- App mobile nativo — fora de escopo (web-first).
- Multiempresa real (cadastro de novas empresas) — fase 1 só prepara schema com empresa_id.
- IA/OpenRouter — restrita à fase 4.

</deferred>

---

*Phase: 02-Categorias e Lançamentos*
*Context gathered: 2026-11-08*
