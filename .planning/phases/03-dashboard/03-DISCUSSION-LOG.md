# Phase 3: Dashboard - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-11-09
**Phase:** 03-Dashboard
**Areas discussed:** Comparação mensal · Gasto por categoria · Seletor de mês · Escopo de UI herdado

---

## Comparação mensal

| Option | Description | Selected |
|--------|-------------|----------|
| Absoluto | Variação apenas em R$ | |
| Percentual | Variação apenas em % | |
| Ambos | R$ + % | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Só saldo | Comparar apenas o saldo | |
| Entradas e saídas | Comparar entradas e saídas | |
| Entradas, saídas e saldo | Comparar as três métricas | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Sem base (—) | Percentual "—" e apenas variação absoluta | ✓ |
| +100% | Tratar base zero como +100% | |
| Outro | — | |

| Option | Description | Selected |
|--------|-------------|----------|
| Mês fechado vs mês fechado | Mês inteiro contra mês inteiro | ✓ |
| Mesmo período parcial | 1–9 nov vs 1–9 out | |

**User's choice:** Ambos (R$ + %), sobre entradas/saídas/saldo; base zero → "—" + absoluto; mês fechado vs mês fechado.

**Notes / clarifications:**
- O percentual só faz sentido com base anterior positiva; base zero/não positiva cai em "—" (tratamento de divisão por zero).

---

## Gasto por categoria

| Option | Description | Selected |
|--------|-------------|----------|
| Tabela/lista | Somente lista ordenada | |
| Gráfico de barras | Somente gráfico | |
| Ambos | Lista ordenada + gráfico de barras | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Somente valor | Valor em R$ | |
| Valor e percentual | R$ + % do total de saídas | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Somente com lançamento | Ocultar categorias zeradas | |
| Todas, inclusive zeradas | Listar todas as categorias de SAÍDA | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Agrupar em Outros | Agrupar categorias pequenas | |
| Listar todas | Sem agrupamento | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| shadcn/ui Charts | Recharts + wrapper shadcn, alinhado ao design system | ✓ |
| Recharts direto | Sem wrapper shadcn | |
| Chart.js | react-chartjs-2 | |

| Option | Description | Selected |
|--------|-------------|----------|
| Somente SAÍDA, incluindo zeradas | "Gasto" = despesas | ✓ |
| Todas, inclusive ENTRADA | Incluir categorias de entrada | |

**User's choice:** Lista + gráfico; valor e percentual; todas as categorias de SAÍDA incluindo zeradas; sem "Outros"; shadcn/ui Charts.

**Notes / clarifications:**
- Barras horizontais ordenadas por valor.
- Percentual sobre o total de saídas; total zero → sem erro (percentual/barras vazios).
- Tokens `--chart-1..5` já existem no `globals.css`.

---

## Seletor de mês

| Option | Description | Selected |
|--------|-------------|----------|
| input type=month | Controle nativo | ✓ |
| Dropdown customizado | Lista de meses | |

| Option | Description | Selected |
|--------|-------------|----------|
| Somente até o mês corrente | Bloquear futuro | |
| Permitir futuros | Meses futuros liberados | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Sem limite | Qualquer mês passado | ✓ |
| Desde o primeiro lançamento | Limite dinâmico | |
| Mês fixo | Limite fixo | |

| Option | Description | Selected |
|--------|-------------|----------|
| URL | `?mes=YYYY-MM` | ✓ |
| Estado do cliente | Somente em memória | |

| Option | Description | Selected |
|--------|-------------|----------|
| Somente seletor | Sem setas | |
| Seletor + setas | Setas ‹ › para navegar | ✓ |

**User's choice:** `input type="month"`; permitir futuros; sem limite inferior; estado na URL; setas ‹ ›.

---

## Escopo de UI herdado

| Option | Description | Selected |
|--------|-------------|----------|
| Somente dashboard | CRUD fica para outra fase | |
| Dashboard + CRUD categorias/lançamentos | UI herdada da Fase 2 | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Sem UI de lote | Lote continua API-only | ✓ |
| Incluir tela de lote | Importação em lote na web | |

| Option | Description | Selected |
|--------|-------------|----------|
| Rotas separadas | `/`, `/lancamentos`, `/categorias` | ✓ |
| Tudo na home | Tudo empilhado em `/` | |

| Option | Description | Selected |
|--------|-------------|----------|
| Desativar (soft delete) | Mantém histórico | ✓ |
| DELETE físico | Remove o registro | |

| Option | Description | Selected |
|--------|-------------|----------|
| Filtrar por mês (limite 50 + aviso) | Reusa o seletor e a API atual | ✓ |
| Paginação real no backend | Endpoint paginado novo | |

**User's choice:** CRUD de categorias e lançamentos nesta fase; sem UI de lote; rotas separadas com navegação no topo; desativar = soft delete; listagem filtrada por mês com aviso de truncamento.

**Notes / clarifications:**
- Escrita de categorias permanece ADMIN-only (backend); UI deve refletir o role.
- Paginação real foi explicitamente adiada.

---

## the agent's Discretion

- Shape/nome do endpoint de agregação e DTOs; forma de incluir categorias zeradas (LEFT JOIN); arredondamento do percentual; labels e ordem de empates.
- Layout do dashboard (grid de KPIs, posição da comparação, lista vs gráfico) e empty states.
- Estrutura dos formulários (client components controlados, sem RHF/zod).

## Deferred Ideas

- UI de importação em lote — API-only nesta fase.
- Paginação real da listagem — adiada; filtro por mês + aviso de 50.
- Importação OFX/CSV, lançamentos recorrentes — v2.
- Exportação PDF/Excel, gráficos interativos avançados — fora de escopo.
- Multiempresa real — fora de escopo.
- IA/OpenRouter — Fase 4.
