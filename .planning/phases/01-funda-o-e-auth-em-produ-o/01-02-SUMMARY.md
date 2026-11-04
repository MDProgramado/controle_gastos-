---
phase: 01-funda-o-e-auth-em-produ-o
plan: 02
subsystem: api
tags: [spring-security, jwt, nimbus, oauth2-resource-server, bcrypt, flyway-java-migration, testcontainers, tenant-isolation, problem-detail]

# Dependency graph
requires:
  - phase: 01-funda-o-e-auth-em-produ-o (plan 01)
    provides: "Monorepo api/ com Flyway V1 (4 tabelas, empresa_id em todas), entidades Empresa/Usuario, /api/health e SecurityConfig mínima estensível"
provides:
  - "POST /api/auth/login emitindo JWT HS256 de 8h (D-01) com claims sub/empresa_id/role via oauth2ResourceServer (Nimbus)"
  - "Seed Flyway V1_1 (Java): 1 empresa + admin@controle.com ADMIN, hash BCrypt(12), senha só de SEED_ADMIN_PASSWORD (D-04)"
  - "TenantContext como ÚNICO ponto de leitura de empresa_id (R3/AUTH-03) — provado por teste com body hostil"
  - "GET /api/auth/me (sessão para o BFF do web — plano 03) + rotas 401 ProblemDetail genéricos (D-05)"
  - "Padrões P2 (TenantContext), P3 (SecurityFilterChain completa), P5 (seed Java), P6 (Testcontainers auth), P7 (ProblemDetail) — vinculantes para as fases 2-4"
affects: [02-crud-de-lancamentos-e-categorias, 03-dashboard, 04-deploy-render-vercel]

# Tech tracking
tech-stack:
  added: [spring-boot-starter-oauth2-resource-server (Nimbus embutido), maven-surefire env fixtures]
  patterns: [P2 TenantContext, P3 SecurityFilterChain completa, P5 V1_1 Java migration, P6 integração Testcontainers, P7 ProblemDetail RFC 7807]

key-files:
  created:
    - api/src/main/java/com/controlefinanceiro/db/migration/V1_1__seed_empresa_admin.java
    - api/src/main/java/com/controlefinanceiro/auth/AuthController.java
    - api/src/main/java/com/controlefinanceiro/auth/AuthService.java
    - api/src/main/java/com/controlefinanceiro/auth/JwtService.java
    - api/src/main/java/com/controlefinanceiro/auth/LoginRequest.java
    - api/src/main/java/com/controlefinanceiro/auth/LoginResponse.java
    - api/src/main/java/com/controlefinanceiro/auth/AuthMeController.java
    - api/src/main/java/com/controlefinanceiro/common/TenantContext.java
    - api/src/main/java/com/controlefinanceiro/common/UsuarioAuth.java
    - api/src/main/java/com/controlefinanceiro/common/UsuarioAuthAuthenticationToken.java
    - api/src/main/java/com/controlefinanceiro/common/CredenciaisInvalidasException.java
    - api/src/main/java/com/controlefinanceiro/common/NaoAutorizadoException.java
    - api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java
    - api/src/main/java/com/controlefinanceiro/usuario/UsuarioRepository.java
    - api/src/test/java/com/controlefinanceiro/AuthIntegrationTest.java
    - api/src/test/java/com/controlefinanceiro/AuthTenantIntegrationTest.java
    - api/src/test/java/com/controlefinanceiro/LoginRequestEstruturaTest.java
    - api/src/test/java/com/controlefinanceiro/probe/TenantProbeController.java
  modified:
    - api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java (ESTENDIDA — nunca 2ª config)
    - api/src/main/resources/application.yml (fail-on-unknown-properties: true)
    - api/pom.xml (oauth2-resource-server + fixtures surefire)

key-decisions:
  - "UsuarioAuthAuthenticationToken custom (Spring Security 6.5 não expõe mais o construtor JwtAuthenticationToken(jwt, principal, authorities)) — mesmo contrato: principal UsuarioAuth + authorities ROLE_*"
  - "JwtService emite E valida com o mesmo segredo (NimbusJwtEncoder + NimbusJwtDecoder/ImmutableSecret) — o resource-server usa jwt.secret da env; fail-fast se < 32 bytes (Pitfall 6)"
  - "UsuarioRepository.findByEmail com join fetch — necessário por open-in-view=false (sessão 1 transação)"
  - "TenantProbeController vive só em src/test com @Profile(\"it\") — nunca promover para main"
  - "Mensagem única genérica \"E-mail ou senha inválidos\" nos 3 casos (inexistente / senha errada / inativo) — anti-enumeração D-05"

patterns-established:
  - "P2: common/TenantContext.getEmpresaId() lê SOMENTE SecurityContext → principal UsuarioAuth — vale para TODAS as fases (PLAN §3.7.3)"
  - "P3: SecurityFilterChain completa (csrf off, STATELESS, permitAll login/health, hasRole ADMIN em /api/categorias/**, anyRequest authenticated, oauth2ResourceServer + converter)"
  - "P5: V1_1__seed_empresa_admin Java migration (System.getenv, BCrypt(12), fail-fast) — imutabilidade por convenção (INF-01); V2 livre para seed de categorias"
  - "P6: @Testcontainers + @ServiceConnection; fixtures surefire SEED_ADMIN_PASSWORD/JWT_SECRET óbvios (D-04)"
  - "P7: @RestControllerAdvice com ProblemDetail — 401 credenciais/não-autenticado, 400 validação/corpo"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, INF-01]

# Metrics
duration: ~2h40 de trabalho ativo (2 sessões: 2026-11-03 Tasks 1-2 + RED Task 3; 2026-11-04 GREEN Task 3)
completed: 2026-11-04
---

# Phase 01 Plan 02: Auth JWT de ponta a ponta Summary

**Login `POST /api/auth/login` emite JWT HS256 de 8h com claims `empresa_id`/`role` via oauth2ResourceServer (Nimbus), seed Flyway V1_1 com BCrypt(12) a partir de `SEED_ADMIN_PASSWORD`, e `TenantContext` provando por teste hostil que o tenant vem só do token — 16/16 testes verdes em `.\mvnw.cmd verify`.**

## Performance

- **Duration:** ~2h40 ativos — sessão 1 (Tasks 1-2 + RED Task 3): 2026-11-03T10:23→12:25 (-0300); sessão 2 (retomada, GREEN Task 3 + gates + SUMMARY): 2026-11-04T09:40→10:05 (-0300)
- **Started:** 2026-11-03T13:23Z (primeiro RED)
- **Completed:** 2026-11-04T13:05Z
- **Tasks:** 3/3
- **Files modified:** 20 (18 criados/estendidos em tasks + 2 reword de Javadoc)

## Accomplishments

- **Seed reproducível do zero:** `V1_1__seed_empresa_admin` (Java, sem DI Spring) lê `SEED_ADMIN_PASSWORD`, hash BCrypt custo 12 e insere 1 empresa + `admin@controle.com` ADMIN ativo; ausente/vazia → fail-fast com mensagem explícita. `senha_hash` no banco começa com `$2` e nunca é a senha em claro (AUTH-02/D-04)
- **Login JWT 8h (D-01):** `AuthService` (BCrypt matches + anti-enumeração D-05) → `JwtService` (`NimbusJwtEncoder` + `ImmutableSecret` HS256, segredo ≥ 32 bytes fail-fast) emite token com `sub`/`empresa_id`/`role`/`iat`/`exp`; `exp - iat ≈ 8h` assertado decodificando o token no teste
- **R3/AUTH-03 provado:** `TenantContext.getEmpresaId()` é o único ponto de leitura do tenant (SecurityContext → principal `UsuarioAuth`); `TenantProbeController` (só teste, `@Profile("it")`) ecoa o tenant com corpo hostil `{"empresa_id": 999}` → resposta `{"empresa_id": 1}`; gate estrutural por reflexão garante que `LoginRequest` nunca menciona "empresa"
- **SecurityConfig ESTENDIDA** (nunca 2ª config): stateless, `permitAll` login/health, `hasRole("ADMIN")` em `/api/categorias/**` (seed da regra Fase 2), `oauth2ResourceServer` + converter `claim empresa_id → principal UsuarioAuth → SecurityContext` (key-link do plano) — sem `JwtAuthFilter` manual (anti-pattern 2)
- **Erros RFC 7807 (P7):** senha errada e e-mail inexistente devolvem a MESMA 401 `"E-mail ou senha inválidos"`; `@Valid` → 400 listando campos; campo extra no body → 400 (`fail-on-unknown-properties: true` explícito no `application.yml`); sem/inválido/expirado token → 401

## Task Commits

Each task was committed atomically (TDD: RED → GREEN por task):

1. **Task 1 (RED): teste do seed** - `35d85b0` (test)
2. **Task 1 (GREEN): V1_1 seed migration** - `a124e02` (feat)
3. **Task 2 (RED): teste de integração de auth** - `7203a51` (test)
4. **Task 2 (GREEN): JWT login + SecurityConfig completa** - `06321c7` (feat)
5. **Task 3 (RED): testes tenant/R3** - `397239b` (test)
6. **Task 3 (GREEN): TenantContext + /api/auth/me + probe hostil** - `206c881` (feat)
7. **SUMMARY** - este commit (docs)

## Files Created/Modified

- `api/.../db/migration/V1_1__seed_empresa_admin.java` — seed fail-fast via `System.getenv`, BCrypt(12), PreparedStatement na connection do Flyway (não fecha)
- `api/.../config/SecurityConfig.java` — extensão do arquivo mínimo do plano 01 para P3 completo (csrf off, STATELESS, rotas, oauth2ResourceServer + converter)
- `api/.../config/ProblemDetailExceptionHandler.java` — 401 credenciais/não-autenticado, 400 validação e corpo malformado (P7)
- `api/.../auth/AuthController.java` + `AuthService.java` — `POST /api/auth/login`; 3 casos → mesma exceção genérica (D-05)
- `api/.../auth/JwtService.java` — emissão/validação HS256 (Nimbus), exp +8h (D-01), fail-fast segredo < 32 bytes
- `api/.../auth/LoginRequest.java` — record mínimo `email`/`senha` (`@Email @NotBlank`, `@Size(max=72)` Pitfall 5); `LoginResponse.java` — `{token}`
- `api/.../auth/AuthMeController.java` — `GET /api/auth/me` → `{email, empresa_id, role}` (validação de sessão do BFF — plano 03)
- `api/.../common/TenantContext.java` — único acesso ao tenant (R3); `UsuarioAuth.java` + `UsuarioAuthAuthenticationToken.java` — principal do SecurityContext
- `api/.../common/CredenciaisInvalidasException.java` + `NaoAutorizadoException.java` — exceções de auth/tenant → ProblemDetail
- `api/.../usuario/UsuarioRepository.java` — `findByEmail` join fetch (open-in-view=false)
- `api/src/main/resources/application.yml` — `spring.jackson.deserialization.fail-on-unknown-properties: true` (pré-req. do teste 3 da Task 2)
- `api/pom.xml` — `spring-boot-starter-oauth2-resource-server`; fixtures surefire `SEED_ADMIN_PASSWORD` + `JWT_SECRET` (óbvios, D-04)
- `api/src/test/.../AuthIntegrationTest.java` — behavior 1-4 da Task 2 (4 testes)
- `api/src/test/.../AuthTenantIntegrationTest.java` — body hostil, `/api/auth/me`, token expirado (3 testes, perfil `it`)
- `api/src/test/.../LoginRequestEstruturaTest.java` — gate estrutural R3 por reflexão (2 testes)
- `api/src/test/.../probe/TenantProbeController.java` — eco do tenant com corpo arbitrário (apenas teste)

## Decisions Made

- **`UsuarioAuthAuthenticationToken` custom:** o Spring Security 6.5 (Boot 3.5.16) não disponibiliza mais `JwtAuthenticationToken(jwt, principal, authorities)` como construtor público utilizável — criou-se um token equivalente com o MESMO contrato (principal `UsuarioAuth`, authorities `ROLE_*`), mantendo o key-link claim → principal → SecurityContext → `TenantContext` intacto.
- **Decoder no mesmo segredo:** `JwtService` também monta `NimbusJwtDecoder` a partir de `jwt.secret` — o resource-server e a emissão compartilham a mesma chave (1 serviço emite e valida, racional HS256 da pesquisa).
- **Probe em `src/test` + `@Profile("it")`:** isola o endpoint de teste do classpath de produção mesmo que o profile vaze em algum contexto — nunca promovido para `main`.
- **Mensagem única D-05:** `CredenciaisInvalidasException` é lançada em usuário inexistente, senha não casa e `ativo=false` com o MESMA string — o teste de integração assera igualdade das respostas.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Docker Desktop reiniciado para destravar o verify (Testcontainers)**
- **Found during:** Task 3 (verify `.\mvnw.cmd verify`)
- **Issue:** daemon Docker está parado/flapando (`Could not find a valid Docker environment`; `npipe dockerDesktopLinuxEngine` inexistente) — 4 testes de integração em erro; sem Docker não há passe honesto (Pitfall 9)
- **Fix:** `Start-Process 'C:\Program Files\Docker\Docker\Desktop.exe'` (1ª instância morreu em seguida; 2ª relançada e estável — 29.5.3, mesma versão da onda 1); nenhum código alterado
- **Verification:** `docker info` estável → `.\mvnw.cmd verify` **BUILD SUCCESS, 16/16**
- **Committed in:** n/a (ambiente, não código)

**2. [Rule 3 - Blocking] Gates de grep do plano falhavam por menções em Javadoc**
- **Found during:** Task 3 (verificação final `<verification>`)
- **Issue:** `grep "empresaId" .../LoginRequest.java` e `grep "JwtAuthFilter" api/src/main` casavam com o próprio texto dos comentários que documentam as proibições — os gates ficariam literalmente não-vazios
- **Fix:** reword de 2 linhas de Javadoc ("campo de tenant" / "filtro JWT manual"); zero mudança de lógica
- **Files modified:** `api/.../auth/LoginRequest.java`, `api/.../config/SecurityConfig.java`
- **Verification:** greps R7/R3/anti-pattern todos vazios; verify 16/16 após `spotless:apply`
- **Committed in:** `206c881`

---

**Total deviations:** 2 auto-fixed (2 Rule 3)
**Impact on plan:** Nenhum escopo extra — 1 é ambiente (Docker), 1 é só comentários para os gates literais passarem. Nenhuma mudança arquitetural (Rule 4 não acionada).

## Issues Encountered

- **Estado real do git difere do briefing de retomada:** o prompt de retomada descrevia a Task 2 como "draft não commitado", mas o repositório já continha `7203a51` (RED) + `06321c7` (GREEN, verify 11/11) da Task 2 e `397239b` (RED) da Task 3 — confiou-se no `git log` e NÃO se repetiu trabalho já commitado; apenas o GREEN da Task 3 foi concluído.
- **Docker Desktop fora do ar na sessão de retomada:** duas relançadas até o daemon ficar estável (ver desvio 1). Sem isso os verifies falham com `Could not start container` — nenhum teste foi contornado ou "falso-verde".
- **Heap limitado mantido:** `MAVEN_OPTS=-Xmx512m` + `-DargLine=-Xmx512m` em todos os verifies (host 7GB, padrão da onda 1) — OOM nativo do surefire não voltou a ocorrer.
- **Spotless (Google Java Format)** roda no `validate` de todo build — `spotless:apply` executado antes do verify final.

## User Setup Required

None for this plan — testes usam fixtures óbvios no surefire (`fixture-somente-testes-it`). As env vars reais de produção (`SEED_ADMIN_PASSWORD`, `JWT_SECRET` ≥ 32 bytes via `openssl rand -base64 48`) são checklist da Task 1 do plano 04 (deploy Render) — D-04: valor real vive só no env var do serviço.

## Next Phase Readiness

- **Fase 2 (plano 02-crud):** endpoints `/api/categorias/**` já têm a regra `hasRole("ADMIN")` no `SecurityFilterChain`; `TenantContext.getEmpresaId()` é o ponto único de tenant para todos os repositories futuros (R3 vale para todas as fases).
- **Plano 03 (web):** `POST /api/auth/login` → `{token}` e `GET /api/auth/me` → `{email, empresa_id, role}` são os dois contratos que o BFF do Next consumirá (P4).
- `ddl-auto=validate` + Flyway continuam donos do schema; `V2__` livre para seed de categorias (P5).

## Threat Flags

None — toda superfície criada (`AuthService`/`JwtService`/`SecurityConfig`/`TenantContext`/seed) já está no threat register do plano (T-2-01…T-2-SC) com disposition `mitigate`; nenhum endpoint, caminho de auth ou schema novo fora do `<threat_model>` foi introduzido.

---
*Phase: 01-funda-o-e-auth-em-produ-o*
*Completed: 2026-11-04*

## Self-Check: PASSED

- Arquivos criados/chave: 7/7 FOUND (V1_1, TenantContext, AuthMeController, TenantProbeController, AuthTenantIntegrationTest, LoginRequestEstruturaTest, SUMMARY.md)
- Commits verificados: 6/6 FOUND (35d85b0 RED T1, a124e02 GREEN T1, 7203a51 RED T2, 06321c7 GREEN T2, 397239b RED T3, 206c881 GREEN T3)
- Verify `.\mvnw.cmd verify`: BUILD SUCCESS, 16/16 testes (Auth 4, AuthTenant 3, Estrutura 2, Seed 3, Health 3, App 1)
- Gates: R7 `double|float` = 0; `empresaId` em LoginRequest = vazio; `JwtAuthFilter` em main = vazio; nenhum `*.log` versionado; zero deletações inesperadas em commits
- Nenhum STATE.md/ROADMAP.md alterado (escopo deste executor)
