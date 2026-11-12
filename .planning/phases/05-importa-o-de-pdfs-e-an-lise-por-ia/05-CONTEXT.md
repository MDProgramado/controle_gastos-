# Phase 5: Importação de PDFs e Análise por IA - Context

**Gathered:** 2026-11-12
**Status:** Ready for planning
**Source:** Discussão com o cliente (sessão de planejamento — decisões fechadas por ele)

<domain>
## Phase Boundary

Fase 5 entrega a **importação de faturas de cartão de crédito e extratos bancários em PDF** (IMP-01..IMP-15): o usuário envia vários PDFs numa requisição, o backend extrai o texto com PDFBox (sem OCR), sanitiza dados sensíveis, a IA classifica cada lançamento em `RECEITA` / `DESPESA_PAGA` / `A_PAGAR`, o sistema devolve um resumo analítico do mês (total recebido, total já pago, total comprometido, alertas) e o usuário **revisa e confirma** antes de qualquer gravação.

**In scope:**
- Endpoint multipart de importação (`POST /api/importacoes/pdf`), confirmação (`POST /api/importacoes/{id}/confirmar`), consulta do histórico (`GET /api/importacoes`) e descarte (`DELETE /api/importacoes/{id}`).
- Extração de texto com Apache PDFBox + sanitização (CPF/CNPJ/cartão) antes de qualquer chamada à OpenRouter.
- Cliente de IA com **cascata de modelos gratuitos** (`google/gemma-4-31b-it:free` → `google/gemma-4-26b-a4b-it:free` → `openrouter/free`), backoff exponencial e validação de JSON estrito.
- Novas tabelas: staging (`importacao_documento`, `importacao_item`, `importacao_fatura`) e definitivas (`fatura_cartao`, `conta_a_pagar`).
- Página `/importacamentos` na web: upload múltiplo, revisão item a item (categoria editável, item desmarcável) e confirmação.

**Out of scope (não pertence a esta fase):**
- OCR de PDF digitalizado (custo zero é requisito — PDF sem texto é avisado, não processado).
- Importação OFX/CSV, lançamentos recorrentes, exportação, app mobile, multiempresa real.
- Persistir o PDF original ou o texto bruto extraído (LGPD).
- Extração de imagem/scaneamento com visão de modelo.

</domain>

<decisions>
## Implementation Decisions

### Fluxo de uso e escopo do endpoint
- **D-01:** Upload **em lote**: uma requisição `multipart/form-data` aceita de 1 a N PDFs no campo `arquivos` (faturas de cartão e/ou extratos bancários, misturados).
- **D-02:** Fluxo em **duas etapas**: a análise **não grava** nada em `lancamento`/`conta_a_pagar` — grava apenas *staging* — e devolve os itens para revisão; a gravação acontece só em `POST /api/importacoes/{id}/confirmar`, disparado pelo usuário. Alinhado a AI-02 ("a IA nunca grava sozinha").
- **D-03:** A entrega inclui a **interface web** (página `/importacamentos`), não só o backend.
- **D-04:** Validação de arquivo no código: content-type `application/pdf` **e** magic bytes `%PDF` **e** tamanho **e** quantidade — nunca confiar só na extensão.

### Modelo de dados e persistência
- **D-05:** `A_PAGAR` fica em **tabelas novas** (`conta_a_pagar` + `fatura_cartao`). `lancamento.tipo` continua `ENTRADA|SAIDA` ⇒ o saldo do dashboard (entradas − saídas) permanece **exato ao centavo** e nenhum filtro existente muda de semântica.
- **D-06:** A análise grava em tabelas de **staging** (`importacao_documento`, `importacao_item`, `importacao_fatura`) para suportar a revisão sem estado em memória (a API é stateless e pode ter mais de uma instância).
- **D-07:** A confirmação grava em **transação tudo-ou-nada** e é **idempotente**: o item de staging passa a `IMPORTADO` guardando o id do lançamento/`conta_a_pagar` criado — confirmar duas vezes não duplica nada.
- **D-08:** `lancamento` ganha `origem` (`CARTAO|EXTRATO|MANUAL`), `documento_id` e `chave_dedupe` (SHA-256 de tipo+data+valor+categoria) com **índice único parcial por empresa** — evita gravar o mesmo lançamento duas vezes.
- **D-09:** Reenvio do **mesmo arquivo** é detectado por SHA-256 (`UNIQUE (empresa_id, hash_sha256)`) ⇒ status `DUPLICADO`, sem reprocessar e sem duplicar.
- **D-10:** `empresa_id` vem **exclusivamente do token JWT** (`TenantContext`), em todas as tabelas — nenhum campo do multipart define tenant (IMP-13, R3).

### Extração, sanitização e custo zero
- **D-11:** Extração de texto com **Apache PDFBox** (`PDFTextStripper`), no backend. **Sem OCR** — é a garantia de custo zero do requisito original.
- **D-12:** PDF **sem texto extraível** (digitalizado) é marcado como `SEM_TEXTO` com aviso e **o lote continua** sem ele — não derruba a importação (IMP-14).
- **D-13:** **Sanitização obrigatória antes do envio** à OpenRouter: CPF (`***.***.***-**`), CNPJ, números de cartão (mantendo só os 4 últimos dígitos) e sequências de 13–19 dígitos, agência/conta. Só o texto mascarado sai do servidor.
- **D-14:** O PDF bruto **não é persistido** (LGPD) — ficam apenas hash, metadados e o resultado estruturado. O texto sanitizado também não é persistido.

### IA: cascata de modelos, JSON estrito e resiliência
- **D-15:** Cascata de modelos **gratuitos**, em ordem de prioridade: `google/gemma-4-31b-it:free` → `google/gemma-4-26b-a4b-it:free` → `openrouter/free`. **Nenhum modelo pago** — fallback para modelo pago é cobrado pela OpenRouter (o `application.yml` atual tem `google/gemma-4-31b-it` pago na lista e precisa ser corrigido). Um teste de guarda falha se um id pago entrar na lista.
- **D-16:** Cascata em **duas camadas**: (a) servidor — `models: [...]` + `route: "fallback"` na própria OpenRouter; (b) cliente — avanço explícito para o próximo modelo com **backoff 1,5s → 3s → 6s** (3 tentativas por modelo, honrando `Retry-After`).
- **D-17:** **Gatilhos de avanço**: HTTP 429 (taxa), HTTP 402 (cota), HTTP 5xx e **JSON inválido** (o `openrouter/free` pode cair num modelo fraco).
- **D-18:** Extração: **1 chamada por documento** (documento longo é dividido em chunks e as respostas são mescladas), `temperature = 0.0`, `max_tokens = 8000` — reduzido para 4000 se o modelo rejeitar o limite de saída.
- **D-19:** O chat existente (`AiService`) passa a **delegar** para a mesma cascata, preservando os defaults atuais (temperature 0.4, max_tokens 800) ⇒ os testes `AiServiceTest`/`AiIntegrationTest` continuam válidos.
- **D-20:** A resposta da IA é **JSON estrito**: o prompt injeta a **lista exata das categorias do tenant** (proíbe inventar) e exige um único objeto JSON sem markdown; o parse remove cercas ``` e valida campo a campo; item inválido vai para a lista `rejeitados` com motivo, **sem derrubar o documento** (IMP-06).
- **D-21:** A resposta da API expõe **`modeloUsado`** e **`degradado`** (`true` quando a resposta veio do secundário/terciário) — transparência sobre o modelo de fallback.
- **D-22:** Falha total da cascata ⇒ **503** com mensagem amigável (`ServicoIaIndisponivelException` já mapeada no `ProblemDetailExceptionHandler`); o app continua funcionando.

### Classificação, mapeamento e análise
- **D-23:** Mapeamento de tipos: `RECEITA` → `lancamento.tipo = ENTRADA`; `DESPESA_PAGA` → `lancamento.tipo = SAIDA`; `A_PAGAR` → `conta_a_pagar` (categoria do tipo **SAÍDA**).
- **D-24:** Categoria resolvida contra as categorias **reais** do tenant (normalização sem acento + sinônimos: `saude`→`Saúde/farmácia`, `alimentacao`→`Alimentação`, `salario`→`Rendimentos e reembolsos`, etc.). Sem correspondência ⇒ fallback (`Outros` para SAÍDA, `Outras receitas` para ENTRADA) + alerta **"revisar categoria"**; o usuário pode corrigir na tela de revisão.
- **D-25:** O resumo do mês traz: total recebido, total já pago, total comprometido a pagar, saldo previsto, total da fatura + data de vencimento, e **alertas**: duplicidade (item repetido), divergência entre a soma dos itens e o total da fatura (|diferença| > 0,05), e pagamento da própria fatura identificado no extrato.
- **D-26:** Gasto **atípico**: item cujo valor supera um limiar relativo dentro da própria categoria (ex.: > 2× a mediana da categoria) — a métrica exata é discricionária, desde que use `BigDecimal` e nunca `double`.

### Web (BFF e UI)
- **D-27:** BFF em `web/app/api/importacoes/route.ts` (multipart) e `.../[id]/confirmar/route.ts`: leem o cookie httpOnly `sess` e repassam o `Authorization: Bearer` — o browser **nunca** fala direto com a API.
- **D-28:** Limite da Vercel (corpo de requisição de 4,5 MB) ⇒ teto de **4 MB por arquivo** e **5 arquivos** por requisição, validado no BFF com mensagem clara para o usuário (o `application.yml` da API pode ser mais permissivo: 8 MB/arquivo).
- **D-29:** Tela de revisão: itens agrupados por tipo, **checkbox por item** (permite desmarcar) e **select de categoria** com o valor mapeado (permite override) antes de confirmar.
- **D-30:** Visual da página conforme a skill `design-taste-frontend` (exigida pelo `AGENTS.md`), seguindo o design system já existente (`components/ui/*`, tokens do `globals.css`).
- **D-31:** `TopNav` ganha o item "Importar PDF" apontando para `/importacamentos`.

### Agente tem discrição sobre
- Nomes/shape exato dos DTOs e dos endpoints de listagem (`GET /api/importacoes`), do payload de confirmação e do shape do JSON devolvido pela IA (desde que cubra IMP-04/IMP-05).
- Prompt textual da IA (desde que exija JSON único, sem markdown, e liste as categorias do tenant).
- Regex e regras exatas da sanitização (desde que cubra CPF, CNPJ e cartão 13–19 dígitos).
- Critério exato de "gasto atípico", número de chunks por documento e layout da tela de revisão.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisitos e escopo (locked)
- `.planning/REQUIREMENTS.md` — IMP-01..IMP-15 (fase 5); AI-02 (confirmação explícita); LAN-04/LAN-05 (BigDecimal, `NUMERIC(14,2)`, `HALF_EVEN`, rejeição de >2 casas); LAN-07 (valor positivo); AUTH-03 (tenant do token); LOT-01/LOT-02 (tudo-ou-nada + índice da linha) e DSH-05 (números do dashboard = SQL de conferência).
- `.planning/ROADMAP.md` — goal e success criteria da Fase 5.
- `.planning/PROJECT.md` — core value (saldo correto ao centavo), constraints de dinheiro/tenant/segurança, deploy Render+Vercel.
- `AGENTS.md` — fluxo GSD obrigatório, `design-taste-frontend` para UI, `security-audit` antes de deploy, TDD no backend.

### Código existente (analogo a seguir)
- `api/src/main/java/com/controlefinanceiro/ai/AiClient.java` — cliente OpenRouter atual (cascata, retry 429, `max_tokens`/`temperature`), a ser **generalizado** sem quebrar o chat.
- `api/src/main/java/com/controlefinanceiro/ai/AiService.java` — padrão de prompt, interpretação e `BigDecimal` (`parseValor`).
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` — padrão de service, validação monetária, `usuarioAtual()`, `@Transactional`.
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` — mapeamento de exceções para RFC 7807.
- `api/src/main/resources/db/migration/V1__schema.sql` — DDL de referência (NUMERIC(14,2), `empresa_id` em todas as tabelas).
- `api/src/main/java/com/controlefinanceiro/db/migration/V2__seed_categorias.java` — padrão de migration Java idempotente (a V3 pode ser SQL, no mesmo estilo da V1).
- `api/src/test/java/com/controlefinanceiro/ai/AiIntegrationTest.java` — padrão de teste de integração (Testcontainers + `@MockBean AiClient` + `TestRestTemplate`).
- `web/app/api/ai/chat/route.ts` — padrão de BFF (cookie `sess` → Bearer).
- `web/lib/api.ts` / `web/lib/api-client.ts` — padrão de client/server API da web.
- `web/components/LoteSheet.tsx`, `web/components/LancamentosClient.tsx` — padrões de UI de lista/diálogo já validados pelo design system.

### Documentação externa (verificada em 2026-11-12)
- OpenRouter Model Fallbacks — `models` + `route: "fallback"` troca de modelo no servidor; "para manter a requisição gratuita, liste apenas modelos `:free`".
- OpenRouter `/api/v1/models` — `google/gemma-4-31b-it:free` (ctx 262.144, out 32.768, US$ 0), `google/gemma-4-26b-a4b-it:free` (idem), `openrouter/free` (Free Models Router, ctx 200.000, US$ 0).

</canonical_refs>

<specifics>
## Specific Ideas

- **Exemplo de resposta da IA esperado** (contrato do prompt):
  `{"itens":[{"data":"2026-11-03","descricao":"Supermercado X","valor":"123.45","tipo":"DESPESA_PAGA","categoria":"Alimentação","origem":"EXTRATO","parcelaAtual":null,"parcelaTotal":null}],"fatura":{"total":"1500.00","vencimento":"2026-12-10"}}`
- **Exemplo de item de fatura**: `{"data":"2026-11-05","descricao":"Netflix","valor":"39.90","tipo":"A_PAGAR","categoria":"Assinaturas","origem":"CARTAO","parcelaAtual":2,"parcelaTotal":10}`.
- **Dicas de extração**: PDFBox `PDFTextStripper` com `setSortByPosition(true)` ajuda a recuperar linhas de extrato; normalizar espaços múltiplos antes de enviar à IA.
- **Erro conhecido a evitar**: o `application.yml` atual lista `google/gemma-4-31b-it` (pago) e `google/gemini-2.5-flash-lite` como fallback da fase 4 — corrigir para a cascata gratuita.
- **Filtro do dashboard**: `DashboardRepository` filtra `tipo = 'ENTRADA'`/`'SAIDA'` ⇒ a nova tabela `conta_a_pagar` não interfere, desde que nada seja gravado em `lancamento` com tipo `A_PAGAR`.

</specifics>

<deferred>
## Deferred Ideas

- **OCR local ou via API** para PDF digitalizado — adiado: exige custo ou dependência pesada.
- **Guardar o PDF original** (criptografado, para reprocessamento) — adiado por LGPD; hoje só o hash e o resultado estruturado ficam no banco.
- **Reimportação forçada** de um arquivo já processado (ignorar o bloqueio por hash) — adiado.
- **Detecção de recorrência** (mensalidade que se repete entre faturas) — adiado para v2.
- **Exportação do resultado da análise** para Excel/CSV — adiado para v2.

</deferred>

---
*Phase: 05-importa-o-de-pdfs-e-an-lise-por-ia*
*Context gathered: 2026-11-12 via discussão com o cliente*
