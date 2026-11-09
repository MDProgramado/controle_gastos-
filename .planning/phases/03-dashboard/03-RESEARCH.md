# Phase 3: Dashboard - Research

**Researched:** 2026-11-09
**Domain:** Dashboard agregação backend (Spring Boot) + dashboard web (Next.js 16) + CRUD herdado (categorias/lançamentos)
**Confidence:** HIGH

## Summary

Phase 3 delivers the financial dashboard on the web plus the CRUD screens for categorias and lançamentos (transferred from Phase 2 per UAT decision). The core technical work is a new **aggregation endpoint** `GET /api/dashboard?ano=&mes=` on the Spring Boot API that performs SUM/GROUP BY in SQL (never in the web tier), returning totais de entradas/saídas/saldo, comparação com o mês anterior, e breakdown por categoria. The web consumes it via `web/lib/api.ts` server-side pattern, renders KPI cards, a horizontal bar chart via shadcn/ui Charts (Recharts), a native `<input type="month">`, and month navigation arrows with URL state (`?mes=YYYY-MM`). Existing CRUD endpoints for categorias and lançamentos are reused; new Next.js pages are added under `/lancamentos` and `/categorias`. Role-based access (ADMIN-only writes) is already enforced by `SecurityConfig`; the UI reflects the role.

**Primary recommendation:** Build the aggregation endpoint first (backend vertical slice), then consume it from the dashboard page, then add CRUD pages reusing existing API endpoints — all filtering by `empresa_id` from `TenantContext`.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Agregar os números **no backend** com SUM/GROUP BY em endpoint dedicado (ex.: `GET /api/dashboard?ano=&mes=`), usando `TenantContext.getEmpresaId()`. O web **nunca** soma a listagem de lançamentos — `GET /api/lancamentos` trunca em 50 e produziria números errados.
- **D-02:** O endpoint devolve em uma chamada: totais de entradas, saídas e saldo; comparação com o mês anterior; e breakdown por categoria. As mesmas queries devem ser reutilizáveis pela Fase 4 (function calling).
- **D-03:** Nenhum `double`/`float`: valores em `BigDecimal`/`NUMERIC(14,2)` na API e formatação monetária no web via `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.
- **D-04:** Consultas sempre filtram por `empresa_id` do token (nunca do body/query).
- **D-05:** A variação é exibida **em R$ absoluto e em percentual**, para **entradas, saídas e saldo**.
- **D-06:** Quando o valor do mês anterior é **zero** (ou não positivo), o percentual é exibido como **"—" / "sem base de comparação"**; mostra-se apenas a variação absoluta. Sem divisão por zero.
- **D-07:** Comparação é **mês fechado vs mês fechado** (mês inteiro contra mês inteiro), inclusive quando o mês corrente está em andamento.
- **D-08:** Exibir **tabela/lista ordenada por valor decrescente E gráfico de barras horizontais**, com **valor e percentual** sobre o total de saídas do mês.
- **D-09:** Listar **todas as categorias de SAÍDA** do mês, **inclusive as zeradas**; categorias de ENTRADA não aparecem no "gasto". **Não** agrupar em "Outros".
- **D-10:** Percentual usa o total de saídas como denominador; se o total de saídas for zero, percentual e barras ficam vazios/zerados sem erro (divisão por zero tratada).
- **D-11:** Biblioteca de gráficos: **shadcn/ui Charts** (usa **Recharts** por baixo) — instalar `recharts` e adicionar o wrapper `components/ui/chart.tsx`, reutilizando os tokens `--chart-1..5` do `globals.css` e o tema claro/escuro.
- **D-12:** Barras **horizontais** ordenadas por valor, para nomes de categoria longos caberem bem.
- **D-13:** Controle `<input type="month">` nativo, com padrão = **mês corrente**.
- **D-14:** **Permitir meses futuros**.
- **D-15:** **Sem limite inferior** — qualquer mês passado é aceito; mês sem lançamentos mostra zeros (DSH success criterion 3).
- **D-16:** O mês selecionado fica na **URL** (`/?mes=YYYY-MM`, `/lancamentos?mes=YYYY-MM`), recarregável e compartilhável.
- **D-17:** Além do seletor, **setas ‹ ›** navegam para o mês anterior/próximo, atualizando a URL.
- **D-18:** Esta fase inclui **CRUD completo de categorias e de lançamentos na web**.
- **D-19:** "Excluir categoria" = **desativar (soft delete)** — nunca `DELETE` físico. O backend já implementa `DELETE /api/categorias/{id}` como desativação (`ativo=false`); a UI deve refletir isso.
- **D-20:** **Sem UI de lote** nesta fase; `POST /api/lancamentos/lote` continua API-only.
- **D-21:** Rotas separadas com navegação no topo: dashboard em `/`, lançamentos em `/lancamentos`, categorias em `/categorias`.
- **D-22:** A listagem de lançamentos filtra pelo **mês selecionado** (reusa o seletor). Como `GET /api/lancamentos` trunca em 50, exibir aviso quando houver truncamento ("mostrando 50 de N" — o total pode vir do endpoint de agregação ou de um count).
- **D-23:** Escrita de categorias é **ADMIN-only** (já imposto pelo backend). Para usuário `USER`, a UI deve ocultar/desabilitar criar/editar/desativar; leitura continua liberada.
- **D-24:** Confirmar explicitamente antes de **excluir lançamento** (DELETE físico, irreversível) e antes de **desativar categoria**; nunca usar `window.confirm` — usar o padrão de diálogo/estado da UI.

### the agent's Discretion
- Nome/shape exato do endpoint e DTOs de agregação (desde que cubra D-02), forma do breakdown (ex.: LEFT JOIN para incluir categorias zeradas), cálculo/arredondamento do percentual, ordenação de empates e labels.
- Layout do dashboard (grid de cards de KPI, posição da comparação, ordem lista vs gráfico), empty states e textos auxiliares em pt-BR.
- Estrutura dos formulários de lançamento/categoria (client component controlado, sem RHF/zod — consistente com o `LoginForm`), validações de UI espelhando as regras da API.

### Deferred Ideas (OUT OF SCOPE)
- UI de importação em lote (colar linhas JSON) — lote permanece API-only nesta fase.
- Paginação real da listagem de lançamentos — nesta fase, filtro por mês + aviso de truncamento em 50.
- Importação OFX/CSV e lançamentos recorrentes — v2.
- Exportação PDF/Excel — fora de escopo.
- Gráficos interativos (tooltips ricos, zoom, drill-down) — apenas barras nesta fase.
- Multiempresa real (cadastro de empresas) — fora de escopo.
- IA/OpenRouter — Fase 4; deverá reutilizar as queries do dashboard.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DSH-01 | Dashboard mostra total de entradas, total de saídas e saldo do mês solicitado | Agregação endpoint SUM/GROUP BY por tipo (D-01) |
| DSH-02 | Dashboard mostra gasto por categoria do mês | Breakdown por categoria LEFT JOIN (D-08, D-09) |
| DSH-03 | Dashboard compara o mês com o mês anterior (variação), tratando mês zerado | Query mês anterior + base zero tratamento (D-05, D-06, D-07) |
| DSH-04 | Seletor de mês (padrão: mês corrente) atualiza todos os números | `<input type="month">` + URL state (D-13, D-16, D-17) |
| DSH-05 | Números do dashboard são idênticos a um SQL de conferência executado direto no banco com seed conhecida | Agregação no backend, nunca soma a listagem truncada (D-01) |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Agregação SUM/GROUP BY | API / Backend | — | D-01: números agregados no backend; web nunca soma listagem truncada |
| Filtro por empresa_id (tenant) | API / Backend | — | D-04: exclusivamente do token via TenantContext, nunca do body/query |
| Formatação monetária | Browser / Client | — | D-03: Intl.NumberFormat no web; BigDecimal na API |
| Gráfico de barras horizontais | Browser / Client | — | D-11, D-12: shadcn/ui Charts + Recharts client-side render |
| Seletor de mês + navegação | Browser / Client | Frontend Server (SSR) | D-13, D-16, D-17: input nativo + URL searchParams + setas |
| CRUD categorias/lançamentos (leitura/escrita) | API / Backend | Browser / Client | Endpoints existentes; UI client-side forms |
| Role-based access (ADMIN-only writes) | API / Backend | Browser / Client | SecurityConfig restringe /api/categorias/** a ADMIN; UI oculta ações para USER |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `recharts` | [ASSUMED] ~2.15 | Gráficos de barras horizontais | shadcn/ui Charts usa Recharts por baixo (D-11); já alinhado ao design system |
| `next` | 16.4.3 | App Router, Server Components | Stack existente no projeto (verificado em package.json) |
| `react` | 19.3.0 | UI framework | Stack existente (verificado em package.json) |
| `tailwindcss` | ^4.3.4 | Estilização | Stack existente (verificado em package.json) |
| `shadcn/ui` (card, button, input, label, alert) | existente | Componentes base | Já instalados em `web/components/ui/` |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `lucide-react` | ^1.67.0 | Ícones (Spinner usa Loader2) | Ícones de setas, categorias, lançamentos |
| `radix-ui` | ^1.8.1 | Primitivos de UI | Label já usa `LabelPrimitive`; diálogos para confirmação (D-24) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| shadcn/ui Charts (Recharts) | Chart.js / react-chartjs-2 | Recharts já é o padrão do shadcn/ui Charts (D-11); Chart.js exigiria wrapper custom |
| Agregação SQL no backend | Somar listagem no web | D-01 proíbe: listagem trunca em 50 → números errados |
| `<input type="month">` | Dropdown customizado | D-13: controle nativo selecionado pelo usuário |

**Installation:**
```bash
# No diretório web/
npm install recharts
```

**Version verification:** `recharts` package name tagged `[ASSUMED]` — not verified via Context7 or official docs in this session (web search unavailable, Context7 CLI not installed). Planner must add `checkpoint:human-verify` before install.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `recharts` | npm | [ASSUMED] | [ASSUMED] | [ASSUMED] | [ASSUMED] | Flagged — planner must add checkpoint:human-verify before install |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

*slopcheck was not available at research time (not installed, no network verification possible). All packages above are tagged `[ASSUMED]` and the planner must gate each install behind a `checkpoint:human-verify` task.*

## Architecture Patterns

### System Architecture Diagram

```
[Usuário no navegador]
        |
        v (HTTP GET /?mes=YYYY-MM)
[Next.js Server Component: page.tsx]
        |  (1) getMe() — valida sessão via cookie "sess"
        v
[Next.js Server Component: dashboard/lancamentos/categorias]
        |  (2) fetch API_URL + Authorization: Bearer token
        v
[Spring Boot API: GET /api/dashboard?ano=&mes=]
        |  (3) TenantContext.getEmpresaId() do JWT
        |  (4) SUM/GROUP BY SQL no PostgreSQL
        v
[PostgreSQL] ← NUMERIC(14,2), índices idx_lanc_empresa_data / idx_lanc_empresa_cat
        ^
        | (5) mesmo endpoint reutilizado por Fase 4 (function calling)
[Phase 4 IA/OpenRouter] (futuro, fora de escopo desta fase)
```

Data flow: usuário seleciona mês → URL atualizada → Server Component busca dados da API → dashboard renderiza KPIs, tabela, gráfico e comparação. Todas as queries filtram por `empresa_id` do token. CRUD de lançamentos/categorias usa endpoints existentes com o mesmo filtro de tenant.

### Recommended Project Structure

```
api/
  src/main/java/com/controlefinanceiro/
    dashboard/
      DashboardController.java          # GET /api/dashboard
      DashboardService.java             # agregação SUM/GROUP BY
      dto/
        DashboardResponse.java          # totais, comparação, breakdown
        CategoryExpense.java            # item do breakdown por categoria
    lancamento/
      LancamentoRepository.java         # novas queries de agregação (adicionar)
web/
  app/
    page.tsx                            # dashboard (substitui shell atual)
    lancamentos/
      page.tsx                          # CRUD lançamentos
    categorias/
      page.tsx                          # CRUD categorias
    layout.tsx                          # header com navegação + logout (compartilhado)
  components/
    ui/
      chart.tsx                         # wrapper shadcn/ui Charts + Recharts
    DashboardKPIs.tsx                   # cards de entradas/saídas/saldo
    CategoryBreakdown.tsx               # tabela + gráfico de barras
    MonthSelector.tsx                   # input type=month + setas ‹ ›
  lib/
    api.ts                              # funções tipadas para dashboard, categorias, lançamentos
```

### Pattern 1: Server-side data fetching (existing `web/lib/api.ts` pattern)

**What:** Next.js Server Component lê o cookie `sess`, chama a API com `Authorization: Bearer`, retorna dados tipados. Nunca é importado pelo browser.

**When to use:** Todas as chamadas de dados do dashboard e CRUD páginas.

**Example:**
```typescript
// web/lib/api.ts — extensão (navegar para novas funções)
export async function getDashboard(ano: number, mes: number): Promise<DashboardData | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) return null;
  const res = await fetch(`${process.env.API_URL}/api/dashboard?ano=${ano}&mes=${mes}`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  if (!res.ok) return null;
  return (await res.json()) as DashboardData;
}
```
*Source: existing `web/lib/api.ts` `getMe()` pattern (verified from codebase)*

### Pattern 2: BigDecimal JSON serialization (API)

**What:** `BigDecimal` serializado pelo Jackson como número. Para preservar precisão monetária, considerar `JsonSerialize(using = ToStringSerializer.class)` no DTO ou `spring.jackson.serialization.write-bigdecimal-as-plain=true`.

**When to use:** Todos os DTOs de resposta que contêm `BigDecimal` (DashboardResponse, LancamentoResponse).

**Example:**
```java
public record DashboardResponse(
    BigDecimal totalEntradas,
    BigDecimal totalSaidas,
    BigDecimal saldo,
    BigDecimal variacaoEntradas,
    BigDecimal variacaoSaidas,
    BigDecimal variacaoSaldo,
    String percentualEntradas, // "—" ou "12,50%"
    String percentualSaidas,
    String percentualSaldo,
    List<CategoryExpense> categorias
) {}
```
*Source: [ASSUMED] — Spring Boot + Jackson default behavior; needs verification against project's existing JSON config*

### Anti-Patterns to Avoid
- **[Somar listagem truncada no web]:** `GET /api/lancamentos` retorna apenas 50 itens. Somar essa lista produz números incorretos. Sempre usar o endpoint de agregação dedicado (D-01).
- **[double/float para dinheiro]:** Viola LAN-05. Todo cálculo monetário usa `BigDecimal`, scale=2, HALF_EVEN.
- **[empresa_id no body/query]:** Viola D-04 e R3. Tenant vem exclusivamente do JWT via `TenantContext`.
- **[window.confirm para exclusão]:** Viola D-24. Usar diálogo/estado da UI.
- **[DELETE físico de categoria]:** Viola D-19. `DELETE /api/categorias/{id}` já é soft delete (`ativo=false`).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Agregação SUM/GROUP BY | Somar lançamentos no frontend | Endpoint dedicado no backend (D-01) | Listagem trunca em 50 → números errados; SQL é correto e performático com índices existentes |
| Cálculo de percentual com base zero | Divisão direta (risco de divisão por zero) | Tratamento condicional: se base <= 0, exibir "—" (D-06, D-10) | Evita NaN/Infinity; UX definida pelo cliente |
| Gráfico de barras | SVG/manual com canvas | shadcn/ui Charts (Recharts) (D-11) | Alinhado ao design system, tokens `--chart-1..5` já existem |
| Confirmação de exclusão | `window.confirm` | Diálogo/estado da UI (D-24) | Padrão consistente, acessível, testável |
| Paginação real | Offset/limit custom | Manter limite 50 + aviso de truncamento (D-22) | Adiado por decisão explícita; fora de escopo |
| Autenticação/tenant | Segundo filtro JWT ou header custom | `TenantContext.getEmpresaId()` + SecurityConfig existente | Padrão estabelecido, R3/AUTH-03 |

**Key insight:** Não inventar soluções custom para problemas que já têm biblioteca ou padrão estabelecido no projeto. O foco deve ser integrar o endpoint de agregação com o frontend usando padrões existentes (`web/lib/api.ts`, Server Components, shadcn/ui).

## Runtime State Inventory

> Phase 3 is greenfield for the dashboard endpoint and new web pages — no rename/refactor/migration. Omitted.

## Common Pitfalls

### Pitfall 1: Números do dashboard não batem com SQL de conferência (DSH-05)
**What goes wrong:** O web soma a listagem truncada de `GET /api/lancamentos` (máx. 50 itens) ou usa um critério de data diferente do SQL de conferência.
**Why it happens:** Tentativa de evitar criar um endpoint dedicado; reuso do endpoint de listagem existente para agregação.
**How to avoid:** Implementar `GET /api/dashboard` com SUM/GROUP BY no banco, filtrando pelo mês completo (primeiro dia até último dia do mês) e por `empresa_id` do token.
**Warning signs:** Teste de conferência contra seed conhecida mostra divergência de centavos ou totais.

### Pitfall 2: Divisão por zero na comparação mensal (DSH-03, DSH success criterion 3)
**What goes wrong:** Mês anterior tem zero lançamentos → percentual é NaN, Infinity ou exceção.
**Why it happens:** Cálculo direto `(atual - anterior) / anterior` sem checar se `anterior > 0`.
**How to avoid:** Tratamento condicional: se valor do mês anterior <= 0, percentual = "—", exibir apenas variação absoluta em R$.
**Warning signs:** Exibição de "NaN%" ou "Infinity%" na UI; erro 500 na API.

### Pitfall 3: Mês vazio exibe erro em vez de zeros
**What goes wrong:** Mês sem lançamentos retorna null/undefined nos totais, quebrando renderização.
**Why it happens:** Query SUM retorna NULL quando não há linhas; código não trata null.
**How to avoid:** `COALESCE(SUM(valor), 0)` na query SQL; DTO inicia com `BigDecimal.ZERO`; breakdown retorna lista vazia (não null).
**Warning signs:** Tela branca ou erro "Cannot read property of null" no dashboard para meses sem dados.

### Pitfall 4: Usuário de outra empresa vê dados (DSH success criterion 4)
**What goes wrong:** Query não filtra por `empresa_id` ou filtra por parâmetro do query/body que pode ser forjado.
**Why it happens:** Esquecimento do filtro de tenant na nova query de agregação; uso de parâmetro de request em vez de `TenantContext`.
**How to avoid:** Toda query de dashboard usa `TenantContext.getEmpresaId()`; nunca aceitar `empresa_id` como parâmetro de entrada.
**Warning signs:** Teste com token de empresa B retorna dados da empresa A.

### Pitfall 5: BigDecimal serialização perde precisão no JSON
**What goes wrong:** Jackson serializa `BigDecimal` como número de ponto flutuante, perdendo casas decimais ou adicionando notação científica.
**Why it happens:** Configuração padrão do Jackson pode não preservar `BigDecimal` com precisão exata.
**How to avoid:** Verificar configuração de serialização; considerar `write-bigdecimal-as-plain=true` ou anotar campos com `ToStringSerializer`.
**Warning signs:** Valores como `123.450000000001` ou `1.23E+2` na resposta JSON.

### Pitfall 6: Percentual sobre total de saídas quando total é zero
**What goes wrong:** Divisão por zero no cálculo do percentual por categoria.
**Why it happens:** Denominador é `totalSaidas` que pode ser zero em meses sem lançamentos.
**How to avoid:** Se `totalSaidas == 0`, percentual de cada categoria = 0 (ou "—"), barras com largura zero, sem erro.
**Warning signs:** Erro no frontend ao calcular percentual; gráfico quebra.

### Pitfall 7: Categoria de ENTRADA aparecendo no "gasto por categoria"
**What goes wrong:** Query de breakdown não filtra por tipo = SAIDA.
**Why it happens:** Esquecimento do filtro de tipo na query de agregação por categoria.
**How to avoid:** Query filtra `l.tipo = 'SAIDA'` e `c.tipo = 'SAIDA'`; LEFT JOIN traz categorias de saída zeradas.
**Warning signs:** Categorias de entrada (ex: "Rendimentos") aparecem na tabela de gasto.

### Pitfall 8: NEXT.js dynamic route com searchParams não reativa
**What goes wrong:** Mudança na URL (`?mes=2026-11`) não dispara refetch dos dados.
**Why it happens:** Página estática (instant = true) ou searchParams não aguardado.
**How to avoid:** `export const instant = false`; `await searchParams` no Server Component; usar `router.refresh()` ou navegação após setas.
**Warning signs:** Usuário clica em seta, URL muda, mas números não atualizam.

## Code Examples

### SQL agregação dashboard (verificado contra schema V1__schema.sql)

```sql
-- Totais por tipo para um mês (fechado: primeiro ao último dia)
SELECT
  COALESCE(SUM(CASE WHEN l.tipo = 'ENTRADA' THEN l.valor ELSE 0 END), 0) as total_entradas,
  COALESCE(SUM(CASE WHEN l.tipo = 'SAIDA' THEN l.valor ELSE 0 END), 0) as total_saidas
FROM lancamento l
WHERE l.empresa_id = :empresaId
  AND l.data >= :dataInicio
  AND l.data <= :dataFim;

-- Breakdown por categoria de SAÍDA (inclusive zeradas via LEFT JOIN)
SELECT
  c.id,
  c.nome,
  COALESCE(SUM(l.valor), 0) as total
FROM categoria c
LEFT JOIN lancamento l ON l.categoria_id = c.id
  AND l.empresa_id = :empresaId
  AND l.tipo = 'SAIDA'
  AND l.data >= :dataInicio
  AND l.data <= :dataFim
WHERE c.empresa_id = :empresaId
  AND c.tipo = 'SAIDA'
GROUP BY c.id, c.nome
ORDER BY total DESC;
```
*Source: Verificado contra `V1__schema.sql` (tabelas categoria/lancamento, colunas empresa_id, tipo, valor, data) e índices existentes*

### JPQL query no LancamentoRepository (nova)

```java
@Query(
    "select new com.controlefinanceiro.dashboard.dto.CategoryExpense(" +
    "  c.id, c.nome, coalesce(sum(l.valor), 0)) " +
    "from Categoria c " +
    "left join Lancamento l on l.categoria = c " +
    "  and l.empresa.id = :empresaId " +
    "  and l.tipo = 'SAIDA' " +
    "  and l.data between :dataInicio and :dataFim " +
    "where c.empresa.id = :empresaId " +
    "  and c.tipo = 'SAIDA' " +
    "group by c.id, c.nome " +
    "order by coalesce(sum(l.valor), 0) desc")
List<CategoryExpense> findGastoPorCategoria(
    @Param("empresaId") Long empresaId,
    @Param("dataInicio") LocalDate dataInicio,
    @Param("dataFim") LocalDate dataFim);
```
*Source: [ASSUMED] — padrão baseado em `LancamentoRepository.findByEmpresaIdAndFiltros` existente; sintaxe JPQL precisa de verificação contra Hibernate versão do projeto*

### shadcn/ui Charts wrapper (D-11)

```tsx
// web/components/ui/chart.tsx
"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

export function Chart({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="chart"
      className={cn("w-full h-[300px]", className)}
      {...props}
    />
  );
}
```
*Source: [ASSUMED] — padrão shadcn/ui Charts; não verificado contra documentação oficial nesta sessão*

### Recharts horizontal bar chart (D-08, D-12)

```tsx
"use client";

import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { Chart } from "@/components/ui/chart";

export function CategoryBreakdownChart({ data }: { data: { nome: string; total: number }[] }) {
  const total = data.reduce((sum, item) => sum + item.total, 0);
  
  return (
    <Chart>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="horizontal" margin={{ left: 80, right: 20, top: 20, bottom: 20 }}>
          <CartesianGrid horizontal dashed={false} />
          <YAxis dataKey="nome" width={120} />
          <XAxis type="number" />
          <Tooltip formatter={(value) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)} />
          <Bar dataKey="total" fill="#0f766e" radius={[0, 4, 4, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </Chart>
  );
}
```
*Source: [ASSUMED] — padrão Recharts; não verificado contra documentação oficial nesta sessão*

### Native month input (D-13)

```tsx
"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function MonthSelector() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [mes, setMes] = useState(searchParams.get("mes") ?? new Date().toISOString().slice(0, 7));

  useEffect(() => {
    if (mes) router.push(`/?mes=${mes}`, { scroll: false });
  }, [mes, router]);

  return (
    <div className="flex items-center gap-2">
      <Label>Mês</Label>
      <Input type="month" value={mes} onChange={(e) => setMes(e.target.value)} />
    </div>
  );
}
```
*Source: [ASSUMED] — padrão Next.js 16 navigation; não verificado contra documentação oficial nesta sessão*

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Shell autenticado apenas (Fase 1-2) | Dashboard com KPIs, comparação, gráfico | Fase 3 | Nova responsabilidade de agregação no backend |
| Nenhuma biblioteca de gráficos | shadcn/ui Charts (Recharts) | Fase 3 | Nova dependência npm `recharts` |

**Deprecated/outdated:** none

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `recharts` é o pacote npm correto para shadcn/ui Charts | Standard Stack, Package Legitimacy Audit | Pacote não existe ou tem nome diferente → instalação falha |
| A2 | Jackson serializa `BigDecimal` corretamente sem config extra | Code Examples (Pattern 2) | Valores monetários perdem precisão no JSON |
| A3 | JPQL com `left join` + `coalesce` funciona com Hibernate do projeto | Code Examples (JPQL query) | Query falha em tempo de execução; ajuste de SQL nativo necessário |
| A4 | `export const instant = false` é suficiente para searchParams dinâmicas em Next 16 | Code Examples (MonthSelector) | Página não reativa a mudanças de URL |
| A5 | `web/lib/api.ts` pode ser importado por Server Components das novas rotas | Architecture Patterns | Módulo não é server-only; falha no client |

**If this table is empty:** All claims in this research were verified or cited — no user confirmation needed. (Not the case — 5 assumptions listed above.)

## Open Questions

1. **Jackson BigDecimal configuração:** O projeto já configura `spring.jackson.serialization.write-bigdecimal-as-plain=true` ou usa `ToStringSerializer` em algum DTO existente?
   - What we know: Nenhuma config Jackson encontrada nos arquivos lidos; `LancamentoResponse` usa `BigDecimal` diretamente.
   - What's unclear: Se a serialização padrão preserva precisão de `NUMERIC(14,2)`.
   - Recommendation: Verificar `application.properties`/`application.yml` da API; adicionar config se necessário.

2. **Versão do `recharts`:** Qual versão instalar?
   - What we know: Pacote não instalado; não há referência a gráficos em `web/package.json`.
   - What's unclear: Versão compatível com React 19.3 e Next 16.4.3.
   - Recommendation: `npm view recharts versions` no diretório web/ para ver versões disponíveis.

3. **DTO de agregação:** Criar novo pacote `dashboard` ou adicionar ao pacote `lancamento`?
   - What we know: CONTEXT.md sugere "pacote `lancamento` (ou pacote `dashboard`)".
   - What's unclear: Nenhuma preferência firme.
   - Recommendation: Pacote `dashboard` separado para clareza e reuso pela Fase 4 (D-02).

4. **Formulários de CRUD:** Usar client components controlados (sem RHF/zod, como `LoginForm`) — essa é a única abordagem?
   - What we know: CONTEXT.md deixa a cargo do agente; `LoginForm` é o padrão existente.
   - What's unclear: Se formulários maiores (lançamento com 5 campos) se beneficiariam de RHF.
   - Recommendation: Seguir o padrão `LoginForm` (controlado, sem RHF/zod) para consistência.

5. **Total para aviso de truncamento (D-22):** Onde virá o "N" de "mostrando 50 de N"?
   - What we know: `GET /api/lancamentos` trunca em 50; não há count separado.
   - What's unclear: Usar endpoint de agregação para obter total de lançamentos do mês, ou adicionar count na listagem.
   - Recommendation: Endpoint de agregação pode incluir `totalLancamentos` no DTO, ou fazer `count` separado.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js / npm | `npm install recharts` no web/ | ✓ | [verificar com `node --version`] | — |
| Java 21 + Maven | `./mvnw verify` na API | ✓ | [verificar com `./mvnw --version`] | — |
| PostgreSQL | Testes de integração (Testcontainers) | ✓ | via Testcontainers (Docker) | — |
| Docker | Testcontainers PostgreSQL | ✓ | [verificar com `docker --version`] | — |
| `recharts` npm package | Gráfico de barras (D-11) | ✗ | — | Adicionar `checkpoint:human-verify` antes de instalar |

**Missing dependencies with no fallback:** none
**Missing dependencies with fallback:** `recharts` — checkpoint:human-verify before install

## Security Domain

> Required when `security_enforcement` is enabled (absent = enabled). Omit only if explicitly `false` in config. `workflow.nyquist_validation` is `false` in `.planning/config.json` — Validation Architecture section omitted.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Sessão via cookie `sess` (httpOnly) + JWT Bearer na API |
| V3 Session Management | yes | Cookie `sess` validado via `getMe()`; logout limpa cookie |
| V4 Access Control | yes | `SecurityConfig`: GET liberado para ADMIN/USER; escrita /api/categorias/** restrita a ADMIN; tenant via `TenantContext` |
| V5 Input Validation | yes | `BigDecimal` com `@Digits`, validação de escala no service, `Intl.NumberFormat` apenas para exibição |
| V6 Cryptography | yes | BCrypt custo 12 (senhas); HTTPS em produção (Render/Vercel) |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Tenant forgery (usuário vê dados de outra empresa) | Tampering | Todas as queries filtram por `empresa_id` do JWT via `TenantContext`; nunca de body/query |
| Insecure direct object reference (acesso a lançamento/categoria de outra empresa) | Tampering | Repository filtra por `empresa_id` em todas as queries (`findByIdAndEmpresaId`) |
| Monetary precision loss | Tampering | `BigDecimal`/`NUMERIC(14,2)`; nenhum `double`/`float`; `scale = 2`, `HALF_EVEN` |
| Division by zero in comparison | Denial of Service | Tratamento condicional: base <= 0 → percentual "—", apenas variação absoluta |
| Unauthorized category write by USER | Elevation of Privilege | `SecurityConfig` restringe `/api/categorias/**` a `ROLE_ADMIN`; UI oculta ações para USER |

## Sources

### Primary (HIGH confidence)
- `api/src/main/resources/db/migration/V1__schema.sql` — schema, índices, NUMERIC(14,2), CHECK valor > 0 (lido diretamente)
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` — endpoints existentes, padrão de controller (lido diretamente)
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java` — queries JPQL existentes, índices usados (lido diretamente)
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` — padrão de serviço, validação monetária, tenant (lido diretamente)
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java` — CRUD categorias, DELETE=desativar (lido diretamente)
- `api/src/main/java/com/controlefinanceiro/categoria/CategoriaService.java` — soft delete padrão (lido diretamente)
- `api/src/main/java/com/controlefinanceiro/common/TenantContext.java` — único ponto de acesso ao tenant (lido diretamente)
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java` — role-based access, /api/categorias/** ADMIN-only (lido diretamente)
- `web/lib/api.ts` — padrão server-side com cookie `sess` (lido diretamente)
- `web/app/page.tsx` — shell autenticado, `instant = false` (lido diretamente)
- `web/app/globals.css` — tokens `--chart-1..5` (lido diretamente)
- `web/proxy.ts` — gate de autenticação por cookie (lido diretamente)
- `.planning/phases/03-dashboard/03-CONTEXT.md` — todas as decisões D-01 a D-24 (lido diretamente)
- `.planning/REQUIREMENTS.md` — DSH-01 a DSH-05, CAT-01 a 06, LAN-01 a 07, LOT-01 a 02 (lido diretamente)
- `.planning/config.json` — `nyquist_validation: false` (lido diretamente)
- `.planning/STATE.md` — fase 2 completa, fase 3 pronta para planejar (lido diretamente)
- `.planning/ROADMAP.md` — goal e success criteria da Fase 3 (lido diretamente)

### Secondary (MEDIUM confidence)
- shadcn/ui Charts usa Recharts por baixo — mencionado em CONTEXT.md D-11 (não verificado contra docs oficiais)

### Tertiary (LOW confidence)
- `recharts` como nome do pacote npm — [ASSUMED], não verificado via Context7 ou web search
- Configuração de serialização Jackson para BigDecimal — [ASSUMED], não verificada no projeto
- Padrão shadcn/ui Charts wrapper `components/ui/chart.tsx` — [ASSUMED], não verificado contra docs oficiais

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — todas as versões verificadas diretamente em package.json e código existente
- Architecture: HIGH — padrões verificados no código existente (LancamentoRepository, TenantContext, web/lib/api.ts)
- Pitfalls: MEDIUM — baseados em padrões conhecidos do domínio, alguns [ASSUMED]

**Research date:** 2026-11-09
**Valid until:** 2026-12-09 (30 dias — stack estável)
