---
phase: 03-dashboard
plan: gap-03
type: execute
wave: 3
depends_on: ["03-gap-02"]
files_modified:
  - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java
  - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
  - web/lib/api.ts
autonomous: true
gap_closure: true
requirements: [DSH-01, D-22]
user_setup: []
must_haves:
  truths:
    - "GET /api/lancamentos?ano=&mes= retorna objeto paginado com campo `total` contendo a contagem real de lançamentos do banco, não o tamanho da página (WR-03 fixado)"
    - "getLancamentos em web/lib/api.ts lê `total` do corpo da resposta JSON em vez de calcular `lista.length` (WR-03 fixado)"
    - "Aviso de truncamento 'Mostrando 50 de {N}' dispara quando total > 50 (D-22)"
    - "Todas as queries de listagem continuam filtrando por empresa_id do TenantContext (sucesso criterion 4, D-04)"
    - "Nenhum double/float introduzido; contagem é long/Integer, não afeta cálculos monetários (LAN-05)"
  artifacts:
    - path: "api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java"
      provides: "Endpoint GET /api/lancamentos retorna resposta paginada com lancamentos + total"
      contains: "GET /api/lancamentos"
      contains: "total"
    - path: "api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java"
      provides: "Método listar retorna total real de lançamentos via page.getTotalElements()"
      contains: "getTotalElements"
    - path: "web/lib/api.ts"
      provides: "getLancamentos retorna { lancamentos, total } com total lido do corpo da resposta"
      contains: "getLancamentos"
      contains: "total"
  key_links:
    - from: "web/lib/api.ts"
      to: "api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java"
      via: "GET /api/lancamentos?ano=&mes= retorna JSON com campo total; getLancamentos lê response.body.total"
      pattern: "/api/lancamentos"
    - from: "api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java"
      to: "api/src/main/java/com/controlefinanceiro/common/TenantContext.java"
      via: "TenantContext.getEmpresaId() filtra todas as queries por tenant"
      pattern: "TenantContext\\.getEmpresaId\\(\\)"

<objectivo>
Corrigir WR-03: o campo `total` retornado por `getLancamentos` é `lista.length` (tamanho da página, max 50), não o total real de lançamentos. Como consequência, o aviso de truncamento "Mostrando 50 de {N}" nunca dispara. Alterar o backend para retornar a contagem real de lançamentos (usando o `Page` já retornado por `findByEmpresaIdAndFiltros`) e atualizar `getLancamentos` no frontend para ler `total` do corpo da resposta.

Purpose: Habilitar o aviso de truncamento de lançamentos quando há mais de 50 itens no mês, conforme D-22.
Output: Backend retorna paginated response com `total` real; frontend lê `total` da resposta JSON; truncamento funciona.
</objective>

<execution_context>
@C:/Users/11/controle-gastos/.opencode/get-shit-done/workflows/execute-plan.md
@C:/Users/11/controle-gastos/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/03-dashboard/03-CONTEXT.md
@.planning/phases/03-dashboard/03-VERIFICATION.md
@.planning/phases/03-dashboard/03-REVIEW.md
@web/lib/api.ts
@api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java
@api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
@api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java
@api/src/main/java/com/controlefinanceiro/lancamento/LancamentoResponse.java
@api/src/main/java/com/controlefinanceiro/common/TenantContext.java
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Backend — GET /api/lancamentos retorna total real de lançamentos (count via Page)</name>
  <files>
    api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java
    api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
  </files>
  <read_first>
    @api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java
    @api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
    @api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java
    @api/src/main/java/com/controlefinanceiro/lancamento/LancamentoResponse.java
    @api/src/main/java/com/controlefinanceiro/common/TenantContext.java
  </read_first>
  <behavior>
    - GET /api/lancamentos?ano=&mes= retorna objeto JSON com campos `lancamentos` (array) e `total` (long)
    - `total` é obtido de `page.getTotalElements()` da query `findByEmpresaIdAndFiltros` (contagem real do banco, não tamanho da página)
    - `lancamentos` é a lista de até 50 itens (mesmo comportamento de truncamento existente)
    - Todas as queries continuam filtrando por `empresa_id` via `TenantContext.getEmpresaId()`
    - Nenhum novo parâmetro `empresa_id` aceito do client (R3/AUTH-03)
    - `LancamentoService.listar` retorna um registro contendo tanto a lista quanto o total
  </behavior>
  <action>
  Em api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java:
  1. Adicionar um record `LancamentoPage` dentro do arquivo (ou como classe separada) com campos `List<LancamentoResponse> lancamentos` e `long total`
  2. Alterar o método `listar` para retornar `LancamentoPage` em vez de `List<LancamentoResponse>`:
     - Chamar `findByEmpresaIdAndFiltros` que já retorna `Page<Lancamento>`
     - Obter `total = page.getTotalElements()`
     - Mapear o conteúdo da page para `List<LancamentoResponse>`
     - Retornar `new LancamentoPage(listaMapeada, total)`

  Em api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java:
  3. Alterar o método `listar` para retornar `ResponseEntity<LancamentoPage>` (ou o tipo criado) em vez de `ResponseEntity<List<LancamentoResponse>>`
  4. O corpo da resposta agora contém `{ lancamentos: [...], total: N }`

  Nenhuma mudança em LancamentoRepository.java (a query findByEmpresaIdAndFiltros já retorna Page com totalElements).
  </action>
  <acceptance_criteria>
  - LancamentoService.java contém um record/tipo com `List<LancamentoResponse> lancamentos` e `long total`
  - Método `listar` em LancamentoService retorna esse tipo com `page.getTotalElements()` como total
  - Método `listar` em LancamentoController retorna `ResponseEntity<LancamentoPage>` (ou equivalente)
  - JSON da resposta GET /api/lancamentos?ano=&mes= contém campos `lancamentos` e `total`
  - `total` é obtido de `page.getTotalElements()`, não de `lista.size()` ou `lista.length`
  - Todas as queries filtram por `TenantContext.getEmpresaId()` (sem empresa_id de request param)
  - `./mvnw compile -pl api` passa sem erros
  </acceptance_criteria>
  <verify>
  grep -q 'getTotalElements' api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java && echo "getTotalElements usado"; grep -q 'total' api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java && echo "controller retorna total"; grep -q 'TenantContext.getEmpresaId' api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java && echo "tenant do token"; ./mvnw compile -pl api -q 2>&1 && echo "compilação OK" || echo "compilação falhou"
  </verify>
  <done>
  Backend: GET /api/lancamentos retorna objeto com `lancamentos` (lista até 50) e `total` (contagem real via page.getTotalElements()).
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Frontend — getLancamentos lê total do corpo da resposta JSON</name>
  <files>web/lib/api.ts</files>
  <read_first>
    @web/lib/api.ts
    @web/components/LancamentosClient.tsx
  </read_first>
  <behavior>
    - getLancamentos faz parse da resposta JSON como `{ lancamentos: LancamentoData[]; total: number }`
    - `total` é lido de `resJson.total` (campo do backend), não de `lista.length`
    - O tipo de retorno continua sendo `{ lancamentos: LancamentoData[]; total: number } | null`
    - O aviso de truncamento em LancamentosClient.tsx (`truncado = total > 50`) agora dispara corretamente quando total > 50
    - Nenhuma alteração em LancamentosClient.tsx necessária (já usa `total` do prop)
  </behavior>
  <action>
  Em web/lib/api.ts:
  1. Linhas 110-132 (função getLancamentos): alterar o parse da resposta:
     - Substituir `const lista = (await res.json()) as LancamentoData[]; return { lancamentos: lista, total: lista.length };`
     - Por: `const resJson = await res.json() as { lancamentos: LancamentoData[]; total: number }; return { lancamentos: resJson.lancamentos, total: resJson.total };`
  2. Manter o tipo de retorno `Promise<{ lancamentos: LancamentoData[]; total: number } | null>`
  3. Nenhuma alteração em LancamentosClient.tsx (já recebe `total` como prop e usa `truncado = total > 50`)

  Ajuste: se o backend retornar o campo `total` dentro de um envelope diferente (ex: `{ data: {...}, total: N }`), adaptar o parse conforme o formato real retornado pelo controller.
  </action>
  <acceptance_criteria>
  - getLancamentos faz `await res.json()` uma vez e acessa `.total` do objeto parseado
  - `total` vem de `resJson.total` (campo do backend), não de `lista.length`
  - `lancamentos` vem de `resJson.lancamentos`
  - Tipo de retorno mantido: `Promise<{ lancamentos: LancamentoData[]; total: number } | null>`
  - `cd web && npx tsc --noErrorTruncation --noEmit` não reporta erros em web/lib/api.ts
  - Quando backend retorna `{ lancamentos: [...], total: 120 }`, getLancamentos retorna total = 120
  - Quando backend retorna `{ lancamentos: [...], total: 30 }`, getLancamentos retorna total = 30
  </acceptance_criteria>
  <verify>
  grep -q 'resJson.total' web/lib/api.ts || grep -q '\.total' web/lib/api.ts | grep -q 'getLancamentos' && echo "total lido da resposta"; ! grep -q 'total: lista.length' web/lib/api.ts && echo "lista.length removido"; grep -q 'total: number' web/lib/api.ts && echo "tipo total mantido"; cd web && npx tsc --noErrorTruncation --noEmit 2>&1 | grep -qE "api\.ts|getLancamentos" && echo "TypeScript errors" || echo "TypeScript OK"
  </verify>
  <done>
  Frontend: getLancamentos lê `total` do corpo da resposta JSON do backend; truncamento "Mostrando 50 de {N}" dispara quando total > 50.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Browser → API GET /api/lancamentos | Cookie sess + Bearer token; campo `total` vem do servidor, nunca do client |
| API → Database | Page query filtra por empresa_id via TenantContext; count é da mesma query filtrada |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-03-13 | Information Disclosure | api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java + LancamentoController.java | mitigate | Query de contagem usa `page.getTotalElements()` da mesma `Page` já filtrada por `empresa_id` via TenantContext; nenhum dado de outra empresa é exposto |
| T-03-04 | Tampering | web/lib/api.ts (getLancamentos) | mitigate | `total` lido do corpo da resposta do servidor; frontend não confia em valor fornecido pelo client; não há parâmetro de total na request |
| T-03-SC | Tampering | Pacotes externos | accept | Nenhum pacote externo instalado neste plano |
</threat_model>

<verification>
- GET /api/lancamentos?ano=&mes= retorna JSON com campos `lancamentos` (array) e `total` (number)
- `total` corresponde à contagem real de lançamentos no banco para o mês/empresa
- `total` não é igual ao tamanho da página (lista.length) quando há mais de 50 lançamentos
- getLancamentos em web/lib/api.ts lê `total` de `resJson.total`
- `truncado = total > 50` em LancamentosClient.tsx dispara corretamente
- Todas as queries filtram por `TenantContext.getEmpresaId()`
- `./mvnw compile -pl api` passa; `cd web && npx tsc --noErrorTruncation --noEmit` passa
</verification>

<success_criteria>
- Backend retorna contagem real de lançamentos (não tamanho da página)
- Frontend lê `total` da resposta JSON
- Aviso de truncamento "Mostrando 50 de {N}" dispara quando total > 50
- Isolamento de tenant mantido (empresa_id do token apenas)
- TypeScript e Java compilam sem erros
</success_criteria>

<output>
Create .planning/phases/03-dashboard/03-gap-03-SUMMARY.md when done
</output>