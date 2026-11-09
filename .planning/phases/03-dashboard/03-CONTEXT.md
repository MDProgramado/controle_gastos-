# Phase 3: Dashboard - Context

**Gathered:** 2026-11-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Fase 3 entrega o **dashboard financeiro na web** (DSH-01..DSH-05) **e as telas de CRUD transferidas da Fase 2** (categorias e lançamentos), conforme decidido na UAT da Fase 2. O dashboard mostra, para o mês selecionado: total de entradas, total de saídas, saldo, gasto por categoria (tabela + gráfico) e comparação com o mês anterior — com números idênticos a um SQL de conferência direto no banco, filtrados pelo `empresa_id` do token, e mês vazio exibindo zeros sem erro.

**In scope:**
- Endpoint de agregação na API (SUM/GROUP BY) reutilizável pela Fase 4.
- Dashboard em `/` com KPIs, comparação mensal, gasto por categoria (lista + gráfico de barras) e seletor de mês.
- Tela `/lancamentos`: listar (filtrado pelo mês), criar, editar e excluir.
- Tela `/categorias`: listar, criar, editar e desativar (soft delete).
- Navegação no topo entre as três rotas + logout.

**Out of scope (não pertence a esta fase):**
- UI de importação em lote (lote continua API-only: `POST /api/lancamentos/lote`).
- Importação OFX/CSV, lançamentos recorrentes, exportação PDF/Excel, app mobile, multiempresa real.
- IA/OpenRouter — Fase 4.
- Gráficos interativos além de barras.

</domain>

<decisions>
## Implementation Decisions

### Agregação e contrato da API
- **D-01:** Agregar os números **no backend** com SUM/GROUP BY em endpoint dedicado (ex.: `GET /api/dashboard?ano=&mes=`), usando `TenantContext.getEmpresaId()`. O web **nunca** soma a listagem de lançamentos — `GET /api/lancamentos` trunca em 50 e produziria números errados.
- **D-02:** O endpoint devolve em uma chamada: totais de entradas, saídas e saldo; comparação com o mês anterior; e breakdown por categoria. As mesmas queries devem ser reutilizáveis pela Fase 4 (function calling).
- **D-03:** Nenhum `double`/`float`: valores em `BigDecimal`/`NUMERIC(14,2)` na API e formatação monetária no web via `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.
- **D-04:** Consultas sempre filtram por `empresa_id` do token (nunca do body/query).

### Comparação mensal (DSH-03)
- **D-05:** A variação é exibida **em R$ absoluto e em percentual**, para **entradas, saídas e saldo**.
- **D-06:** Quando o valor do mês anterior é **zero** (ou não positivo), o percentual é exibido como **"—" / "sem base de comparação"**; mostra-se apenas a variação absoluta. Sem divisão por zero.
- **D-07:** Comparação é **mês fechado vs mês fechado** (mês inteiro contra mês inteiro), inclusive quando o mês corrente está em andamento.

### Gasto por categoria (DSH-02)
- **D-08:** Exibir **tabela/lista ordenada por valor decrescente E gráfico de barras horizontais**, com **valor e percentual** sobre o total de saídas do mês.
- **D-09:** Listar **todas as categorias de SAÍDA** do mês, **inclusive as zeradas**; categorias de ENTRADA não aparecem no "gasto". **Não** agrupar em "Outros".
- **D-10:** Percentual usa o total de saídas como denominador; se o total de saídas for zero, percentual e barras ficam vazios/zerados sem erro (divisão por zero tratada).
- **D-11:** Biblioteca de gráficos: **shadcn/ui Charts** (usa **Recharts** por baixo) — instalar `recharts` e adicionar o wrapper `components/ui/chart.tsx`, reutilizando os tokens `--chart-1..5` do `globals.css` e o tema claro/escuro.
- **D-12:** Barras **horizontais** ordenadas por valor, para nomes de categoria longos caberem bem.

### Seletor de mês (DSH-04)
- **D-13:** Controle `<input type="month">` nativo, com padrão = **mês corrente**.
- **D-14:** **Permitir meses futuros**.
- **D-15:** **Sem limite inferior** — qualquer mês passado é aceito; mês sem lançamentos mostra zeros (DSH success criterion 3).
- **D-16:** O mês selecionado fica na **URL** (`/?mes=YYYY-MM`, `/lancamentos?mes=YYYY-MM`), recarregável e compartilhável.
- **D-17:** Além do seletor, **setas ‹ ›** navegam para o mês anterior/próximo, atualizando a URL.

### Escopo de UI herdado da Fase 2
- **D-18:** Esta fase inclui **CRUD completo de categorias e de lançamentos na web**.
- **D-19:** "Excluir categoria" = **desativar (soft delete)** — nunca `DELETE` físico. O backend já implementa `DELETE /api/categorias/{id}` como desativação (`ativo=false`); a UI deve refletir isso.
- **D-20:** **Sem UI de lote** nesta fase; `POST /api/lancamentos/lote` continua API-only.
- **D-21:** Rotas separadas com navegação no topo: dashboard em `/`, lançamentos em `/lancamentos`, categorias em `/categorias`.
- **D-22:** A listagem de lançamentos filtra pelo **mês selecionado** (reusa o seletor). Como `GET /api/lancamentos` trunca em 50, exibir aviso quando houver truncamento ("mostrando 50 de N" — o total pode vir do endpoint de agregação ou de um count).
- **D-23:** Escrita de categorias é **ADMIN-only** (já imposto pelo backend). Para usuário `USER`, a UI deve ocultar/desabilitar criar/editar/desativar; leitura continua liberada.
- **D-24:** Confirmar explicitamente antes de **excluir lançamento** (DELETE físico, irreversível) e antes de **desativar categoria**; nunca usar `window.confirm` — usar o padrão de diálogo/estado da UI.

### Agente tem discrição sobre
- Nome/shape exato do endpoint e DTOs de agregação (desde que cubra D-02), forma do breakdown (ex.: LEFT JOIN para incluir categorias zeradas), cálculo/arredondamento do percentual, ordenação de empates e labels.
- Layout do dashboard (grid de cards de KPI, posição da comparação, ordem lista vs gráfico), empty states e textos auxiliares em pt-BR.
- Estrutura dos formulários de lançamento/categoria (client component controlado, sem RHF/zod — consistente com o `LoginForm`), validações de UI espelhando as regras da API.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisitos e escopo (locked)
- `.planning/ROADMAP.md` — goal e success criteria da Fase 3 (linhas ~78–90) e dependência da Fase 4 (mesmas queries).
- `.planning/REQUIREMENTS.md` — DSH-01..DSH-05; CAT-01..06, LAN-01..07 e LOT-01..02 (telas de CRUD herdadas).
- `.planning/PROJECT.md` — core value (números confiáveis ao centavo), constraints monetárias (BigDecimal/NUMERIC(14,2)/HALF_EVEN), tenant do token, lote tudo-ou-nada.

### UI e padrões herdados
- `.planning/phases/01-funda-o-e-auth-em-produ-o/01-UI-SPEC.md` — design tokens (teal `#0f766e`, slate, raio, tipografia), copywriting pt-BR, acessibilidade, cores de entrada/saída e formatação monetária.
- `.planning/phases/01-funda-o-e-auth-em-produ-o/01-CONTEXT.md` — sessão por cookie httpOnly `sess` (D-01..D-08), BFF Next.js.
- `.planning/phases/02-categorias-e-lan-amentos/02-CONTEXT.md` — decisões e padrões da Fase 2 (seed, validação monetária, integridade categoria×tipo).
- `.planning/phases/02-categorias-e-lan-amentos/02-VERIFICATION.md` — achados não-bloqueantes: paginação fixa de 50, `GET /{id}` limitado, categoria desativada aceita lançamentos.
- `.planning/phases/02-categorias-e-lan-amentos/02-HUMAN-UAT.md` — decisão de transferir a UI de categorias/lançamentos para a Fase 3.

### Código existente
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` — endpoints de CRUD/lote/listagem e filtros `ano/mes/tipo/categoriaId`; não há endpoint de agregação ainda.
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java` — queries por tenant/data; base para as novas queries SUM/GROUP BY.
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java` — CRUD de categorias; `DELETE` = desativar.
- `api/src/main/resources/db/migration/V1__schema.sql` — tabelas `categoria` (tipo, ativo, pessoal) e `lancamento` (NUMERIC(14,2), CHECK valor>0) + índices.
- `web/lib/api.ts` — padrão server-side com cookie `sess` e `Authorization: Bearer`; ponto de extensão para as novas chamadas.
- `web/app/page.tsx` — shell autenticado atual; vira o dashboard.
- `web/app/globals.css` — tokens de tema claro/escuro e `--chart-1..5`.
- `web/proxy.ts` — gate de autenticação por cookie para todas as rotas de página (novas rotas já ficam protegidas).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `web/lib/api.ts` (`getMe`) — modelo de chamada server-side autenticada; estender com funções tipadas para dashboard, categorias e lançamentos.
- `web/components/ui/card.tsx`, `button.tsx`, `alert.tsx`, `input.tsx`, `label.tsx` — base visual dos KPIs, tabelas, formulários e diálogos.
- `web/components/Spinner.tsx` e padrão de estados do `LoginForm` — reaproveitar para loading/erro/submitting das novas telas.
- `web/app/globals.css` — tokens `--chart-1..5`, cores de entrada/saída, raio e tema; usar no gráfico e nos números.
- `TenantContext` + `ProblemDetailExceptionHandler` — padrão obrigatório no novo endpoint (tenant do token, erros RFC 7807).
- `LancamentoRepository` — índices `idx_lanc_empresa_data` e `idx_lanc_empresa_cat` favorecem filtros por empresa/data e agregação por categoria.

### Established Patterns
- Controller fino + service com regra de negócio; sem lógica no controller.
- `empresa_id` exclusivamente do token JWT; nunca do body/query.
- Migrations imutáveis; qualquer ajuste de schema via nova versão Flyway.
- Páginas dinâmicas no Next 16 usam `export const instant = false` quando dependem de cookies/searchParams (padrão já usado em `page.tsx` e `login/page.tsx`).
- Textos de UI em pt-BR; mensagens de erro claras; acessibilidade (labels, foco visível, alvos ≥ 44px).

### Integration Points
- Novo endpoint de dashboard no pacote `lancamento` (ou pacote `dashboard`) sob `/api/dashboard`.
- Novas funções em `web/lib/api.ts` para o endpoint de agregação e para CRUD (categorias/lançamentos).
- `web/app/page.tsx` substituído pelo dashboard; novas rotas `web/app/lancamentos/page.tsx` e `web/app/categorias/page.tsx`.
- Layout autenticado compartilhado (header com navegação + logout) para as três rotas.
- `web/proxy.ts` já protege as novas rotas — nenhuma mudança necessária.

</code_context>

<specifics>
## Specific Ideas

- Usuário quer **os dois formatos** de comparação (absoluto + percentual), **os dois formatos** de categoria (lista + gráfico) e quer a **biblioteca shadcn/ui Charts**.
- "Listar todas" foi escolha explícita: nenhuma categoria de SAÍDA é omitida, mesmo com valor zero.
- Meses futuros são permitidos de propósito (planejamento).
- A URL é a fonte da verdade do mês selecionado.
- O escopo de UI desta fase é deliberadamente amplo: dashboard + CRUD de categorias + CRUD de lançamentos, em rotas separadas.

</specifics>

<deferred>
## Deferred Ideas

- UI de importação em lote (colar linhas JSON) — lote permanece API-only nesta fase.
- Paginação real da listagem de lançamentos — nesta fase, filtro por mês + aviso de truncamento em 50.
- Importação OFX/CSV e lançamentos recorrentes — v2.
- Exportação PDF/Excel — fora de escopo.
- Gráficos interativos (tooltips ricos, zoom, drill-down) — apenas barras nesta fase.
- Multiempresa real (cadastro de empresas) — fora de escopo.
- IA/OpenRouter — Fase 4; deverá reutilizar as queries do dashboard.

</deferred>

---

*Phase: 03-Dashboard*
*Context gathered: 2026-11-09*
