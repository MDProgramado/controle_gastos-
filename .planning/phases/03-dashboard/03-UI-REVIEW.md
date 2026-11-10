# Phase 03 — UI Review

**Audited:** Tue Nov 10 2026
**Baseline:** UI-SPEC.md (`.planning/phases/03-dashboard/03-UI-SPEC.md`) — design contract
**Screenshots:** not captured (no dev server running on ports 3000/5173/8080 — code-only audit)

---

## Pillar Scores

| Pillar | Score | Key Finding |
|--------|-------|-------------|
| 1. Copywriting | 2/4 | Multiple §7 copywriting contract deviations (CTA labels, validation messages, empty-state/error text) |
| 2. Visuals | 2/4 | Icon-only buttons lack aria-labels; h1 uses text-[20px] instead of spec'd text-[28px] |
| 3. Color | 2/4 | Hardcoded hex colors bypass design tokens; accent used on brand text contrary to §1.3 reservation |
| 4. Typography | 2/4 | h1 uses lg (20px) instead of xl (28px); font-medium/font-bold used though spec allows only 400/600 |
| 5. Spacing | 2/4 | Off-scale values p-3/px-2.5/py-0.5; MonthSelector input misses 44px touch target |
| 6. Experience Design | 3/4 | Strong state coverage (loading/error/empty/disabled/confirmation/role-based); gaps in a11y aria-labels and dashboard loading UI |

**Overall: 13/24**

---

## Top 3 Priority Fixes

1. **Replace hardcoded hex colors with semantic token classes** — `text-[#16A34A]`, `text-[#DC2626]`, `bg-[#16A34A]/10`, `border-[#16A34A]`, and `text-[#0F766E]` appear in `LancamentosClient.tsx`, `CategoriasClient.tsx`, and `TopNav.tsx`, bypassing the token system and misusing the reserved accent color on brand text — **fix:** use `text-green-600`/`text-red-600`/`text-primary` and their `bg-`/`border-` equivalents per §1.3 — affects Pillars 2 and 3.

2. **Align copywriting to §7 contract** — submit buttons say `"Criar"`/`"Atualizar"` instead of `"Salvar"`/`"Salvar alterações"`; validation messages diverge (`"Informe a categoria."` vs `"Selecione uma categoria."`, `"O valor deve ter no máximo 2 casas decimais."` vs `"O valor aceita no máximo 2 casas decimais."`); empty-state text `"Nenhuma categoria de saída neste mês"` vs spec `"Nenhum lançamento neste mês"` — **fix:** update strings in `LancamentosClient.tsx`, `CategoriasClient.tsx`, `CategoryBreakdown.tsx` to match §7.2/§7.3/§7.5 — affects Pillar 1.

3. **Fix typography scale to §1.2** — page `<h1>` elements use `text-[20px]` (lg) instead of `text-[28px]` (xl) as required for h1; `font-medium` (500) and `font-bold` (700) appear though spec allows only 400/600 — **fix:** change h1 to `text-[28px] font-semibold`, replace `font-medium`/`font-bold` with `font-semibold`/`font-normal` — affects Pillar 4.

---

## Detailed Findings

### Pillar 1: Copywriting (2/4)

The implemented copy largely follows pt-BR conventions but deviates from the §7 Copywriting Contract in several concrete places.

**CTA labels (§7.2) — deviations:**
- `LancamentosClient.tsx:525` and `CategoriasClient.tsx:439`: form submit button renders `"Criar"` / `"Atualizar"` depending on mode. Spec §7.2 requires `"Salvar"` (create) and `"Salvar alterações"` (edit). The dashboard CTA `"Adicionar lançamento"` is correct (`LancamentosClient.tsx:224`), and `"Sair"`/`"Cancelar"`/`"Excluir"`/`"Desativar"` are correct.

**Validation messages (§7.5) — deviations:**
- `LancamentosClient.tsx:122`: `"O valor deve ter no máximo 2 casas decimais."` — spec requires `"O valor aceita no máximo 2 casas decimais."`
- `LancamentosClient.tsx:113`: `"Informe a categoria."` — spec requires `"Selecione uma categoria."`
- `LancamentosClient.tsx:114`: `"Informe o tipo."` — spec requires `"Selecione o tipo (entrada ou saída)."`
- `LancamentosClient.tsx:118` `"Informe o valor."` ✓ matches spec; `:124` `"O valor deve ser maior que zero."` ✓ matches spec; `CategoriasClient.tsx:86` `"Informe o nome da categoria."` ✓ matches spec.

**Empty/error states (§7.3) — deviations:**
- `CategoryBreakdown.tsx:65`: empty-state text `"Nenhuma categoria de saída neste mês"` — spec §7.3 requires `"Nenhum lançamento neste mês"` for empty dashboard/lista estados.
- `web/app/page.tsx` (dashboard): no empty-state helper text rendered at all when the month is empty; relies solely on KPIs showing `R$ 0,00` and CategoryBreakdown's own empty card. Spec §4.4 suggests optional auxiliary text `"Nenhum lançamento neste mês"`.
- `LancamentosClient.tsx:170` and `CategoriasClient.tsx:130`: network error `"Não foi possível conectar. Tente novamente em instantes."` — spec §7.3 generic API error requires `"Não foi possível carregar os dados. Tente novamente em instantes."`

**Destructive confirmations (§7.4) — compliant:**
- `DeleteConfirmationDialog.tsx:40`: dynamic template `Tem certeza que deseja ${action} este ${target}?` produces `"Tem certeza que deseja excluir este lançamento?"` and `"Tem certeza que deseja desativar esta categoria?"` — matches §7.4 exactly. Buttons `"Excluir"`/`"Desativar"` / `"Cancelar"` ✓. No `window.confirm` ✓ (D-24).

**Labels (§7.1) — minor deviation:**
- `CategoriasClient.tsx:423`: checkbox label `"Categoria pessoal"` — spec §7.1 label for the Pessoal field is just `"Pessoal"`.

**Evidence summary:** 6+ specific string deviations across `LancamentosClient.tsx`, `CategoriasClient.tsx`, `CategoryBreakdown.tsx`, `web/app/page.tsx`. Score 2/4 — contract substantially met but multiple copy items diverge from §7.

---

### Pillar 2: Visuals (2/4)

**Visual hierarchy — mostly compliant:**
- Each page has a clear `<h1>` focal point (`Dashboard`/`Lançamentos`/`Categorias`) with a muted subtitle below it (`web/app/page.tsx:53`, `LancamentosClient.tsx:208`, `CategoriasClient.tsx:171`).
- KPI cards use colored left borders (`border-l-green-600`/`border-l-red-600`) and colored values for visual differentiation (`DashboardKPIs.tsx:70,82,94`).

**Icon-only buttons without accessible labels — deviation:**
- `MonthSelector.tsx:46-50`: the `‹`/`›` navigation buttons use `variant="outline" size="icon"` with only the arrow character as content. No `aria-label`, no `aria-labelledby`, and no tooltip. Icon-only buttons must be paired with aria-labels or tooltips per WCAG AA and the Visuals audit method.

**h1 sizing — deviation:**
- Page `<h1>` elements use `text-[20px]` (`web/app/page.tsx:53`, `LancamentosClient.tsx:208`, `CategoriasClient.tsx:171`) instead of the spec'd `text-[28px]` (xl scale) for h1. This weakens the h1's visual prominence as the page focal point.

**Brand color on non-accent element — deviation:**
- `TopNav.tsx:50`: brand wordmark `"Controle"` uses hardcoded `text-[#0F766E]`. §1.3 reserves accent (#0F766E) for CTA primary, focus ring, chart bar, primary action icons, and active nav links — not brand wordmarks.

Score 2/4 — hierarchy exists but icon buttons lack accessibility and h1 sizing deviates from contract.

---

### Pillar 3: Color (2/4)

**Hardcoded hex colors bypassing the design token system — deviation (BLOCKER for color consistency):**
Multiple hardcoded hex values appear in component code instead of semantic Tailwind token classes:

| File:line | Hardcoded value | Intended semantic | Spec token |
|-----------|----------------|-------------------|------------|
| `LancamentosClient.tsx:275` | `text-[#16A34A]` | Entrada valor | `text-green-600` |
| `LancamentosClient.tsx:275` | `text-[#DC2626]` | Saída valor | `text-red-600` |
| `LancamentosClient.tsx:286` | `border-[#16A34A] text-[#16A34A] bg-[#16A34A]/10` | Entrada badge | `border-green-600 text-green-600 bg-green-600/10` |
| `LancamentosClient.tsx:286` | `border-[#DC2626] text-[#DC2626] bg-[#DC2626]/10` | Saída badge | `border-red-600 text-red-600 bg-red-600/10` |
| `CategoriasClient.tsx:243-244` | same as above | Tipo badge | same |
| `CategoriasClient.tsx:260` | `border-[#16A34A] text-[#16A34A] bg-[#16A34A]/10` | Ativo badge | `border-green-600...` |
| `TopNav.tsx:50` | `text-[#0F766E]` | Brand wordmark | accent reserved, not for brand |

**Accent reservation — deviation:**
- §1.3 states accent #0F766E is reserved for: botão CTA primário, focus ring, cor de barra do gráfico, ícones de ação primária, links de navegação ativa. `TopNav.tsx:50` uses it on the brand wordmark, which is not a declared accent element.

**Compliant usage:**
- `DashboardKPIs.tsx`: uses `text-green-600`, `text-red-600`, `border-l-green-600`, `border-l-red-600`, `corSaldo`/`corVariacao` returning `text-green-600`/`text-red-600`/`text-muted-foreground` — semantic and correct.
- `CategoryBreakdown.tsx:121`: chart bar uses `fill="var(--chart-1)"` — correct token usage.
- `globals.css:103,108`: `--ring: #0f766e`, `--chart-1` token defined correctly.
- Destructive actions use `variant="destructive"` which maps to `--destructive: #dc2626` ✓.

Score 2/4 — semantic colors are used in KPIs and charts, but pervasive hardcoded hex values in tables/badges/brand undermine the token system and accent reservation.

---

### Pillar 4: Typography (2/4)

**Font-size distribution — deviations:**
Declared scale (§1.2): xs (14px), base (16px), lg (20px), xl (28px). Only weights 400 and 600 allowed.

| Size | Used where | Spec compliance |
|------|-----------|-----------------|
| `text-xs` | type badges (`LancamentosClient.tsx:286`), table body (`CategoryBreakdown.tsx:78`) ✓ | matches xs |
| `text-sm` | labels, alert descriptions, table cells ✓ | not in declared scale (spec has xs/base/lg/xl only; text-sm = 14px = xs equivalent but arbitrary class) |
| `text-base` | card titles, inputs ✓ | matches base |
| `text-lg` | KPI card descriptions (`DashboardKPIs.tsx:73,85`) ✓ | matches lg |
| `text-[20px]` | page `<h1>` (`page.tsx:53`, `LancamentosClient.tsx:208`, `CategoriasClient.tsx:171`) | **deviation** — h1 must be xl `text-[28px]`, not lg |
| `text-[28px]` | login h1 (`login/page.tsx:24`) ✓ | matches xl (but login is Phase 1, inherited) |
| `text-[0.8rem]` | button `sm` size (`button.tsx:26`) | arbitrary, not in scale |

**h1 size deviation (key finding):**
- §1.2 explicitly: `xl | 28px (1.75rem) | 600 | 1.2 | h1 de página (único h1 por página)`. All three Phase 3 page h1s use `text-[20px]` (`font-semibold`) — the lg scale — instead of `text-[28px]`. Login page (Phase 1) correctly uses `text-[28px]`.

**Font-weight distribution — deviations:**
- §1.2: pesos exatamente 2 — 400 (regular) e 600 (semibold/medium).
- `font-medium` (500) appears in `label.tsx:15`, `button.tsx` base variant, `card.tsx` CardTitle (`font-medium`), and TopNav item active state (`font-medium`) — **deviation**, 500 is not allowed.
- `font-bold` (700) appears in `TopNav.tsx:50` brand wordmark — **deviation**, 700 is not allowed.
- `font-semibold` (600) used correctly on h1s, card titles, buttons, labels ✓.

**Único h1 por página — compliant:**
- Dashboard: one `<h1>` (`page.tsx:53`); Lancamentos: one (`LancamentosClient.tsx:208`); Categorias: one (`CategoriasClient.tsx:171`); Login: one (`login/page.tsx:24`) ✓.

Score 2/4 — h1 uses wrong size (lg instead of xl), disallowed weights (500, 700) present, arbitrary `text-[0.8rem]` used.

---

### Pillar 5: Spacing (2/4)

**Spacing scale (§1.1): 8-point scale 4, 8, 16, 24, 32, 48, 64.** Decision: "Nenhuma exceção além do touch target de 44px." Declared: gap-2 (8px), gap-4 (16px), gap-6 (24px), p-4 (16px), p-6 (24px), px-4, py-2.

**Off-scale values — deviations:**
| File:line | Value | px equivalent | On 8-pt scale? |
|-----------|-------|---------------|----------------|
| `LancamentosClient.tsx:264-269` | `p-3` (table header cells) | 12px | ❌ No |
| `LancamentosClient.tsx:280,282,292` | `p-3` (table body cells) | 12px | ❌ No |
| `LancamentosClient.tsx:429,452` | `px-2.5` (select inputs) | 10px | ❌ No |
| `LancamentosClient.tsx:286` | `py-0.5` (badge padding) | 2px | ❌ No |
| `CategoriasClient.tsx:218-222` | `p-3` (table header) | 12px | ❌ No |
| `CategoriasClient.tsx:234,250` | `p-3` (table body) | 12px | ❌ No |
| `CategoryBreakdown.tsx:95` | `py-3` (table rows) | 12px | ❌ No |

**Touch target 44px — deviation:**
- §1.1: mínimo 44px para botões e inputs interativos. §9: inputs use `h-8` (32px) default, maintain 44px in mobile via `h-11`.
- `MonthSelector.tsx:44`: `<Input ... className="w-40" />` — no `h-11` override, so it inherits the shadcn Input default `h-8` (32px) — **below the 44px minimum touch target** on mobile.
- All other interactive buttons use `h-11` (44px) ✓ (`LancamentosClient.tsx:222`, `CategoriasClient.tsx:185`, `LogoutButton.tsx:28`).

**Compliant patterns:**
- `gap-4`, `gap-6`, `p-4`, `p-6`, `px-4`, `py-12`, `py-2`, `mb-8`, `mt-8`, `space-y-6` — all on the 8-point scale ✓.
- Dashboard KPI grid: `gap-4 sm:grid-cols-3` ✓; page container `px-4 py-12` ✓.

Score 2/4 — several off-scale spacing values (p-3, px-2.5, py-0.5) and one touch-target violation on MonthSelector input.

---

### Pillar 6: Experience Design (3/4)

**Loading states — compliant:**
- `Spinner` used inside submit buttons during `loading` state (`LancamentosClient.tsx:522`, `CategoriasClient.tsx:436`); `loading` boolean state gates button `disabled={loading}` ✓.

**Error states — compliant:**
- `Alert variant="destructive"` with `role="alert"` for API errors (`LancamentosClient.tsx:231`, `CategoriasClient.tsx:194`); try/catch around all mutations with fallback `"Não foi possível conectar..."` ✓.

**Empty states — compliant:**
- `LancamentosClient.tsx:254` `"Nenhum lançamento neste mês"` ✓; `CategoriasClient.tsx:208` `"Nenhuma categoria cadastrada"` ✓; `CategoryBreakdown.tsx:58-68` empty card ✓.

**Disabled states — compliant:**
- All primary/destructive buttons use `disabled={loading}` ✓.

**Destructive confirmation — compliant (D-24):**
- `DeleteConfirmationDialog.tsx` uses Radix `Dialog`, never `window.confirm` ✓; dialog text matches §7.4 ✓; confirm button `variant="destructive"` ✓; cancel `variant="ghost"` ✓.

**Role-based UI — compliant (D-23):**
- `CategoriasClient.tsx:56` `podeEscrever = userRole === "ADMIN"`; criar/editar/desativar buttons and form hidden for USER (`CategoriasClient.tsx:179,303,269,281`) ✓. TopNav receives `userRole` but correctly does not gate navigation (routes visible to all roles; write restrictions are in page actions) ✓.

**Layout & Navigation (§3) — compliant:**
- `TopNav` shared across `/`, `/lancamentos`, `/categorias` with brand, route links, active state (#0F766E), and `Sair` (`LogoutButton`) ✓.
- Each page is a Server Component with `export const instant = false` + `getMe()` session validation + redirect `/login?expirada=1` ✓.
- Single `<h1>` per page ✓; `<main>` landmark present ✓; `<html lang="pt-BR">` in `layout.tsx` ✓.

**Responsive (§9) — mostly compliant:**
- `MonthSelector` uses `key={urlMes}` remount pattern; `sm:` breakpoints on grids ✓; `md:flex` on TopNav nav items ✓; `ResponsiveContainer` on chart ✓.
- Deviation: MonthSelector input touch target (32px < 44px) on mobile — covered in Pillar 5.

**Accessibility gaps (§8) — warnings:**
- MonthSelector `‹`/`›` icon buttons lack `aria-label` (icon-only buttons need accessible names) ✗ §8.
- `Alert` components have `role="alert"` but no `aria-live` (spec §8 suggests `aria-live` polite as agent discretion) — minor.
- No visible focus-ring deviation: `globals.css:176` defines `:focus-visible { outline: 2px solid #0f766e; outline-offset: 2px; }` ✓; components use `focus-visible:border-ring focus-visible:ring-3` — functionally equivalent to the ring token though not the exact outline declaration.

**Dashboard-specific gap:**
- `web/app/page.tsx` is a Server Component with no client loading skeleton or error boundary UI. When `getDashboard()` returns null it redirects (auth failure only). Empty months show zeros (correct per D-15), but there's no loading skeleton/spinner overlay during initial data fetch — the user sees nothing until the server render completes. This is acceptable for a Server Component but worth noting as a WARNING.

Score 3/4 — strong state coverage across loading/error/empty/disabled/confirmation/role-based UI; a11y gaps (icon button labels) and missing dashboard loading UI keep it from 4.

---

## Registry Safety

`components.json` exists with `"registries": {}` (empty — no third-party registries declared, per UI-SPEC §10). shadcn is initialized but no third-party registry blocks were installed.

**Registry audit: 0 third-party blocks checked, no flags.** No Registry Safety section required.

---

## Files Audited

- `web/app/page.tsx` — Dashboard page (Server Component)
- `web/app/lancamentos/page.tsx` — Lançamentos CRUD page (Server Component)
- `web/app/categorias/page.tsx` — Categorias CRUD page (Server Component)
- `web/app/layout.tsx` — Root layout
- `web/app/globals.css` — Design tokens
- `web/app/login/page.tsx` — Login page (Phase 1, inherited; referenced for h1 scale)
- `web/lib/api.ts` — API client functions + TypeScript types
- `web/components/MonthSelector.tsx` — Month selector
- `web/components/DashboardKPIs.tsx` — KPI cards
- `web/components/CategoryBreakdown.tsx` — Category table + horizontal bar chart
- `web/components/TopNav.tsx` — Top navigation
- `web/components/DeleteConfirmationDialog.tsx` — Destructive confirmation dialog
- `web/components/LancamentosClient.tsx` — Lancamentos client interactivity
- `web/components/CategoriasClient.tsx` — Categorias client interactivity
- `web/components/Spinner.tsx` — Loading spinner
- `web/components/LogoutButton.tsx` — Logout button
- `web/components/ui/button.tsx` — Button (shadcn)
- `web/components/ui/card.tsx` — Card (shadcn)
- `web/components/ui/input.tsx` — Input (shadcn)
- `web/components/ui/label.tsx` — Label (shadcn)
- `web/components/ui/alert.tsx` — Alert (shadcn)
- `web/components/ui/chart.tsx` — Chart wrapper (custom)
