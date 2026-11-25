# Resumo: gestão de usuários, helpdesk e Ogum

Branch: `feat/gestao-usuarios-helpdesk-ogum`. Base: `6f9892c`. HEAD final antes deste resumo: `7c43110`. Nenhum push foi realizado.

## Entregas

- `a1554c6` — plano GSD.
- `585389d`, `36b10e6` — migration V12 do helpdesk e teste com metadados/origem ADMIN.
- `e1396b0`, `c011433` — núcleo helpdesk na API com isolamento por empresa/usuário; correção de `atualizadoEm` no PATCH.
- `afc313a` — edição e exclusão lógica de usuários, com guardas de último ADMIN e autoedição preservadas.
- `8b7f77a` — allowlist e rotas BFF do helpdesk.
- `cad1b86` — navegação por role.
- `fc42c95` — painel USER de relato de problemas.
- `c23c2e5`, `7167438` — helpdesk ADMIN; correção de chaves herdadas nos helpers de status.
- `d23bff0` — página ADMIN de usuários.
- `f962599` — launcher do Ogum no canto inferior direito, com menu por role.
- `7c43110` — CI incluindo a spec `e2e/user-helpdesk.spec.ts`.

## Verificação

- API: 263 testes unitários passaram; Spotless passou.
- Web: lint sem erros (6 avisos preexistentes), `tsc --noEmit` limpo, 75 specs Playwright passaram, build de produção passou.
- Auditoria de segurança focada no diff: nenhum achado confirmado no escopo.
- Testes Docker-backed não rodaram: Docker indisponível neste ambiente.

## Trabalho concorrente

Alterações RAG não comitadas permanecem na árvore de trabalho (`api/src/.../ai/rag/`, `web/app/admin/rag/`, `web/app/api/ai/rag/`, `web/components/RagAdminClient.tsx`, `web/e2e/rag-admin.spec.ts`, `web/lib/rag-admin.ts`, além de ajustes em `web/lib/bff-helpers.ts` e `web/lib/navegacao.ts`). Foram preservadas e não foram incluídas nos commits desta feature. Os gates finais foram executados na árvore conforme autorização do usuário e passaram também com esse trabalho presente.

Pendências detalhadas: `.superpowers/sdd/261124-PLAN/task-10-report.md`.
