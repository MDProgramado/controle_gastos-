# Phase 1: Fundação e Auth em Produção - Pattern Map

**Mapped:** 2026-10-30
**Files analyzed:** 0 (greenfield)
**Analogs found:** 0 / N — **repositório greenfield**: apenas `.opencode/` (GSD) e `.planning/` existem; glob `**/*.{java,ts,tsx,sql,xml}` não retornou nenhum arquivo de aplicação.

## Status: NO EXISTING ANALOGS — Phase 1 ESTABLISHES the patterns

Não há código de aplicação no repositório. Todos os arquivos abaixo são **criados do zero** nesta fase. Em vez de copiar de analogos locais, cada arquivo segue o **padrão canônico** extraído de `.planning/PLAN.md` §3 (arquitetura) e `.planning/phases/01-funda-o-e-auth-em-produ-o/01-RESEARCH.md` (padrões verificados em docs oficiais). **Os padrões definidos aqui são vinculantes para as fases 2–4** (Fase 2: seed V2 + CRUD lançamentos; Fase 3: dashboard; Fase 4: IA).

## File Classification

| New File | Role | Data Flow | Pattern Source (no local analog) | Match Quality |
|----------|------|-----------|----------------------------------|---------------|
| `api/pom.xml` | config | — | RESEARCH.md Standard Stack (Boot 3.5.16 parent BOM) | canonical (RESEARCH) |
| `api/src/main/resources/application.yml` | config | — | RESEARCH Pitfall 1 (`server.port=${PORT:8080}`) + PLAN §3.2 | canonical (RESEARCH) |
| `api/.../config/SecurityConfig.java` | config/middleware | request-response | RESEARCH Pattern 2 (resource-server + JwtAuthenticationConverter) | canonical (RESEARCH) |
| `api/.../auth/AuthController.java` | controller | request-response | PLAN §3.3 (controller valida formato, delega) + RESEARCH Pattern 2 | canonical (RESEARCH) |
| `api/.../auth/JwtService.java` | service | transform | RESEARCH Pattern 2 emissão (NimbusJwtEncoder, HS256, exp 8h) | canonical (RESEARCH) |
| `api/.../auth/LoginRequest/LoginResponse.java` | model (DTO) | request-response | PLAN §3.7.6 (`@Valid`, sem `empresaId` — R3) | canonical (PLAN) |
| `api/.../common/TenantContext.java` | utility | transform | RESEARCH Pattern 2 (lê do SecurityContext; PLAN §3.7.3) | canonical (RESEARCH) |
| `api/.../common/` exceptions + ProblemDetail advice | middleware | request-response | PLAN §3.3 (RFC 7807 `@RestControllerAdvice`) | canonical (PLAN) |
| `api/.../empresa/Empresa.java`, `api/.../usuario/Usuario.java` | model (entity) | CRUD | PLAN §3.4 DDL (Spring Data JPA) | canonical (PLAN) |
| `api/.../health/HealthController.java` | controller | request-response | RESEARCH Code Examples (`SELECT 1` via JdbcTemplate) | canonical (RESEARCH) |
| `api/src/main/resources/db/migration/V1__schema.sql` | migration | batch (DDL) | PLAN §3.4 DDL completo (copiar literal) | canonical (PLAN) |
| `api/src/main/java/.../db/migration/V1_1__seed_empresa_admin.java` | migration | batch | RESEARCH Pattern 4 (`BaseJavaMigration`, `System.getenv`, fail-fast) | canonical (RESEARCH) |
| `api/src/test/java/.../AuthIntegrationTest.java` | test | request-response | RESEARCH Pattern 3 (`@ServiceConnection` Testcontainers) | canonical (RESEARCH) |
| `web/app/login/page.tsx` | component | request-response | D-05 (minimalista pt-BR) — sem analogo, definir aqui | new (D-05) |
| `web/app/api/auth/login/route.ts` | route (BFF) | request-response | RESEARCH Pattern 1 (cookie httpOnly via Next Route Handler) | canonical (RESEARCH) |
| `web/app/api/auth/logout/route.ts` | route (BFF) | request-response | RESEARCH Pattern 1 (limpa cookie) | canonical (RESEARCH) |
| `web/lib/api.ts` | utility | request-response | PLAN §3.6 (fetch server-side, env `API_URL`) | canonical (PLAN) |
| `web/proxy.ts` | middleware | request-response | RESEARCH Pattern 1/Pitfall 7 (`proxy.ts` no Next 16; redirect → /login) | canonical (RESEARCH) |

## Patterns ESTABLISHED this Phase (binding for Phases 2–4)

Planner deve tratar os excertos abaixo como o padrão a copiar em planos das fases seguintes — não re-descobrir.

### P1. Estrutura de pacotes Backend (PLAN §3.3)
```
com.controlefinanceiro
├── config/     SecurityConfig, ProblemDetail advice
├── auth/       login, JWT (claims: sub, empresa_id, role)
├── empresa/    entity Empresa
├── usuario/    entity Usuario (role ADMIN|USER, ativo)
├── common/     TenantContext, exceptions, ProblemDetail handlers
└── db/migration/  V1_1__seed_empresa_admin.java
```
Camadas: **Controller** (valida formato, monta DTO, delega — nunca regra de negócio) → **Service** (regras) → **Repository** (Spring Data JPA; toda query filtra por `empresa_id` do contexto do token). Fases 2–4 adicionam `categoria/`, `lancamento/`, `dashboard/`, `ia/` no mesmo formato.

### P2. Tenant via token (R3/AUTH-03) — `TenantContext` (RESEARCH Pattern 2)
```java
// common/TenantContext.java — único ponto de acesso ao tenant (PLAN §3.7.3)
public final class TenantContext {
  public static Long getEmpresaId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth
        && jwtAuth.getPrincipal() instanceof UsuarioAuth p) {
      return p.empresaId();
    }
    throw new NaoAutorizadoException("Não autenticado");
  }
}
```
Populado pelo `JwtAuthenticationConverter` no `SecurityConfig` (claim `empresa_id` do JWT). **Nunca** ler tenant de request body; DTOs de request não têm campo `empresaId`. Vale para TODAS as fases.

### P3. SecurityFilterChain (RESEARCH Pattern 2 — Spring Security reference)
```java
http
  .csrf(csrf -> csrf.disable())               // API stateless com Bearer
  .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/login", "/api/health").permitAll()
    .requestMatchers("/api/categorias/**").hasRole("ADMIN")  // seed da regra; completa Fase 2
    .anyRequest().authenticated())
  .oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
```
**Anti-pattern:** não implementar `JwtAuthFilter` manual — `oauth2ResourceServer()` (Nimbus) resolve exp/alg/clock-skew.

### P4. BFF cookie httpOnly (D-02) — `web/app/api/auth/login/route.ts` (RESEARCH Pattern 1)
```ts
import { cookies } from 'next/headers';

export async function POST(request: Request) {
  const body = await request.json();
  const res = await fetch(`${process.env.API_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    return Response.json({ erro: 'E-mail ou senha inválidos' }, { status: 401 }); // D-05 genérico
  }
  const { token } = await res.json();
  const cookieStore = await cookies();          // Next 16: SOMENTE assíncrono
  cookieStore.set('sess', token, {
    httpOnly: true, sameSite: 'lax', path: '/',
    secure: process.env.NODE_ENV === 'production',
    maxAge: 8 * 60 * 60,                        // D-01: 8h, alinhado ao exp do JWT
  });
  return Response.json({ ok: true });
}
```
Toda chamada browser→API futura passa por route handler BFF que lê o cookie e forward com `Authorization: Bearer`. Env `API_URL` **server-side apenas** (nunca `NEXT_PUBLIC_`).

### P5. Flyway V1 SQL + V1_1 Java seed (D-03/D-04/AUTH-04/INF-01)
- `V1__schema.sql` = DDL **literal** do PLAN §3.4 (empresa, usuario, categoria, lancamento + 3 índices; `NUMERIC(14,2)`, `CHECK (valor > 0)`, `TIMESTAMPTZ`). `ddl-auto=none`.
- `V1_1__seed_empresa_admin.java` (RESEARCH Pattern 4):
```java
public class V1_1__seed_empresa_admin extends BaseJavaMigration {
  @Override
  public void migrate(Context context) throws Exception {
    String senha = System.getenv("SEED_ADMIN_PASSWORD");  // Flyway instancia por reflexão — SEM DI Spring
    if (senha == null || senha.isBlank()) {
      throw new IllegalStateException("SEED_ADMIN_PASSWORD não definida — necessária para a migration de seed");
    }
    String hash = new BCryptPasswordEncoder(12).encode(senha);
    // PreparedStatement na connection do Context; NÃO fechar a connection
  }
}
```
Imutabilidade: Java migrations não têm checksum — por convenção (documentar). Fase 2 usa `V2__seed_categorias.sql` (SQL puro, livre).

### P6. Testes de integração (RESEARCH Pattern 3 — padrão para TODAS as fases)
```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {
  @Container
  @ServiceConnection                       // dispensa @DynamicPropertySource (Boot ≥ 3.1)
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
  @Autowired TestRestTemplate rest;
}
```
Flyway roda de verdade a cada contexto de teste (valida V1 + seed). `JWT_SECRET` e `SEED_ADMIN_PASSWORD` via props de teste — nunca valores reais. Docker é pré-requisito (Pitfall 9).

### P7. Erros — RFC 7807 ProblemDetail (PLAN §3.3)
`@RestControllerAdvice` em `config/` com `ProblemDetail` (`type`, `title`, `status`, `detail`). Mensagens de auth genéricas (D-05). Fase 2 estende para `{ linha, erro }` do lote (R4).

### P8. Erro de integração com deploy (não código, mas vincula todos os arquivos)
- `application.yml`: `server.port=${PORT:8080}` (Pitfall 1 — Render injeta PORT).
- `pom.xml`: `maven.compiler.release=21` (Pitfall 11) + `flyway-database-postgresql` (Pitfall 3).
- JDBC URL do Render convertida `postgresql://` → `jdbc:postgresql://` (Pitfall 2).

## No Analog Found — decisões de design sem precedente no repo

| File/Concern | Reason | Source a usar no plano |
|--------------|--------|------------------------|
| `web/app/login/page.tsx` | UI inexistente; D-05 define visual (minimalista, pt-BR, erro genérico) | D-05 CONTEXT.md; sem lib UI extra decidida (`--no-tailwind` no scaffold RESEARCH) |
| Formato do `ProblemDetail`/erros 401/400 | Discrição do agente (CONTEXT.md) | PLAN §3.3 RFC 7807 — planner detalha |
| CORS, custo BCrypt, geração JWT | Discrição do agente | RESEARCH: HS256, BCrypt(12), segredo ≥ 32 bytes env `JWT_SECRET` |
| Staging infra (D-08) | Pendência explícita → planner decide | RESEARCH Open Questions 1 (dados de custo Render) |
| Auth: JWT issuance/validação | Sem código anterior; **não** hand-roll | RESEARCH "Don't Hand-Roll": oauth2-resource-server (Nimbus) |

## Anti-Patterns bloqueantes (verificar no execute-phase)

Copiados de RESEARCH Anti-Patterns — planner deve incluir como critérios de aceite/verificação:
1. JWT em `localStorage`/`sessionStorage` (viola D-02) — grep.
2. `JwtAuthFilter` manual (parse Base64/HMAC à mão).
3. `NEXT_PUBLIC_*` para token ou `API_URL`.
4. `ddl-auto=update/create` (Flyway é dono do schema).
5. Spring DI em Java migrations (instanciação por reflexão).
6. `double`/`float` em qualquer código (R7) — grep no review.
7. `server.port` hardcoded 8080.
8. `cookies()` síncrono ou `middleware.ts` (Next 16: `await cookies()`, `proxy.ts`).

## Metadata

**Analog search scope:** repo inteiro (glob `**/*.{java,ts,tsx,sql,xml}` → 0 arquivos de aplicação; `.opencode/` e `.planning/` são tooling GSD, não fonte de padrão de código).
**Files scanned:** 0 application files.
**Pattern extraction date:** 2026-10-30
**Fonte dos padrões:** `.planning/PLAN.md` §3 (canônico) + `.planning/phases/01-funda-o-e-auth-em-produ-o/01-RESEARCH.md` (Patterns 1–4, Standard Stack, Pitfalls — verificados em docs oficiais Spring/Next/Flyway/Render).
**Nota para o planner:** Fase 1 é a única fase greenfield — os padrões P1–P8 acima devem ser referenciados explicitamente nos PLANs das fases 2–4 em vez de pesquisa nova.
