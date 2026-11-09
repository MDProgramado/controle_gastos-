---
phase: 03-dashboard
verified: 2026-11-09T15:30:00Z
status: gaps_found
score: 23/26 must-haves verified
overrides_applied: 0
re_verification: false
gaps:
  - truth: "Criar lançamento/categoria de tipo SAÍDA — frontend envia 'SAÍDA' com acento, backend espera 'SAIDA' sem acento (CR-01)"
    status: failed
    reason: "LancamentosClient.tsx:18 define TipoLancamento = 'ENTRADA' | 'SAÍDA' (com acento) e o <option value='SAÍDA'> envia o valor com acento. CategoriasClient.tsx:407 também usa <option value='SAÍDA'>. O backend (DashboardRepository, LancamentoRepository) filtra l.tipo = 'SAIDA' (sem acento). LancamentoService.validarIntegridadeCategoriaTipo faz categoria.getTipo().equals(tipoLancamento) — 'SAIDA'.equals('SAÍDA') é false, então criar saídas ou categorias de saída falha. O teste de integração usa 'SAIDA' sem acento, confirmando o contrato do backend."
    artifacts:
      - path: "web/components/LancamentosClient.tsx"
        issue: "Linha 18: enum TipoLancamento usa 'SAÍDA' com acento; linha 456: <option value='SAÍDA'> envia acento ao backend"
      - path: "web/components/CategoriasClient.tsx"
        issue: "Linha 77: cast 'ENTRADA' | 'SAÍDA'; linha 407: <option value='SAÍDA'> envia acento ao backend"
    missing:
      - Alterar valores do tipo para 'SAIDA' (sem acento) em LancamentosClient.tsx e CategoriasClient.tsx para bater com o contrato do backend
  - truth: "Guard de divisão por zero em CategoryBreakdown — totalSaidas chega como string (BigDecimal serializado via ToStringSerializer), guard if (!totalSaidas) não dispara para '0.00', resultando em NaN% (CR-02)"
    status: failed
    reason: "DashboardResponse serializa BigDecimal via @JsonSerialize(ToStringSerializer) e application.yml write-bigdecimal-as-plain: true. O frontend tipa totalSaidas como number em api.ts:43 e CategoryBreakdown.tsx:40, mas em runtime chega como string '0.00'. O guard if (!totalSaidas) avalia !('0.00') → false (string não vazia é truthy), então o guard é bypassado. A expressão categoria.total / totalSaidas calcula 0 / 0 = NaN, exibindo 'NaN%' na UI."
    artifacts:
      - path: "web/components/CategoryBreakdown.tsx"
        issue: "Linha 53: if (!totalSaidas) não protege quando totalSaidas é string '0.00'; linha 56: categoria.total / totalSaidas produz NaN"
      - path: "web/lib/api.ts"
        issue: "Linha 43: totalSaidas: number — tipo incorreto, backend envia string"
      - path: "web/components/DashboardKPIs.tsx"
        issue: "Linhas 47-51: campos monetários tipados como number, backend envia strings"
    missing:
      - Fazer parse de totalSaidas para number antes do guard (Number(totalSaidas) || 0) em CategoryBreakdown.tsx
      - Alterar tipos TypeScript de campos monetários recebidos da API para string e fazer parse apenas na exibição (WR-02)
  - truth: "Aviso de truncamento 'Mostrando 50 de N' nunca dispara — total é o tamanho da página, não o total real (WR-03)"
    status: failed
    reason: "getLancamentos em web/lib/api.ts:128 retorna { lancamentos: lista, total: lista.length } — total é o número de itens retornados na página (limitado a 50 pela API), não o total real de lançamentos do mês. O aviso truncado = total > 50 em LancamentosClient.tsx:201 nunca será true porque total sempre equals lista.length."
    artifacts:
      - path: "web/lib/api.ts"
        issue: "Linha 128: total: lista.length — deveria ser o total real do banco"
      - path: "web/components/LancamentosClient.tsx"
        issue: "Linha 201: truncado = total > 50 nunca dispara"
    missing:
      - Backend retornar total real de lançamentos (count separado ou campo total paginado) e getLancamentos lê-lo do corpo da resposta

deferred: []
---

# Phase 3: Dashboard — Verification Report

**Phase Goal:** Entradas, saídas, saldo por mês, gasto por categoria e comparação mensal na web

**Verified:** 2026-11-09T15:30:00Z

**Status:** gaps_found

**Score:** 23/26 must-haves verified

**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | GET /api/dashboard?ano=&mes= retorna totais de entradas, saídas e saldo do mês (DSH-01) | ✓ VERIFIED | DashboardController, DashboardService, DashboardRepository implementados. DashboardIntegrationTest (4 testes GREEN, Testcontainers + PostgreSQL real) valida totais corretos contra seed conhecida. |
| 2 | GET /api/dashboard?ano=&mes= retorna comparação com mês anterior: variação absoluta + percentual (DSH-03) | ✓ VERIFIED | DashboardService calcula variações e percentuais; teste `comparacaoMesAnteriorBaseZeroRetornaPercentualTrace` valida percentual "—" e variações não-null. |
| 3 | GET /api/dashboard?ano=&mes= retorna breakdown por categoria de SAÍDA, inclusive zeradas via LEFT JOIN (DSH-02) | ✓ VERIFIED | DashboardRepository.breakdownSaida usa LEFT JOIN Categoria c LEFT JOIN Lancamento l, filtra c.tipo='SAIDA', ordena por valor desc. Teste valida Supermercado com 100.00 e categorias zeradas. |
| 4 | Mês sem lançamentos retorna zeros (COALESCE), não null (DSH-05, SC 3) | ✓ VERIFIED | Queries usam COALESCE(SUM(valor), 0). Teste `mesSemLancamentosRetornaZeros` valida totalEntradas=0, totalSaidas=0, saldo=0, categorias não-null. |
| 5 | Todas as queries filtram por empresa_id do TenantContext, nunca de body/query (DSH-05, SC 4; D-04) | ✓ VERIFIED | DashboardController chama TenantContext.getEmpresaId(). Repository queries filtram WHERE l.empresa.id = :empresaId e c.empresa.id = :empresaId. Teste `endpointNaoExpõeEmpresaIdETenantIsolaDados` valida isolamento. |
| 6 | Nenhum double/float — BigDecimal, scale=2, HALF_EVEN (LAN-05) | ✓ VERIFIED | DashboardService usa BigDecimal com CEM = new BigDecimal("100"), calcularPercentual usa multiply/divide com scale=2, HALF_EVEN. application.yml write-bigdecimal-as-plain: true + ToStringSerializer nos DTOs. |
| 7 | application.yml configuração Jackson para BigDecimal preservar precisão | ✓ VERIFIED | application.yml linhas 25-28: spring.jackson.serialization.write-bigdecimal-as-plain: true. DashboardResponse campos BigDecimal com @JsonSerialize(ToStringSerializer.class). |
| 8 | Usuário vê seletor de mês com input type=month, padrão = mês corrente (D-13, DSH-04) | ✓ VERIFIED | MonthSelector.tsx:41-44: <Input type="month" value={urlMes}>, sem min/max. page.tsx:33-35: default = new Date().toISOString().slice(0,7). |
| 9 | URL atualizada com ?mes=YYYY-MM ao selecionar mês ou navegar com setas ‹ › (D-16, D-17) | ✓ VERIFIED | MonthSelector.tsx:30 (onChange) e linhas 46-51 (setas ‹ ›) usam router.push(`/?mes=${proximo}`). key={urlMes} no container para re-mount. |
| 10 | KPIs mostram entradas, saídas e saldo do mês selecionado (DSH-01) | ✓ VERIFIED | DashboardKPIs.tsx renderiza 3 cards (Entradas/verde, Saídas/vermelho, Saldo/cor por sinal). page.tsx passa dados.totalEntradas, totalSaidas, saldo como props. |
| 11 | Variação vs mês anterior em R$ absoluto e percentual para entradas, saídas e saldo (D-05, DSH-03) | ✓ VERIFIED | DashboardKPIs.tsx formatarVariacao exibe "+R$ X,00 (Y,YY%)" quando percentual disponível, "R$ X,00" quando "—". page.tsx passa todos os campos de variacao e percentual. |
| 12 | Quando mês anterior é zero, percentual = "—", apenas variação absoluta (D-06) | ✓ VERIFIED | Backend calcularPercentual retorna "—" quando anterior <= 0. DashboardKPIs.tsx linha 25: if (percentual === "—") retorna apenas absoluto. |
| 13 | Todos os dados vêm de GET /api/dashboard (não da listagem truncada de lançamentos) (D-01) | ✓ VERIFIED | page.tsx chama getDashboard(ano, mes). DashboardData type em api.ts espelha o DTO do backend. |
| 14 | Sessão validada via getMe() cookie auth; sem sessão → redirect /login (AUTH-03) | ✓ VERIFIED | page.tsx:24-27: getMe() com redirect /login?expirada=1. getDashboard retorna null sem token. |
| 15 | Tabela lista todas as categorias de SAÍDA do mês, inclusive zeradas, ordenada por valor decrescente (D-08, D-09) | ✓ VERIFIED | CategoryBreakdown.tsx:50 ordena por total desc. Backend LEFT JOIN traz categorias zeradas. Empty state quando categorias vazio. |
| 16 | Categorias de ENTRADA não aparecem no gasto por categoria | ✓ VERIFIED | DashboardRepository.breakdownSaida filtra c.tipo='SAIDA' e l.tipo='SAIDA'. |
| 17 | Não agrupa categorias em "Outros" (D-09) | ✓ VERIFIED | CategoryBreakdown renderiza cada categoria individualmente, sem agrupamento. |
| 18 | Gráfico de barras horizontais via shadcn/ui Charts (Recharts) com wrapper chart.tsx (D-11, D-12) | ✓ VERIFIED | web/components/ui/chart.tsx existe (wrapper <div data-slot="chart">). CategoryBreakdown.tsx usa BarChart layout="horizontal" com Chart wrapper, fill="var(--chart-1)". recharts ^2.15.4 instalado em package.json. |
| 19 | Percentual sobre total de saídas; se total=0, percentual='—' e barras com largura zero, sem erro (D-10) | ✗ FAILED | CR-02: totalSaidas chega como string "0.00" (BigDecimal serializado via ToStringSerializer). Guard if (!totalSaidas) → !("0.00") → false, guard bypassado. categoria.total / totalSaidas = 0 / 0 = NaN → "NaN%" na UI. |
| 20 | Valor formatado com Intl.NumberFormat('pt-BR', BRL) (D-03) | ✓ VERIFIED | CategoryBreakdown.tsx usa MONETARY = Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'}). DashboardKPIs.tsx mesma formatação. |
| 21 | Dados vêm de GET /api/dashboard (não da listagem truncada) (D-01) | ✓ VERIFIED | page.tsx passa dados.categorias e dados.totalSaidas do getDashboard para CategoryBreakdown. |
| 22 | Rota /lancamentos: listar (filtrado por mês), criar, editar, excluir lançamentos (D-18, LAN-01..07) | ✗ FAILED | CR-01: Criar/editar lançamento com tipo "SAÍDA" envia acento, backend espera "SAIDA". Listagem, editar e excluir funcionam para ENTRADA; criação de saídas está quebrada. |
| 23 | Rota /categorias: listar, criar, editar, desativar (soft delete) categorias (D-18, D-19, CAT-01..06) | ✗ FAILED | CR-01: Criar/editar categoria com tipo "SAÍDA" envia acento, backend espera "SAIDA". Soft delete (ativo=false) implementado. |
| 24 | TopNav com navegação entre /, /lancamentos, /categorias + LogoutButton (D-21) | ✓ VERIFIED | TopNav.tsx implementado com next/link, 3 rotas, LogoutButton, item ativo #0F766E. Integrado nas 3 páginas. |
| 25 | DeleteConfirmationDialog para excluir lançamento e desativar categoria, sem window.confirm (D-24) | ✓ VERIFIED | DeleteConfirmationDialog.tsx usa @radix-ui/react-dialog, sem window.confirm. Props isOpen, onConfirm, onClose, action, target. Usado em LancamentosClient e CategoriasClient. |
| 26 | Escrita de categorias ADMIN-only na UI; USER só lê (D-23) | ✓ VERIFIED | CategoriasClient.tsx:56 podeEscrever = userRole === 'ADMIN'. Botões Editar/Desativar condicionados a podeEscrever. Backend SecurityConfig restringe /api/categorias/** a ROLE_ADMIN. |
| 27 | Listagem de lançamentos filtra pelo mês selecionado da URL (?mes=YYYY-MM) (D-22) | ✓ VERIFIED | lancamentos/page.tsx:37-39 parseia ano/mes do searchParams, chama getLancamentos(ano, mes). |
| 28 | Aviso de truncamento "Mostrando 50 de {N}" quando houver mais de 50 lançamentos (D-22) | ✗ FAILED | WR-03: getLancamentos retorna total = lista.length (tamanho da página), não o total real. truncado = total > 50 nunca dispara. |
| 29 | Validação monetária UI espelha API: valor > 0, máximo 2 casas decimais (LAN-04, LAN-05) | ✓ VERIFIED | LancamentosClient.tsx:111-129 validarForm: VALOR_RE = /^-?\d+(\.\d{1,2})?$/, v <= 0 rejeitado. |
| 30 | Lint e build do web passam (npx eslint, next build) | ✓ VERIFIED | 03-04-SUMMARY.md relata npx eslint . (0 erros), npx next build, npx tsc --noEmit passando. Commits c2d7e09, 62b7d3f existem. |

**Score:** 23/30 truths verified (3 FAILED due to CR-01, CR-02, WR-03)

---

### Deferred Items

None — all gaps are real, actionable issues in the current phase.

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `api/src/main/java/com/controlefinanceiro/dashboard/DashboardController.java` | @RestController @RequestMapping("/api/dashboard") | ✓ VERIFIED | @GetMapping, construtor com DashboardService, TenantContext.getEmpresaId() |
| `api/src/main/java/com/controlefinanceiro/dashboard/DashboardService.java` | @Service, agregação SUM/GROUP BY | ✓ VERIFIED | BigDecimal, scale=2, HALF_EVEN, calcularPercentual com guard base<=0 |
| `api/src/main/java/com/controlefinanceiro/dashboard/DashboardRepository.java` | @Repository, JPQL agregação | ✓ VERIFIED | totalEntradas, totalSaidas, breakdownSaida com LEFT JOIN, COALESCE, filtro empresa_id |
| `api/src/main/java/com/controlefinanceiro/dashboard/dto/DashboardResponse.java` | record DTO com BigDecimal ToStringSerializer | ✓ VERIFIED | 9 campos, percentuais como String, sem empresa_id (R3) |
| `api/src/main/java/com/controlefinanceiro/dashboard/dto/CategoryExpense.java` | record (id, nome, total) | ✓ VERIFIED | Sem empresa_id (R3) |
| `api/src/test/java/com/controlefinanceiro/dashboard/DashboardIntegrationTest.java` | Testcontainers, 4 testes GREEN | ✓ VERIFIED | Valida totais, zeros, percentual "—", isolamento de tenant |
| `api/src/main/resources/application.yml` | write-bigdecimal-as-plain: true | ✓ VERIFIED | Linha 28 |
| `web/components/ui/chart.tsx` | Wrapper shadcn/ui Charts | ✓ VERIFIED | <div data-slot="chart" className="w-full h-[300px]"> |
| `web/components/MonthSelector.tsx` | input type=month + setas ‹ › | ✓ VERIFIED | Sem min/max, router.push ?mes=YYYY-MM |
| `web/components/DashboardKPIs.tsx` | 3 cards KPI com variação | ✓ VERIFIED | formatarVariacao, corSaldo, Intl.NumberFormat pt-BR |
| `web/components/CategoryBreakdown.tsx` | Tabela + gráfico barras horizontais | ✗ FAILED | CR-02: NaN% quando totalSaidas=0 (string) |
| `web/lib/api.ts` | getDashboard + CRUD functions | ✓ VERIFIED | getDashboard, getLancamentos, criarLancamento, editarLancamento, excluirLancamento, getCategorias, criarCategoria, editarCategoria, desativarCategoria |
| `web/app/page.tsx` | Server Component dashboard | ✓ VERIFIED | instant=false, getMe(), getDashboard, MonthSelector, DashboardKPIs, CategoryBreakdown |
| `web/app/lancamentos/page.tsx` | CRUD lançamentos | ✗ FAILED | CR-01: criar saídas envia "SAÍDA" com acento |
| `web/app/categorias/page.tsx` | CRUD categorias | ✗ FAILED | CR-01: criar categoria SAÍDA envia "SAÍDA" com acento |
| `web/components/TopNav.tsx` | Navegação 3 rotas | ✓ VERIFIED | next/link, item ativo #0F766E |
| `web/components/DeleteConfirmationDialog.tsx` | Diálogo sem window.confirm | ✓ VERIFIED | @radix-ui/react-dialog |
| `web/components/LancamentosClient.tsx` | Cliente interativo lançamentos | ✗ FAILED | CR-01 (tipo SAÍDA), WR-06 (categoriaMap lookup por ID errado), WR-07 (parseFloat) |
| `web/components/CategoriasClient.tsx` | Cliente interativo categorias | ✗ FAILED | CR-01 (tipo SAÍDA) |
| `web/package.json` | recharts instalado | ✓ VERIFIED | recharts ^2.15.4 em dependencies |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| DashboardController → DashboardService | constructor injection | `dashboardService.obterDashboard(...)` | ✓ WIRED | @Service injetado no construtor do @RestController |
| DashboardService → TenantContext | `TenantContext.getEmpresaId()` | empresa_id do JWT | ✓ WIRED | Nenhum parâmetro empresa_id no endpoint |
| DashboardRepository → lancamento/categoria tabelas | JPQL @Query | `l.empresa.id = :empresaId` | ✓ WIRED | Filtro por tenant em todas as queries |
| page.tsx → api.ts | `getDashboard(ano, mes)` | fetch com Bearer token | ✓ WIRED | cookie "sess" + Authorization: Bearer |
| page.tsx → MonthSelector | props mes/onMesChange | router.push com ?mes= | ✓ WIRED | URL searchParams sincronizada |
| page.tsx → DashboardKPIs | props totais/variacoes/percentuais | dados do getDashboard | ✓ WIRED | Todos os 9 campos passados |
| page.tsx → CategoryBreakdown | props categorias/totalSaidas | dados do getDashboard | ✓ WIRED | Dados do backend chegam como props |
| CategoryBreakdown → chart.tsx | `<Chart>` wrapper | Recharts BarChart dentro de Chart | ✓ WIRED | fill="var(--chart-1)", layout="horizontal" |
| api.ts → LancamentoController | `/api/lancamentos` | GET/POST/PUT/DELETE + Bearer | ✓ WIRED | Cookie sess + Bearer token |
| api.ts → CategoriaController | `/api/categorias` | GET/POST/PUT/DELETE + Bearer | ✓ WIRED | Cookie sess + Bearer token |
| lancamentos/page → DeleteConfirmationDialog | `onConfirm` → `onExcluir` | DELETE físico de lançamento | ✓ WIRED | Sem window.confirm |
| categorias/page → DeleteConfirmationDialog | `onConfirm` → `onDesativar` | soft delete (ativo=false) | ✓ WIRED | Sem window.confirm |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|-------------------|--------|
| DashboardController | DashboardResponse | DashboardRepository queries | ✓ Real (SUM/GROUP BY SQL) | ✓ FLOWING |
| DashboardKPIs | totalEntradas/totalSaidas/saldo | getDashboard → API → BigDecimal strings | ⚠️ Tipado como number, chega como string | ⚠️ STATIC (tipo mentiroso) |
| CategoryBreakdown | categorias[].total / totalSaidas | getDashboard → API → BigDecimal strings | ⚠️ totalSaidas string quebra guard de zero | ✗ HOLLOW (guard quebrado, NaN%) |
| LancamentosClient | lancamentos[] | getLancamentos → API | ✓ Real (lista do banco) | ✓ FLOWING |
| CategoriasClient | categorias[] | getCategorias → API | ✓ Real (lista do banco) | ✓ FLOWING |

---

### Behavioral Spot-Checks

Step 7b: SKIPPED — no running server available to execute HTTP calls against. All verification performed via static code analysis. The DashboardIntegrationTest (Testcontainers + PostgreSQL real) provides the strongest behavioral evidence for the backend.

---

### Probe Execution

Step 7c: SKIPPED — no `scripts/*/tests/probe-*.sh` files found in the project, and no probe declarations in PLAN/SUMMARY files.

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DSH-01 | 03-01, 03-02 | Dashboard mostra total de entradas, saídas e saldo do mês | ✓ SATISFIED | DashboardController + DashboardKPIs implementados e testados |
| DSH-02 | 03-01, 03-03 | Dashboard mostra gasto por categoria do mês | ⚠️ PARTIAL | Breakdown implementado, mas CR-02 quebra percentual quando total=0 |
| DSH-03 | 03-01, 03-02 | Dashboard compara mês com mês anterior (variação) | ✓ SATISFIED | Variação absoluta + percentual, base zero = "—" no backend |
| DSH-04 | 03-02 | Seletor de mês atualiza todos os números | ✓ SATISFIED | MonthSelector + ?mes=YYYY-MM + getDashboard refetch |
| DSH-05 | 03-01, 03-02, 03-03 | Números idênticos a SQL de conferência | ✓ SATISFIED | DashboardIntegrationTest valida contra seed conhecida (Testcontainers) |
| CAT-01..06 | 03-04 | CRUD categorias | ⚠️ PARTIAL | CRUD implementado, mas CR-01 quebra criação de categorias tipo SAÍDA |
| LAN-01..07 | 03-04 | CRUD lançamentos | ⚠️ PARTIAL | CRUD implementado, mas CR-01 quebra criação de lançamentos tipo SAÍDA |
| LOT-01..02 | 03-04 | Lote tudo-ou-nada | ✓ SATISFIED | Herdado da Fase 2 (endpoints existentes) |

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `web/components/LancamentosClient.tsx` | 18, 456 | `"SAÍDA"` com acento (CR-01) | 🛑 Blocker | Criar/editar lançamentos de saída falha no backend |
| `web/components/CategoriasClient.tsx` | 77, 407 | `"SAÍDA"` com acento (CR-01) | 🛑 Blocker | Criar/editar categorias de saída falha no backend |
| `web/components/CategoryBreakdown.tsx` | 53 | `if (!totalSaidas)` com string (CR-02) | 🛑 Blocker | NaN% exibido quando total de saídas é zero |
| `web/components/CategoryBreakdown.tsx` | 48 | `sum + c.total` com c.total string (WR-01) | ⚠️ Warning | Concatenação de strings em vez de soma numérica (código morto atualmente) |
| `web/lib/api.ts` | 41-52 | Campos monetários tipados como `number` mas backend envia strings (WR-02) | ⚠️ Warning | Tipos TypeScript mentirosos, código frágil em runtime |
| `web/lib/api.ts` | 128 | `total: lista.length` (WR-03) | ⚠️ Warning | Aviso de truncamento nunca dispara |
| `web/components/LancamentosClient.tsx` | 277 | `categoriaMap.get(l.id)` — lookup por ID do lançamento (WR-06) | ⚠️ Warning | Código morto/incorreto, categoriaNome já vem da API |
| `web/components/LancamentosClient.tsx` | 120, 142 | `parseFloat` para valor monetário (WR-07) | ⚠️ Warning | Representação de ponto flutuante binário para dinheiro |
| `web/components/MonthSelector.tsx` | 23-25 | `urlMes.split("-").map(Number)` sem validação (WR-04) | ⚠️ Warning | URL malformada (ex: "abc") causa RangeError e crash do client |
| `web/app/page.tsx` | 33-35 | `parseInt(anoStr, 10)` sem validação de range (WR-05) | ⚠️ Warning | mes=13 causa DateTimeException 500 no backend |
| `web/components/TopNav.tsx` | 20, 34 | `userRole` declarado mas não usado (IN-01) | ℹ️ Info | Campo morto na interface |
| `web/app/lancamentos/page.tsx` | 91-113 | `fetchCategorias` duplica padrão de getCategorias (IN-02) | ℹ️ Info | Inconsistente com api.ts wrapper |
| `web/components/TopNav.tsx` | 5 | `import { cn } from "cn"` (IN-03) | ℹ️ Info | Pacote npm diferente do utilitário local shadcn |
| `api/src/test/java/com/controlefinanceiro/dashboard/DashboardIntegrationTest.java` | 42 | Senha de teste hardcoded (IN-04) | ℹ️ Info | Apenas testes, não vazia para produção |

---

### Human Verification Required

No human verification items identified. All gaps are code-level issues detectable through static analysis. The visual appearance (colors, layout, chart rendering) would benefit from human UAT but is not a blocker for gap closure.

---

### Gaps Summary

Three verified gaps block full phase goal achievement:

1. **CR-01 (Critical):** Frontend enum/option values use `"SAÍDA"` (with accent) in `LancamentosClient.tsx` and `CategoriasClient.tsx`, but the backend repository queries filter on `l.tipo = 'SAIDA'` (without accent) and `LancamentoService.validarIntegridadeCategoriaTipo` does `categoria.getTipo().equals(tipoLancamento)`. Creating or editing lançamentos/categorias of type SAÍDA will fail. This breaks core functionality — users cannot create expenses (saídas) or expense categories through the web UI.

2. **CR-02 (Critical):** `CategoryBreakdown.tsx` guard `if (!totalSaidas)` receives `totalSaidas` as a string (BigDecimal serialized via `ToStringSerializer` to `"0.00"`). `!("0.00")` evaluates to `false`, so the zero-guard is bypassed. The expression `categoria.total / totalSaidas` computes `0 / 0 = NaN`, producing `"NaN%"` in the UI whenever categories exist but total saídas is zero (e.g., a month with categories registered but no expenses). This violates success criterion 3 (mês sem lançamentos mostra zeros sem erro) and D-10 (percentual = "—" quando total=0).

3. **WR-03 (Warning):** `getLancamentos` returns `total: lista.length` — the page size, not the real total count. The truncation warning `"Mostrando 50 de {N}"` will never trigger because `total` always equals the returned list length (capped at 50 by the API). This violates D-22.

To accept these deviations, add overrides to VERIFICATION.md frontmatter. Otherwise, fix the issues and re-run `/gsd-plan-phase 3 --gaps` or re-verify.

---
_Verified: 2026-11-09T15:30:00Z_
_Verifier: the agent (gsd-verifier)_