---
phase: 05
slug: importa-o-de-pdfs-e-an-lise-por-ia
status: draft
shadcn_initialized: true
preset: radix-nova
created: 2026-11-12
---

# Phase 5 — UI Design Contract

> Contrato visual e de interação da Fase 5 (Importação de PDFs e Análise por IA).
> Fonte de verdade visual: `web/app/globals.css` (tokens vivos) + componentes já validados
> (`Sidebar`, `Header`, `DashboardKPIs`, `LancamentosTable`, `DeleteConfirmationDialog`).
> **Não introduzir paleta, tipografia, espaçamento ou componente fora do que já existe.**

---

## Design System

| Property | Value |
|----------|-------|
| Tool | shadcn/ui (registry oficial, já inicializado) |
| Preset | `radix-nova` (`web/components.json`: `style: radix-nova`, `baseColor: neutral`, `cssVariables: true`, `tailwind.config: ""` — Tailwind v4) |
| Component library | radix (`@radix-ui/react-*`, via `radix-ui` no `Button`/`Select`/`Dialog`) |
| Icon library | lucide-react (`iconLibrary: "lucide"` no `components.json`) |
| Font | `system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif` (`--font-sans`, já em `globals.css`; `html { font-size: 16px }`) |
| Idioma da UI | Português do Brasil (`<html lang="pt-BR">` em `web/app/layout.tsx`) |
| Moeda | `Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" })` — reutilizar o `MONETARY` já exportado por `web/components/DashboardKPIs.tsx` (D-03 Fase 3) |

**Herança obrigatória:** esta fase **não cria** design system. Reusa:

- Tokens de cor de `web/app/globals.css` (`:root` — kit dark verde).
- Stat editorial de `DashboardKPIs.tsx` (borda superior, sem card) para o resumo do mês.
- Tabela densa de `LancamentosTable.tsx` (10 itens/página, skeleton no loading, `Alert variant="destructive"` no erro).
- Confirmação destrutiva de `DeleteConfirmationDialog.tsx` (Radix `Dialog`, **nunca** `window.confirm`).
- Shell de página de `web/app/page.tsx`: `Sidebar` + `SidebarContent` + `SidebarInset` + `Header` + `main#main-content` + `mx-auto w-full max-w-[1280px]`.
- Componentes shadcn já instalados: `alert`, `alert-dialog`, `badge`, `button`, `card`, `dialog`, `dropdown-menu`, `input`, `label`, `progress`, `scroll-area`, `select`, `sheet`, `skeleton`, `spinner` (`components/Spinner.tsx`), `switch`, `tooltip`.

**Nota de supersessão:** a paleta clara/teal descrita em `03-UI-SPEC.md` (fase 3) foi substituída pelo kit dark verde
que hoje está em `globals.css`. **Este documento usa a paleta viva.** O executor não deve reintroduzir `#0F766E`/`#F8FAFC`.

---

## Spacing Scale

Escala 8 pontos (Tailwind v4, rem → px com `html { font-size: 16px }`). Todos os valores são múltiplos de 4.

| Token | Valor | Usage |
|-------|-------|-------|
| xs | 4px (`gap-1`, `p-1`) | Gap ícone↔texto, `px-1` interno de badge |
| sm | 8px (`gap-2`, `p-2`, `h-8`) | Gap entre label e controle, padding interno de item de lista |
| md | 16px (`gap-4`, `p-4`) | Gap padrão entre controles de um grupo, padding de item de revisão |
| lg | 24px (`gap-6`, `p-6`, `py-6`) | Gap entre seções da página, padding de card, gap entre grupos de itens |
| xl | 32px (`gap-8`, `py-8`) | Separação entre blocos de conteúdo dentro de uma seção |
| 2xl | 48px (`py-12`, `mt-12`) | Break entre seções da página |
| 3xl | 64px | Reservado — não usado nesta fase |

**Exceções (mesmas da Fase 3, mantidas):**

1. **Alvo de toque:** controles interativos tocáveis com **44px** de altura mínima (`h-11`) — dropzone, botão de remover arquivo, linha do checkbox, botão primário do rodapé de confirmação. Alvos dentro de tabela densa (ações de linha) usam `h-8`/`h-10` do shadcn, como já existe no projeto.
2. **Linha de stat do resumo** usa `border-t border-border pt-4` + `gap-y-8` entre colunas (padrão `DashboardKPIs.tsx`), não um card com padding.
3. **Dropzone:** altura mínima `160px` (`min-h-[160px]`) no desktop, `min-h-[200px]` no mobile — não é outlier da escala (altura de componente, não gap).

---

## Typography

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Display (h1 da página, stat principal) | 30px (`text-3xl`) | 600 (`font-semibold`) | 1.15 |
| Heading (h2 de seção, valor de stat secundário) | 20px (`text-xl`) | 600 (`font-semibold`) | 1.2 |
| Body (texto, inputs, botões, células) | 16px (`text-base`) | 400 | 1.5 |
| Label (rótulos, legendas, células de tabela, badges) | 14px (`text-sm`) | 400 | 1.5 |

**Regras:**

- **Exatamente 2 pesos** no conteúdo desta fase: `400` e `600`. O `font-medium` (500) que o `Button` shadcn já aplica por padrão (`text-sm font-medium`) permanece — é o token do componente existente, não um terceiro peso.
- Escala auxiliar já existente e em uso no projeto: `text-xs` (12px) **apenas** dentro de `Badge` (`badge.tsx` usa `text-xs font-semibold`). Não introduzir 12px em texto corrido.
- Números monetários e datas: `tabular-nums` (padrão `t-stat` do `DashboardKPIs`).
- Títulos de seção: `text-lg font-semibold text-foreground` — **exatamente um `<h1>` por página** (a11y). A hierarquia de heading da página é `h1` → `h2` → `h3` (h3 só dentro de card de grupo de itens).
- `line-clamp-2` para lead da página (padrão `hero__lead`).

---

## Color

Paleta **viva** de `web/app/globals.css` (kit dark verde). Não criar cor nova.

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `--background` `#0F3D2E` | Superfície da aplicação, fundo da página |
| Secondary (30%) | `--card` `#0F3D2E` + `--accent` `#164532` | Cards, painel de upload, linhas da tabela, item de nav ativo (`bg-accent`), chips |
| Accent (10%) | `--primary` `#4C956C` | **Reservado para:** botão primário (bg-primary/text-primary-foreground), barra da `Progress` da análise, borda de foco do dropzone em estado de arraste, `Badge variant="default"` do mês de referência |
| Destructive | `--destructive` `#E5484D` | **Somente:** botão destrutivo do diálogo de descarte, `Alert variant="destructive"`, valor de despesa/valor negativo |
| Positive (dinheiro) | `--positive` `#A8D5BA` | Valor de entrada / total recebido / saldo positivo; `Badge variant="success"` |
| Foreground | `--foreground` `#A8D5BA` | Texto principal e títulos |
| Muted foreground | `--muted-foreground` `#9DBBA9` | Legendas, rótulos, texto secundário, placeholders |
| Border | `--border` `#9DBBA9` | Divisórias, borda de card, borda de input, borda da tabela |
| Warning | `Badge variant="warning"` (`bg-yellow-500`) — **já existe** em `badge.tsx` | Alertas não bloqueantes: atypicalo, duplicidade, "revisar categoria", fallback de modelo |
| Ring / Focus | `--ring` `#A8D5BA` | `outline: 2px solid var(--ring)` + `outline-offset: 2px` (já em `globals.css`, `:focus-visible`) |

**Accent reservado para (lista explícita):** botão primário "Analisar PDFs", botão primário "Confirmar importação", preenchimento da `Progress` durante a análise, anel do dropzone quando um arquivo está sendo arrastado, `Badge variant="default"` do período (mês) da importação. **Nunca** aplicar `bg-primary` em linhas de item, ícones decorativos, texto corrido ou cabeçalhos de tabela.

**Nunca usar:** `#0F766E`, `#F8FAFC`, `#DC2626`, `#16A34A`, gradientes, `text-white` cru (exceto via `text-primary-foreground`), roxo/violeta (proibido pelo kit `design-taste-frontend`).

**Regra de elevação (kit dark):** separar blocos por **borda 1px `--border` + diferença de luminância**, não por sombra. `box-shadow` só onde já existe (`LancamentosTable` usa `shadow-sm` no card).

---

## Component Inventory

### 4.1 Reutilizados sem alteração (já instalados)

| Componente | Caminho | Uso nesta fase |
|------------|---------|-----------------|
| `Button` | `components/ui/button.tsx` | CTA primário (`variant="default"`), secundário (`outline`), destrutivo (`destructive`), remover arquivo (`ghost`/`icon-xs`) |
| `Input` | `components/ui/input.tsx` | (não usado na fase — sem campo de texto livre) |
| `Label` | `components/ui/label.tsx` | Rótulo do select de categoria e do limite de upload |
| `Select` | `components/ui/select.tsx` | Select de categoria por item (D-29) |
| `Badge` | `components/ui/badge.tsx` | `outline` (origem, parcela), `warning` (alertas), `success` (receita), `destructive` (despesa) |
| `Alert` / `AlertTitle` / `AlertDescription` / `AlertAction` | `components/ui/alert.tsx` | Erros, avisos, banners de estado |
| `Progress` | `components/ui/progress.tsx` | Progresso da análise (indeterminado por arquivo) |
| `Skeleton` | `components/ui/skeleton.tsx` | Loading da lista de itens e do resumo |
| `ScrollArea` | `components/ui/scroll-area.tsx` | Lista longa de itens (opcional, desktop) |
| `Tooltip` | `components/ui/tooltip.tsx` | Explicação do badge "modelo de fallback" |
| `Dialog` (via `DeleteConfirmationDialog`) | `components/DeleteConfirmationDialog.tsx` | Confirmação de descarte |
| `Spinner` | `components/Spinner.tsx` | Indicador durante análise e confirmação |
| `MONETARY` | `components/DashboardKPIs.tsx` | Formatação BRL (importar, **não** recriar) |
| `Sidebar` / `SidebarContent` / `SidebarInset` | `components/ui/sidebar.tsx` | Shell da página + item de nav "Importar PDF" |
| `Header` | `components/header.tsx` | Cabeçalho com `titulo="Importar PDFs"` |

### 4.2 Novos (a criar nesta fase)

| Componente | Caminho | Tipo | Descrição |
|------------|---------|------|-----------|
| `checkbox` (shadcn) | `web/components/ui/checkbox.tsx` | shadcn/ui (registry oficial) | **Novo a instalar** (`npx shadcn add checkbox`) — não existe no projeto. Usado na marcação de item da revisão (D-29) |
| `Importacoes` page | `web/app/importacamentos/page.tsx` | Server Component | Shell da página + sessão (padrão `web/app/page.tsx`: `Sidebar`, `Header`, `main#main-content`) |
| `ImportacoesClient` | `web/components/ImportacoesClient.tsx` | Client Component | Máquina de estados da página (upload → revisão → resultado), orquestra BFF |
| `UploadDropzone` | `web/components/importacoes/UploadDropzone.tsx` | Client | Dropzone multi-arquivo + validação de tamanho/quantidade/tipo (D-04, D-28) |
| `ImportFileList` | `web/components/importacoes/ImportFileList.tsx` | Client | Lista de arquivos com estado por arquivo (aguardando / analisando / pronto / ignorado / duplicado / erro) |
| `RevisaoItens` | `web/components/importacoes/RevisaoItens.tsx` | Client | Grupos por tipo + linha de item (checkbox, data, descrição, select categoria, valor, badges) |
| `ImportItemRow` | `web/components/importacoes/ImportItemRow.tsx` | Client | Linha de item (desktop: linha de tabela; mobile: bloco) |
| `ResumoMesImportacao` | `web/components/importacoes/ResumoMesImportacao.tsx` | Client | Stat editorial: total recebido, total já pago, total comprometido, saldo previsto + total da fatura e vencimento (D-25) |
| `AlertasImportacao` | `web/components/importacoes/AlertasImportacao.tsx` | Client | Lista de alertas (duplicidade, atípico, divergência, sem texto, revisar categoria, rejeitados) |
| `ResultadoImportacao` | `web/components/importacoes/ResultadoImportacao.tsx` | Client | Estado de confirmação (o que foi gravado) e de falha |
| `ImportacoesPendentes` | `web/components/importacoes/ImportacoesPendentes.tsx` | Client | Histórico de importações pendentes + ação "Descartar" (D-2xx, `DELETE /api/importacoes/{id}`) |

### 4.3 Alterações em componentes existentes (contratadas)

| Arquivo | Alteração |
|---------|-----------|
| `web/components/ui/sidebar.tsx` | Adicionar `{ href: "/importacamentos", label: "Importar PDF", icon: FileUp }` ao array `sidebarItems`, **após** "Categorias". Ícone `FileUp` do lucide-react. Estado ativo automático via `pathname === item.href` |
| `web/components/TopNav.tsx` | Manter em sincronia: adicionar `{ href: "/importacamentos", label: "Importar PDF" }` ao array `ROTAS` (componente hoje sem uso nas páginas — D-31 cita TopNav; a navegação viva é a Sidebar) |
| `web/components/DeleteConfirmationDialog.tsx` | Ampliar `type Action = "excluir" | "desativar" | "descartar"` e adicionar prop opcional `descricao?: string` (quando presente, substitui o texto padrão). Rótulo do botão para "descartar" = **"Descartar"**; título = **"Confirmar descarte"**. Texto padrão gerado: "Tem certeza que deseja descartar esta importação pendente?" — **nunca** `window.confirm` |
| `web/lib/api-client.ts` | Novas funções client: `analisarPDFs(files)`, `confirmarImportacao(id, payload)`, `listarImportacoes()`, `descartarImportacao(id)`. Usar o mesmo padrão de `chatComIA`/`gravarLancamentos`: `fetch("/api/importacoes…")`, `cache: "no-store"`, ler `detail` do ProblemDetail (RFC 7807) para a mensagem |

### 4.4 Novas dependências

| Pacote | Propósito | Status |
|--------|-----------|--------|
| `@radix-ui/react-checkbox` | shadcn `checkbox` | **Instalar** via `npx shadcn add checkbox` (registry oficial shadcn) |

---

## Layout & Navigation

### 5.1 Rota

| Rota | Título (h1) | Acesso | Descrição |
|------|-------------|--------|-----------|
| `/importacamentos` | "Importar PDFs" | Autenticado (qualquer role; escrita permitida — igual a `/lancamentos`) | Upload múltiplo → revisão → confirmação → histórico |

- Server Component com `export const instant = false` (depende de cookies/searchParams), igual a `web/app/page.tsx`.
- Sessão: `getMe()` → `redirect("/login?expirada=1")` se ausente.
- Sem `?mes=` — o mês vem do próprio documento importado (fatura/extrato), não do seletor global.

### 5.2 Shell da página (copiar de `web/app/page.tsx`)

```
<Sidebar>
  <SkipLink />
  <SidebarContent />
  <SidebarInset>
    <div className="flex h-full w-full flex-col overflow-hidden">
      <Header titulo="Importar PDFs" />
      <main id="main-content" className="flex-1 overflow-y-auto bg-background px-4 py-6 md:px-8 lg:px-10">
        <div className="mx-auto w-full max-w-[1280px]">
          …seções…
        </div>
      </main>
    </div>
  </SidebarInset>
</Sidebar>
```

- **Um único `<h1>`** por página: "Importar PDFs" (visível a partir de `md:`, `sr-only` no mobile — padrão do `hero-h` do dashboard).
- Lead da página (`hero__lead`): **"Envie faturas de cartão e extratos bancários em PDF. A análise monta os lançamentos do mês para você revisar antes de gravar."**
- Seções da página, nesta ordem e com estes `<h2>` exatos:
  1. `Enviar PDFs` — dropzone + lista de arquivos + CTA
  2. `Resumo do mês` — stats + total da fatura/vencimento
  3. `Alertas` — lista de alertas
  4. `Itens para revisar` — 3 grupos (Receitas / Despesas pagas / Contas a pagar)
  5. `Importações pendentes` — histórico + descarte
- A seção 1 só aparece no estado **upload**; 2–4 só no estado **revisão**; 5 aparece em todos os estados (é o histórico).
- Toda a sessão da página dentro de `<section className="py-6 md:py-8">` com `<h2 className="mb-6 text-lg font-semibold text-foreground">`.

### 5.3 Navegação

- Item **"Importar PDF"** na Sidebar, após "Categorias" (ícone `FileUp`).
- Rótulo no singular ("Importar PDF") conforme D-31; o `Header` da página usa o plural ("Importar PDFs") porque é o nome da ação/tela.
- Estado ativo: `bg-accent text-accent-foreground` (já gerado por `SidebarNav` via `pathname === item.href`).

---

## Screen Contracts

### 6.0 Máquina de estados

```
[envio] ──"Analisar PDFs"──▶ [analisando] ──200──▶ [revisão] ──"Confirmar importação"──▶ [confirmando] ──200──▶ [resultado]
   ▲                             │                     │                                  │
   │                             └──503/erro──▶ [erro]  └──erro──▶ [revisão com erro]     └──erro──▶ [erro na confirmação]
   └──── "Nova importação" / "Analisar outro arquivo" ◀── [resultado]
```

- Uma **única** instância de análise por vez; botão desabilitado durante `analisando`/`confirmando` (evita duplo envio).
- Transição de estado move o foco programmaticamente para o `<h2>` da nova seção (`tabIndex={-1}`) e anuncia em `aria-live="polite"`.

### 6.1 Estado A — Envio (`[envio]`)

**Dropzone** (`UploadDropzone`)

- Área clicável + arrastar-e-soltar, `min-h-[160px]`, borda `border-2 border-dashed border-border`, `rounded-lg`, `bg-card`, `text-center`.
- Ícone `FileUp` (lucide) `h-8 w-8 text-muted-foreground`, `aria-hidden="true"`.
- Título dentro da área: **"Arraste os PDFs aqui"**; subtexto: **"ou clique para escolher arquivos"**.
- Botão de escolha: `Button variant="outline"` **"Selecionar PDFs"**.
- `<input type="file" accept="application/pdf,.pdf" multiple className="sr-only">` — **nunca** `accept="*/*"`.
- Estado de arraste: `border-primary bg-primary/5` + texto **"Solte os arquivos para adicionar"**.
- Limite exibido sob a área, ligado por `aria-describedby`: **"Até 5 PDFs por importação, 4 MB por arquivo."**

**Lista de arquivos** (`ImportFileList`)

Cada arquivo em uma linha (`flex items-center justify-between gap-4 border-b border-border py-3`):

| Coluna | Conteúdo |
|--------|----------|
| Nome | `text-sm font-medium text-foreground` + `truncate` + `title` para o nome completo |
| Tamanho | `text-sm text-muted-foreground` — "2,4 MB" / "812 KB" (`(bytes / 1024 / 1024).toFixed(1)` com vírgula decimal pt-BR; abaixo de 1 MB, em KB) |
| Origem inferida | `Badge variant="outline"` — **"Fatura de cartão"** ou **"Extrato bancário"** (heurística pelo nome do arquivo; se ambíguo, **"Documento"**) |
| Estado | `Badge` + `aria-live="polite"` — `outline` "Aguardando", `default` "Analisando" (com `Spinner`), `success` "Pronto", `warning` "Ignorado", `destructive` "Erro" |
| Ação | `Button variant="ghost" size="icon-xs"` com ícone `X` (lucide), `aria-label="Remover {nome do arquivo}"` |

**Validação client-side (antes de qualquer chamada — D-04, D-28):**

| Regra | Comportamento | Mensagem (por arquivo, `Alert variant="destructive"` sob a linha) |
|-------|---------------|------------------------------------------------------|
| Extensão/tipo ≠ PDF | Arquivo rejeitado na hora da seleção | **"Este arquivo não é um PDF. Envie faturas de cartão ou extratos bancários em PDF."** |
| Tamanho > 4 MB (4 194 304 bytes) | Arquivo rejeitado na hora da seleção | **"O arquivo {nome} tem {tamanho} e o limite é 4 MB por arquivo. Envie um PDF menor."** |
| Mais de 5 arquivos | Arquivo excedente rejeitado | **"Você pode enviar até 5 PDFs por vez. Remova um arquivo para continuar."** |
| Soma > 4,5 MB (limite do corpo da requisição, Vercel) | Envio bloqueado | **"O envio somaria {tamanho} e o limite é 4,5 MB por requisição. Envie menos arquivos."** |

- Arquivo inválido **não** entra na lista; a mensagem aparece em `Alert variant="destructive"` no topo da lista.
- Se **nenhum** arquivo válido restar: o botão **"Analisar PDFs"** fica `disabled` e a helper text abaixo dele diz: **"Selecione ao menos um PDF para analisar."**

**CTA e rodapé da seção**

- Primário: `Button variant="default"` **"Analisar PDFs"** (`h-11`, `w-full sm:w-auto`), desabilitado se 0 arquivos válidos.
- Secundário: `Button variant="ghost"` **"Limpar lista"** — só aparece com ≥1 arquivo.
- Abaixo do CTA, `text-sm text-muted-foreground`: **"Nenhum lançamento é gravado antes da sua confirmação."** (transparência IMP-09).

**Estado "analisando"**

- `Progress` (`bg-primary`) + `Spinner` + `text-sm text-muted-foreground`: **"Analisando {n} de {total} arquivos…"**.
- Skeletons no lugar do conteúdo de revisão (3 linhas de skeleton, `h-10 w-full rounded-md bg-muted animate-pulse`).
- Botão "Analisar PDFs" em estado loading: `disabled` + `Spinner` + rótulo **"Analisando…"**.

### 6.2 Estado B — Revisão (`[revisão]`)

**Cabeçalho da seção**

- `<h2>` "Resumo do mês" + `text-sm text-muted-foreground` com os documentos: **"2 documentos · fatura de cartão e extrato bancário"** (ou contagem genérica **"{n} documentos analisados"**).

**Stats (`ResumoMesImportacao`)** — padrão editorial de `DashboardKPIs.tsx` (borda superior, sem card), `grid grid-cols-1 gap-x-6 gap-y-8 md:grid-cols-4`:

| Stat | Rótulo | Valor | Cor |
|------|--------|-------|-----|
| 1 | **"Total recebido"** | `R$ 0,00` | `text-positive` |
| 2 | **"Total já pago"** | `R$ 0,00` | `text-destructive` |
| 3 | **"Total comprometido a pagar"** | `R$ 0,00` | `text-foreground` |
| 4 | **"Saldo previsto"** | `R$ 0,00` | `text-positive` se ≥ 0, `text-destructive` se < 0, `text-muted-foreground` se = 0 |

- Valores em `text-xl font-semibold tabular-nums` (`t-stat`), legendas em `text-sm text-muted-foreground`.
- **Os totais são recalculados a partir dos itens marcados.** Ao desmarcar um item, o valor sai do total imediatamente. Legenda do stat 1: **"Receitas identificadas no período"**; stat 2: **"Despesas já quitadas"**; stat 3: **"Contas a pagar em aberto"**; stat 4: **"Receitas menos despesas pagas"**.
- **Regra do centavo:** a diferença entre a soma dos itens marcados e o total exibido é sempre 0,00 (arredondamento do mesmo `BigDecimal` em scale 2 — nunca `float`).
- **Total da fatura** (abaixo dos stats, linha própria `border-t border-border pt-4 mt-6`): **"Total da fatura"** + valor em `text-xl font-semibold tabular-nums` + **"Vencimento: 10/12/2026"** (data `dd/mm/aaaa`). Se não houver fatura no lote, a linha não aparece.
- Se a **soma dos itens marcados ≠ total da fatura** (|diferença| > 0,05), o valor da fatura recebe `Badge variant="warning"` **"Divergência de {R$ X}"**.

**Alertas (`AlertasImportacao`)**

- Lista de itens, cada um em `Alert` (variant `default`) com `AlertTitle` + `AlertDescription`, `Badge variant="warning"` à direita quando for aviso.
- Título da seção `<h2>` "Alertas" + contador `Badge variant="outline"` com o número de alertas (ex.: "3").
- Cada alerta tem um `id` estável e um `kind` (`DUPLICIDADE` | `GASTO_ATIPICO` | `DIVERGENCIA_SOMA` | `PAGAMENTO_FATURA` | `SEM_TEXTO` | `REVISAR_CATEGORIA` | `ITEM_REJEITADO` | `ARQUIVO_DUPLICADO`).
- Ao clicar no alerta (quando `kind` tem alvo), o item correspondente recebe foco/scroll (âncora por `id` do item) — o alerta de "revisar categoria" destaca os itens afetados.

Copywriting dos alertas (texto final, obrigatório):

| `kind` | Título | Descrição |
|-------|--------|-----------|
| `ARQUIVO_DUPLICADO` | "Arquivo já importado" | "{nome do arquivo} já foi importado em {dd/mm/aaaa}. Ele não será processado de novo." |
| `SEM_TEXTO` | "PDF sem texto extraível" | "{nome do arquivo} é digitalizado e não tem texto extraível. Ele foi ignorado nesta análise — envie a versão digital do PDF." |
| `DUPLICIDADE` | "Possível duplicidade" | "{n} lançamentos têm a mesma data, valor e descrição. Revise se são compras distintas." |
| `GASTO_ATIPICO` | "Gasto atípico" | "{descrição} de {R$ valor} está acima do padrão da categoria {categoria} neste período." |
| `DIVERGENCIA_SOMA` | "Soma diferente do total da fatura" | "A soma dos itens ({R$ soma}) não bate com o total da fatura ({R$ total}). Diferença de {R$ diferença}." |
| `PAGAMENTO_FATURA` | "Pagamento da própria fatura" | "O pagamento desta fatura aparece no extrato. Confira se não é a mesma compra sendo contada duas vezes." |
| `REVISAR_CATEGORIA` | "Categoria para revisar" | "{n} itens usam a categoria padrão porque a IA não reconheceu a categoria original. Escolha a categoria certa na lista abaixo." |
| `ITEM_REJEITADO` | "Itens ignorados pela leitura" | "{n} itens não puderam ser lidos e foram ignorados. Nada foi gravado a partir deles." |

- Quando não houver nenhum alerta: **não** renderizar a seção — em vez disso, uma linha `text-sm text-muted-foreground`: **"Nenhum alerta nesta importação."** (a seção `Alertas` continua visível com o h2, evitando salto de layout).

**Itens para revisar (`RevisaoItens`)**

- `<h2>` "Itens para revisar" + `Badge variant="outline"` com a contagem **"{n} itens"**.
- Três grupos, **nesta ordem e com estes títulos exatos**:
  1. `Receitas` (badge `success`, subtotal `text-positive`)
  2. `Despesas pagas` (badge `destructive`, subtotal `text-destructive`)
  3. `Contas a pagar` (badge `outline`, subtotal `text-foreground`)
- Cada grupo é um `Card` (`rounded-lg border border-border bg-card p-4 md:p-6`), com `CardHeader` contendo `<h3 className="text-base font-semibold">`, o badge do tipo e o subtotal do grupo (`text-sm tabular-nums`).
- Se um grupo não tiver itens, **não renderizar o grupo** (sem card vazio).

**Linha de item (`ImportItemRow`)** — desktop (≥768px), linha de tabela densa no padrão de `LancamentosTable`:

| Coluna | Conteúdo |
|--------|----------|
| Marcação | `Checkbox` shadcn (`h-4 w-4`) com `aria-label="Incluir {descrição} de {R$ valor} na importação"`; wrapper clicável de 44px (`py-2` + `px-1`) |
| Data | `text-sm text-muted-foreground tabular-nums` — `dd/mm/aaaa` |
| Descrição | `text-sm text-foreground` + `truncate` + `title` |
| Categoria | `Select` shadcn (trigger `h-8`, `w-full min-w-[160px]`, `text-sm`) com `aria-label="Categoria de {descrição}"` |
| Valor | `text-sm font-semibold tabular-nums` — `text-positive` para `RECEITA`, `text-destructive` para `DESPESA_PAGA`, `text-foreground` para `A_PAGAR`; **sempre** `R$` com 2 casas |
| Badges | `Badge variant="outline"` "Cartão"/"Extrato"; `Badge variant="outline"` "Parcela 2/10" (só `A_PAGAR`); `Badge variant="warning"` conforme item 6.4 |

- Item **desmarcado**: `opacity-60` no conteúdo + linha com `bg-muted/30`; o checkbox continua visível e clicável; o item **não** é removido da lista (permite religar — D-29).
- **Select de categoria (D-24/D-29):**
  - Opções = categorias reais do tenant, filtradas por tipo: `RECEITA` → categorias `ENTRADA`; `DESPESA_PAGA` e `A_PAGAR` → categorias `SAÍDA`.
  - Valor inicial = categoria mapeada pela IA (ou a categoria de fallback "Outros"/"Outras receitas").
  - Ao trocar a categoria manualmente, o badge **"Revisar categoria"** do item **desaparece** e o alerta `REVISAR_CATEGORIA` é recalculado (se nenhum item restante precisar de revisão, o alerta sai da lista).
  - Item sem categoria selecionada: `SelectValue placeholder="Selecionar"` e o item conta como **não confirmável** — aparece a mensagem sob a linha: **"Escolha uma categoria para incluir este item."**
- **Badges de sinalização (D-21, D-25):**

| Badge | Quando | Texto |
|-------|--------|-------|
| Fallback de modelo | `degradado === true` no documento | **"Extraído com modelo de fallback"** + `Tooltip` (texto: **"Este documento foi analisado por um modelo de reserva, menos preciso. Confira os valores antes de confirmar."**) |
| Categoria para revisar | a IA não reconheceu a categoria | **"Revisar categoria"** |
| Gasto atípico | item acima do limiar da categoria | **"Gasto atípico"** |
| Possível duplicata | item repetido no lote | **"Possível duplicata"** |
| Parcela | `A_PAGAR` com `parcelaAtual`/`parcelaTotal` | **"Parcela 2/10"** |

- **Rodapé da seção (barra de confirmação)** — `sticky bottom-0` dentro do `main`, `border-t border-border bg-background/95 backdrop-blur px-4 py-3`:
  - Esquerda: `text-sm text-muted-foreground` — **"{n} de {total} itens marcados · {R$ total}"**.
  - Direita: `Button variant="ghost"` **"Analisar outro arquivo"** e `Button variant="default"` **"Confirmar importação"** (`h-11`).
  - "Confirmar importação" **disabled** com 0 itens marcados; helper text abaixo: **"Marque ao menos um item para confirmar."**
  - Durante a confirmação: `disabled` + `Spinner` + rótulo **"Confirmando…"**; todos os checkboxes e selects da página ficam `disabled` (evita corrida).

### 6.3 Estado C — Resultado (`[resultado]`)

**Sucesso**

- `Alert` (variant `default`) com `AlertTitle` **"Importação confirmada"** e `AlertDescription` **"{n} lançamentos e {m} contas a pagar foram gravados. Nada mais será criado se você confirmar de novo."** (idempotência, IMP-10).
- Bloco `Resumo` reaproveitando `ResumoMesImportacao` com os mesmos 4 stats.
- `Button variant="default"` **"Ver lançamentos"** (link para `/lancamentos`) e `Button variant="outline"` **"Nova importação"** (volta ao estado `[envio]` e limpa a lista local).
- Se houve itens desmarcados, uma linha `text-sm text-muted-foreground`: **"{k} itens não foram importados porque você desmarcou."**
- `aria-live="polite"` anuncia o sucesso.

**Falha na confirmação**

- `Alert variant="destructive"` com `AlertTitle` **"Não foi possível confirmar"** e `AlertDescription` **"Nenhum lançamento foi gravado. Verifique a importação e tente novamente."** + `Button variant="outline"` **"Tentar novamente"**.
- A tela volta ao estado `[revisão]` com a seleção preservada (o usuário não perde o trabalho).

### 6.4 Estado de erro da análise (503 / indisponibilidade — D-22)

- `Alert variant="destructive"` no topo da seção "Enviar PDFs":
  - `AlertTitle` **"A análise por IA está indisponível"**
  - `AlertDescription` **"Nenhum dado foi gravado. A IA gratuita está sobrecarregada agora — tente novamente em alguns minutos."**
- `Button variant="outline"` **"Tentar novamente"** reenvia **os mesmos arquivos** (mantidos em memória, `File` objects) — sem obrigar o usuário a selecionar de novo.
- Se a falha for de cota/limite (429/402 traduzido): `AlertTitle` **"A análise está sobrecarregada"** + descrição **"O serviço de IA gratuito atingiu o limite de requisições. Nenhum dado foi gravado — tente novamente em alguns minutos."**
- Se a falha for de rede: `AlertTitle` **"Não foi possível enviar os arquivos"** + descrição **"Verifique sua conexão e tente novamente. Nenhum dado foi gravado."**

### 6.5 Importações pendentes (histórico)

- Seção `<h2>` **"Importações pendentes"**, sempre visível.
- Tabela (padrão `LancamentosTable`): colunas **Arquivo** | **Data** | **Itens** | **Estado** | **Ações**.
- Estado: `Badge variant="warning"` **"Aguardando revisão"** · `Badge variant="success"` **"Confirmada"** · `Badge variant="outline"` **"Descartada"**.
- Ação **"Descartar"** (`Button variant="ghost" size="sm"`, `text-destructive`) — visível só em importações **pendentes** (nunca em "Confirmada"/"Descartada").
- **Confirmação destrutiva (nunca `window.confirm`)** — `DeleteConfirmationDialog` com `action="descartar"`:
  - Título: **"Confirmar descarte"**
  - Descrição: **"Tem certeza que deseja descartar esta importação pendente? Os itens revisados serão perdidos e o arquivo poderá ser enviado de novo."**
  - Botões: **"Descartar"** (`variant="destructive"`) | **"Cancelar"** (`variant="ghost"`)
- Após o descarte: a linha sai da lista e um `Alert` de sucesso **"Importação descartada."**
- Se não houver importações: `text-sm text-muted-foreground` centralizado em `p-12`: **"Nenhuma importação por aqui ainda."** com subtexto **"Envie a fatura do cartão ou o extrato bancário em PDF para começar."**

---

## Copywriting Contract (pt-BR)

### 7.1 CTAs e botões

| Contexto | Rótulo | Variante |
|----------|--------|----------|
| Abrir seletor de arquivos | **"Selecionar PDFs"** | outline |
| Enviar para análise | **"Analisar PDFs"** | default (primary) |
| Em análise | **"Analisando…"** | default + Spinner, disabled |
| Remover arquivo da lista | **"Remover"** (aria-label: "Remover {nome}") | ghost / icon-xs |
| Esvaziar a lista | **"Limpar lista"** | ghost |
| Confirmar gravação | **"Confirmar importação"** | default (primary) |
| Em confirmação | **"Confirmando…"** | default + Spinner, disabled |
| Refazer a análise | **"Analisar outro arquivo"** | ghost |
| Ver os lançamentos gravados | **"Ver lançamentos"** | default |
| Nova importação | **"Nova importação"** | outline |
| Tentar novamente (erro) | **"Tentar novamente"** | outline |
| Descartar importação pendente | **"Descartar"** | ghost (`text-destructive`) |
| Confirmar descarte | **"Descartar"** | destructive (no diálogo) |
| Cancelar | **"Cancelar"** | ghost |
| Erro genérico de API | **"Tentar novamente"** | outline |

### 7.2 Títulos de tela e seções

| Elemento | Texto |
|----------|-------|
| h1 da página | **"Importar PDFs"** |
| Lead da página | **"Envie faturas de cartão e extratos bancários em PDF. A análise monta os lançamentos do mês para você revisar antes de gravar."** |
| h2 seção 1 | **"Enviar PDFs"** |
| h2 seção 2 | **"Resumo do mês"** |
| h2 seção 3 | **"Alertas"** |
| h2 seção 4 | **"Itens para revisar"** |
| h2 seção 5 | **"Importações pendentes"** |
| Título do dropzone | **"Arraste os PDFs aqui"** |
| Subtítulo do dropzone | **"ou clique para escolher arquivos"** |
| Limite do dropzone | **"Até 5 PDFs por importação, 4 MB por arquivo."** |
| Grupo de itens 1 | **"Receitas"** |
| Grupo de itens 2 | **"Despesas pagas"** |
| Grupo de itens 3 | **"Contas a pagar"** |

### 7.3 Empty states

| Estado | Headline | Body (+ próximo passo) |
|--------|----------|-----------------------|
| Sem importações (histórico) | **"Nenhuma importação por aqui ainda."** | **"Envie a fatura do cartão ou o extrato bancário em PDF para começar."** |
| Nenhum item identificado | **"Nenhum lançamento identificado."** | **"A IA não encontrou transações nos PDFs enviados. Confira se o arquivo tem texto selecionável e tente de novo."** |
| Nenhum item marcado | (sem headline; helper do botão) | **"Marque ao menos um item para confirmar."** |
| Nenhum arquivo válido | (no dropzone) | **"Selecione ao menos um PDF para analisar."** |
| Sem alertas | — | **"Nenhum alerta nesta importação."** |

### 7.4 Estados de erro (copy final)

| Situação | Título | Mensagem |
|----------|--------|----------|
| Arquivo não é PDF | — | **"Este arquivo não é um PDF. Envie faturas de cartão ou extratos bancários em PDF."** |
| Arquivo > 4 MB | — | **"O arquivo {nome} tem {tamanho} e o limite é 4 MB por arquivo. Envie um PDF menor."** |
| Mais de 5 arquivos | — | **"Você pode enviar até 5 PDFs por vez. Remova um arquivo para continuar."** |
| Soma > 4,5 MB | — | **"O envio somaria {tamanho} e o limite é 4,5 MB por requisição. Envie menos arquivos."** |
| IA indisponível (503) | **"A análise por IA está indisponível"** | **"Nenhum dado foi gravado. A IA gratuita está sobrecarregada agora — tente novamente em alguns minutos."** |
| IA sobrecarregada (429/402) | **"A análise está sobrecarregada"** | **"O serviço de IA gratuito atingiu o limite de requisições. Nenhum dado foi gravado — tente novamente em alguns minutos."** |
| Erro de rede | **"Não foi possível enviar os arquivos"** | **"Verifique sua conexão e tente novamente. Nenhum dado foi gravado."** |
| Falha na confirmação | **"Não foi possível confirmar"** | **"Nenhum lançamento foi gravado. Verifique a importação e tente novamente."** |
| Sessão expirada | — | **"Sua sessão expirou. Entre novamente."** (redirect `/login?expirada=1`) |
| Erro genérico | — | **"Não foi possível carregar os dados. Tente novamente em instantes."** (mesma string da Fase 3) |

### 7.5 Mensagens de campo

| Campo | Mensagem |
|-------|----------|
| Categoria não selecionada | **"Escolha uma categoria para incluir este item."** |
| Item sem categoria é ignorado no envio | (sem toast — o item fica com `SelectValue placeholder="Selecionar"`) |

### 7.6 Confirmação destrutiva

| Ação | Título do diálogo | Texto | Botão confirmar | Botão cancelar |
|------|-------------------|-------|-----------------|----------------|
| Descartar importação pendente | **"Confirmar descarte"** | **"Tem certeza que deseja descartar esta importação pendente? Os itens revisados serão perdidos e o arquivo poderá ser enviado de novo."** | Descartar | Cancelar |

> Regras: **nunca** `window.confirm`; o diálogo é `DeleteConfirmationDialog` (Radix `Dialog`) com foco preso; o botão destrutivo é o da esquerda em mobile e o da direita em desktop (padrão `flex-col-reverse sm:flex-row sm:justify-end` já existente no componente).

### 7.7 Feedback de sucesso

| Situação | Texto |
|----------|-------|
| Importação confirmada | **"Importação confirmada"** — *"{n} lançamentos e {m} contas a pagar foram gravados. Nada mais será criado se você confirmar de novo."* |
| Importação descartada | **"Importação descartada."** |

---

## Accessibility (WCAG AA)

- **Foco:** `outline: 2px solid var(--ring)` + `offset 2px`, só teclado (`:focus-visible`) — já em `globals.css`; não remover.
- **Dropzone acessível por teclado:** `role="button"`, `tabIndex={0}`, `onKeyDown` tratando `Enter` e `Space` para abrir o seletor de arquivos; `aria-label="Selecionar PDFs"`; `aria-describedby` apontando para o texto de limites.
- **Checkbox por item:** `aria-label="Incluir {descrição} de {R$ valor} na importação"`. Estado marcado/desmarcado é o estado nativo do Radix Checkbox.
- **Select de categoria:** `aria-label="Categoria de {descrição}"`; quando sem categoria, `SelectValue placeholder="Selecionar"`.
- **Labels:** todo controle com rótulo visível ou `aria-label`; erros de campo com `aria-invalid` + `aria-describedby`.
- **Região viva:** `aria-live="polite"` para: mudança de estado da análise, contagem de itens marcados, mensagens de sucesso/erro. `role="alert"` nos `Alert` destrutivos (já no `alert.tsx`).
- **Foco entre estados:** ao passar para revisão, mover o foco para o `<h2>` "Resumo do mês" (`tabIndex={-1}`); ao confirmar, mover para o `AlertTitle` de sucesso.
- **Alvos de toque:** ≥ 44px nos controles principais (dropzone, remover arquivo, linha do checkbox, botões do rodapé de confirmação).
- **Contraste:** a paleta do kit já atende AA em dark (foreground `#A8D5BA` sobre `#0F3D2E`, muted `#9DBBA9` sobre `#0F3D2E`). Badge `warning` (amarelo sobre texto escuro) mantém contraste do kit existente.
- **Headings:** um `<h1>` por página; hierarquia `h1 → h2 → h3` sem pular níveis (h3 só nos cards de grupo).
- **Tabelas:** no mobile (390px) a linha de item vira bloco (não tabela rolável) — evita `off-viewport` do kit; no desktop ≥768px é linha de tabela com `th` escopo `col`.
- **Ícones decorativos** com `aria-hidden="true"` (padrão `Spinner`).

---

## Responsive Behavior

| Breakpoint | Comportamento |
|------------|---------------|
| < 768px (mobile) | Stats em 1 coluna; cada item da revisão vira **bloco empilhado** (checkbox + data na 1ª linha, descrição, select full-width, valor à direita); grupos empilhados; rodapé de confirmação com botões `w-full` empilhados (`flex-col-reverse`); dropzone `min-h-[200px]`; histórico em lista (não tabela) |
| ≥ 768px (md) | Stats em 4 colunas (`md:grid-cols-4`); itens em linhas de tabela; rodapé de confirmação em linha, botões à direita; histórico em tabela |
| ≥ 1024px (lg) | Conteúdo centralizado em `max-w-[1280px]` com `lg:px-10` (padrão do shell) |
| 320px | Nenhum overflow horizontal: nomes de arquivo e descrições com `truncate`; valores monetários com `tabular-nums` e sem quebra |

- Altura mínima do botão principal: `h-11` em todas as larguras.
- Sem `overflow-x` em tabela de itens no mobile (a linha vira bloco).

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official (`ui.shadcn.com`) | `checkbox` (novo, `npx shadcn add checkbox`) | **not required** — registry oficial do shadcn; estilo `radix-nova` idêntico ao `components.json` |
| shadcn official (já instalados, sem alteração) | `alert`, `alert-dialog`, `badge`, `button`, `card`, `dialog`, `dropdown-menu`, `input`, `label`, `progress`, `scroll-area`, `select`, `sheet`, `skeleton`, `switch`, `tooltip` | not required |
| Terceiros | **nenhum** — `components.json` mantém `"registries": {}` | — |

- **Nenhum registry third-party** é declarado nesta fase ⇒ o gate de vetting (`npx shadcn view`) não se aplica.
- Nenhum pacote npm novo além do `@radix-ui/react-checkbox` (dependência oficial do shadcn `checkbox`).
- **Não alterar** os arquivos do kit (`design-taste-frontend`) para "limpar" um check visual.

---

## Pre-População e Fontes

| Seção | Fonte | Decisões usadas |
|-------|-------|-----------------|
| Design System | `web/components.json`, `web/app/globals.css`, `web/app/layout.tsx` | Preset `radix-nova`, radix, lucide, `system-ui`, tokens vivos dark verde |
| Spacing | `03-UI-SPEC.md` §1.1 + classes Tailwind em uso no projeto | Escala 8 pontos, touch target 44px |
| Typography | `web/components/DashboardKPIs.tsx`, `web/components/header.tsx`, `globals.css` | 14/16/20/30, pesos 400/600, `tabular-nums` |
| Color | `globals.css` `:root` + `@theme inline` | `#0F3D2E` / `#164532` / `#4C956C` / `#E5484D` / `#A8D5BA` / `#9DBBA9` |
| Component Inventory | `web/components/ui/*`, `web/components/*` (código vivo) | Reutilização; `checkbox` novo; extensões em `sidebar.tsx`, `TopNav.tsx`, `DeleteConfirmationDialog.tsx`, `api-client.ts` |
| Layout & Navigation | `05-CONTEXT.md` D-03, D-31; `web/app/page.tsx` | Rota `/importacamentos`, item "Importar PDF", shell Sidebar+Header |
| Tela de envio | `05-CONTEXT.md` D-01, D-04, D-28; IMP-01, IMP-15 | Multipart, validação de tipo/tamanho/quantidade, limites 4 MB / 5 arquivos |
| Tela de revisão | `05-CONTEXT.md` D-02, D-21, D-24, D-25, D-26, D-29; IMP-04, IMP-05, IMP-09 | Agrupamento por tipo, checkbox, select de categoria, badges, stats, alertas |
| Tela de resultado | `05-CONTEXT.md` D-07, D-22; IMP-10 | Idempotência, 503, nada gravado antes da confirmação |
| Histórico / descarte | `05-CONTEXT.md` (D-02, staging) + `03-UI-SPEC.md` §5.4 | `DELETE /api/importacoes/{id}` com confirmação explícita |
| Copywriting | **Redigido nesta sessão** (run não-interativo) | Todos os rótulos, estados vazios, erros, alertas e confirmações acima |
| Accessibility | `03-UI-SPEC.md` §8 + `globals.css` | Foco, labels, alvos de toque, aria-live, h1 único |
| Responsive | `03-UI-SPEC.md` §9 + `LancamentosTable.tsx` | Colapso da linha de item em bloco no mobile |
| Registry Safety | `web/components.json` (`registries: {}`) | Só registry oficial; `checkbox` via `npx shadcn add checkbox` |

**Campos marcados como `default` (sem resposta em CONTEXT/REQUIREMENTS/UI-SPEC da Fase 3):** layout exato da tela de revisão (discretion explícita em D-25/D-29), critério visual de "gasto atípico" exibido como badge, ordem dos grupos e rótulos dos stats, copy final de todos os estados, partição da revisão em 3 estados com barra de confirmação sticky, e a extensão do `DeleteConfirmationDialog` com a prop `descricao`.

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS

**Approval:** pending
