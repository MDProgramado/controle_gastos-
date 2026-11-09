---
phase: 03-dashboard
plan: 04
subsystem: web
tags:
  - CRUD
  - lançamentos
  - categorias
  - TopNav
  - DeleteConfirmationDialog
  - role-based
  - ADMIN-only
dependency-graph:
  requires: ["03-03"]
  provides: ["03-04"]
  affects: []
tech-stack:
  added:
    - "@radix-ui/react-dialog" (DeleteConfirmationDialog)
  patterns:
    - Server Component com `export const instant = false` + `getMe()` validacao de sessao
    - Server Actions com `"use server"` para mutacoes (cookie sess + Bearer token)
    - Client components controlados (padrao LoginForm, sem RHF/zod)
    - TopNav compartilhado nas 3 rotas com item ativo #0F766E
key-files:
  created:
    - path: "web/app/lancamentos/page.tsx"
      provides: "Pagina CRUD de lançamentos: listagem, criar, editar, excluir"
    - path: "web/app/categorias/page.tsx"
      provides: "Pagina CRUD de categorias: listagem, criar, editar, desativar (soft delete)"
    - path: "web/components/TopNav.tsx"
      provides: "Navegacao top entre as 3 rotas + LogoutButton"
    - path: "web/components/DeleteConfirmationDialog.tsx"
      provides: "Dialogo reutilizavel de confirmacao destrutiva (sem window.confirm)"
    - path: "web/components/LancamentosClient.tsx"
      provides: "Cliente interativo para lancamentos (tabela, form, dialog, alertas)"
    - path: "web/components/CategoriasClient.tsx"
      provides: "Cliente interativo para categorias (tabela, form, dialog, alertas)"
  modified:
    - path: "web/lib/api.ts"
      provides: "Funca̧ões CRUD tipadas: getLancamentos, criarLancamento, editarLancamento, excluirLancamento, getCategorias, criarCategoria, editarCategoria, desativarCategoria"
    - path: "web/app/page.tsx"
      provides: "Dashboard com TopNav integrado"
    - path: "web/app/lancamentos/page.tsx"
      provides: "TopNav integrado na pagina de lancamentos"
    - path: "web/app/categorias/page.tsx"
      provides: "TopNav integrado na pagina de categorias"
    - path: "web/components/MonthSelector.tsx"
      provides: "Correcao setState-in-effect (lint)"
decisions:
  - "Server Actions usadas para mutacoes CRUD (criar/editar/excluir/desativar) pois executam no servidor e tem acesso ao cookie httpOnly 'sess' + Bearer token"
  - "DeleteConfirmationDialog usa @radix-ui/react-dialog (Dialog/DialogContent/DialogTitle/DialogDescription/DialogTrigger) em vez de window.confirm (D-24)"
  - "LancamentosClient e CategoriasClient sao componentes cliente separados pois as paginas sao Server Components e a interatividade (formulario, dialog) requer estado cliente"
  - "TopNav usa `next/link` (nao `<a>`) para navegacao entre rotas, atendendo regra do ESLint do Next.js"
  - "Categorias inativas exibidas com `opacity-60` (estilo atenuado) e acoes Editar/Desativar ocultas para USER (D-23)"
  - "desativarCategoria marca `ativo=false` (soft delete) no backend; UI nao executa DELETE fisico (D-19)"
deviations:
  - type: "Rule 2 - Missing critical functionality"
    description: "Criado web/components/LancamentosClient.tsx e web/components/CategoriasClient.tsx como componentes cliente separados nao listados nos <files> do plano. Necessario porque as paginas sao Server Components e a interatividade (formulario controlado, dialog de exclusao) requer estado no cliente."
    files: ["web/components/LancamentosClient.tsx", "web/components/CategoriasClient.tsx"]
  - type: "Rule 2 - Missing critical functionality"
    description: "web/components/MonthSelector.tsx modificado para corrigir erro de lint pre-existente (setState sincrono dentro de useEffect) bloqueando o `npx eslint .` do plano."
    files: ["web/components/MonthSelector.tsx"]
  - type: "Scope adjustment"
    description: "O aviso de truncamento 'Mostrando 50 de {N}' foi implementado em LancamentosClient.tsx (cliente) em vez de web/app/lancamentos/page.tsx (servidor), pois depende de estado cliente (total do mês) e renderizacao condicional."
    files: ["web/components/LancamentosClient.tsx"]
  - type: "Scope adjustment"
    description: "TopNav nao usa o prop `activeRoute` para Estado ativo via `usePathname()` inicialmente, mas foi ajustado para usar o prop `activeRoute` passado pelas paginas (mais simples e previsível). O prop `userRole` esta na interface por contrato mas nao afeta a navegacao (rotas visiveis para ADMIN e USER; restricao de escrita esta nas acoes das paginas, D-23)."
    files: ["web/components/TopNav.tsx"]
metrics:
  duration_minutes: 24
  completed_date: "2026-11-09T11:56:57Z"
  tasks_completed: 2
  tasks_total: 2
  files_created: 6
  files_modified: 5
commits:
  - hash: "c2d7e09"
    message: "feat(03-04): pagina /lancamentos CRUD com DeleteConfirmationDialog e funcoes API de lancamentos"
  - hash: "62b7d3f"
    message: "feat(03-04): pagina /categorias CRUD com role ADMIN-only, TopNav e funcoes API de categorias"

# Phase 03-dashboard Plan 04: CRUD paginas /lancamentos e /categorias, TopNav, DeleteConfirmationDialog, role ADMIN-only

## One-liner

CRUD completo de lançamentos e categorias na web com navegacao top compartilhada, dialogo de confirmacao destrutiva sem `window.confirm`, controle de acesso por role (ADMIN-only para escritas) e lint/build passando.

## Objectivo

Adicionar paginas de CRUD para lançamentos e categorias (herdadas da Fase 2 per D-18), com navegacao top compartilhada, dialogo de confirmacao destrutiva sem window.confirm, role-based UI (ADMIN-only writes), e validacao monetaria espelhando a API. Quarta e ultima fatia vertical da Fase 3.

## Tasks Executados

| Task | Nome | Status | Commit |
| ---- | ---- | ------ | ------ |
| 1 | Pagina /lancamentos com CRUD + DeleteConfirmationDialog + funcoes API de lançamentos | Completo | c2d7e09 |
| 2 | Pagina /categorias com CRUD + role ADMIN-only + TopNav + lint/build | Completo | 62b7d3f |

## Acceptance Criteria (Task 1)

| Criterio | Status |
|----------|--------|
| web/lib/api.ts exporta getLancamentos, criarLancamento, editarLancamento, excluirLancamento | PASS |
| getLancamentos le cookie sess, envia Bearer token, filtra por ano/mes | PASS |
| excluirLancamento faz DELETE /api/lancamentos/{id}, retorna boolean | PASS |
| web/components/DeleteConfirmationDialog.tsx existe: "use client", sem window.confirm | PASS |
| DeleteConfirmationDialog props: isOpen, onConfirm, onClose, action, target | PASS |
| web/app/lancamentos/page.tsx existe: Server Component, instant=false, getMe() valida sessao | PASS |
| Tabela colunas: Data, Descricao, Categoria, Tipo, Valor, Acoes | PASS |
| Valor ENTRADA verde (#16A34A); Valor SAIDA vermelho (#DC2626) | PASS |
| Formulario criar/editar com validacao: valor > 0, max 2 casas, categoria obrigatoria, tipo obrigatoria | PASS |
| DeleteConfirmationDialog texto: "Tem certeza que deseja excluir este lancamento?" | PASS |
| Apos exclusao bem-sucedida: item removido da lista + alerta de sucesso | PASS |
| Aviso truncamento: "Mostrando 50 de {N} lancamentos" quando total > 50 | PASS |
| Empty state: "Nenhum lancamento neste mes" | PASS |
| npx tsc --noEmit passa | PASS |

## Acceptance Criteria (Task 2)

| Criterio | Status |
|----------|--------|
| web/lib/api.ts exporta getCategorias, criarCategoria, editarCategoria, desativarCategoria | PASS |
| desativarCategoria faz DELETE /api/categorias/{id}, retorna boolean | PASS |
| web/components/TopNav.tsx existe: "use client", navegacao entre 3 rotas + LogoutButton | PASS |
| TopNav item ativo: cor #0F766E; inativo: #64748B | PASS |
| web/app/categorias/page.tsx existe: Server Component, instant=false, getMe() valida sessao | PASS |
| Tabela colunas: Nome, Tipo, Pessoal, Ativo, Acoes | PASS |
| Categorias inativas com estilo atenuado (opacity) | PASS |
| Acoes Editar/Desativar ocultas/desabilitadas para USER (D-23) | PASS |
| Formulario criar/editar categoria apenas para ADMIN | PASS |
| Desativar usa DeleteConfirmationDialog com texto "Tem certeza que deseja desativar esta categoria?" | PASS |
| Soft delete: categoria marcada como inativa (ativo=false), nao DELETE fisico | PASS |
| Empty state: "Nenhuma categoria cadastrada" | PASS |
| TopNav integrado nas 3 paginas (/, /lancamentos, /categorias) | PASS |
| cd web && npx eslint . passa sem erros | PASS (0 erros) |
| cd web && npx next build passa sem erros | PASS |
| npx tsc --noEmit passa | PASS |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Criado LancamentosClient.tsx e CategoriasClient.tsx**
- **Found during:** Task 1 e Task 2 implementacao
- **Issue:** As paginas sao Server Components (precisam de `export const instant = false` e `getMe()`), mas formulario controlado e dialog de exclusao exigem estado no cliente. Nenhum componente cliente estava listado nos `<files>` do plano.
- **Fix:** Criado `web/components/LancamentosClient.tsx` e `web/components/CategoriasClient.tsx` como componentes cliente que recebem Server Actions como props.
- **Files modified:** web/components/LancamentosClient.tsx, web/components/CategoriasClient.tsx
- **Commit:** c2d7e09, 62b7d3f

**2. [Rule 2 - Missing critical functionality] Corrigido MonthSelector.tsx setState-in-effect**
- **Found during:** `npx eslint .` execucao
- **Issue:** `MonthSelector.tsx` (codigo pre-existente da Fase 3) chamava `setMes` sincronamente dentro de um `useEffect`, violando a regra `react-hooks/set-state-in-effect` do ESLint e bloqueando o criterio de aceitacao "eslint passa sem erros".
- **Fix:** Reescrito para usar `useSearchParams()` diretamente com `key={urlMes}` para remount quando a URL muda, sem `useEffect` nem estado local sincronizado.
- **Files modified:** web/components/MonthSelector.tsx
- **Commit:** 62b7d3f

**3. [Scope adjustment] Aviso de truncamento no cliente**
- **Found during:** Task 1 implementacao
- **Issue:** O plano indica o aviso "Mostrando 50 de {N}" em `web/app/lancamentos/page.tsx`, mas o total de lancamentos depende do estado carregado client-side.
- **Fix:** Implementado em `LancamentosClient.tsx` onde o estado de `total` e `lancamentos` esta disponível.
- **Files modified:** web/components/LancamentosClient.tsx
- **Commit:** c2d7e09

**4. [Scope adjustment] TopNav prop `userRole`**
- **Found during:** Task 2 implementacao
- **Issue:** O plano especifica `userRole` como prop do TopNav, mas a navegacao e visivel para todos os roles (restricao de escrita esta nas acoes das paginas, D-23).
- **Fix:** `userRole` mantido na interface por contrato, mas nao afeta a renderizacao do TopNav.
- **Files modified:** web/components/TopNav.tsx
- **Commit:** 62b7d3f

## Threat Surface Scan

Nenhuma nova surface de seguranca introduzida fora do `<threat_model>` do plano. Todas as chamadas de API usam cookie `sess` + Bearer token com `empresa_id` do JWT (sem `empresa_id` no body/query). O `DeleteConfirmationDialog` mitiga T-03-08 (confirmacao obrigatoria antes de DELETE).

## Known Stubs

Nenhum stub encontrado. Todos os formularios e acoes estao conectados a funcoes de API reais via Server Actions.

## Self-Check

- [x] Todos os arquivos-chave existem
- [x] Commits existem no git log (c2d7e09, 62b7d3f)
- [x] npx tsc --noEmit passa
- [x] npx eslint . passa (0 erros)
- [x] npx next build passa
- [x] Todos os acceptance criteria atendidos

## TDD Gate Compliance

N/A — plano tipo `execute` (nao `type: tdd`).
