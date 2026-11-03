---
phase: 01-funda-o-e-auth-em-produ-o
plan: 01
subsystem: api
tags: [spring-boot, java-21, flyway, postgresql, testcontainers, spring-security, nextjs, shadcn]

# Dependency graph
requires: []
provides:
  - Monorepo api/ (Spring Boot 3.5.16, Java 21) + web/ (Next.js 16.4.3, shadcn) com builds verdes
  - Flyway V1__schema.sql (4 tabelas, NUMERIC(14,2) CHECK(valor>0), empresa_id em todas) aplicado em PostgreSQL real via Testcontainers
  - GET /api/health público (SELECT 1) + SecurityConfig mínima extensível (P3) para a Fase 2
  - Padrões P1 (pacotes), P6 (Testcontainers @ServiceConnection), P8 (deploy-proof config) estabelecidos
affects: [02-crud-de-lancamentos-e-categorias, 03-dashboard, 04-ia-openrouter]

# Tech tracking
tech-stack:
  added: [spring-boot 3.5.16, spring-security, spring-data-jpa, flyway-core, flyway-database-postgresql, postgresql driver, testcontainers, next.js 16.4.3, shadcn/ui]
  patterns: [P1 pacotes backend, P3 SecurityFilterChain (versão mínima), P6 Testcontainers @ServiceConnection, P8 deploy-proof config]

key-files:
  created:
    - api/src/main/resources/db/migration/V1__schema.sql
    - api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java
    - api/src/main/java/com/controlefinanceiro/empresa/Empresa.java
    - api/src/main/java/com/controlefinanceiro/usuario/Usuario.java
    - api/src/main/java/com/controlefinanceiro/health/HealthController.java
    - api/src/test/java/com/controlefinanceiro/HealthIntegrationTest.java
  modified:
    - .gitignore
    - api/pom.xml (Task 1)
    - api/src/main/resources/application.yml (Task 1)

key-decisions:
  - "Controller custom /api/health (não actuator) — caminho exigido é /api/health (RESEARCH Pattern 4)"
  - "SecurityConfig mínima nesta onda (csrf off, STATELESS, permitAll /api/health); plano 02 estende o MESMO arquivo para P3 completo com Bearer Nimbus"
  - "senha_hash mapeado com @JsonIgnore — nunca serializado em resposta (T-1-04)"
  - "Gate R1/R3 automatizado via information_schema/pg_constraint no teste (não asserção manual)"
  - "Verifies de teste rodam com heap limitado (-DargLine=-Xmx512m) — host de 7GB já derruvou o JVM do surefire com OOM nativo"

patterns-established:
  - "P1: pacotes com.controlefinanceiro.{config,empresa,usuario,health} — fases 2-4 seguem o mesmo formato"
  - "P6: @Testcontainers + @Container @ServiceConnection PostgreSQLContainer postgres:16-alpine"
  - "P5: V1__schema.sql imutável (comentário INF-01) — correção só via nova V<n>__"
  - "P8: ddl-auto=validate (Flyway é dono do schema), ${PORT:8080}, release=21"

requirements-completed: [INF-01]

# Metrics
duration: 45min
completed: 2026-11-03
---

# Phase 01 Plan 01: Fundação do monorepo Summary

**Flyway V1 (4 tabelas, NUMERIC(14,2) com CHECK(valor>0), empresa_id em todas) aplicado de zero em PostgreSQL real via Testcontainers, com GET /api/health 200 OK e gate R1/R3 automatizado em 3 testes verdes.**

## Performance

- **Duration:** ~45 min (execução da Task 2; Task 1 concluída em sessão anterior)
- **Started:** 2026-11-03T09:30Z (retomada após 2 restarts do servidor)
- **Completed:** 2026-11-03T09:44Z
- **Tasks:** 2/2
- **Files modified:** 8 (6 criados + .gitignore + draft do teste corrigido)

## Accomplishments

- Monorepo com build verde: `api/` (`.\mvnw.cmd verify` — compilação + spotless + Testcontainers) e `web/` (Task 1)
- `V1__schema.sql` DDL literal do PLAN §3.4 sobe do zero em `postgres:16-alpine` — 4 tabelas, 3 índices, `empresa_id` em todas
- Gate R1 automatizado: `lancamento.valor` = `NUMERIC(14,2)` + `CHECK (valor > 0)` + FKs NOT NULL assertados contra `information_schema`/`pg_constraint`
- `GET /api/health` responde 200 `{"status":"OK"}` com `SELECT 1` (SecurityConfig mínima permite o acesso sem expor o resto da API)

## Task Commits

Each task was committed atomically:

1. **Task 1: Scaffold do monorepo api/ + web/** - `0af5be9` (feat)
2. **Task 2 (RED): teste de integração** - `2ce266d` (test)
3. **Task 2 (GREEN): Flyway V1 + entidades + health + SecurityConfig** - `e02e437` (feat)
4. **Task 2 (ambiente): .gitignore JVM crash logs** - `8514153` (chore)
5. **SUMMARY** - este commit (docs)

## Files Created/Modified

- `api/src/main/resources/db/migration/V1__schema.sql` — DDL imutável §3.4 (empresa, usuario, categoria, lancamento + 3 índices)
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java` — cadeia mínima STATELESS; plano 02 a estende (nunca um segundo config)
- `api/src/main/java/com/controlefinanceiro/empresa/Empresa.java` — entidade tabela empresa
- `api/src/main/java/com/controlefinanceiro/usuario/Usuario.java` — entidade tabela usuario, `senha_hash` `@JsonIgnore`
- `api/src/main/java/com/controlefinanceiro/health/HealthController.java` — `GET /api/health` → `SELECT 1` → `{"status":"OK"}`
- `api/src/test/java/com/controlefinanceiro/HealthIntegrationTest.java` — 3 testes (health, flyway history, gates R1/R3)
- `.gitignore` — adicionados `hs_err_pid*`, `replay_pid*`, `*.log`

## Decisions Made

- **Controller custom em vez de actuator:** o caminho exigido é `/api/health` (RESEARCH Pattern 4) — decisão já registrada no plan/research, executada aqui.
- **SecurityConfig mínima agora, completa no plano 02:** evita 401 no teste de saúde sem pular a definição das regras por rota/Bearer (P3) antes do escopo de auth existir.
- **Heap limitado nos verifies:** `-DargLine=-Xmx512m` + `MAVEN_OPTS=-Xmx512m` nas execuções locais de teste (ver Issues Encountered).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Asserção do CHECK corrigida para a normalização do Postgres**
- **Found during:** Task 2 (verify `.\mvnw.cmd verify -Dtest=HealthIntegrationTest`)
- **Issue:** `pg_get_constraintdef` do Postgres retorna `CHECK ((valor > (0)::numeric))` — a asserção literal `contains("valor > 0")` falhou mesmo com o DDL correto
- **Fix:** normaliza `::numeric`/parênteses antes de asserir `valor > 0` no teste
- **Files modified:** `api/src/test/java/com/controlefinanceiro/HealthIntegrationTest.java`
- **Verification:** verify reexecutado — 3/3 verdes
- **Committed in:** `e02e437` (part of GREEN commit; a versão inicial do teste foi o RED `2ce266d`)

**2. [Rule 3 - Blocking] Logs de crash do JVM removidos e ignorados**
- **Found during:** Task 2 (estado inicial)
- **Issue:** artefatos `hs_err_pid*.log`/`replay_pid*.log` de 2 execuções interrompidas (OOM nativo no surefire — host 7GB com Docker) ficaram órfãos e não podiam ser commitados
- **Fix:** logs deletados do working tree; `.gitignore` recebeu `hs_err_pid*`, `replay_pid*`, `*.log`
- **Files modified:** `.gitignore` (4 logs removidos, não versionados)
- **Verification:** `git status` limpo de logs; grep `*.log` fora de artefatos = 0
- **Committed in:** `8514153`

---

**Total deviations:** 2 auto-fixed (1 Rule 1, 1 Rule 3)
**Impact on plan:** Nenhum escopo extra — ambos corrigem para atingir os verifies do plano. Nenhuma mudança arquitetural (Rule 4 não acionada).

## Issues Encountered

- **OOM nativo do JVM nos verifies (interrupções anteriores):** o crash (`Native memory allocation (malloc) failed`, arena.cpp) ocorria no fork do surefire com heap default em host de 7GB com Docker Desktop (~3.8GB para o daemon). Mitigação executada nos verifies: `MAVEN_OPTS=-Xmx512m` e `-DargLine=-Xmx512m` — sem isso o JVM pode morrer de novo; isso é flag de execução, não alteração de código (o `pom.xml` do plano não muda). Docker 29.5.3 disponível e Testcontainers funcionou normalmente.
- **Spotless (Google Java Format)** roda no `validate` de todo build — arquivos passaram por `spotless:apply` antes do verify.
- Dois executores anteriores foram interrompidos por restart do servidor antes da Task 2; esta sessão retomou do ponto (draft do teste já existia) sem repetir a Task 1.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Pronto para a Fase 2 (plano 02): estender `SecurityConfig` (P3 completo), seed `V1_1__seed_empresa_admin` + `V2__seed_categorias`, CRUD de categorias/lançamentos sobre o schema V1 já validado.
- `ddl-auto=validate` garante que divergências futuras entre entidade e schema apareçam no build.

---
*Phase: 01-funda-o-e-auth-em-produ-o*
*Completed: 2026-11-03*

## Self-Check: PASSED

- Arquivos criados: 7/7 FOUND (V1__schema.sql, SecurityConfig, Empresa, Usuario, HealthController, HealthIntegrationTest, SUMMARY.md)
- Commits verificados: 4/4 FOUND (0af5be9 Task 1, 2ce266d RED, e02e437 GREEN, 8514153 chore)
- Verify `.\mvnw.cmd verify -Dtest=HealthIntegrationTest`: BUILD SUCCESS, 3/3 testes
- Gates: R7 `double|float` = 0; `ddl-auto` somente `validate`; zero `*.log` versionados; nenhum deletamento inesperado em commits
