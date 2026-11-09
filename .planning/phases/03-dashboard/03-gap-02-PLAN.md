---
phase: 03-dashboard
plan: gap-02
type: execute
wave: 2
depends_on: ["03-gap-01"]
files_modified:
  - web/components/CategoryBreakdown.tsx
  - web/lib/api.ts
autonomous: true
gap_closure: true
requirements: [DSH-02, DSH-05, D-10]
user_setup: []
must_haves:
  truths:
    - "totalSaidas é tipado como string em CategoryBreakdownProps e DashboardData, refletindo que o backend serializa BigDecimal via ToStringSerializer (CR-02 fixado)"
    - "Guard de divisão por zero usa `Number(totalSaidas) || 0` antes da comparação, então totalSaidas = '0.00' resulta em 0 e o guard dispara corretamente (CR-02 fixado)"
    - "categoria.total é parseado com Number() antes da divisão, evitando concatenação de strings e NaN (CR-02 fixado, WR-01 fixado)"
    - "Quando totalSaidas é '0.00' ou '0', percentual exibe '—' em vez de 'NaN%' (D-10, CR-02 fixado)"
    - "Campos monetários de DashboardData (totalEntradas, totalSaidas, saldo, variações) são tipados como string, parseados apenas na exibição com Intl.NumberFormat (WR-02 fixado)"
  artifacts:
    - path: "web/components/CategoryBreakdown.tsx"
      provides: "Componente de breakdown com totalSaidas tipado como string, guard de zero corrigido, parse numérico antes da divisão"
      contains: "totalSaidas: string;"
      contains: "const totalSaidasNum = Number(totalSaidas) || 0;"
      contains: "if (!totalSaidasNum)"
      contains: "Number(categoria.total)"
    - path: "web/lib/api.ts"
      provides: "Tipo DashboardData com campos monetários como string; tipo LancamentoData com valor como string"
      contains: "totalEntradas: string;"
      contains: "totalSaidas: string;"
      contains: "saldo: string;"
  key_links:
    - from: "web/components/CategoryBreakdown.tsx"
      to: "web/lib/api.ts"
      via: "totalSaidas vem de DashboardData.totalSaidas, tipado como string do JSON do backend"
      pattern: "totalSaidas"
    - from: "web/lib/api.ts"
      to: "api/src/main/java/com/controlefinanceiro/dashboard/dto/DashboardResponse.java"
      via: "BigDecimal serializado como string JSON via @JsonSerialize(ToStringSerializer) + write-bigdecimal-as-plain: true"
      pattern: "ToStringSerializer"

<objective>
Corrigir CR-02 (guard de divisão por zero quebrado porque totalSaidas chega como string "0.00" do backend) e WR-02 (tipos TypeScript mentirosos: campos monetários tipados como number mas backend envia strings). Em CategoryBreakdown.tsx, mudar totalSaidas para string, fazer parse com Number() antes do guard e da divisão. Em web/lib/api.ts, mudar tipos monetários de DashboardData (e LancamentoData/LancamentoRequest) para string, parseando apenas na exibição com Intl.NumberFormat.

Purpose: Eliminar "NaN%" na UI quando total de saídas é zero, e corrigir tipos TypeScript para refletir o contrato real do backend (BigDecimal → string JSON).
Output: CategoryBreakdown.tsx com totalSaidas como string e parse numérico; web/lib/api.ts com tipos monetários como string.
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
@web/components/CategoryBreakdown.tsx
@web/lib/api.ts
@api/src/main/java/com/controlefinanceiro/dashboard/dto/DashboardResponse.java
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Corrigir tipos monetários em web/lib/api.ts (number → string para campos BigDecimal)</name>
  <files>web/lib/api.ts</files>
  <read_first>
    @web/lib/api.ts
    @api/src/main/java/com/controlefinanceiro/dashboard/dto/DashboardResponse.java
  </read_first>
  <behavior>
    - DashboardData.totalEntradas, totalSaidas, saldo, variacaoEntradas, variacaoSaidas, variacaoSaldo são tipados como string (não number)
    - DashboardData.categorias[].total é tipado como string
    - LancamentoData.valor é tipado como string
    - LancamentoRequest.valor é tipado como string
    - Nenhum campo monetário recebido da API é tipado como number
  </behavior>
  <action>
  Em web/lib/api.ts:
  1. Linhas 41-52 (tipo DashboardData): alterar totalEntradas, totalSaidas, saldo, variacaoEntradas, variacaoSaidas, variacaoSaldo de `number` para `string`; alterar categorias[].total de `number` para `string`
  2. Linhas 86-94 (tipo LancamentoData): alterar valor de `number` para `string`
  3. Linhas 97-103 (tipo LancamentoRequest): alterar valor de `number` para `string`
  Nenhum outro arquivo é modificado nesta tarefa. O parse para número acontece apenas nos componentes de exibição (CategoryBreakdown, DashboardKPIs, LancamentosClient).
  </action>
  <acceptance_criteria>
  - `grep -c ': number' web/lib/api.ts` retorna 0 para campos de DashboardData, LancamentoData, LancamentoRequest (campos monetários)
  - Linha do tipo DashboardData contém `totalEntradas: string;`, `totalSaidas: string;`, `saldo: string;`
  - Linha do tipo DashboardData contém `variacaoEntradas: string;`, `variacaoSaidas: string;`, `variacaoSaldo: string;`
  - Linha do tipo DashboardData contém `total: string;` dentro de categorias
  - Linha do tipo LancamentoData contém `valor: string;`
  - Linha do tipo LancamentoRequest contém `valor: string;`
  - `cd web && npx tsc --noErrorTruncation --noEmit` não reporta erros de tipo em web/lib/api.ts
  </acceptance_criteria>
  <verify>
  grep -q 'totalEntradas: string;' web/lib/api.ts && echo "totalEntradas string"; grep -q 'totalSaidas: string;' web/lib/api.ts && echo "totalSaidas string"; grep -q 'saldo: string;' web/lib/api.ts && echo "saldo string"; grep -q 'total: string;' web/lib/api.ts && echo "categorias[].total string"; grep -q 'valor: string;' web/lib/api.ts && echo "valor string"; ! grep -qE 'totalEntradas: number|totalSaidas: number|saldo: number|valor: number' web/lib/api.ts && echo "sem campos monetários como number"
  </verify>
  <done>
  web/lib/api.ts: todos os campos monetários recebidos da API são tipados como string, refletindo o contrato BigDecimal → string do backend.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Corrigir guard de divisão por zero e parse numérico em CategoryBreakdown.tsx</name>
  <files>web/components/CategoryBreakdown.tsx</files>
  <read_first>
    @web/components/CategoryBreakdown.tsx
    @web/lib/api.ts
  </read_first>
  <behavior>
    - totalSaidas prop é tipado como string
    - Antes de qualquer uso numérico, totalSaidas é convertido com `Number(totalSaidas) || 0`
    - O guard de zero usa `if (!totalSaidasNum)` (não `if (!totalSaidas)`), então "0.00" → 0 → guard dispara
    - categoria.total é convertido com `Number(categoria.total)` antes da divisão
    - Quando totalSaidasNum é 0, percentual retorna "—" (não "NaN%")
    - Quando totalSaidasNum > 0, percentual é calculado normalmente como (categoria.totalNum / totalSaidasNum)
  </behavior>
  <action>
  Em web/components/CategoryBreakdown.tsx:
  1. Linha 35 (tipo CategoriaGasto): alterar `total: number;` para `total: string;`
  2. Linha 40 (tipo CategoryBreakdownProps): alterar `totalSaidas: number;` para `totalSaidas: string;`
  3. Linha 48: substituir `const total = categorias.reduce((sum, c) => sum + c.total, 0);` por `const total = categorias.reduce((sum, c) => sum + Number(c.total), 0);`
  4. Linhas 52-57: substituir a função percentual por:
     - `const totalSaidasNum = Number(totalSaidas) || 0;`
     - `if (!totalSaidasNum) return "—";`
     - `const categoriaTotal = Number(categoria.total);`
     - `return PERCENTUAL.format(categoriaTotal / totalSaidasNum);`
  5. Linha 98: alterar `MONETARY.format(categoria.total)` para `MONETARY.format(Number(categoria.total))`
  6. Linha 122: o fill do Bar continua usando `var(--chart-1)` (sem alteração)
  </action>
  <acceptance_criteria>
  - Tipo CategoriaGasto contém `total: string;`
  - Tipo CategoryBreakdownProps contém `totalSaidas: string;`
  - Código contém `const totalSaidasNum = Number(totalSaidas) || 0;`
  - Código contém `if (!totalSaidasNum)` (não `if (!totalSaidas)`)
  - Código contém `Number(categoria.total)` (pelo menos uma ocorrência antes da divisão)
  - `MONETARY.format(Number(categoria.total))` na linha da tabela (não `MONETARY.format(categoria.total)`)
  - `cd web && npx tsc --noErrorTruncation --noEmit` não reporta erros em CategoryBreakdown.tsx
  - Quando totalSaidas = "0.00", percentual retorna "—" (não "NaN%")
  </acceptance_criteria>
  <verify>
  grep -q 'totalSaidas: string;' web/components/CategoryBreakdown.tsx && echo "totalSaidas string"; grep -q 'total: string;' web/components/CategoryBreakdown.tsx && echo "categoria.total string"; grep -q 'Number(totalSaidas) || 0' web/components/CategoryBreakdown.tsx && echo "parse totalSaidas presente"; grep -q 'if (!totalSaidasNum)' web/components/CategoryBreakdown.tsx && echo "guard corrigido"; grep -q 'Number(categoria.total)' web/components/CategoryBreakdown.tsx && echo "parse categoria.total presente"; ! grep -q 'if (!totalSaidas)' web/components/CategoryBreakdown.tsx && echo "guard antigo removido"
  </verify>
  <done>
  CategoryBreakdown.tsx: totalSaidas tipado como string, guard usa Number() || 0, categoria.total parseado antes da divisão, sem NaN% possível.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| API JSON → Frontend TypeScript | BigDecimal serializado como string JSON; TypeScript deve tipar como string e parsear apenas na exibição |
| Frontend cálculo → UI render | Divisão categoria.total / totalSaidas pode produzir NaN se guard não usar parse numérico |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-03-12 | Data Integrity | web/components/CategoryBreakdown.tsx | mitigate | totalSaidas tipado como string, parseado com Number() || 0 antes do guard; categoria.total parseado com Number() antes da divisão; sem concatenação de strings |
| T-03-03 | Denial of Service | web/components/CategoryBreakdown.tsx | mitigate | totalSaidasNum = 0 → percentual = "—", sem divisão por zero; sem NaN/Infinity na UI |
| T-03-SC | Tampering | Pacotes externos | accept | Nenhum pacote externo instalado neste plano |
</threat_model>

<verification>
- totalSaidas tipado como string em CategoryBreakdownProps
- categoria.total tipado como string em CategoriaGasto
- `const totalSaidasNum = Number(totalSaidas) || 0;` presente antes do guard
- `if (!totalSaidasNum)` usado (não `if (!totalSaidas)`)
- `Number(categoria.total)` usado antes de qualquer divisão ou formatação
- MONETARY.format recebe Number(categoria.total), não categoria.total diretamente
- Campos monetários de DashboardData em web/lib/api.ts são string
- `cd web && npx tsc --noErrorTruncation --noEmit` passa sem erros
- totalSaidas = "0.00" resulta em percentual "—" (não "NaN%")
</verification>

<success_criteria>
- Nenhum "NaN%" exibido na UI, mesmo quando total de saídas é zero
- Guard de divisão por zero funciona corretamente com totalSaidas como string "0.00"
- Tipos TypeScript refletem o contrato real do backend (BigDecimal → string JSON)
- TypeScript compila sem erros
</success_criteria>

<output>
Create .planning/phases/03-dashboard/03-gap-02-SUMMARY.md when done
</output>