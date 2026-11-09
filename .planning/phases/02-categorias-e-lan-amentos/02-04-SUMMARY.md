---
phase: 02-categorias-e-lan-amentos
plan: 04
subsystem: api
tags: [spring-boot, java-21, jpa, transactions, batch, problem-detail, testcontainers, postgres]

# Dependency graph
requires:
  - phase: 02-categorias-e-lan-amentos
    provides: "Lancamento CRUD + validação monetária estrita (02-03); Categoria read/write + V2 seed (02-01/02-02)"
provides:
  - "POST /api/lancamentos/lote — lote tudo-ou-nada de até 500 linhas"
  - "LinhaInvalidaException com índice base-0 da linha inválida"
  - "LoteDemasiadoGrandeException para o limite de 500 linhas"
  - "ProblemDetailExceptionHandler mapeando ambas as exceções para 400"
  - "Testes de integração de lote + seed V2 (Testcontainers)"
affects: [dashboard, ia-openrouter]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Batch all-or-nothing via @Transactional + per-line try/catch wrapping into indexed exception"
    - "Inner record LancamentoLinha.toRequest() reutiliza o DTO unitário do service"
    - "ProblemDetail com property extra (indiceLinha) além de detail"

key-files:
  created:
    - api/src/main/java/com/controlefinanceiro/common/LinhaInvalidaException.java
    - api/src/main/java/com/controlefinanceiro/common/LoteDemasiadoGrandeException.java
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoLoteRequest.java
    - api/src/test/java/com/controlefinanceiro/LancamentoLoteIntegrationTest.java
    - api/src/test/java/com/controlefinanceiro/V2SeedMigrationTest.java
  modified:
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java
    - api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java
    - api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java

key-decisions:
  - "processarLote retorna List<LancamentoResponse> e o controller responde 201 com o corpo criado (o teste já commitado exige corpo JSON não nulo; ver desvios)"
  - "Validação de valor fica no service (por linha), não em @Digits no DTO do lote, para preservar o índice da linha no erro"
  - "LinhaInvalidaException cobre RuntimeException de criar() e preserva a mensagem original como causa textual"
  - "empresa_id nunca no DTO do lote (R3/T-02-10) — sempre de TenantContext"

patterns-established:
  - "Lote: limite checado no service antes de iterar; cada linha delega a criar() reutilizando toda a validação unitária"
  - "Rollback total garantido pelo @Transactional externo em processarLote (self-invocation de criar() participa da mesma transação)"

requirements-completed: [LOT-01, LOT-02, INF-02]

# Metrics
duration: 15min
completed: 2026-11-09
---

# Phase 2 Plan 4: Lote tudo-ou-nada + testes de integração Summary

**Endpoint POST /api/lancamentos/lote transacional em lote de até 500 linhas, com rollback completo e índice da linha inválida no ProblemDetail 400**

## Performance

- **Duration:** ~15 min (Task 2; Task 1 RED já estava commitado)
- **Started:** 2026-11-09T07:07:00-03:00
- **Completed:** 2026-11-09T07:20:46-03:00
- **Tasks:** 2 (Task 1 pré-existente + Task 2 nesta execução)
- **Files modified:** 6 (3 novos, 3 alterados)

## Accomplishments
- `POST /api/lancamentos/lote` aceita lote em transação tudo-ou-nada (LOT-01), limitado a 500 linhas (T-02-09)
- Falha em qualquer linha reverte o lote inteiro e retorna 400 com o índice base-0 da linha (LOT-02, T-02-08)
- `empresa_id` extraído exclusivamente de `TenantContext` (R3 / T-02-10), nunca do corpo do lote
- Suíte completa `./mvnw verify` (api): **40 testes, 0 falhas, 0 erros** — inclui Phase 1 e os testes de lote/seed da Fase 2 (INF-02)

## Task Commits

Each task was committed atomically:

1. **Task 1: Testes de integração de lote + seed V2 (RED)** — `fe1da5b` (test) — *pré-existente, não reexecutado*
2. **Task 2: Implementar lote tudo-ou-nada (GREEN)** — `0a82ed4` (feat)

**Plan metadata:** _commit de docs abaixo_ (docs: complete plan)

## Files Created/Modified
- `api/src/main/java/com/controlefinanceiro/common/LinhaInvalidaException.java` — exceção com `int indiceLinha` + `getIndiceLinha()`, mensagem "Linha N inválida: ..."
- `api/src/main/java/com/controlefinanceiro/common/LoteDemasiadoGrandeException.java` — exceção de limite de 500 linhas
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoLoteRequest.java` — record `List<LancamentoLinha> linhas` + inner record com `toRequest()`; sem `empresa_id`
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java` — `@Transactional processarLote(...)` com check de limite, iteração indexada e rollback
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java` — `@PostMapping("/lote")` delegando ao service
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` — handlers 400 para ambas as exceções (com property `indiceLinha`)
- `api/src/test/java/com/controlefinanceiro/LancamentoLoteIntegrationTest.java` — lote válido, rollback com índice, limite >500 (Task 1, RED)
- `api/src/test/java/com/controlefinanceiro/V2SeedMigrationTest.java` — 16 categorias, flags pessoal, idempotência, subquery de empresa (Task 1, RED)

## Decisions Made
- Validação monetária permanece no service (por linha) em vez de `@Digits` no DTO do lote: garante que o erro carregue o índice da linha em vez de falhar na desserialização/validação do DTO.
- `processarLote` retorna as respostas dos lançamentos criados; controller responde 201 com esse corpo (ver desvio 1).
- `LinhaInvalidaException` embrulha `RuntimeException` de `criar()` preservando `ex.getMessage()` para diagnóstico.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking / Test-compatibility] Controller responde 201 com corpo em vez de `ResponseEntity<Void>` vazio**
- **Found during:** Task 2 (ao integrar com o teste de Task 1 já commitado)
- **Issue:** O plano especifica `ResponseEntity.status(201).build()` (corpo vazio), mas `LancamentoLoteIntegrationTest.processarLoteValidoCriaTodos` faz `mapper.readTree(response.getBody())` e exige `body != null`. Com corpo vazio, `readTree("")` retorna `null` e o teste falharia.
- **Fix:** `processarLote` retorna `List<LancamentoResponse>` e o controller responde `201` com o array de lançamentos criados — JSON válido, corpo não nulo.
- **Files modified:** `LancamentoController.java`, `LancamentoService.java`
- **Verification:** `LancamentoLoteIntegrationTest` passa (4/4); suíte completa 40/40.
- **Committed in:** `0a82ed4` (Task 2 commit)

**2. [Rule 2 - Missing Critical] ProblemDetail de `LinhaInvalidaException` inclui property `indiceLinha`**
- **Found during:** Task 2 (handler)
- **Issue:** O plano pede o índice na mensagem; expor também como property estruturada ajuda o cliente web e o ponto de controle do cliente sem custo.
- **Fix:** `problemDetail.setProperty("indiceLinha", ex.getIndiceLinha())` além do `detail` textual.
- **Files modified:** `ProblemDetailExceptionHandler.java`
- **Verification:** Teste de rollback verifica o `detail` (contém "1"); build GREEN.
- **Committed in:** `0a82ed4`

---

**Total deviations:** 2 auto-fixed (1 blocking/test-compatibility, 1 missing-critical)
**Impact on plan:** Ambos sem scope creep. Nenhum arquivo de teste de Task 1 foi modificado.

## Issues Encountered
- O daemon do Docker estava parado (reinício de servidor anterior). Iniciei o Docker Desktop e aguardei o daemon antes de rodar os testes Testcontainers — sem impacto no código.

## Stub Tracking
Nenhum stub introduzido. Endpoints e DTOs totalmente ligados ao service/repositório.

## Threat Flags
Nenhuma superfície nova além da prevista no threat model do plano (`POST /api/lancamentos/lote`, autenticado, tenant do JWT, limite de 500 linhas, transação all-or-nothing).

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Fase 2 concluída: seed V2, CRUD de categorias, CRUD de lançamentos e lote tudo-ou-nada prontos.
- Pronto para a Fase 3 (Dashboard), que consumirá os lançamentos persistidos.
- Pendências herdadas (não bloqueiam): rotação de segredos, upgrade do Postgres Render, staging ausente (ver 01-04-SUMMARY).

## Self-Check: PASSED

- Todos os arquivos criados/alterados existem no disco.
- Commits verificados: `fe1da5b` (RED) e `0a82ed4` (GREEN).
- `./mvnw verify` (api): 40 testes, 0 falhas, 0 erros.

---
*Phase: 02-categorias-e-lan-amentos*
*Completed: 2026-11-09*
