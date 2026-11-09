# UI-SPEC — Phase 3: Dashboard (Controle Financeiro)

**Phase:** 03 — Dashboard  
**Mode:** MVP  
**Status:** draft  
**File:** `.planning/phases/03-dashboard/03-UI-SPEC.md`  
**Requirements:** DSH-01, DSH-02, DSH-03, DSH-04, DSH-05 (+ CAT-01..06, LAN-01..07, LOT-01..02 para CRUD herdado)  
**Context source:** `03-CONTEXT.md` (locked decisions D-01..D-24), `03-RESEARCH.md`, Phase 1 `01-UI-SPEC.md` (design tokens), `globals.css`

---

## 1. Design System

Todos os valores abaixo são herdados da Phase 1 UI-SPEC e do `web/app/globals.css` existente. Não reespecificar o que já existe.

### 1.1 Spacing

| Token | Valor | Uso |
|-------|-------|-----|
| Espaçamento base | Escala 8 pontos: 4, 8, 16, 24, 32, 48, 64 | Padding, margin, gap, grid gutters |
| Exceção touch target | Mínimo 44px (altura/largura) para botões e inputs interativos | Acessibilidade WCAG AA (tela sensível ao toque) |

**Decisão:** Nenhuma exceção além do touch target de 44px. Usar Tailwind `gap-2` (8px), `gap-4` (16px), `gap-6` (24px), `p-4` (16px), `p-6` (24px), `px-4`, `py-2` etc.

### 1.2 Typography

| Escala | Tamanho | Peso | Line-height | Uso |
|--------|---------|------|-------------|-----|
| xs | 14px (0.875rem) | 400 | 1.5 | Labels secundários, texto de tabela pequeno, muted |
| base | 16px (1rem) | 400 | 1.5 | Body texto principal, inputs, botões |
| lg | 20px (1.25rem) | 600 | 1.2 | Títulos de seção (KPI card title, h2 de tela) |
| xl | 28px (1.75rem) | 600 | 1.2 | h1 de página (único h1 por página) |

- **Fonte:** `system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif` (zero dependência, suporte pt-BR) — já definido em `globals.css :root --font-sans`
- **html font-size:** 16px — já definido em `globals.css`
- **Pesos:** exatamente 2 — `400` (regular) e `600` (semibold/medium)

### 1.3 Color (60/30/10)

| Função | Valor (claro) | Valor (escuro) | Reserva |
|--------|---------------|----------------|---------|
| Background (60% dominante) | `#F8FAFC` (slate-50) | oklch(0.145 0 0) | Superfície principal da aplicação |
| Card / Secondary (30%) | `#FFFFFF` | oklch(0.205 0 0) | Cards, sidebar, navegação, superficiões elevadas |
| Accent / Primary (10%) | `#0F766E` (teal-600) | oklch(0.922 0 0) | **Reservado para:** botão CTA primário, focus ring, cor de barra do gráfico, ícones de ação primária, links de navegação ativa |

**Semânticas adicionais:**

| Função | Valor | Uso |
|--------|-------|-----|
| Foreground (texto principal) | `#0F172A` (slate-900) | oklch(0.985 0 0) | Texto corporal, títulos |
| Muted foreground | `#64748B` (slate-500) | oklch(0.708 0 0) | Texto secundário, legendas, placeholders |
| Border | `#E2E8F0` (slate-200) | oklch(1 0 0 / 10%) | Divisórias, inputs, cards |
| Destructive | `#DC2626` (red-600) | oklch(0.704 0.191 22.216) | Ações destrutivas (excluir, desativar), estados de erro |
| Entrada (dinheiro) | `#16A34A` (green-600) | — | Valores positivos, KPI de entradas, texto de entrada |
| Saída (dinheiro) | `#DC2626` (red-600) | — | Valores negativos, KPI de saídas, texto de saída |
| Ring / Focus | `#0F766E` (teal-600) | oklch(0.556 0 0) | Anel de foco 2px, outline-offset 2px |
| Hover CTA primário | `#115E59` (teal-800) | — | Hover do botão `variant="default"` |

### 1.4 Radius

| Token | Valor | Uso |
|-------|-------|-----|
| Base | 8px (`0.5rem`, `--radius`) | Componentes gerais, inputs |
| Card | 12px (`--radius-xl`) | Cards de KPI, cards de tela |
| Sm / Md | `calc(var(--radius) * 0.6)` / `calc(var(--radius) * 0.8)` | Elementos pequenos internos |

### 1.5 Focus

- Seletor: `:focus-visible` (apenas teclado, nunca mouse)
- Outline: `2px solid #0F766E`
- Outline-offset: `2px`
- Já definido em `globals.css` — não reespecificar no componente.

---

## 2. Component Inventory

### 2.1 shadcn/ui (já instalados — reutilizar sem alteração)

| Componente | Caminho | Uso nesta fase |
|------------|---------|----------------|
| `Card` | `web/components/ui/card.tsx` | KPI cards, cards de formulário, cards de empty state |
| `CardHeader`, `CardTitle`, `CardDescription`, `CardContent`, `CardFooter` | `web/components/ui/card.tsx` | Estrutura interna dos cards |
| `Button` | `web/components/ui/button.tsx` | CTA primário, secundário, destrutivo, navegação, logout |
| `Input` | `web/components/ui/input.tsx` | Campos de formulário (nome categoria, valor, descrição, data) |
| `Label` | `web/components/ui/label.tsx` | Rótulos associados a inputs (obrigatório, `htmlFor`/`id`) |
| `Alert` | `web/components/ui/alert.tsx` | Estados de erro, sucesso, aviso (truncamento, vazio) |

### 2.2 Custom (existentes — reutilizar)

| Componente | Caminho | Uso nesta fase |
|------------|---------|----------------|
| `Spinner` | `web/components/Spinner.tsx` | Estados de loading em dashboard, listagens, formulários |
| `LoginForm` | `web/components/LoginForm.tsx` | Fase 1 — não usado nesta fase (padrão de form controlado a seguir) |
| `LogoutButton` | `web/components/LogoutButton.tsx` | Navegação top — ação "Sair" |

### 2.3 Novos componentes a criar (esta fase)

| Componente | Caminho | Tipo | Descrição |
|------------|---------|------|-----------|
| `chart.tsx` | `web/components/ui/chart.tsx` | shadcn/ui Charts wrapper | Wrapper `<div data-slot="chart">` com `w-full h-[300px]`, reutiliza tokens `--chart-1..5` |
| `DashboardKPIs` | `web/components/DashboardKPIs.tsx` | Client | 3 cards (Entradas, Saídas, Saldo) com variação vs mês anterior |
| `CategoryBreakdown` | `web/components/CategoryBreakdown.tsx` | Client | Tabela ordenada por valor decrescente + gráfico de barras horizontais |
| `MonthSelector` | `web/components/MonthSelector.tsx` | Client | `<input type="month">` + setas ‹ › + sync com URL `?mes=YYYY-MM` |
| `Dashboard` page | `web/app/page.tsx` | Server | Composição dos componentes acima, dados via `web/lib/api.ts` |
| `Lancamentos` page | `web/app/lancamentos/page.tsx` | Server | Listagem (mês filtrado), criar, editar, excluir |
| `Categorias` page | `web/app/categorias/page.tsx` | Server | Listagem, criar, editar, desativar (soft delete) |
| `DeleteConfirmationDialog` | `web/components/DeleteConfirmationDialog.tsx` | Client | Diálogo reutilizável para excluir lançamento / desativar categoria (D-24: sem `window.confirm`) |
| `TopNav` | `web/components/TopNav.tsx` | Client | Navegação entre `/`, `/lancamentos`, `/categorias` + `LogoutButton` |

### 2.4 Novas dependências

| Pacote | Propósito | Status |
|--------|-----------|--------|
| `recharts` | Gráfico de barras horizontais (via shadcn/ui Charts) | A instalar no `web/` — `checkpoint:human-verify` antes do install (planner) |
| `lucide-react` | Ícones (setas, categorias, lançamentos, spinner) | Já instalado (usado por `Spinner`) |

---

## 3. Layout & Navigation

### 3.1 Estrutura de rotas

| Rota | Título (h1) | Acesso | Descrição |
|------|-------------|--------|-----------|
| `/` | "Dashboard" | Público (autenticado) | KPIs, comparação mensal, gasto por categoria, seletor de mês |
| `/lancamentos` | "Lançamentos" | Público (autenticado) | Listagem filtrada por mês, criar, editar, excluir |
| `/categorias` | "Categorias" | Público (autenticado) | Listagem, criar, editar, desativar (ADMIN-only escrita) |
| `/login` | "Controle Financeiro" | Público | Tela de login (fase 1, mantida) |

### 3.2 TopNav (compartilhado nas 3 rotas)

- **Posição:** topo da página, largura total
- **Itens (esquerda → direita):**
  1. Marca "Controle Financeiro" (link para `/`)
  2. "Dashboard" (ativo quando em `/`)
  3. "Lançamentos" (ativo quando em `/lancamentos`)
  4. "Categorias" (ativo quando em `/categorias`)
  5. (spacer)
  6. "Sair" (`LogoutButton`, variant outline)
- **Estado ativo:** cor de texto `primary` (#0F766E), fundo `accent` sutil
- **Inativo:** `muted-foreground` (#64748B)
- **Mobile:** itens empilhados ou menu hambúrguer (discrição do agente; manter usável em toque)

### 3.3 Shell da página

- Cada página é um Next.js Server Component com `export const instant = false` (depende de `searchParams` e cookies)
- Valida sessão via `getMe()` → redirect `/login?expirada=1` se não autenticado
- Layout raiz (`web/app/layout.tsx`): `<html lang="pt-BR">`, `<body>` sem wrapper adicional
- Cada página envolve conteúdo em `<main>` com `flex min-h-dvh w-full flex-col items-center justify-center px-4 py-12` (padrão já usado em `page.tsx` e `login/page.tsx`)
- **Único `<h1>` por página** (a11y)

---

## 4. Dashboard Screen Contracts (`/`)

### 4.1 Seletor de mês (`MonthSelector`)

- Controle: `<input type="month">` nativo (D-13)
- Valor padrão: mês corrente (D-13)
- Permitir meses futuros (D-14) — sem `max`
- Sem limite inferior (D-15) — sem `min`
- Mês vazio mostra zeros, sem erro (D-15)
- URL: `/?mes=YYYY-MM` (D-16) — fonte da verdade, recarregável e compartilhável
- Setas ‹ › (D-17): navegam mês anterior/próximo, atualizam URL
- Mudança no input ou nas setas → `router.push` / `router.replace` com novo `mes`
- `export const instant = false` na página para reatividade a `searchParams`

### 4.2 KPI Cards (`DashboardKPIs`)

Três cards lado a lado (desktop) ou empilhados (mobile):

| Card | Label | Valor | Variação vs mês anterior |
|------|-------|-------|--------------------------|
| Entradas | "Entradas" | `R$ 0,00` (green #16A34A) | `+R$ 0,00` / `—` (absoluto + percentual) |
| Saídas | "Saídas" | `R$ 0,00` (red #DC2626) | `+R$ 0,00` / `—` (absoluto + percentual) |
| Saldo | "Saldo" | `R$ 0,00` (cor depende do sinal) | `+R$ 0,00` / `—` (absoluto + percentual) |

**Regras de variação (D-05, D-06, D-07):**
- Exibir variação em **R$ absoluto e percentual** para entradas, saídas e saldo
- Quando valor do mês anterior é zero (ou não positivo): percentual = `"—"` (sem base de comparação), mostrar apenas variação absoluta
- Sem divisão por zero — nunca exibir `NaN%` ou `Infinity%`
- Comparação é mês fechado vs mês fechado, inclusive mês corrente em andamento

**Cores do Saldo:**
- Positivo: verde #16A34A
- Negativo: vermelho #DC2626
- Zero: muted #64748B

### 4.3 Gasto por categoria (`CategoryBreakdown`)

**Tabela (D-08, D-09):**
- Listar **todas** as categorias de SAÍDA do mês, inclusive zeradas
- Categorias de ENTRADA não aparecem
- Não agrupar em "Outros"
- Ordenadas por **valor decrescente**
- Colunas: Categoria | Valor | Percentual sobre total de saídas
- Valor formatado: `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`
- Percentual: `0,00%` quando total de saídas > 0; `"—"` quando total de saídas = 0 (D-10)

**Gráfico de barras horizontais (D-08, D-011, D-12):**
- Biblioteca: shadcn/ui Charts (Recharts por baixo) — wrapper `components/ui/chart.tsx`
- Layout: `horizontal` (barras horizontais)
- Ordenadas por valor decrescente (mesma ordem da tabela)
- Cor da barra: `--chart-1` (#0F766E / teal) — token de tema
- Tooltip: valor formatado em BRL
- Eixo Y: nomes de categoria (largura suficiente para nomes longos, mínimo 120px)
- Eixo X: numérico (valores em BRL)
- Sem tooltips ricos, sem zoom, sem drill-down (deferido)

**Estado zerado (D-10):**
- Quando total de saídas = 0: tabela lista categorias com valor `R$ 0,00` e percentual `"—"`; barras com largura zero; sem erro

### 4.4 Empty state

Quando mês sem lançamentos:
- KPIs mostram `R$ 0,00` para todos
- Variação mostra `"—"`
- Tabela de categorias: vazia (ou lista de categorias de saída zeradas se existirem cadastradas)
- Gráfico: barras de largura zero
- Texto auxiliar opcional: "Nenhum lançamento neste mês" (copywriting aprovado)

### 4.5 Loading / Error

- Loading: `Spinner` sobreposto ou skeletons nos cards
- Error: `Alert variant="destructive"` com mensagem: "Não foi possível carregar os dados. Tente novamente em instantes." (copywriting aprovado)

---

## 5. Lançamentos Screen Contracts (`/lancamentos`)

### 5.1 Listagem

- Filtro pelo mês selecionado (reusa `?mes=YYYY-MM` da URL, compartilhado com dashboard)
- Tabela com colunas: Data | Descrição | Categoria | Tipo | Valor | Ações
- Tipo ENTRADA: valor em verde #16A34A; Tipo SAÍDA: valor em vermelho #DC2626
- Ações por linha: Editar | Excluir (destructive)
- **Truncamento (D-22):** `GET /api/lancamentos` retorna máximo 50 itens. Quando houver mais de 50, exibir aviso: "Mostrando 50 de N lançamentos" (o total N vem do endpoint de agregação ou count separado — discrição do agente)
- Empty state: "Nenhum lançamento neste mês"

### 5.2 Criar lançamento

Formulário controlado (sem RHF/zod — consistente com `LoginForm`):
- Campos: Data (date input), Tipo (ENTRADA/SAÍDA — select ou rádio), Categoria (select filtrado por tipo), Valor (input numérico, positivo, máximo 2 casas decimais), Descrição (texto)
- Validação de UI espelha regras da API: valor > 0, máximo 2 casas, categoria do tipo correto (entrada não aceita saída e vice-versa)
- CTA: "Salvar" (primary)
- Cancelar: volta para listagem
- Sucesso: volta para listagem com alerta de sucesso (opcional)
- Erro: `Alert variant="destructive"` com mensagem da API

### 5.3 Editar lançamento

- Mesmos campos da criação, pré-preenchidos com dados existentes
- Mesmas validações
- CTA: "Salvar alterações"
- Excluir: ação separada (veja 5.4)

### 5.4 Excluir lançamento (DELETE físico, irreversível)

- **Confirmação explícita obrigatória (D-24):** nunca `window.confirm`
- Usar `DeleteConfirmationDialog` reutilizável
- Texto do diálogo: "Tem certeza que deseja excluir este lançamento?" (copywriting aprovado)
- Botões: "Excluir" (destructive) | "Cancelar"
- Após confirmação: chamada `DELETE /api/lancamentos/{id}`, se sucesso → remover da lista e mostrar alerta de sucesso; se falha → `Alert variant="destructive"`

---

## 6. Categorias Screen Contracts (`/categorias`)

### 6.1 Listagem

- Lista todas as categorias da empresa (filtradas por `empresa_id` do token)
- Colunas: Nome | Tipo (Entrada/Saída) | Pessoal (sim/não) | Ativo (sim/não) | Ações
- Categorias desativadas (soft delete): exibidas com estilo atenuado (opacity ou texto riscado), indicando "Inativo"
- Ações por linha (depende do role — veja 6.4):
  - Editar (se ativo e usuário tem permissão)
  - Desativar (se ativo e usuário tem permissão)
  - (não há "reativar" em MVP — D-19 diz desativar = excluir)

### 6.2 Criar categoria

Formulário controlado:
- Campos: Nome, Tipo (ENTRADA/SAÍDA), Pessoal (checkbox, opcional)
- Validação: nome único por (empresa, tipo) — erro da API exibido em `Alert variant="destructive"`
- CTA: "Salvar" (primary)
- **Restrição de role:** apenas ADMIN pode criar (UI oculta/desabilita para USER — D-23)

### 6.3 Editar categoria

- Campos: Nome, Tipo, Pessoal, Ativo (toggle)
- Mesmas validações
- CTA: "Salvar alterações"
- **Restrição de role:** apenas ADMIN (D-23)

### 6.4 Desativar categoria (soft delete, D-19)

- **Confirmação explícita obrigatória (D-24):** nunca `window.confirm`
- Usar `DeleteConfirmationDialog` reutilizável
- Texto do diálogo: "Tem certeza que deseja desativar esta categoria?" (copywriting aprovado)
- Botões: "Desativar" (destructive) | "Cancelar"
- Backend já implementa `DELETE /api/categorias/{id}` como `ativo=false` — UI reflete isso
- Após sucesso: categoria marcada como inativa na lista
- **Restrição de role:** apenas ADMIN (D-23)

### 6.5 Role-based UI (D-23)

| Ação | USER | ADMIN |
|------|------|-------|
| Listar categorias | ✓ | ✓ |
| Criar categoria | Oculto / desabilitado | ✓ |
| Editar categoria | Oculto / desabilitado | ✓ |
| Desativar categoria | Oculto / desabilitado | ✓ |

---

## 7. Copywriting Contract (pt-BR)

### 7.1 Labels

| Campo | Rótulo |
|-------|--------|
| E-mail (login) | E-mail |
| Senha (login) | Senha |
| Data | Data |
| Tipo | Tipo |
| Categoria | Categoria |
| Valor | Valor |
| Descrição | Descrição |
| Nome (categoria) | Nome |
| Pessoal | Pessoal |
| Mês | Mês |

### 7.2 Botões e CTAs

| Contexto | Rótulo | Variante |
|----------|--------|----------|
| Login | Entrar | default (primary) |
| Logout | Sair | outline |
| Dashboard CTA | Adicionar lançamento | default (primary) |
| Salvar lançamento | Salvar | default |
| Salvar alterações | Salvar alterações | default |
| Salvar categoria | Salvar | default |
| Salvar alterações categoria | Salvar alterações | default |
| Excluir lançamento | Excluir | destructive (no diálogo) |
| Desativar categoria | Desativar | destructive (no diálogo) |
| Cancelar | Cancelar | ghost / secondary |
| Voltar | (navegação padrão) | — |

### 7.3 Estados

| Estado | Texto |
|--------|-------|
| Empty dashboard (sem lançamentos) | "Nenhum lançamento neste mês" |
| Empty lista lançamentos | "Nenhum lançamento neste mês" |
| Empty lista categorias | "Nenhuma categoria cadastrada" |
| Error API (genérico) | "Não foi possível carregar os dados. Tente novamente em instantes." |
| Error credencial (login) | "E-mail ou senha inválidos. Verifique os dados e tente novamente." |
| Error rede (login) | "Não foi possível conectar. Tente novamente em instantes." |
| Sessão expirada | "Sua sessão expirou. Entre novamente." |
| Truncamento (50 de N) | "Mostrando 50 de {N} lançamentos" |
| Loading | (Spinner, sem texto obrigatório) |

### 7.4 Confirmações destrutivas (D-24)

| Ação | Texto do diálogo | Botão confirmar | Botão cancelar |
|------|------------------|-----------------|----------------|
| Excluir lançamento | "Tem certeza que deseja excluir este lançamento?" | Excluir | Cancelar |
| Desativar categoria | "Tem certeza que deseja desativar esta categoria?" | Desativar | Cancelar |

### 7.5 Validação de UI (mensagens de campo)

| Campo | Mensagem |
|-------|----------|
| Valor obrigatório | "Informe o valor." |
| Valor > 2 casas | "O valor aceita no máximo 2 casas decimais." |
| Valor <= 0 | "O valor deve ser maior que zero." |
| Categoria obrigatória | "Selecione uma categoria." |
| Tipo obrigatório | "Selecione o tipo (entrada ou saída)." |
| Nome categoria obrigatório | "Informe o nome da categoria." |

---

## 8. Accessibility (WCAG AA)

- **Focus visible:** outline 2px #0F766E, outline-offset 2px, apenas teclado (`:focus-visible`) — já em `globals.css`
- **Labels:** todo input tem `<Label htmlFor="id">` associado; `aria-invalid` e `aria-describedby` para erros de campo (padrão `LoginForm`)
- **Touch targets:** mínimo 44px para botões e inputs interativos
- **Contraste:** cores da Phase 1 já atendem WCAG AA (teal #0F766E sobre branco, red #DC2626 sobre branco, texto #0F172A sobre #F8FAFC)
- **Único h1 por página:** cada tela tem exatamente um `<h1>`
- **Landmarks:** `<main>` como região principal; navegação no topo
- **Keyboard:** todos os interativos acessíveis por Tab; diálogos capturam foco (discrição do agente ao implementar `DeleteConfirmationDialog`)
- **Lang:** `<html lang="pt-BR">` já em `layout.tsx`
- **Screen reader:** `aria-live` polite para alertas de erro/sucesso (discrição do agente)
- **Icons:** `aria-hidden="true"` em ícones decorativos (padrão `Spinner`)

---

## 9. Responsive Behavior

**Mobile-first dentro do desktop-first do projeto:**

| Breakpoint | Comportamento |
|------------|---------------|
| < 768px (mobile) | Cards KPI empilhados verticalmente; tabela de lançamentos com scroll horizontal se necessário; TopNav itens podem empilhar; inputs full-width |
| ≥ 768px (md) | Cards KPI em linha (grid 3 colunas); tabela completa; TopNav horizontal |
| Todos | `px-4 py-12` no container principal (padrão existente); `md:p-8` para cards maiores |

- Nenhum breakpoint abaixo de mobile (o projeto já é desktop-first com adaptações `md:`)
- Inputs: `h-8` (32px) default, `md:text-base` — manter altura mínima 44px em mobile via `className="h-11 md:text-base"` (padrão `LoginForm`)
- Gráfico: `ResponsiveContainer` do Recharts, `width="100%" height="100%"`

---

## 10. Registration Safety

### 10.1 shadcn state

- `components.json` encontrado: `style: radix-nova`, `iconLibrary: lucide`, `css: app/globals.css`, `baseColor: neutral`, `cssVariables: true`
- Registries: `{}` (vazio — nenhum third-party registry declarado)
- Componentes instalados: card, button, alert, input, label

### 10.2 shadcn/ui Charts

- O wrapper `components/ui/chart.tsx` é **custom** (criado nesta fase), não um bloco de registry shadcn
- Não há entrada de registry para chart
- **Vale:** não aplicável (não é bloco de registry)

### 10.3 Third-party npm packages

| Pacote | Tipo | Vetting gate | Status |
|--------|------|--------------|--------|
| `recharts` | npm (não shadcn registry) | Não é bloco de registry shadcn → gate de registry não aplicável. Pacote npm padrão da comunidade, mantido pelo Recharts project. | A instalar — `checkpoint:human-verify` antes do install (planner) |

**Conclusão:** Nenhum third-party registry block foi declarado. UI-SPEC não bloqueado por registry. `recharts` é dependência npm, não exige vetting de registry shadcn.

---

## Pre-População e Fontes

| Seção | Fonte | Decisões usadas |
|-------|-------|-----------------|
| Design System | Phase 1 UI-SPEC + `globals.css` | Cores, tipografia, spacing, radius, focus |
| Component inventory | Código existente (`web/components/ui/`, `web/components/Spinner.tsx`, `LogoutButton.tsx`) | Componentes reutilizados + novos listados |
| Layout & Navigation | `03-CONTEXT.md` D-21, D-16, D-17 | Rotas, URL state, setas, logout |
| Dashboard | `03-CONTEXT.md` D-01..D-17, `REQUIREMENTS.md` DSH-01..05 | KPIs, comparação, categoria, mês selector |
| Lançamentos | `03-CONTEXT.md` D-18, D-20, D-22, D-24 | CRUD, truncamento, confirmação |
| Categorias | `03-CONTEXT.md` D-18, D-19, D-23, D-24 | CRUD, soft delete, role ADMIN-only |
| Copywriting | User input (esta sessão) | CTA, empty state, error, confirmações |
| Accessibility | Phase 1 UI-SPEC + `globals.css` | WCAG AA, focus, labels, touch targets |
| Responsive | Discrição do agente + padrão existente | Mobile-usável, desktop-first |
| Registration Safety | `components.json` | sem third-party registries, recharts = npm |
