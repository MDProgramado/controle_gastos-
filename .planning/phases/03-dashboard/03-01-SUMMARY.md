---
phase: 03-dashboard
plan: 01
subsystem: api
tags: [spring-boot, java-21, jpa, postgresql, testcontainers, bigdecimal, dashboard, aggregation]

# Dependency graph
requires:
  - phase: 02-categorias-lancamentos
    provides: V2 schema with lancamento/categoria tables, empresa_id tenant isolation, TenantContext, JWT auth
provides:
  - GET /api/dashboard?ano=&mes= backend aggregation endpoint
  - DashboardController, DashboardService, DashboardRepository
  - DashboardResponse, CategoryExpense DTOs
affects:
  - 03-02 (frontend dashboard page consumes this endpoint)
  - 03-03 (frontend breakdown by category consumes this endpoint)
  - 04-ia (AI function calling reuses dashboard queries)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Controller fin delegando ao service (padrão P1)
    - Repository com @Query JPQL agregação SUM/GROUP BY
    - Record DTOs imutáveis sem empresa_id (R3 gate)
    - TenantContext.getEmpresaId() para isolamento de tenant
    - BigDecimal scale=2 HALF_EVEN para cálculos monetários (LAN-05)

key-files:
  created:
    - api/src/main/java/com/controlefinanceiro/dashboard/DashboardController.java
    - api/src/main/java/com/controlefinanceiro/dashboard/DashboardService.java
    - api/src/main/java/com/controlefinanceiro/dashboard/DashboardRepository.java
    - api/src/main/java/com/controlefinanceiro/dashboard/dto/DashboardResponse.java
    - api/src/main/java/com/controlefinanceiro/dashboard/dto/CategoryExpense.java
    - api/src/test/java/com/controlefinanceiro/dashboard/DashboardIntegrationTest.java
  modified:
    - api/src/main/resources/application.yml

key-decisions:
  - "Percentuais como String para suportar '—' quando base do mês anterior é zero (T-03-03)"
  - "LEFT JOIN no breakdown traz categorias zeradas, ordenado por valor decrescente (DSH-02)"
  - "@JsonSerialize(ToStringSerializer) nos campos BigDecimal como defesa em profundidade além do write-bigdecimal-as-plain"
  - "COALESCE(SUM(valor), 0) para meses vazios — nunca retorna null (DSH-05)"

patterns-established:
  - "DashboardController fino: apenas delega ao DashboardService, nunca contém regra de negócio"
  - "Todas as queries filtram por empresa_id via TenantContext, nunca aceitam empresa_id como parâmetro (T-03-01)"
  - "Calculos monetários em service com BigDecimal, scale=2, HALF_EVEN; nenhum double/float"

requirements-completed: [DSH-01, DSH-02, DSH-03, DSH-05]

# Metrics
duration: 15min
completed: 2026-11-09T10:41:02-03:00

# Phase 3 Plan 01: Dashboard Backend Aggregation Endpoint

**GET /api/dashboard?ano=&mes= endpoint de agregação com SUM/GROUP BY no backend, retornando totais de entradas/saídas/saldo, comparação com mês anterior e breakdown por categoria de SAÍDA (inclusive zeradas via LEFT JOIN), tudo filtrado por empresa_id do token JWT.**

## Performance

- **Duration:** 15 min
- **Started:** 2026-11-09T10:26:12Z
- **Completed:** 2026-11-09T10:41:02-03:00
- **Tasks:** 2/2 completed
- **Files modified:** 8

## Accomplishments

- GET /api/dashboard?ano=&mes= implementado com agregação SUM/GROUP BY no SQL (nunca soma na camada web)
- DashboardIntegrationTest passa (GREEN, 4 testes, Testcontainers + PostgreSQL real)
- Mês sem lançamentos retorna zeros (COALESCE), não null
- Percentual = "—" quando base do mês anterior é zero (sem divisão por zero)
- Dados filtrados por empresa_id do token (isolamento de tenant, T-03-01)
- Números em BigDecimal com scale=2, HALF_EVEN; nenhum double/float (LAN-05, T-03-02)

## Task Commits

Each task was committed atomically:

1. **Task 1: Escrever teste de integração falhando para GET /api/dashboard (RED)** - `5ff4db3` (test)
2. **Task 2: Implementar endpoint de agregação dashboard (GREEN)** - `c25e1c2` (feat)

## Files Created/Modified

- `api/src/main/java/com/controlefinanceiro/dashboard/DashboardController.java` - @RestController @RequestMapping("/api/dashboard"), @GetMapping delegando ao serviço
- `api/src/main/java/com/controlefinanceiro/dashboard/DashboardService.java` - @Service, lógica de agregação, cálculos monetários, percentuais
- `api/src/main/java/com/controlefinanceiro/dashboard/DashboardRepository.java` - @Repository, JPQL queries de agregação (totais por tipo + breakdown LEFT JOIN)
- `api/src/main/java/com/controlefinanceiro/dashboard/dto/DashboardResponse.java` - record DTO com totais, comparação, breakdown; BigDecimal com @JsonSerialize(ToStringSerializer)
- `api/src/main/java/com/controlefinanceiro/dashboard/dto/CategoryExpense.java` - record DTO item do breakdown (id, nome, total)
- `api/src/test/java/com/controlefinanceiro/dashboard/DashboardIntegrationTest.java` - teste de integração com Testcontainers (4 testes GREEN)
- `api/src/main/resources/application.yml` - spring.jackson.serialization.write-bigdecimal-as-plain: true

## Decisions Made

- Usar String para campos percentuais para suportar "—" quando base é zero, evitando divisão por zero (T-03-03)
- LEFT JOIN no breakdown garante que categorias sem lançamentos apareçam com total 0 (DSH-02)
- ToStringSerializer nos campos BigDecimal como defesa em profundidade além do write-bigdecimal-as-plain no application.yml
- COALESCE(SUM(valor), 0) em todas as queries de agregação para evitar null em meses vazios (DSH-05)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- Test assertions iniciais para `mesSemLancamentosRetornaZeros` e `endpointNaoExpõeEmpresaIdETenantIsolaDados` esperavam lista vazia / tamanho 1, mas o LEFT JOIN correto retorna todas as categorias SAÍDA (incluindo zeradas) — conforme DSH-02. Ajustadas as asserções para validar que todos os totais são zero e que Supermercado aparece com total 100.00. Não é desvio de plano, apenas correção de expectativa de teste.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Backend endpoint de dashboard completo e testado, pronto para consumo pelo frontend na 03-02
- Queries de agregação reutilizáveis pela Fase 4 (IA via OpenRouter)
- Nenhum bloqueio para a próxima wave

---
*Phase: 03-dashboard*
*Completed: 2026-11-09*
