# Phase 03 — UI Review

**Audited:** 2026-11-10  
**Baseline:** UI-SPEC.md (`.planning/phases/03-dashboard/03-UI-SPEC.md`) — design contract  
**Screenshots:** not captured (no dev server at localhost:3000/5173/8080 — code-only audit)

---

## Pillar Scores

| Pillar | Score | Key Finding |
|--------|-------|-------------|
| 1. Copywriting | 3/4 | All approved CTAs/labels/empty/error strings match; minor deviation: "Categoria pessoal" vs spec "Pessoal" label |
| 2. Visuals | 4/4 | Clear focal point, icon-only buttons have aria-labels, single h1 per page, proper card hierarchy |
| 3. Color | 4/4 | Accent #0F766E reserved for CTAs/focus/active-nav; semantic colors (green/red/destructive) correct; 60/30/10 respected |
| 4. Typography | 2/4 | font-medium (500) used where spec allows only 400/600; text-lg renders 18px not spec's 20px; KPI titles not 20px/600 |
| 5. Spacing | 2/4 | Main CTAs/inputs h-11 (44px) OK, but table action/dialog/cancel buttons at h-7/h-8 (28-32px) below 44px touch-target minimum |
| 6. Experience Design | 3/4 | Empty/error/disabled/confirmation states present on CRUD pages; dashboard missing §4.5 error Alert and loading Spinner |

**Overall: 18/24**

---

## Top 3 Priority Fixes

1. **Typography: font-weight violations (font-medium/500 where only 400/600 allowed)** — KPI card values (`text-lg font-medium` at DashboardKPIs.tsx:73,85,97), shadcn `CardTitle` (`font-medium` at ui/card.tsx:40), and category name (`font-medium` at CategoriasClient.tsx:235) use weight 500, violating UI-SPEC §1.2 which allows exactly 400 and 600. Fix: replace `font-medium` with `font-semibold` (600) for emphasized text or `font-normal`/`text-base` (400) for regular text.

2. **Spacing: touch-target violations below 44px minimum** — Table row action buttons (Editar/Excluir at `h-8` = 32px, LancamentosClient.tsx:306,316), DeleteConfirmationDialog confirm/cancel buttons (default `h-8` = 32px, DeleteConfirmationDialog.tsx:58-70), and form Cancel buttons (`size="sm"` = `h-7` = 28px, LancamentosClient.tsx:413, CategoriasClient.tsx:366) all fall below the 44px touch target required by UI-SPEC §1.1 and §8. Fix: use `h-11` (44px) for all interactive buttons.

3. **Dashboard missing error state per §4.5** — `web/app/page.tsx` silently redirects to `/login?expirada=1` when `getDashboard()` returns null, with no `Alert variant="destructive"` displaying "Não foi possível carregar os dados. Tente novamente em instantes." as required by UI-SPEC §4.5. No page-level Spinner/skeleton exists either (§4.5: "Loading: Spinner sobreposto ou skeletons nos cards"). Fix: add error Alert UI and loading indicator to the dashboard page.

---

## Detailed Findings

### Pillar 1: Copywriting (3/4)

All approved copywriting contract strings are present and correct:

- **Labels (§7.1):** `Data` (LancamentosClient.tsx:480), `Tipo` (:443), `Categoria` (:420), `Valor` (:460), `Descrição` (:498), `Nome` (CategoriasClient.tsx:373), `Mês` (MonthSelector.tsx:36), `Pessoal` — UI shows "Categoria pessoal" (CategoriasClient.tsx:418), spec §7.1 requires just "Pessoal" — **minor deviation**.
- **CTAs (§7.2):** `Sair` (LogoutButton.tsx:31), `Adicionar lançamento` (LancamentosClient.tsx:224), `Salvar` (:523), `Salvar alterações` (:523, CategoriasClient.tsx:435), `Excluir` (DeleteConfirmationDialog.tsx:38), `Desativar` (:38), `Cancelar` (multiple sites) — all match.
- **Estados (§7.3):** "Nenhum lançamento neste mês" (page.tsx:82, LancamentosClient.tsx:257), "Nenhuma categoria cadastrada" (CategoriasClient.tsx:211), "Não foi possível carregar os dados. Tente novamente em instantes." (LancamentosClient.tsx:170,193), "Mostrando 50 de {N} lançamentos" (LancamentosClient.tsx:247) — all match.
- **Confirmações (§7.4):** `DeleteConfirmationDialog` template `Tem certeza que deseja ${action} este ${target}?` produces exactly "Tem certeza que deseja excluir este lançamento?" (action="excluir", target="lançamento") and "Tem certeza que deseja desativar esta categoria?" (action="desativar", target="categoria") — matches.
- **Validação (§7.5):** "Informe o valor." (:118), "O valor aceita no máximo 2 casas decimais." (:122), "O valor deve ser maior que zero." (:124), "Selecione uma categoria." (:113), "Selecione o tipo (entrada ou saída)." (:114), "Informe o nome da categoria." (CategoriasClient.tsx:86) — all match.

**Deviations:**
- `Categoria pessoal` label instead of `Pessoal` (CategoriasClient.tsx:418) — spec §7.1 label table.
- Categoria select options render raw tipo values `{c.nome} ({c.tipo})` (LancamentosClient.tsx:433), displaying "ENTRADA"/"SAIDA" to users instead of human-friendly labels.

### Pillar 2: Visuals (4/4)

- **Clear focal point:** h1 "Dashboard" (text-[28px] font-semibold) at top, followed by MonthSelector, then 3 KPI cards (web/app/page.tsx:46-78). Visual hierarchy is clear.
- **Icon-only buttons paired with aria-labels:** MonthSelector navigation arrows ‹ › have `aria-label="Mês anterior"` (:46) and `aria-label="Mês seguinte"` (:49).
- **Visual hierarchy through size/weight/color:** h1 at 28px/font-semibold, KPI values at text-lg, labels at text-sm/text-muted-foreground, muted secondary text throughout.
- **Single h1 per page:** Dashboard (page.tsx:53), Lançamentos (LancamentosClient.tsx:208), Categorias (CategoriasClient.tsx:171) — each page has exactly one `<h1>`.
- **Card structure:** shadcn `Card` with `rounded-xl` (12px per §1.4) used for KPI cards, breakdown card, form cards.
- **Shell structure:** `<main className="flex min-h-dvh w-full flex-col items-center justify-center px-4 py-12">` matches §3.3 exactly (page.tsx:49, lancamentos/page.tsx:79, categorias/page.tsx:47).

No deviations found.

### Pillar 3: Color (4/4)

**Color usage analysis (class counts):**

| Function | Spec Value | Implementation | Usage |
|----------|-----------|----------------|-------|
| Background (60%) | #F8FAFC slate-50 | `bg-background` (--background: #f8fafc) | Body/page |
| Card (30%) | #FFFFFF | `bg-card` (--card: #ffffff) | Cards, surfaces |
| Accent (10%) | #0F766E teal-600 | `text-primary`/`bg-primary` (--primary: #0f766e) | TopNav active, brand |
| Foreground | #0F172A | `text-foreground` | Body text, titles |
| Muted | #64748B | `text-muted-foreground` | Secondary text, labels |
| Border | #E2E8F0 | `border-border` | Inputs, cards, dividers |
| Destructive | #DC2626 | `variant="destructive"`/`text-destructive` | Delete/desativar buttons, error text |
| Entrada | #16A34A green-600 | `text-green-600`/`border-green-600`/`bg-green-600/10` | KPI Entradas, table ENTRADA values, tipo badges |
| Saída | #DC2626 red-600 | `text-red-600`/`border-red-600`/`bg-red-600/10` | KPI Saídas, table SAÍDA values, tipo badges |
| Ring / Focus | #0F766E | `focus-visible:ring-ring` + globals.css `:focus-visible { outline: 2px solid #0f766e; }` | Inputs, selects, focus |
| Hover CTA | #115E59 teal-800 | globals.css `[data-slot="button"][data-variant="default"]:hover { background-color: #115e59; }` | Primary button hover |

**60/30/10 distribution:** Background dominates (body), cards are secondary surfaces, accent (#0F766E/primary) is limited to CTAs, focus rings, and active navigation — correctly reserved per §1.3.

**Note:** `--chart-1` in globals.css is `oklch(0.87 0 0)` (light gray), not #0F766E teal as UI-SPEC §4.3 describes. This is a pre-existing Phase 1 design token; Phase 3 correctly uses `var(--chart-1)` (CategoryBreakdown.tsx:121) and does not re-specify tokens per §1 ("não reespecificar o que já existe").

No Phase 3 color deviations found.

### Pillar 4: Typography (2/4)

**Font-size distribution (from grep):**
- `text-[28px]` — h1 "Dashboard" (page.tsx:53) — matches xl (28px/600/1.2) ✓
- `text-lg` — KPI card values (DashboardKPIs.tsx:73,85,97) — Tailwind default = 18px, spec lg = 20px ✗
- `text-sm` — table text, labels, muted text — 14px, matches xs size ✓
- `text-base` — inputs, buttons — 16px, matches base ✓

**Font-weight distribution (from grep):**
- `font-semibold` (600) — h1, KPI card titles, active nav items ✓
- `font-medium` (500) — KPI card values (DashboardKPIs.tsx:73,85,97), shadcn CardTitle (ui/card.tsx:40), category name (CategoriasClient.tsx:235) ✗ — spec §1.2 allows exactly 400 and 600 only

**Deviations:**
1. `font-medium` (weight 500) used in Phase 3 code where spec permits only 400 and 600 (UI-SPEC §1.2: "Pesos: exatamente 2 — 400 (regular) e 600 (semibold/medium)").
2. `text-lg` renders 18px (Tailwind default) where spec §1.2 requires lg = 20px for "Títulos de seção (KPI card title, h2 de tela)".
3. KPI card titles (`CardTitle` at DashboardKPIs.tsx:72,84,96) inherit shadcn default `text-base font-medium` (16px/500) instead of the spec's 20px/600 lg scale.

### Pillar 5: Spacing (2/4)

**Spacing classes in use (8-point scale compliant):**
- `gap-4` (16px), `gap-6` (24px) — MonthSelector, page header sections ✓
- `p-4` (16px), `p-6` (24px) — table cells, form cards, empty states ✓
- `px-4` (16px) — TopNav, table header cells ✓
- `py-2` (8px) — TopNav nav items ✓
- `py-12` (48px) — page container ✓
- `px-4 py-12` — page container shell (§3.3, §9) ✓
- `mb-8` (32px), `mt-4` (16px), `mt-6` (24px), `mt-8` (32px) — section spacing ✓

**Touch target (min 44px per §1.1, §8):**
- ✅ Main CTAs and inputs: `h-11` (44px) — Adicionar lançamento button (LancamentosClient.tsx:222), form submit buttons (:516), all Input/Select fields (:427,:450,:473,:491,:507, CategoriasClient.tsx:385,399,416), MonthSelector input (:44).
- ❌ Table action buttons: `size="sm" className="h-8 px-2 text-xs"` = 32px (LancamentosClient.tsx:306,316) — below 44px.
- ❌ DeleteConfirmationDialog buttons: default size = `h-8` = 32px (DeleteConfirmationDialog.tsx:58-70) — below 44px.
- ❌ Form Cancel buttons: `size="sm"` = `h-7` = 28px (LancamentosClient.tsx:413, CategoriasClient.tsx:366) — below 44px.

**Deviations:** Small interactive buttons (table actions, dialog confirm/cancel, form cancel) fall below the 44px touch-target minimum required by UI-SPEC §1.1 and §8.

### Pillar 6: Experience Design (3/4)

**Loading states:**
- ✅ Spinner in form submit buttons during `loading` (LancamentosClient.tsx:518-523, CategoriasClient.tsx:430-435).
- ❌ Dashboard page (`web/app/page.tsx`) has no Spinner or skeleton during data fetch — §4.5 requires "Spinner sobreposto ou skeletons nos cards". SSR handles initial load, but no client loading indicator exists.

**Error states:**
- ✅ `Alert variant="destructive"` on LancamentosClient (:230-234) and CategoriasClient (:193-197) with error messages.
- ❌ Dashboard page redirects silently on `getDashboard()` null return (page.tsx:42-44) with no `Alert variant="destructive"` showing "Não foi possível carregar os dados. Tente novamente em instantes." as required by §4.5.

**Empty states:**
- ✅ Dashboard: "Nenhum lançamento neste mês" (page.tsx:80-84).
- ✅ Lancamentos: "Nenhum lançamento neste mês" (LancamentosClient.tsx:255-259).
- ✅ Categorias: "Nenhuma categoria cadastrada" (CategoriasClient.tsx:208-213).

**Disabled states:** Buttons use `disabled={loading}` (LancamentosClient.tsx:221,305,314,515; CategoriasClient.tsx:184,271,283,427) ✓.

**Destructive confirmation:** `DeleteConfirmationDialog` used for both excluir lançamento and desativar categoria, no `window.confirm` anywhere ✓ (D-24).

**Role-based UI (D-23):** `podeEscrever = userRole === "ADMIN"` gates criar/editar/desativar in CategoriasClient (:56,179,299); Edit/Desativar buttons hidden for USER (:265,277). ✓

**Accessibility (WCAG AA §8):**
- ✅ Focus visible: globals.css `:focus-visible { outline: 2px solid #0f766e; outline-offset: 2px; }` (:175-178).
- ✅ Labels: `<Label htmlFor="...">` on all inputs.
- ✅ `aria-invalid` on fields with errors.
- ✅ Single h1 per page.
- ✅ `<main>` landmark, `lang="pt-BR"` in layout.tsx.
- ✅ `aria-hidden="true"` on Spinner icon (Spinner.tsx:7).
- ✅ `aria-label` on MonthSelector arrow buttons (:46-50).
- ✅ `aria-current="page"` on active TopNav links (TopNav.tsx:69).

**Deviations:** Dashboard missing error Alert per §4.5; no page-level loading Spinner/skeleton per §4.5.

---

## Files Audited

- `web/app/page.tsx` — Dashboard server component
- `web/app/lancamentos/page.tsx` — Lancamentos server component
- `web/app/categorias/page.tsx` — Categorias server component
- `web/app/layout.tsx` — Root layout
- `web/app/globals.css` — Design tokens and theme
- `web/lib/api.ts` — API client functions and types
- `web/components/MonthSelector.tsx` — Month selector
- `web/components/DashboardKPIs.tsx` — KPI cards
- `web/components/CategoryBreakdown.tsx` — Category table + chart
- `web/components/LancamentosClient.tsx` — Lancamentos client (CRUD + form)
- `web/components/CategoriasClient.tsx` — Categorias client (CRUD + form)
- `web/components/TopNav.tsx` — Top navigation
- `web/components/DeleteConfirmationDialog.tsx` — Destructive confirmation dialog
- `web/components/Spinner.tsx` — Loading spinner
- `web/components/LogoutButton.tsx` — Logout button
- `web/components/ui/card.tsx` — shadcn Card
- `web/components/ui/button.tsx` — shadcn Button
- `web/components/ui/alert.tsx` — shadcn Alert
- `web/components/ui/input.tsx` — shadcn Input
- `web/components/ui/label.tsx` — shadcn Label
- `web/components/ui/chart.tsx` — shadcn/ui Charts wrapper
