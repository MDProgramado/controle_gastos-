---
phase: 03-dashboard
reviewed: 2026-11-09T00:00:00Z
depth: standard
files_reviewed: 21
files_reviewed_list:
  - api/src/main/java/com/controlefinanceiro/dashboard/DashboardController.java
  - api/src/main/java/com/controlefinanceiro/dashboard/DashboardService.java
  - api/src/main/java/com/controlefinanceiro/dashboard/DashboardRepository.java
  - api/src/main/java/com/controlefinanceiro/dashboard/dto/DashboardResponse.java
  - api/src/main/java/com/controlefinanceiro/dashboard/dto/CategoryExpense.java
  - api/src/test/java/com/controlefinanceiro/dashboard/DashboardIntegrationTest.java
  - api/src/main/resources/application.yml
  - web/app/page.tsx
  - web/app/lancamentos/page.tsx
  - web/app/categorias/page.tsx
  - web/components/MonthSelector.tsx
  - web/components/DashboardKPIs.tsx
  - web/components/CategoryBreakdown.tsx
  - web/components/TopNav.tsx
  - web/components/DeleteConfirmationDialog.tsx
  - web/components/LancamentosClient.tsx
  - web/components/CategoriasClient.tsx
  - web/components/ui/chart.tsx
  - web/lib/api.ts
  - web/package.json
findings:
  critical: 2
  warning: 7
  info: 4
  total: 13
status: needs_fixes
---

# Phase 3: Code Review Report

**Reviewed:** 2026-11-09T00:00:00Z
**Depth:** standard
**Files Reviewed:** 21
**Status:** needs_fixes

## Summary

Phase 3 implements the dashboard (GET /api/dashboard) plus lançamentos/categorias CRUD pages. The backend is solid: BigDecimal consistently, COALESCE for empty months, division-by-zero guard, empresa_id exclusively from TenantContext, no empresa_id in response DTOs. The frontend has several real bugs, most stemming from a **type mismatch between backend (BigDecimal serialized as JSON string via ToStringSerializer) and frontend (monetary fields typed as `number`)**. This causes string concatenation instead of addition, broken division-by-zero guards, and potential NaN/Infinity display. A second category of bugs is the **"SAÍDA" (with accent) vs "SAIDA" (without accent) enum mismatch** between frontend and backend, which will break creation/editing of saídas.

---

## Critical Issues

### CR-01: Tipo enum mismatch — frontend sends "SAÍDA", backend expects "SAIDA"

**File:** `web/components/LancamentosClient.tsx:18`, `web/components/CategoriasClient.tsx:47-58`

**Issue:** The frontend defines `TipoLancamento = "ENTRADA" | "SAÍDA"` (with accent on SAÍDA) and the category/select option uses `<option value="SAÍDA">Saída</option>`. The backend repository queries filter on `l.tipo = 'SAIDA'` (without accent, confirmed by `DashboardIntegrationTest` using `"SAIDA"`). When a user creates or edits a lançamento/categoria with tipo "SAÍDA", the backend receives a value that does not match the stored "SAIDA" enum, causing validation/storage failures. Core functionality (creating saídas/despesas) is broken.

**Fix:** Change the frontend enum and option values to `"SAIDA"` (without accent) to match the backend:

```typescript
// LancamentosClient.tsx line 18
type TipoLancamento = "ENTRADA" | "SAIDA";

// CategoriasClient.tsx select option (line 407)
<option value="SAIDA">Saída</option>
```

### CR-02: Division-by-zero guard broken because totalSaidas is a string

**File:** `web/components/CategoryBreakdown.tsx:53`

**Issue:** `totalSaidas` prop is typed as `number` but arrives from the backend as a JSON string (BigDecimal serialized via `ToStringSerializer`). The guard `if (!totalSaidas)` evaluates `!("0.00")` → `false` (non-empty string is truthy), so zero totals bypass the guard. The subsequent `categoria.total / totalSaidas` computes `0 / 0 = NaN`, producing "NaN%" in the UI instead of "—". This occurs whenever categories exist but total saídas is zero (e.g., a month with categories but no expenses).

**Fix:** Parse `totalSaidas` to a number before the guard, and guard against NaN:

```typescript
const totalSaidasNum = Number(totalSaidas) || 0;

const percentual = (categoria: CategoriaGasto): string => {
  if (!totalSaidasNum) {
    return "—";
  }
  return PERCENTUAL.format(categoria.total / totalSaidasNum);
};
```

---

## Warnings

### WR-01: String concatenation instead of numeric sum in CategoryBreakdown reduce

**File:** `web/components/CategoryBreakdown.tsx:48`

**Issue:** `categorias.reduce((sum, c) => sum + c.total, 0)` — `c.total` is a string (BigDecimal serialized as string). `0 + "100.00"` produces `"100.00"`, then `"100.00" + "50.00"` produces `"100.0050.00"`. The result is currently unused (dead code), but the logic is incorrect and would produce garbage if wired up.

**Fix:** Parse to number before reducing, or remove the dead code:

```typescript
const total = categorias.reduce((sum, c) => sum + Number(c.total), 0);
```

### WR-02: Monetary fields typed as `number` but backend sends BigDecimal strings

**File:** `web/lib/api.ts:41-52`, `web/components/DashboardKPIs.tsx:47-56`, `web/components/CategoryBreakdown.tsx:32-36`, `web/components/LancamentosClient.tsx:20-28`

**Issue:** Backend serializes BigDecimal as JSON strings via `@JsonSerialize(using = ToStringSerializer.class)` and `write-bigdecimal-as-plain: true`. Frontend TypeScript types declare `totalEntradas: number`, `total: number`, `valor: number`, etc. At runtime these are strings. JS coercion (`>=`, `-`, `/`, `Intl.NumberFormat.format`) makes most operations "work" by accident, but the types are lying and the code is fragile — any non-numeric string would break silently.

**Fix:** Change TypeScript types to `string` for monetary values received from the API, and parse to number only at display time:

```typescript
// web/lib/api.ts
export type DashboardData = {
  totalEntradas: string;
  totalSaidas: string;
  saldo: string;
  variacaoEntradas: string;
  variacaoSaidas: string;
  variacaoSaldo: string;
  percentualEntradas: string;
  percentualSaidas: string;
  percentualSaldo: string;
  categorias: Array<{ id: number; nome: string; total: string }>;
};
```

Then parse in components: `MONETARY.format(Number(dados.totalEntradas))`.

### WR-03: Lancamentos total is page length, not actual total count

**File:** `web/app/lancamentos/page.tsx:48`, `web/lib/api.ts:128`

**Issue:** `getLancamentos` returns `{ lancamentos: lista, total: lista.length }` — `total` is the number of items on the current page, not the actual total count from the API. The frontend uses this for the truncation warning (`truncado = total > 50`), which will never trigger because `total` always equals the returned list length (capped by the API page size).

**Fix:** Have the backend return the actual total count (e.g., a paginated response with `total` separate from `lancamentos`), and update `getLancamentos` to read it from the response body instead of computing `lista.length`.

### WR-04: Malformed `mes` URL param crashes MonthSelector

**File:** `web/components/MonthSelector.tsx:23-25`

**Issue:** `urlMes.split("-").map(Number)` produces `NaN` if the URL `mes` param is malformed (e.g., "abc", "2026"). `new Date(NaN, NaN - 1 + delta, 1)` creates an Invalid Date, and `data.toISOString()` throws a `RangeError`, crashing the client component.

**Fix:** Validate the parsed month/year before constructing the date:

```typescript
function mudarMes(delta: number) {
  const [ano, mesAtual] = urlMes.split("-").map(Number);
  if (Number.isNaN(ano) || Number.isNaN(mesAtual)) return;
  const data = new Date(ano, mesAtual - 1 + delta, 1);
  if (Number.isNaN(data.getTime())) return;
  const proximo = data.toISOString().slice(0, 7);
  router.push(`/?mes=${proximo}`, { scroll: false });
}
```

### WR-05: No validation of `ano`/`mes` query params in dashboard page

**File:** `web/app/page.tsx:33-35`

**Issue:** `parseInt(anoStr, 10)` and `parseInt(mesStr, 10)` can return `NaN` if the URL param is malformed or missing parts. `getDashboard(NaN, NaN)` is called, and the backend `LocalDate.of(ano, mes, 1)` will throw `DateTimeException` (e.g., month 13) or fail on null, producing a 500 error instead of a 400.

**Fix:** Validate `ano` and `mes` are finite integers in range before calling `getDashboard`:

```typescript
if (Number.isNaN(ano) || Number.isNaN(mes) || mes < 1 || mes > 12) {
  redirect("/login?expirada=1"); // or return a 400 / error UI
}
```

### WR-06: categoriaMap lookup uses lancamento ID instead of category ID

**File:** `web/components/LancamentosClient.tsx:276-277`

**Issue:** `categoriaMap.get(l.id)` looks up the category name using the **lançamento ID** as the key, but `categoriaMap` is built from category IDs (`new Map(categorias.map((c) => [c.id, c.nome]))`). A lançamento ID will almost never match a category ID, so the fallback always returns `undefined` → `"—"`. Since `l.categoriaNome` is already populated from the API, this is dead/incorrect code.

**Fix:** Remove the incorrect fallback (categoriaNome is already provided):

```typescript
const categoriaNome = l.categoriaNome || "—";
```

### WR-07: parseFloat used for monetary value in LancamentosClient

**File:** `web/components/LancamentosClient.tsx:120, 142`

**Issue:** `parseFloat(valor)` is used for money input. While the regex `VALOR_RE` limits to 2 decimal places, `parseFloat` can introduce binary floating-point representation issues (e.g., `parseFloat("0.1") + parseFloat("0.2")` → `0.30000000000000004`). The value is then serialized to JSON and sent to the backend, which re-parses to BigDecimal. Not a data-loss risk for 2-decimal inputs, but inappropriate for money handling.

**Fix:** Send the value as a string to the backend and let the backend parse to BigDecimal with scale=2:

```typescript
// In onSubmit, send valor as string:
const request = {
  categoriaId: categoriaId!,
  tipo,
  valor, // string, backend parses to BigDecimal
  data,
  descricao: descricao || undefined,
};
```

(Note: this requires the backend `@RequestParam`/DTO to accept a string for valor and parse it. If backend expects a number, keep parseFloat but document the trade-off.)

---

## Info

### IN-01: Unused `userRole` parameter in TopNav

**File:** `web/components/TopNav.tsx:20, 34`

**Issue:** `userRole` is declared in `TopNavProps` but `void userRole` marks it as intentionally unused. The comment says it's kept for contract; write restrictions are in page actions. Not a bug, but dead interface field.

**Fix:** Remove `userRole` from `TopNavProps` and the call sites, or keep it with a clear TODO if role-based UI gating is planned.

### IN-02: Duplicate auth/cookie pattern in fetchCategorias

**File:** `web/app/lancamentos/page.tsx:91-113`

**Issue:** `fetchCategorias` manually reads the `sess` cookie and calls `fetch` with Bearer auth, duplicating the pattern already encapsulated in `getCategorias` from `@/lib/api`. Inconsistent with the rest of the codebase which uses the api.ts wrapper.

**Fix:** Replace `fetchCategorias` with a call to `getCategorias()` from `@/lib/api`.

### IN-03: `cn` imported from npm package instead of local utility

**File:** `web/components/TopNav.tsx:5`, `web/components/DeleteConfirmationDialog.tsx:4`, `web/components/CategoriasClient.tsx:8`, `web/package.json:14`

**Issue:** `import { cn } from "cn"` imports from the `cn` npm package (`^0.5.2` in package.json) rather than a local `cn` utility (typical shadcn pattern: `@/lib/utils`). The `cn` package on npm is a different utility than the usual `clsx`+`tailwind-merge` helper. Works, but unconventional and may confuse developers.

**Fix:** Confirm the `cn` package is the intended utility, or replace with a local `@/lib/utils` `cn` helper following shadcn/ui conventions.

### IN-04: Hardcoded test fixture password

**File:** `api/src/test/java/com/controlefinanceiro/dashboard/DashboardIntegrationTest.java:42`

**Issue:** `SENHA_FIXTURE = "fixture-somente-testes-it"` is hardcoded. Comment says the real password lives only in env var (D-04). Acceptable for tests but worth noting.

**Fix:** No change required for tests; ensure this never leaks into production config.

---

## Structural Findings (fallow)

No structural findings block was provided with this review.

---

_Reviewed: 2026-11-09T00:00:00Z_
_Reviewer: gsd-code-reviewer_
_Depth: standard_