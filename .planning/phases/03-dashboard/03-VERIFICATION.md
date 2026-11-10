---
phase: 03-dashboard
verified: 2026-11-10T00:00:00Z
status: passed
score: 30/30 must-haves verified
overrides_applied: 0
re_verification: true
previous_status: gaps_found
previous_score: 23/26
gaps_closed:
  - "CR-01: Frontend enum/option values use 'SAÍDA' → changed to 'SAIDA' in LancamentosClient.tsx and CategoriasClient.tsx"
  - "CR-02: Division-by-zero guard broken (totalSaidas string) → now uses Number(totalSaidas) || 0 with if (!totalSaidasNum)"
  - "WR-03: getLancamentos returned total: lista.length → now reads total from response body (resJson.total)"
gaps_remaining: []
regressions: []
---

# Phase 3: Dashboard — Verification Report (Re-verification)

**Phase Goal:** Entradas, saídas, saldo por mês, gasto por categoria e comparação mensal na web

**Verified:** 2026-11-10T00:00:00Z

**Status:** passed

**Score:** 30/30 must-haves verified

**Re-verification:** Yes — after gap closure fixes. Previous status was `gaps_found` with 3 gaps (CR-01, CR-02, WR-03). All three gaps are now closed and verified against the actual codebase.

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
| 7 | application.yml configuração Jackson para BigDecimal preservar precisão | ✓ VERIFIED | application.yml: spring.jackson.serialization.write-bigdecimal-as-plain: true. DashboardResponse campos BigDecimal com @JsonSerialize(ToStringSerializer.class). |
| 8 | Usuário vê seletor de mês com input type=month, padrão = mês corrente (D-13, DSH-04) | ✓ VERIFIED | MonthSelector.tsx:41-44: <Input type="month" value={urlMes}>, sem min/max. page.tsx:33-35: default = new Date().toISOString().slice(0,7). |
| 9 | URL atualizada com ?mes=YYYY-MM ao selecionar mês ou navegar com setas ‹ › (D-16, D-17) | ✓ VERIFIED | MonthSelector.tsx:30 (onChange) e linhas 46-51 (setas ‹ ›) usam router.push(`/?mes=${proximo}`). key={urlMes} no container para re-mount. |
| 10 | KPIs mostram entradas, saídas e saldo do mês selecionado (DSH-01) | ✓ VERIFIED | DashboardKPIs.tsx renderiza 3 cards (Entradas/verde, Saídas/vermelho, Saldo/cor por sinal). page.tsx passa dados.totalEntradas, totalSaidas, saldo como props (parseados com Number() do string do backend). |
| 11 | Variação vs mês anterior em R$ absoluto e percentual para entradas, saídas e saldo (D-05, DSH-03) | ✓ VERIFIED | DashboardKPIs.tsx formatarVariacao exibe "+R$ X,00 (Y,YY%)" quando percentual disponível, "R$ X,00" quando "—". page.tsx passa todos os campos de variacao e percentual. |
| 12 | Quando mês anterior é zero, percentual = "—", apenas variação absoluta (D-06) | ✓ VERIFIED | Backend calcularPercentual retorna "—" quando anterior <= 0. DashboardKPIs.tsx linha 24: if (percentual === "—") retorna apenas absoluto. |
| 13 | Todos os dados vêm de GET /api/dashboard (não da listagem truncada de lançamentos) (D-01) | ✓ VERIFIED | page.tsx chama getDashboard(ano, mes). DashboardData type em api.ts espelha o DTO do backend. |
| 14 | Sessão validada via getMe() cookie auth; sem sessão → redirect /login (AUTH-03) | ✓ VERIFIED | page.tsx:24-27: getMe() com redirect /login?expirada=1. getDashboard retorna null sem token. |
| 15 | Tabela lista todas as categorias de SAÍDA do mês, inclusive zeradas, ordenada por valor decrescente (D-08, D-09) | ✓ VERIFIED | CategoryBreakdown.tsx:47 ordena por total decrescente (Number(b.total) - Number(a.total)). Backend LEFT JOIN traz categorias zeradas. Empty state quando categorias vazio. |
| 16 | Categorias de ENTRADA não aparecem no gasto por categoria | ✓ VERIFIED | DashboardRepository.breakdownSaida filtra c.tipo='SAIDA' e l.tipo='SAIDA'. |
| 17 | Não agrupa categorias em "Outros" (D-09) | ✓ VERIFIED | CategoryBreakdown renderiza cada categoria individualmente, sem agrupamento. |
| 18 | Gráfico de barras horizontais via shadcn/ui Charts (Recharts) com wrapper chart.tsx (D-11, D-12) | ✓ VERIFIED | web/components/ui/chart.tsx existe (wrapper <div data-slot="chart">). CategoryBreakdown.tsx usa BarChart layout="horizontal" com Chart wrapper, fill="var(--chart-1)". recharts ^2.15.4 instalado em package.json. |
| 19 | Percentual sobre total de saídas; se total=0, percentual='—' sem NaN% (CR-02 — FIXED) | ✓ VERIFIED | CategoryBreakdown.tsx:35 `total: string;`; linha 40 `totalSaidas: string;`; linha 49 `const totalSaidasNum = Number(totalSaidas) || 0;`; linha 52 `if (!totalSaidasNum)` (não `if (!totalSaidas)`); linha 55 `PERCENTUAL.format(Number(categoria.total) / totalSaidasNum)`. totalSaidas="0.00" → Number()→0 → guard dispara → "—". Sem NaN%. |
| 20 | Valor formatado com Intl.NumberFormat('pt-BR', BRL) (D-03) | ✓ VERIFIED | CategoryBreakdown.tsx usa MONETARY = Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'}). DashboardKPIs.tsx mesma formatação. |
| 21 | Dados vêm de GET /api/dashboard (não da listagem truncada) (D-01) | ✓ VERIFIED | page.tsx passa dados.categorias e dados.totalSaidas do getDashboard para CategoryBreakdown. |
| 22 | Rota /lancamentos: listar (filtrado por mês), criar, editar, excluir lançamentos (D-18, LAN-01..07) (CR-01 — FIXED) | ✓ VERIFIED | LancamentosClient.tsx:18 `type TipoLancamento = "ENTRADA" | "SAIDA";` (sem acento). Linha 456 `<option value="SAIDA">Saída</option>`. Backend filtra l.tipo='SAIDA'. Validação categoria.getTipo().equals(tipoLancamento) agora passa. |
| 23 | Rota /categorias: listar, criar, editar, desativar (soft delete) categorias (D-18, D-19, CAT-01..06) (CR-01 — FIXED) | ✓ VERIFIED | CategoriasClient.tsx:77 `setTipo(c.tipo as "ENTRADA" | "SAIDA");` (sem acento). Linha 407 `<option value="SAIDA">Saída</option>`. Soft delete (ativo=false) implementado. |
| 24 | TopNav com navegação entre /, /lancamentos, /categorias + LogoutButton (D-21) | ✓ VERIFIED | TopNav.tsx implementado com next/link, 3 rotas, LogoutButton, item ativo #0F766E. Integrado nas 3 páginas. |
| 25 | DeleteConfirmationDialog para excluir lançamento e desativar categoria, sem window.confirm (D-24) | ✓ VERIFIED | DeleteConfirmationDialog.tsx usa @radix-ui/react-dialog, sem window.confirm. Props isOpen, onConfirm, onClose, action, target. Usado em LancamentosClient e CategoriasClient. |
| 26 | Escrita de categorias ADMIN-only na UI; USER só lê (D-23) | ✓ VERIFIED | CategoriasClient.tsx:56 podeEscrever = userRole === 'ADMIN'. Botões Editar/Desativar condicionados a podeEscrever. Backend SecurityConfig restringe /api/categorias/** a ROLE_ADMIN. |
| 27 | Listagem de lançamentos filtra pelo mês selecionado da URL (?mes=YYYY-MM) (D-22) | ✓ VERIFIED | lancamentos/page.tsx:37-39 parseia ano/mes do searchParams, chama getLancamentos(ano, mes). |
| 28 | Aviso de truncamento "Mostrando 50 de {N}" quando houver mais de 50 lançamentos (D-22) (WR-03 — FIXED) | ✓ VERIFIED | web/lib/api.ts:127-131: getLancamentos faz `await res.json()` e lê `resJson.total` (não `lista.length`). Backend LancamentoService.java:34 record LancamentoPage(List<LancamentoResponse> lancamentos, long total), linha 173 `return new LancamentoPage(lista, page.getTotalElements());`. Controller linha 69 retorna ResponseEntity<LancamentoPage>. truncado = total > 50 agora dispara corretamente. |
| 29 | Validação monetária UI espelha API: valor > 0, máximo 2 casas decimais (LAN-04, LAN-05) | ✓ VERIFIED | LancamentosClient.tsx:111-129 validarForm: VALOR_RE = /^-?\d+(\.\d{1,2})?$/, v <= 0 rejeitado. |
| 30 | Lint e build do web passam (npx eslint, next build) | ✓ VERIFIED | 03-04-SUMMARY.md relata npx eslint . (0 erros), npx next build, npx tsc --noEmit passando. Commits c2d7e09, 62b7d3f existem. Gap closure summaries também confirmam tsc/eslint/next build passando após as correções. |

**Score:** 30/30 truths verified

---

### Deferred Items

None — all gaps were real, actionable issues in the current phase, and all have been closed.

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
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` | LancamentoPage record, getTotalElements | ✓ VERIFIED | Linha 34 record, linha 173 `new LancamentoPage(lista, page.getTotalElements())` |
| `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` | ResponseEntity<LancamentoPage> | ✓ VERIFIED | Linha 69 retorna ResponseEntity<LancamentoPage> |
| `web/components/ui/chart.tsx` | Wrapper shadcn/ui Charts | ✓ VERIFIED | <div data-slot="chart" className="w-full h-[300px]"> |
| `web/components/MonthSelector.tsx` | input type=month + setas ‹ › | ✓ VERIFIED | Sem min/max, router.push ?mes=YYYY-MM |
| `web/components/DashboardKPIs.tsx` | 3 cards KPI com variação | ✓ VERIFIED | formatarVariacao, corSaldo, Intl.NumberFormat pt-BR, props tipados como number (parseados em page.tsx) |
| `web/components/CategoryBreakdown.tsx` | Tabela + gráfico barras horizontais | ✓ VERIFIED | totalSaidas: string, guard Number(totalSaidas)||0, if(!totalSaidasNum), Number(categoria.total) antes da divisão |
| `web/lib/api.ts` | getDashboard + CRUD functions | ✓ VERIFIED | getDashboard, getLancamentos (lê resJson.total), criarLancamento, editarLancamento, excluirLancamento, getCategorias, criarCategoria, editarCategoria, desativarCategoria. Campos monetários tipados como string. |
| `web/app/page.tsx` | Server Component dashboard | ✓ VERIFIED | instant=false, getMe(), getDashboard, MonthSelector, DashboardKPIs, CategoryBreakdown. Parsa string→Number com Number() antes de passar props. |
| `web/app/lancamentos/page.tsx` | CRUD lançamentos | ✓ VERIFIED | CRUD completo, filtro por mês da URL, DeleteConfirmationDialog |
| `web/app/categorias/page.tsx` | CRUD categorias | ✓ VERIFIED | CRUD completo, role ADMIN-only, soft delete, DeleteConfirmationDialog |
| `web/components/TopNav.tsx` | Navegação 3 rotas | ✓ VERIFIED | next/link, item ativo #0F766E |
| `web/components/DeleteConfirmationDialog.tsx` | Diálogo sem window.confirm | ✓ VERIFIED | @radix-ui/react-dialog |
| `web/components/LancamentosClient.tsx` | Cliente interativo lançamentos | ✓ VERIFIED | TipoLancamento = "ENTRADA" | "SAIDA", <option value="SAIDA">, validação monetária |
| `web/components/CategoriasClient.tsx` | Cliente interativo categorias | ✓ VERIFIED | cast "ENTRADA" | "SAIDA", <option value="SAIDA">, podeEscrever = ADMIN |
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
| page.tsx → DashboardKPIs | props totais/variacoes/percentuais | dados do getDashboard (parseados Number()) | ✓ WIRED | Todos os 9 campos passados como number |
| page.tsx → CategoryBreakdown | props categorias/totalSaidas | dados do getDashboard (strings do backend) | ✓ WIRED | Dados do backend chegam como props string, parseados no componente |
| CategoryBreakdown → chart.tsx | `<Chart>` wrapper | Recharts BarChart dentro de Chart | ✓ WIRED | fill="var(--chart-1)", layout="horizontal" |
| api.ts → LancamentoController | `/api/lancamentos` | GET/POST/PUT/DELETE + Bearer | ✓ WIRED | Cookie sess + Bearer token |
| api.ts → CategoriaController | `/api/categorias` | GET/POST/PUT/DELETE + Bearer | ✓ WIRED | Cookie sess + Bearer token |
| lancamentos/page → DeleteConfirmationDialog | `onConfirm` → `onExcluir` | DELETE físico de lançamento | ✓ WIRED | Sem window.confirm |
| categorias/page → DeleteConfirmationDialog | `onConfirm` → `onDesativar` | soft delete (ativo=false) | ✓ WIRED | Sem window.confirm |
| web/lib/api.ts → backend /api/lancamentos | `resJson.total` | total real via page.getTotalElements() | ✓ WIRED | getLancamentos lê total do corpo da resposta, não de lista.length |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|-------------------|--------|
| DashboardController | DashboardResponse | DashboardRepository queries | ✓ Real (SUM/GROUP BY SQL) | ✓ FLOWING |
| DashboardKPIs | totalEntradas/totalSaidas/saldo | getDashboard → API → BigDecimal strings → Number() parse em page.tsx | ✓ Real (strings parseados para number) | ✓ FLOWING |
| CategoryBreakdown | categorias[].total / totalSaidas | getDashboard → API → BigDecimal strings → Number() parse no componente | ✓ Real (strings parseados, guard zero funcional) | ✓ FLOWING |
| LancamentosClient | lancamentos[] | getLancamentos → API | ✓ Real (lista do banco) | ✓ FLOWING |
| CategoriasClient | categorias[] | getCategorias → API | ✓ Real (lista do banco) | ✓ FLOWING |

---

### Behavioral Spot-Checks

Step 7b: SKIPPED — no running server available to execute HTTP calls against. All verification performed via static code analysis. The DashboardIntegrationTest (Testcontainers + PostgreSQL real) provides the strongest behavioral evidence for the backend, and all 4 tests are GREEN.

---

### Probe Execution

Step 7c: SKIPPED — no `scripts/*/tests/probe-*.sh` files found in the project, and no probe declarations in PLAN/SUMMARY files.

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| DSH-01 | 03-01, 03-02 | Dashboard mostra total de entradas, saídas e saldo do mês | ✓ SATISFIED | DashboardController + DashboardKPIs implementados e testados |
| DSH-02 | 03-01, 03-03 | Dashboard mostra gasto por categoria do mês | ✓ SATISFIED | Breakdown implementado, CR-02 corrigido (percentual "—" quando total=0) |
| DSH-03 | 03-01, 03-02 | Dashboard compara mês com mês anterior (variação) | ✓ SATISFIED | Variação absoluta + percentual, base zero = "—" no backend |
| DSH-04 | 03-02 | Seletor de mês atualiza todos os números | ✓ SATISFIED | MonthSelector + ?mes=YYYY-MM + getDashboard refetch |
| DSH-05 | 03-01, 03-02, 03-03 | Números idênticos a SQL de conferência | ✓ SATISFIED | DashboardIntegrationTest valida contra seed conhecida (Testcontainers) |
| CAT-01..06 | 03-04 | CRUD categorias | ✓ SATISFIED | CRUD implementado, CR-01 corrigido (categorias tipo SAÍDA funcionam) |
| LAN-01..07 | 03-04 | CRUD lançamentos | ✓ SATISFIED | CRUD implementado, CR-01 corrigido (lançamentos tipo SAÍDA funcionam) |
| LOT-01..02 | 03-04 | Lote tudo-ou-nada | ✓ SATISFIED | Herdado da Fase 2 (endpoints existentes) |
| D-22 | 03-04, 03-gap-03 | Aviso de truncamento "Mostrando 50 de {N}" | ✓ SATISFIED | WR-03 corrigido, total lido do corpo da resposta via page.getTotalElements() |

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `web/components/LancamentosClient.tsx` | 120, 142 | `parseFloat` para valor monetário (WR-07) | ⚠️ Warning | Representação de ponto flutuante binário para dinheiro — valor enviado como string para o backend, baixo risco |
| `web/components/MonthSelector.tsx` | 23-25 | `urlMes.split("-").map(Number)` sem validação (WR-04) | ⚠️ Warning | URL malformada pode causar RangeError — não foi corrigido nesta fase |
| `web/app/page.tsx` | 33-35 | `parseInt(anoStr, 10)` sem validação de range (WR-05) | ⚠️ Warning | mes=13 pode causar DateTimeException 500 — não foi corrigido nesta fase |
| `web/components/LancamentosClient.tsx` | 277 | `categoriaMap.get(l.id)` lookup incorreto (WR-06) | ⚠️ Warning | Código morto/incorreto, categoriaNome já vem da API |
| `web/components/TopNav.tsx` | 20, 34 | `userRole` declarado mas não usado (IN-01) | ℹ️ Info | Campo morto na interface |
| `web/app/lancamentos/page.tsx` | 91-113 | `fetchCategorias` duplica padrão de getCategorias (IN-02) | ℹ️ Info | Inconsistente com api.ts wrapper |
| `web/components/TopNav.tsx` | 5 | `import { cn } from "cn"` (IN-03) | ℹ️ Info | Pacote npm diferente do utilitário local shadcn |
| `api/src/test/java/com/controlefinanceiro/dashboard/DashboardIntegrationTest.java` | 42 | Senha de teste hardcoded (IN-04) | ℹ️ Info | Apenas testes, não vazia para produção |

**Note:** WR-04, WR-05, WR-06, WR-07 and Info items are pre-existing non-blocking warnings from the code review. None of them block the phase goal. CR-01, CR-02, WR-03 (the 3 blockers) are all resolved.

No stub patterns (`return null`, `return {}`, `return []`, `=> {}`, `console.log` only, `TODO`/`FIXME`/`XXX`/`HACK`) found in any component file. The only `placeholder` matches are legitimate HTML input attributes.

---

### Human Verification Required

No human verification items identified. All gaps were code-level issues detectable through static analysis, and all have been fixed and verified. Visual appearance (colors, layout, chart rendering) would benefit from human UAT but is not a blocker.

---

### Code Review Findings Resolution

| Review Finding | Severity | Status | Resolution |
|---------------|----------|--------|------------|
| CR-01: Tipo enum mismatch — "SAÍDA" vs "SAIDA" | Critical | ✓ RESOLVIDO | LancamentosClient.tsx:18 e CategoriasClient.tsx:77/407 usam "SAIDA" sem acento |
| CR-02: Division-by-zero guard broken (string totalSaidas) | Critical | ✓ RESOLVIDO | CategoryBreakdown.tsx:49 `Number(totalSaidas) || 0`, linha 52 `if (!totalSaidasNum)` |
| WR-03: getLancamentos total = lista.length | Warning | ✓ RESOLVIDO | web/lib/api.ts:127-131 lê `resJson.total`; backend LancamentoService.java:173 `page.getTotalElements()` |

---

### Locked Decisions Compliance (D-01..D-24)

All locked decisions verified as honored:
- **D-01:** Agregação no backend com SUM/GROUP BY — ✓ (DashboardRepository)
- **D-02:** Endpoint devolve totais, comparação, breakdown em uma chamada — ✓ (DashboardResponse)
- **D-03:** Nenhum double/float, BigDecimal scale=2 HALF_EVEN — ✓
- **D-04:** empresa_id exclusivamente do token JWT — ✓ (TenantContext.getEmpresaId())
- **D-05:** Variação absoluta + percentual para entradas/saídas/saldo — ✓
- **D-06:** Percentual "—" quando base zero — ✓
- **D-07:** Comparação mês fechado vs mês fechado — ✓
- **D-08:** Tabela + gráfico barras horizontais, valor e percentual — ✓
- **D-09:** Todas categorias SAÍDA inclusive zeradas, sem "Outros" — ✓
- **D-10:** Percentual "—" quando totalSaidas=0, sem divisão por zero — ✓ (CR-02 fixado)
- **D-11:** shadcn/ui Charts + Recharts — ✓
- **D-12:** Barras horizontais — ✓
- **D-13:** input type=month, padrão mês corrente — ✓
- **D-14:** Meses futuros permitidos — ✓
- **D-15:** Sem limite inferior, mês vazio mostra zeros — ✓
- **D-16:** Mês na URL (?mes=YYYY-MM) — ✓
- **D-17:** Setas ‹ › navegam mês anterior/próximo — ✓
- **D-18:** CRUD completo categorias e lançamentos — ✓
- **D-19:** Excluir categoria = soft delete (ativo=false) — ✓
- **D-20:** Sem UI de lote — ✓
- **D-21:** Rotas /, /lancamentos, /categorias + navegação top — ✓
- **D-22:** Filtro por mês + aviso truncamento — ✓ (WR-03 fixado)
- **D-23:** Escrita categorias ADMIN-only — ✓
- **D-24:** Confirmação antes de excluir/desativar, sem window.confirm — ✓

---

### Gaps Summary

All three previously-identified gaps are now closed:

1. **CR-01 (Critical) — RESOLVED:** Frontend enum/option values changed from `"SAÍDA"` (with accent) to `"SAIDA"` (without accent) in both `LancamentosClient.tsx` (line 18 enum, line 456 option) and `CategoriasClient.tsx` (line 77 cast, line 407 option). No remaining `"SAÍDA"` enum values in any code file. Creating/editing lançamentos and categorias of type SAÍDA now works.

2. **CR-02 (Critical) — RESOLVED:** `CategoryBreakdown.tsx` now types `totalSaidas` as `string`, parses with `Number(totalSaidas) || 0` (line 49), and guards with `if (!totalSaidasNum)` (line 52, not `if (!totalSaidas)`). `categoria.total` is parsed with `Number()` before division (line 55). `totalSaidas="0.00"` → `0` → guard triggers → percentual `"—"`. No more `"NaN%"`.

3. **WR-03 (Warning) — RESOLVED:** `getLancamentos` in `web/lib/api.ts` (lines 127-131) now reads `total` from `resJson.total` (backend response body), not from `lista.length`. Backend `LancamentoService.java` (line 34 record `LancamentoPage`, line 173 `page.getTotalElements()`) and `LancamentoController.java` (line 69 `ResponseEntity<LancamentoPage>`) return the real total count. Truncation warning `"Mostrando 50 de {N}"` now triggers when `total > 50`.

Phase goal achieved. All 30 must-haves verified. Ready to proceed to Phase 4.

---

_Verified: 2026-11-10T00:00:00Z_
_Verifier: the agent (gsd-verifier)_
