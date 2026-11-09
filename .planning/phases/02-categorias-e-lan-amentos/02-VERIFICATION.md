---
phase: 02-categorias-e-lan-amentos
verified: 2026-11-09T10:45:00Z
status: human_needed
score: 29/29 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Reformatar o goal da fase como User Story (modo MVP)"
    expected: "Rodar `/gsd mvp-phase 2` e reexecutar a verificação, OU aceitar explicitamente a verificação goal-backward padrão para um goal que não está no formato 'As a ..., I want to ..., so that ...'"
    why_human: "ROADMAP declara `**Mode:** mvp`, mas o goal da Fase 2 não é uma User Story. `gsd-sdk query user-story.validate` retorna `valid=false`. A seção obrigatória 'User Flow Coverage' não pode ser produzida com qualidade para um goal não-User-Story; a decisão de reformatar ou aceitar o formato atual é do desenvolvedor."
  - test: "Comportamento de categoria desativada em novos lançamentos"
    expected: "Segundo 02-RESEARCH.md:449, `ativo=false` deve bloquear novos lançamentos (400). O código atual NÃO verifica `ativo` em LancamentoService.criar/editar — a categoria desativada continua aceita (201)."
    why_human: "É uma área cinza que o CONTEXT mandou o planner resolver; o RESEARCH declarou resolvida ('bloqueia novos lançamentos'), mas o PLAN 02-02 e o código só implementam `ativo=false` sem bloqueio no lançamento. A semântica desejada nunca foi confirmada com o usuário. É necessário decidir: (a) implementar o bloqueio (gap de execução) ou (b) aceitar que categorias desativadas ainda possam receber lançamentos."
  - test: "Escopo de UI da Fase 2 (ROADMAP `UI hint: yes`)"
    expected: "Confirmar se a Fase 2 deveria entregar UI web de categorias/lançamentos ou se a entrega API-only é aceitável, com a UI ficando para a Fase 3 (Dashboard)."
    why_human: "O ROADMAP marca `UI hint: yes` para a Fase 2, mas os 4 PLANs e o código entregam apenas API. O diretório `web/` não tem nenhuma referência a categoria/lançamento. Nenhum Success Criterion exige UI, então só o desenvolvedor pode decidir se isso é escopo aceitável ou omissão."
---

# Phase 2: Categorias e Lançamentos Verification Report

**Phase Goal:** Seed V2 de categorias, CRUD de lançamentos com validação monetária estrita e lote tudo-ou-nada
**Verified:** 2026-11-09T10:45:00Z
**Status:** human_needed
**Re-verification:** No — initial verification (não existia `02-VERIFICATION.md`; apenas o output do plan-checker `VERIFICATION-RESULT.md`)

> **Nota de modo MVP:** `gsd-sdk query roadmap.get-phase 2 --pick mode` retorna `mvp`, mas o goal não está no formato de User Story (`user-story.validate` → `valid=false`). Conforme `references/verify-mvp-mode.md`, a discrepância é escalada abaixo em Human Verification Required. Nenhuma seção "User Flow Coverage" foi fabricada para um goal que não é User Story. A verificação técnica goal-backward padrão foi executada integralmente contra os 4 Success Criteria do ROADMAP e os `must_haves` dos 4 PLANs.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | **SC1:** Testes de integração (Testcontainers) passam em `./mvnw verify` | ✓ VERIFIED | `.\mvnw.cmd -q verify` (api) terminou com `EXIT:0`. Surefire: **Tests run: 40, Failures: 0, Errors: 0, Skipped: 0** (10 classes, timestamps da mesma execução). |
| 2 | **SC2:** Lote com uma linha inválida retorna o índice e **não grava nenhuma linha** | ✓ VERIFIED | `LancamentoService.processarLote` lança `LinhaInvalidaException(i, ...)` (linha 200) e é `@Transactional` (184); handler adiciona `indiceLinha` (linhas 81-86). Teste `LancamentoLoteIntegrationTest:101-138` verifica 400 e `SELECT COUNT(*) FROM lancamento = 0`. |
| 3 | **SC3:** Valor com mais de 2 casas decimais é rejeitado; nenhum `double`/`float` no código | ✓ VERIFIED | `validarValor`: `scale() > 2` → `MonetarioException` (LancamentoService:207-213); `@Digits(integer=12, fraction=2)` em `LancamentoRequest`; teste `valorComTresCasasDecimaisRejeitado` (LancamentoIntegrationTest:166). Grep de `double`/`float` só encontra ocorrências em **comentários** — nenhum tipo real. |
| 4 | **SC4:** Categoria ENTRADA rejeita lançamento de SAÍDA e vice-versa | ✓ VERIFIED | `validarIntegridadeCategoriaTipo` compara `categoria.getTipo()` com o tipo do lançamento e lança `CategoriaTipoIncompativelException` (LancamentoService:224-230), chamado em criar (68) e editar (113); lote reusa `criar`. Teste `categoriaTipoIncompativelRejeitado` (LancamentoIntegrationTest:198-211). |
| 5 | **02-01:** Usuário pode listar categorias via GET /api/categorias | ✓ VERIFIED | `CategoriaController:31-33` → `categoriaService.listar(TenantContext.getEmpresaId())`; teste `listarCategoriasRetorna16CategoriasSeedadas` (CategoriaIntegrationTest:98-134) valida 200 + 16 itens. |
| 6 | **02-01:** 16 categorias seedadas: 12 saídas e 4 entradas | ✓ VERIFIED | `V2__seed_categorias.java:27-46` contém 16 entradas (12 SAIDA, 4 ENTRADA); testes `seedCria16Categorias` (V2SeedMigrationTest:31-41) e contagem na API (CategoriaIntegrationTest:110-123). |
| 7 | **02-01:** Categorias [P] (Alimentação, Saúde/farmácia, Lazer, Rendimentos e reembolsos) têm `pessoal=true` | ✓ VERIFIED | Seed linhas 28-30 e 45 marcam exatamente esses 4 nomes; testes V2SeedMigrationTest:44-53 e CategoriaIntegrationTest:125-133. **Atenção:** o SUMMARY 02-01 afirma "5 pessoais" — a claim está errada; o código tem 4. |
| 8 | **02-01:** Categorias filtradas por `empresa_id` do token (tenant isolation, nunca do body) | ✓ VERIFIED | `CategoriaRepository.findByEmpresaId` filtra por `c.empresa.id` (linhas 20-22); controller usa `TenantContext.getEmpresaId()`; DTO sem `empresa_id`. `AuthTenantIntegrationTest:51-67` prova que `empresa_id` do body é ignorado. Teste `categoriasNaoExpoemEmpresaIdNoBodyR3` (137-153). |
| 9 | **02-01 / D-01:** Seed V2 como Java migration `V2__seed_categorias.java` | ✓ VERIFIED | Arquivo existe (98 linhas), `extends BaseJavaMigration`, `migrate(Context)` (linha 50), Flyway aplica "Migrating schema ... to version 2 - seed categorias" no log do teste. |
| 10 | **02-01 / D-02:** `empresa_id` via subquery, nunca hardcoded | ✓ VERIFIED | `SELECT id FROM empresa WHERE nome = 'Controle Financeiro'` (linhas 54-64), com fail-fast se ausente. |
| 11 | **02-01 / D-03:** Idempotência: checar se a categoria já existe antes de inserir | ✓ VERIFIED | `SELECT 1 FROM categoria WHERE empresa_id = ? AND nome = ? AND tipo = ?` + `continue` antes do INSERT (linhas 71-83). (Teste dedicado é fraco — ver Anti-Patterns.) |
| 12 | **02-01 / D-04:** Ordem da lista do cliente mantida na migration (12 SAIDA + 4 ENTRADA; [P]) | ✓ VERIFIED | Array `CATEGORIAS` nas linhas 27-46 segue exatamente a ordem e os flags do CONTEXT/PLAN. |
| 13 | **02-02:** ADMIN pode criar categoria via POST /api/categorias (201) | ✓ VERIFIED | `CategoriaController:37-40`; `CategoriaService.criar` (48-67); teste `criarCategoriaComSucessoRetorna201` (156-172). |
| 14 | **02-02:** ADMIN pode editar categoria via PUT /api/categorias/{id} (200) | ✓ VERIFIED | `CategoriaController:44-48`; `CategoriaService.editar` (77-94); teste `editarCategoriaComSucessoRetorna200` (188-207). |
| 15 | **02-02:** ADMIN pode desativar categoria via DELETE /api/categorias/{id} (204) | ✓ VERIFIED | `CategoriaController:51-53`; `CategoriaService.desativar` seta `ativo=false` (103-114); teste `desativarCategoriaRetorna204` (209-222). |
| 16 | **02-02:** USER pode listar (GET 200) mas não criar/editar/deletar (403) | ✓ VERIFIED | `SecurityConfig:42-45`: GET `/api/categorias` → `hasAnyRole(ADMIN, USER)`; `/api/categorias/**` (escrita) → `hasRole(ADMIN)`. Teste `usuarioNaoPodeCriarCategoriaRetorna403` (175-186). (Editar/deletar por USER cobertos por config, não por teste — ver Anti-Patterns.) |
| 17 | **02-02:** Categoria ENTRADA rejeita lançamento SAIDA e vice-versa (CAT-04) | ✓ VERIFIED | Implementado em `LancamentoService.validarIntegridadeCategoriaTipo` (chamado em criar/editar/lote); teste `categoriaTipoIncompativelRejeitado` (LancamentoIntegrationTest:198-211). |
| 18 | **02-02:** Nome único por (empresa_id, nome, tipo) | ✓ VERIFIED | Constraint `UNIQUE (empresa_id, nome, tipo)` em `V1__schema.sql:31`; pré-checagem `existsByEmpresaIdAndNomeAndTipo` em `CategoriaService.criar` (52-57). Ressalva: `editar` não faz pré-checagem (ver Anti-Patterns) — a unicidade ainda é garantida pelo banco. |
| 19 | **02-03 / LAN-01:** Usuário pode criar lançamento com data, tipo, valor, categoria e descrição | ✓ VERIFIED | `LancamentoController:33-36` + `LancamentoService.criar` (55-81); teste `criarLancamentoComSucessoRetorna201` (74-96). |
| 20 | **02-03 / LAN-02:** Usuário pode editar lançamento existente com as mesmas validações | ✓ VERIFIED | `LancamentoController:52-55` + `LancamentoService.editar` (91-123), que reusa `validarValor`, lookup por empresa, integridade de tipo; teste `editarLancamentoComSucessoRetorna200` (98-129). |
| 21 | **02-03 / LAN-03:** Usuário pode excluir lançamento | ✓ VERIFIED | `LancamentoController:59-61` + `LancamentoService.excluir` (131-143); teste `excluirLancamentoComSucessoRetorna204` (131-164). |
| 22 | **02-03 / LAN-04:** Valor com mais de 2 casas decimais é rejeitado, não arredondado | ✓ VERIFIED | `@Digits(fraction=2)` no DTO + defesa em profundidade `valor.scale() > 2` → 400 (LancamentoService:207-213); teste `valorComTresCasasDecimaisRejeitado` (166-180). |
| 23 | **02-03 / LAN-05:** Todo cálculo monetário usa BigDecimal, NUMERIC(14,2), scale=2, HALF_EVEN; nenhum double/float | ✓ VERIFIED (com nota) | `Lancamento.valor` é `BigDecimal` `@Column(precision=14, scale=2)` (linhas 50-51); banco `NUMERIC(14,2)` (V1:40); nenhum `double`/`float` real no código. **Nota:** a fase não contém nenhuma operação aritmética monetária — a cláusula `HALF_EVEN` é vacuamente satisfeita; só aparece em Javadoc. O uso efetivo de `HALF_EVEN` será exercitado na Fase 3 (dashboard). |
| 24 | **02-03 / LAN-06:** Usuário pode filtrar lançamentos por mês, tipo e categoria dentro da empresa | ✓ VERIFIED | Controller repassa `ano`, `mes`, `tipo`, `categoriaId` (LancamentoController:68-76); query `findByEmpresaIdAndFiltros` filtra `empresa.id` + `extract(year/month)` + tipo + categoria (LancamentoRepository:55-68); teste por tipo (LancamentoIntegrationTest:214-245). Ressalva: limite fixo de 50 linhas (ver Anti-Patterns). |
| 25 | **02-03 / LAN-07:** Valor aceito é sempre positivo; sinal vem do tipo | ✓ VERIFIED | `valor.signum() <= 0` → `MonetarioException` (LancamentoService:214-217); teste `valorNaoPositivoRejeitado` (182-196). |
| 26 | **02-04 / LOT-01:** Endpoint aceita lote de até 500 lançamentos em transação tudo-ou-nada | ✓ VERIFIED | `processarLote` é `@Transactional` (184) e rejeita `> 500` com `LoteDemasiadoGrandeException` (189-192); teste `loteExcedeLimite500Rejeitado` (141-170). |
| 27 | **02-04 / LOT-02:** Em erro, a resposta informa o índice da linha inválida e nenhuma linha é gravada | ✓ VERIFIED | Loop captura `RuntimeException` e relança `LinhaInvalidaException(i, ...)` (195-201); handler emite `indiceLinha` (ProblemDetailExceptionHandler:81-86); teste de rollback (101-138). |
| 28 | **02-04:** Todos os testes de integração (Testcontainers) passam em `./mvnw verify` (INF-02) | ✓ VERIFIED | `EXIT:0` + 40/0/0/0; `@Testcontainers`/`@Container @ServiceConnection` em CategoriaIntegrationTest:34-38, LancamentoIntegrationTest:34-38, LancamentoLoteIntegrationTest:31-35, V2SeedMigrationTest:21-25. |
| 29 | **02-04:** Lote com uma linha inválida faz rollback completo (zero linhas persistidas) | ✓ VERIFIED | `@Transactional` no método + exceção runtime; teste consulta o banco e exige `COUNT(*) = 0` (LancamentoLoteIntegrationTest:135-137). |

**Score:** 29/29 truths verified

### MVP Mode Note

A Fase 2 está marcada `mode: mvp` no ROADMAP, mas o goal não é uma User Story. Por instrução de `verify-mvp-mode.md` (linha 20), a discrepância é **escalada** em vez de gerar uma seção "User Flow Coverage" artificial. Ver item 1 de Human Verification Required.

### Required Artifacts

Verificação em 3 níveis (existência, substância, wiring). `gsd-sdk query verify.artifacts` passou 23/24; o único "fail" é falso negativo por regex inválida (`@ExceptionHandler\\(LinhaInvalidaException`) — o handler existe e foi verificado manualmente em `ProblemDetailExceptionHandler:81`.

| Artifact | Exists | Substantive | Wired | Status |
|----------|--------|-------------|-------|--------|
| `api/src/main/java/com/controlefinanceiro/db/migration/V2__seed_categorias.java` (98 linhas) | ✓ | ✓ 16 inserts + subquery + idempotência | ✓ Flyway aplica na inicialização | ✓ VERIFIED |
| `categoria/Categoria.java` (90) | ✓ | ✓ `@Entity @Table(name="categoria")`, `@ManyToOne Empresa` | ✓ usado por repository/service | ✓ VERIFIED |
| `categoria/CategoriaRepository.java` (55) | ✓ | ✓ queries por empresa + unicidade | ✓ injetado no service | ✓ VERIFIED |
| `categoria/CategoriaRequest.java` (14) | ✓ | ✓ record com Bean Validation | ✓ `@Valid @RequestBody` | ✓ VERIFIED |
| `categoria/CategoriaResponse.java` (16) | ✓ | ✓ record sem `empresa_id` | ✓ retornado pelos endpoints | ✓ VERIFIED |
| `categoria/CategoriaService.java` (123) | ✓ | ✓ criar/editar/desativar/listar + `validarTipo` | ✓ injetado no controller | ✓ VERIFIED |
| `categoria/CategoriaController.java` (56) | ✓ | ✓ GET/POST/PUT/DELETE | ✓ rotas `/api/categorias` + SecurityConfig | ✓ VERIFIED |
| `lancamento/Lancamento.java` (156) | ✓ | ✓ 3 `@ManyToOne`, `BigDecimal(14,2)` | ✓ usado pelo service/repository | ✓ VERIFIED |
| `lancamento/LancamentoRepository.java` (69) | ✓ | ✓ filtros por empresa/mês/ano/tipo/categoria | ✓ injetado no service | ✓ VERIFIED |
| `lancamento/LancamentoRequest.java` (22) | ✓ | ✓ `@Digits(fraction=2)`, sem tenant | ✓ `@Valid @RequestBody` | ✓ VERIFIED |
| `lancamento/LancamentoResponse.java` (23) | ✓ | ✓ record sem `empresa_id` | ✓ retornado | ✓ VERIFIED |
| `lancamento/LancamentoLoteRequest.java` (29) | ✓ | ✓ wrapper `linhas` + `toRequest()` | ✓ `@Valid` no controller | ✓ VERIFIED |
| `lancamento/LancamentoService.java` (242) | ✓ | ✓ CRUD + validação + integridade + lote | ✓ injetado no controller | ✓ VERIFIED |
| `lancamento/LancamentoController.java` (90) | ✓ | ✓ POST/GET/PUT/DELETE + `/lote` | ✓ rotas `/api/lancamentos` | ✓ VERIFIED |
| `common/CategoriaTipoIncompativelException.java` (12) | ✓ | ✓ runtime exception | ✓ lançada no service + handler | ✓ VERIFIED |
| `common/MonetarioException.java` (12) | ✓ | ✓ runtime exception | ✓ lançada no service + handler | ✓ VERIFIED |
| `common/LinhaInvalidaException.java` (27) | ✓ | ✓ carrega `indiceLinha` | ✓ lançada no lote + handler | ✓ VERIFIED |
| `common/LoteDemasiadoGrandeException.java` (13) | ✓ | ✓ runtime exception | ✓ lançada no lote + handler | ✓ VERIFIED |
| `common/CategoriaDuplicadaException.java` | ✓ | ✓ runtime exception | ✓ lançada no criar + handler | ✓ VERIFIED |
| `common/CategoriaNaoEncontradaException.java` | ✓ | ✓ runtime exception | ✓ lançada em categoria/lançamento + handler 404 | ✓ VERIFIED |
| `config/SecurityConfig.java` (78) | ✓ | ✓ split GET vs escrita | ✓ aplicado pelo Spring Security | ✓ VERIFIED |
| `config/ProblemDetailExceptionHandler.java` (94) | ✓ | ✓ 7 handlers + `indiceLinha` | ✓ `@RestControllerAdvice` | ✓ VERIFIED |
| `api/src/test/.../CategoriaIntegrationTest.java` (258) | ✓ | ✓ 8 testes (1 scaffolding RED) | ✓ roda no `verify` | ✓ VERIFIED |
| `api/src/test/.../LancamentoIntegrationTest.java` | ✓ | ✓ 8 testes (1 scaffolding RED) | ✓ roda no `verify` | ✓ VERIFIED |
| `api/src/test/.../LancamentoLoteIntegrationTest.java` | ✓ | ✓ 4 testes (1 scaffolding RED) | ✓ roda no `verify` | ✓ VERIFIED |
| `api/src/test/.../V2SeedMigrationTest.java` (81) | ✓ | ✓ 4 testes | ✓ roda no `verify` | ✓ VERIFIED |
| `api/src/main/resources/db/migration/V1__schema.sql` | ✓ | ✓ NUMERIC(14,2), CHECK(valor>0), UNIQUE(empresa,nome,tipo), FKs | ✓ Flyway aplica | ✓ VERIFIED |

### Key Link Verification

`gsd-sdk query verify.key-links` reportou falsos negativos por padrões regex mal escapados (`@ExceptionHandler\\(...` é regex inválida; `ManyToOne.*Categoria` não atravessa newline; `categoriaService\\.listar` existe). Todos os 12 links foram verificados manualmente:

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| CategoriaController | CategoriaService | constructor injection + `listar()` | ✓ WIRED | CategoriaController:27 e 33 |
| CategoriaService | TenantContext | `getEmpresaId()` | ✓ WIRED (via controller) | O service recebe `empresaId` como parâmetro; o controller obtém do `TenantContext` (CategoriaController:33,40,47,53). Intenção de tenant preservada. |
| V2__seed_categorias | V1__schema.sql | `INSERT INTO categoria` | ✓ WIRED | V2:87; tabela definida em V1:24-32 |
| CategoriaController | SecurityConfig | `hasRole("ADMIN")` protege escrita | ✓ WIRED (config) | SecurityConfig:42-45 |
| CategoriaService | CategoriaTipoIncompativelException | tipo inválido no `validarTipo` | ✓ WIRED | CategoriaService:117-121; CAT-04 cross-type fica no `LancamentoService:224-230` |
| ProblemDetailExceptionHandler | CategoriaTipoIncompativelException | `@ExceptionHandler` → 400 | ✓ WIRED | Handler:57-59 |
| Lancamento | Categoria | `@ManyToOne` + `@JoinColumn(categoria_id)` | ✓ WIRED | Lancamento:42-44 |
| LancamentoService | TenantContext | `empresaId` do token | ✓ WIRED (via controller) | LancamentoController:36,48,55,61,75; service recebe o id e filtra todas as queries |
| LancamentoService | MonetarioException | `scale()>2` / `signum()<=0` | ✓ WIRED | LancamentoService:210,216 |
| LancamentoController | LancamentoService | `processarLote` | ✓ WIRED | LancamentoController:44-48 |
| LancamentoService | LinhaInvalidaException | índice base-0 da linha | ✓ WIRED | LancamentoService:200 |
| ProblemDetailExceptionHandler | LinhaInvalidaException | 400 + `indiceLinha` | ✓ WIRED | Handler:81-86 (verificado manualmente; regex do tool era inválida) |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `GET /api/categorias` | `List<CategoriaResponse>` | `CategoriaRepository.findByEmpresaId` → PostgreSQL | ✓ 16 linhas seedadas (teste) | ✓ FLOWING |
| `POST/PUT/DELETE /api/categorias` | entidade `Categoria` | `categoriaRepository.save` → PostgreSQL | ✓ persistência confirmada por testes 201/200/204 | ✓ FLOWING |
| CRUD `/api/lancamentos` | `Lancamento` | `lancamentoRepository.save/find` → PostgreSQL | ✓ testes criam/leem/editam/excluem | ✓ FLOWING |
| `POST /api/lancamentos/lote` | lista de `LancamentoResponse` | `criar()` por linha → `save` → PostgreSQL | ✓ 3 linhas persistidas no caso válido; 0 no rollback | ✓ FLOWING |
| `V2__seed_categorias` | linhas `categoria` | JDBC INSERT → PostgreSQL | ✓ 16 linhas verificadas no banco | ✓ FLOWING |
| `GET /api/lancamentos` | `List<LancamentoResponse>` | query `findByEmpresaIdAndFiltros` → PostgreSQL | ✓ filtro por tipo validado | ✓ FLOWING (com truncamento de 50 — ver Anti-Patterns) |

Nenhum prop/estado hardcoded vazio encontrado; os endpoints são server-side e todas as respostas vêm de queries reais.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Suíte de integração completa | `.\mvnw.cmd -q verify` em `api/` | `EXIT:0` | ✓ PASS |
| Contagem de testes | Soma dos `target/surefire-reports/*.txt` | `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0` | ✓ PASS |
| Nenhum double/float real | `Select-String "\b(double|float|Double|Float)\b"` em main+test | Apenas 3 ocorrências, todas em comentários Javadoc | ✓ PASS |
| Commits dos 4 planos existem | `git log --oneline` | `8a6d314`, `5c1bedc`, `ec132ca`, `dcb9d5a`, `b548696`, `fe1da5b`, `0a82ed4` presentes | ✓ PASS |
| Artefatos dos PLANs | `gsd-sdk query verify.artifacts` | 23/24 (1 falso negativo de regex; verificado manual) | ✓ PASS |
| Key links dos PLANs | `gsd-sdk query verify.key-links` | Falsos negativos de regex; 12/12 confirmados manualmente | ✓ PASS |

### Probe Execution

Nenhum probe declarado nos PLANs/SUMMARYs e nenhum `scripts/*/tests/probe-*.sh` encontrado. Verificação por probe não se aplica a esta fase (API Java + Testcontainers). **SKIPPED (no probes declared/found).**

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CAT-01 | 02-01 | Categoria possui tipo ENTRADA ou SAIDA | ✓ SATISFIED | `Categoria.tipo` (34-35) + `validarTipo` (CategoriaService:117-121) |
| CAT-02 | 02-01 | Seed V2 cria 12 saída + 4 entrada | ✓ SATISFIED | V2 seed + testes de contagem |
| CAT-03 | 02-01 | Categorias [P] têm `pessoal=true` | ✓ SATISFIED | 4 nomes marcados; testes V2SeedMigrationTest:44-53 |
| CAT-04 | 02-02 | ENTRADA rejeita SAIDA e vice-versa | ✓ SATISFIED | `validarIntegridadeCategoriaTipo` (LancamentoService:224-230) |
| CAT-05 | 02-02 | ADMIN cria/edita/desativa; USER apenas lista | ✓ SATISFIED | SecurityConfig:42-45 |
| CAT-06 | 02-01 | Categoria pertence à empresa do token; nome único por (empresa,tipo) | ✓ SATISFIED | FK/unique no schema; queries por empresa; pré-checagem em criar |
| LAN-01 | 02-03 | Criar lançamento | ✓ SATISFIED | Service/Controller + teste 201 |
| LAN-02 | 02-03 | Editar lançamento com mesmas validações | ✓ SATISFIED | `editar` reusa validações; teste 200 |
| LAN-03 | 02-03 | Excluir lançamento | ✓ SATISFIED | `excluir`; teste 204 |
| LAN-04 | 02-03 | Máx. 2 casas; rejeita (não arredonda) | ✓ SATISFIED | `@Digits` + `scale()>2`; teste 400 |
| LAN-05 | 02-03 | BigDecimal/NUMERIC(14,2)/scale=2/HALF_EVEN; sem double/float | ✓ SATISFIED (HALF_EVEN vacuous) | Entity/DB corretos; sem double/float; nenhuma aritmética nesta fase |
| LAN-06 | 02-03 | Filtrar por mês, tipo e categoria na empresa | ✓ SATISFIED | Query `findByEmpresaIdAndFiltros`; teste por tipo |
| LAN-07 | 02-03 | Valor sempre positivo; sinal pelo tipo | ✓ SATISFIED | `signum()<=0` → 400; teste |
| LOT-01 | 02-04 | Lote até 500 em transação tudo-ou-nada | ✓ SATISFIED | `@Transactional` + limite 500 + teste |
| LOT-02 | 02-04 | Erro informa índice e não grava nada | ✓ SATISFIED | `LinhaInvalidaException` + `indiceLinha` + teste de rollback |
| INF-02 | 02-04 | Testes de integração com Testcontainers (Postgres real) | ✓ SATISFIED | 40 testes, 0 falhas, Postgres 16 via Testcontainers |

**Cobertura:** 16/16 requirements satisfeitos; nenhum requirement órfão (todos os IDs do ROADMAP aparecem no frontmatter de algum PLAN). **Bookkeeping:** `REQUIREMENTS.md` ainda marca CAT-04, CAT-05 e LAN-01..07 como `[ ]`/`Pending`, apesar de implementados e testados — ver Anti-Patterns.

### Anti-Patterns Found

Nenhum `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` real (busca case-sensitive nos fontes da fase: zero). Nenhum `return null`, `return {}`, `return []` ou handler vazio. As ocorrências de "todo" em português ("todo cálculo") são falsos positivos do grep case-insensitive.

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `categoria/CategoriaService.java` | 78-93 | `editar` não faz pré-checagem de duplicidade | ⚠️ WARNING | Se o novo nome/tipo colidir, o `DataIntegrityViolationException` do banco não tem handler → provável **500** em vez de 400. A unicidade em si continua garantida pelo banco. |
| `lancamento/LancamentoService.java` | 166 | `PageRequest.of(0, 50)` hardcoded | ⚠️ WARNING | `GET /api/lancamentos` trunca silenciosamente em 50 registros; não há parâmetro de paginação. Pode esconder lançamentos do usuário. |
| `lancamento/LancamentoController.java` | 80-88 | `buscarPorId` lista os primeiros 50 e filtra em memória | ⚠️ WARNING | `GET /api/lancamentos/{id}` retorna 404 para registros fora dos 50 mais recentes. (Endpoint não exigido pelos requirements, mas está exposto.) |
| `lancamento/LancamentoService.java` | 55-81, 91-123 | Nenhuma checagem de `categoria.ativo` | ⚠️ WARNING | Categoria desativada (`ativo=false`) continua aceita em novos lançamentos. Divergente do "RESOLVED" em 02-RESEARCH.md:449. Ver Human Verification #2. |
| `LancamentoLoteIntegrationTest.java` | 133 | `assertThat(detalhe).contains("1")` | ⚠️ WARNING | Não valida a property `indiceLinha` do ProblemDetail; poderia passar por coincidência textual. O handler está correto (linha 85), mas a asserção é fraca. |
| `V2SeedMigrationTest.java` | 56-65 | `migrationV2Idempotente` não reaplica V2 | ⚠️ WARNING | Compara `COUNT` antes/depois sem executar a migration de novo; não prova idempotência. A idempotência do código foi verificada por inspeção (SELECT-before-INSERT). |
| `V2SeedMigrationTest.java` | 68-80 | `empresaIdViaSubqueryNaoHardcoded` não prova subquery | ⚠️ WARNING | Apenas verifica que as 16 categorias pertencem à única empresa existente; não distingue subquery de valor hardcoded. A subquery foi verificada no código (V2:54-64). |
| `CategoriaIntegrationTest.java` | 90 | `categoriaRequestNaoExisteAindaRed` | ⚠️ WARNING | Teste de scaffolding da fase RED permaneceu na suíte (trivial e sem valor). |
| `LancamentoIntegrationTest.java` | 65 | `lancamentoRequestNaoExisteAindaRed` | ⚠️ WARNING | Idem. |
| `LancamentoLoteIntegrationTest.java` | 56 | `lancamentoLoteRequestNaoExisteAindaRed` | ⚠️ WARNING | Idem. |
| `CategoriaIntegrationTest.java` | 175-186 | Só testa USER→POST 403 | ℹ️ INFO | USER→PUT/DELETE 403 são garantidos pela config (`/api/categorias/**` → ADMIN), mas não têm teste dedicado. |
| `LancamentoService.java` | 101, 139 | `CategoriaNaoEncontradaException` para lançamento inexistente | ℹ️ INFO | HTTP 404 correto, mas o nome da exceção é semanticamente errado para lançamentos. |
| `CategoriaService.java` | 35-36 | `listar` não filtra `ativo=true` | ℹ️ INFO | Após DELETE, a categoria desativada continua aparecendo no GET (com `ativo=false` no DTO). Pode ser intencional para o ADMIN. |
| `02-01-SUMMARY.md` | 12, 58 | "5 pessoais [P]" | ℹ️ INFO | Claim do SUMMARY incorreta: o código/plan têm **4** categorias pessoais. Reforça que SUMMARY não é evidência. |
| `.planning/REQUIREMENTS.md` | — | Checkboxes `[ ]`/`Pending` para CAT-04, CAT-05, LAN-01..07 | ℹ️ INFO | Bookkeeping desatualizado; implementação e testes existem. |

### Human Verification Required

#### 1. Reformatar o goal da fase como User Story (MVP mode)

**Test:** Rodar `/gsd mvp-phase 2` para reformatar o goal, ou confirmar explicitamente que a verificação goal-backward padrão é aceitável.
**Expected:** Goal no formato "As a ..., I want to ..., so that ..."; nova verificação produz a seção "User Flow Coverage".
**Why human:** `mode: mvp` no ROADMAP, mas `gsd-sdk query user-story.validate --story "<goal>" --pick valid` retorna `false`. A seção obrigatória de MVP não pode ser produzida com qualidade. É uma decisão de processo do desenvolvedor.

#### 2. Comportamento de categoria desativada em novos lançamentos

**Test:** 1) Criar categoria via POST; 2) desativá-la via DELETE; 3) tentar criar um lançamento apontando para ela.
**Expected (research):** 400 — `ativo=false` bloqueia novos lançamentos (02-RESEARCH.md:449). **Actual (código):** o lançamento é aceito (201), pois `LancamentoService.criar/editar` não consulta `categoria.isAtivo()`.
**Why human:** A semântica nunca foi confirmada com o usuário (área cinza do CONTEXT). Decidir entre implementar o bloqueio (correção) ou aceitar o comportamento atual.

#### 3. Escopo de UI da Fase 2 (`UI hint: yes`)

**Test:** Confirmar se a Fase 2 deveria ter UI web de categorias/lançamentos.
**Expected:** ou aceitar API-only (UI na Fase 3) ou registrar a UI como pendência.
**Why human:** O ROADMAP marca `UI hint: yes`, mas os PLANs entregam só API e `web/` não tem nenhuma referência a categoria/lançamento. Nenhum Success Criterion exige UI; só o desenvolvedor pode decidir se é aceitável.

### Gaps Summary

**Nenhum gap bloqueante.** Todos os 4 Success Criteria do ROADMAP e todos os 29 `must_haves` dos 4 PLANs foram verificados no código, não apenas nos SUMMARYs. A suíte de integração real (Testcontainers + PostgreSQL 16) passou com **40 testes, 0 falhas, 0 erros, 0 skipped** e `EXIT:0`, incluindo as asserções de rollback do lote e de rejeição monetária.

Os achados não-bloqueantes são de três naturezas:

1. **Qualidade/robustez** — `editar` de categoria sem pré-checagem de duplicidade (500 em colisão), paginação fixa de 50 em `GET /api/lancamentos` e `GET /{id}` limitado aos 50 primeiros, e 3 testes de scaffolding RED remanescentes. Nenhum viola um Success Criterion ou requirement.
2. **Cobertura de teste fraca** — idempotência/subquery da V2 e a property `indiceLinha` do lote são verificadas por asserções mais fracas que o ideal; o código correspondente foi inspecionado e está correto.
3. **Decisões humanas pendentes** — formato do goal em modo MVP, semântica de categoria desativada (research vs. código) e escopo de UI (`UI hint: yes` vs. entrega API-only).

**Classificação:** `human_needed` — a implementação atende ao contrato técnico da fase, mas há 3 decisões que não podem ser resolvidas programaticamente. Nenhum item exige retrabalho de código antes da decisão humana; se o usuário decidir que categoria desativada deve bloquear lançamentos, isso vira um gap de execução pontual (`LancamentoService.criar/editar` + teste).

---

_Verified: 2026-11-09T10:45:00Z_
_Verifier: the agent (gsd-verifier)_
