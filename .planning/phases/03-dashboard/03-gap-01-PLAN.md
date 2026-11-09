---
phase: 03-dashboard
plan: gap-01
type: execute
wave: 1
depends_on: []
files_modified:
  - web/components/LancamentosClient.tsx
  - web/components/CategoriasClient.tsx
autonomous: true
gap_closure: true
requirements: [DSH-01, DSH-02, LAN-01, LAN-02, LAN-03, LAN-04, LAN-05, LAN-06, LAN-07, CAT-01, CAT-02, CAT-03, CAT-04, CAT-05, CAT-06]
user_setup: []
must_haves:
  truths:
    - "Criar lançamento de tipo SAÍDA envia 'SAIDA' (sem acento) ao backend, batendo com o contrato l.tipo = 'SAIDA' do LancamentoRepository (CR-01 fixado)"
    - "Editar lançamento existente com tipo SAÍDA envia 'SAIDA' sem acento, permitindo validação categoria.getTipo().equals(tipoLancamento) passar (CR-01 fixado)"
    - "Criar categoria de tipo SAÍDA envia 'SAIDA' sem acento, batendo com contrato do backend (CR-01 fixado)"
    - "Editar categoria existente com tipo SAÍDA envia 'SAIDA' sem acento (CR-01 fixado)"
    - "Não resta nenhuma ocorrência de 'SAÍDA' (com acento) como valor de enum ou option nos dois arquivos cliente"
  artifacts:
    - path: "web/components/LancamentosClient.tsx"
      provides: "TipoLancamento enum e <option> do tipo de lançamento corrigidos para 'SAIDA'"
      contains: "type TipoLancamento = \"ENTRADA\" | \"SAIDA\";"
      contains: "<option value=\"SAIDA\">Saída</option>"
    - path: "web/components/CategoriasClient.tsx"
      provides: "Cast de tipo e <option> do tipo de categoria corrigidos para 'SAIDA'"
      contains: "setTipo(c.tipo as \"ENTRADA\" | \"SAIDA\");"
      contains: "<option value=\"SAIDA\">Saída</option>"
  key_links:
    - from: "web/components/LancamentosClient.tsx"
      to: "api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java"
      via: "valor do campo tipo enviado em POST/PUT /api/lancamentos deve bater com l.tipo = 'SAIDA' no JPQL"
      pattern: "SAIDA"
    - from: "web/components/CategoriasClient.tsx"
      to: "api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java"
      via: "valor do campo tipo enviado em POST/PUT /api/categorias deve bater com categoria.getTipo().equals(tipoLancamento)"
      pattern: "SAIDA"

<objective>
Corrigir o descompasso de acento no tipo de lançamento/categoria entre frontend e backend. O frontend envia "SAÍDA" (com acento) no enum TipoLancamento e nos <option> de select, mas o backend filtra e valida com "SAIDA" (sem acento). Isso impede criar/editar lançamentos e categorias de tipo saída. Mudar todos os valores "SAÍDA" para "SAIDA" em LancamentosClient.tsx e CategoriasClient.tsx, mantendo o label de exibição "Saída" inalterado.

Purpose: Restaurar a funcionalidade de criar/editar saídas e categorias de saída (DSH-01, DSH-02, CAT-01..06, LAN-01..07).
Output: Dois arquivos cliente corrigidos, nenhum valor "SAÍDA" (com acento) restante, TypeScript compila limpo.
</objective>

<execution_context>
@C:/Users/11/controle-gastos/.opencode/get-shit-done/workflows/execute-plan.md
@C:/Users/11/controle-gastos/.opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/03-dashboard/03-CONTEXT.md
@.planning/phases/03-dashboard/03-VERIFICATION.md
@.planning/phases/03-dashboard/03-REVIEW.md
@web/components/LancamentosClient.tsx
@web/components/CategoriasClient.tsx
@api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java
@api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Corrigir TipoLancamento enum e option value em LancamentosClient.tsx (SAÍDA → SAIDA)</name>
  <files>web/components/LancamentosClient.tsx</files>
  <read_first>
    @web/components/LancamentosClient.tsx
    @api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java
  </read_first>
  <behavior>
    - O enum TipoLancamento aceita apenas "ENTRADA" | "SAIDA" (sem acento)
    - O <option> do tipo de lançamento tem value="SAIDA" e label "Saída" (label inalterado)
    - Quando o usuário seleciona "Saída" no select, o valor enviado ao backend é "SAIDA", não "SAÍDA"
    - Ao editar um lançamento existente com tipo "SAIDA" vindo da API, o select mostra a opção correta selecionada
  </behavior>
  <action>
  Em web/components/LancamentosClient.tsx:
  1. Linha 18: alterar `type TipoLancamento = "ENTRADA" | "SAÍDA";` para `type TipoLancamento = "ENTRADA" | "SAIDA";`
  2. Linha 456: alterar `<option value="SAÍDA">Saída</option>` para `<option value="SAIDA">Saída</option>`
  Nenhuma outra alteração neste arquivo. O label de exibição "Saída" permanece com acento; apenas o valor do option muda.
  </action>
  <acceptance_criteria>
  - `grep -c 'SAÍDA' web/components/LancamentosClient.tsx` retorna 0
  - `grep -c 'SAIDA' web/components/LancamentosClient.tsx` retorna 2 (linha do enum e linha do option)
  - Linha 18 contém exatamente `type TipoLancamento = "ENTRADA" | "SAIDA";`
  - Linha do option contém exatamente `<option value="SAIDA">Saída</option>`
  - `cd web && npx tsc --noErrorTruncation --noEmit` não reporta erros em LancamentosClient.tsx
  </acceptance_criteria>
  <verify>
  grep -q 'type TipoLancamento = "ENTRADA" | "SAIDA";' web/components/LancamentosClient.tsx && echo "enum corrigido"; grep -q '<option value="SAIDA">Saída</option>' web/components/LancamentosClient.tsx && echo "option corrigido"; ! grep -q 'SAÍDA' web/components/LancamentosClient.tsx && echo "sem acento restante"
  </verify>
  <done>
  LancamentosClient.tsx: enum TipoLancamento e <option> usam "SAIDA" sem acento, batendo com contrato do backend.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Corrigir cast de tipo e option value em CategoriasClient.tsx (SAÍDA → SAIDA)</name>
  <files>web/components/CategoriasClient.tsx</files>
  <read_first>
    @web/components/CategoriasClient.tsx
    @api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
  </read_first>
  <behavior>
    - O cast `c.tipo as "ENTRADA" | "SAIDA"` aceita apenas "ENTRADA" | "SAIDA" (sem acento)
    - O <option> do tipo de categoria tem value="SAIDA" e label "Saída" (label inalterado)
    - Quando o usuário seleciona "Saída" no select de categoria, o valor enviado ao backend é "SAIDA"
    - Ao editar uma categoria existente com tipo "SAIDA" vindo da API, o cast não lança erro de tipo
  </behavior>
  <action>
  Em web/components/CategoriasClient.tsx:
  1. Linha 77: alterar `setTipo(c.tipo as "ENTRADA" | "SAÍDA");` para `setTipo(c.tipo as "ENTRADA" | "SAIDA");`
  2. Linha 407: alterar `<option value="SAÍDA">Saída</option>` para `<option value="SAIDA">Saída</option>`
  Nenhuma outra alteração neste arquivo. O label de exibição "Saída" permanece com acento; apenas o valor do option muda.
  </action>
  <acceptance_criteria>
  - `grep -c 'SAÍDA' web/components/CategoriasClient.tsx` retorna 0
  - `grep -c 'SAIDA' web/components/CategoriasClient.tsx` retorna 2 (linha do cast e linha do option)
  - Linha 77 contém exatamente `setTipo(c.tipo as "ENTRADA" | "SAIDA");`
  - Linha 407 contém exatamente `<option value="SAIDA">Saída</option>`
  - `cd web && npx tsc --noErrorTruncation --noEmit` não reporta erros em CategoriasClient.tsx
  </acceptance_criteria>
  <verify>
  grep -q 'setTipo(c.tipo as "ENTRADA" | "SAIDA");' web/components/CategoriasClient.tsx && echo "cast corrigido"; grep -q '<option value="SAIDA">Saída</option>' web/components/CategoriasClient.tsx && echo "option corrigido"; ! grep -q 'SAÍDA' web/components/CategoriasClient.tsx && echo "sem acento restante"
  </verify>
  <done>
  CategoriasClient.tsx: cast de tipo e <option> usam "SAIDA" sem acento, batendo com contrato do backend.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Browser → API POST/PUT /api/lancamentos | Campo `tipo` enviado no body JSON; backend compara com `l.tipo = 'SAIDA'` no JPQL |
| Browser → API POST/PUT /api/categorias | Campo `tipo` enviado no body JSON; `LancamentoService.validarIntegridadeCategoriaTipo` faz `categoria.getTipo().equals(tipoLancamento)` |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-03-11 | Tampering | web/components/LancamentosClient.tsx + CategoriasClient.tsx | mitigate | Valores de tipo enviados ao backend usam "SAIDA" (sem acento), exatamente como o backend armazena e consulta; nenhuma validação contorna o contrato |
| T-03-SC | Tampering | Pacotes externos | accept | Nenhum pacote externo instalado neste plano; apenas alteração de strings existentes |
</threat_model>

<verification>
- Nenhuma ocorrência de "SAÍDA" (com acento) em LancamentosClient.tsx ou CategoriasClient.tsx
- TipoLancamento enum = "ENTRADA" | "SAIDA" em LancamentosClient.tsx
- Cast de tipo = "ENTRADA" | "SAIDA" em CategoriasClient.tsx
- <option value="SAIDA">Saída</option> em ambos os arquivos (label "Saída" com acento mantido)
- `cd web && npx tsc --noErrorTruncation --noEmit` passa sem erros
</verification>

<success_criteria>
- Criar/editar lançamento de tipo saída envia "SAIDA" ao backend (não "SAÍDA")
- Criar/editar categoria de tipo saída envia "SAIDA" ao backend (não "SAÍDA")
- Validação `categoria.getTipo().equals(tipoLancamento)` passa para saídas
- TypeScript compila sem erros
</success_criteria>

<output>
Create .planning/phases/03-dashboard/03-gap-01-SUMMARY.md when done
</output>