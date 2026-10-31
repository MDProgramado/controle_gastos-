# Phase 1: Fundação e Auth em Produção - Research

**Researched:** 2026-10-30
**Domain:** Spring Boot 3 (Java 21) + Next.js 16 (App Router) · JWT/BCrypt · Flyway · Render + Vercel
**Confidence:** HIGH (stack e padrões verificados em fontes oficiais; 4 itens ASSUMED marcados)

## Summary

Fase greenfield total: o repo contém apenas `.opencode/` e `.planning/` — esta fase **cria** os padrões que as fases 2–4 herdam. As versões atuais do stack foram verificadas nas fontes oficiais: **Spring Boot 3.5.16** (último patch OSS da linha 3.5 — a linha OSS terminou em jun/2026; as releases 3.5.17+ são apenas do fork comercial HeroDevs NES) e **Next.js 16.4.3** (LTS ativo; React 19.3.1). O ponto de arquitetura mais importante da fase é o mecanismo do D-02 (JWT em cookie httpOnly): o padrão correto com apps separados (web na Vercel, API na Render) é um **BFF na Next Route Handler** — o browser nunca vê o JWT, o cookie é same-origin (SameSite=Lax sem necessidade de CORS), e a API continua recebendo apenas `Authorization: Bearer` (compatível com o diagrama do PLAN §3.1).

O segundo ponto crítico é o **seed via Flyway (D-03/D-04)**: Flyway SQL não lê env vars, então a abordagem prescrita é uma **Java migration** (`V1_1__seed_empresa_admin` estendendo `BaseJavaMigration`) que lê `SEED_ADMIN_PASSWORD` via `System.getenv`, gera o hash BCrypt dentro da migration e insere empresa + admin. Java migrations não têm checksum no Flyway (imutabilidade por convenção) e não recebem injeção de Spring — instanciadas reflexivamente pelo Flyway. O schema (empresa, usuario, categoria, lancamento) fica em `V1__schema.sql` puro, deixando `V2` livre para o seed de categorias da Fase 2.

**Recomendação primária:** Spring Security via `oauth2-resource-server` (Nimbus embutido — sem dependência extra de lib JWT) para emissão e validação do JWT, BFF route handler no Next 16 para o cookie httpOnly, Flyway Java migration para o seed, e Testcontainers com `@ServiceConnection` (sem `@DynamicPropertySource`) nos testes de integração. Deploy: Render aceita monorepo via Root Directory `api/` com build `./mvnw clean package` e start `java -jar ... --server.port=$PORT` — **o `PORT` da Render (default 10000) é a pegadinha nº 1 de deploy**.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** JWT com expiração de **8h** (confirmado pelo usuário: "esse tempo mesmo").
- **D-02:** Sessão mantida em **cookie httpOnly** — token nunca em `localStorage` (proteção contra XSS). Sem refresh token na v1: expirou, refaz login.
- **D-03:** Seed cria 1 empresa + usuário **`admin@controle.com`** com role `ADMIN`, via migration Flyway V1.
- **D-04:** **Senha não versionada** (regra GSD: nada de senha em documentos/commits). A senha vem de variável de ambiente `SEED_ADMIN_PASSWORD` (Render: segredo do serviço; dev: valor local em `.env` gitignored). Recomendação registrada: **rotacionar a senha após o primeiro login em produção** — usuário foi avisado.
- **D-05:** Login **simples e minimalista**: apenas e-mail + senha, interface em **pt-BR**, mensagens de erro claras (credenciais inválidas genéricas — sem dizer qual campo falhou), sem extras (lembrar-me, social login, "esqueci a senha").
- **D-06:** **Três ambientes: local, staging e produção** — staging criado porque o usuário pretende **escalar** depois (mais clientes/empresas).
- **D-07:** Deploy por push: Vercel (web, com preview por branch) + Render (API), HTTPS nativo, migrations rodam no boot.
- **D-08 (pendente → plan-phase):** infraestrutura do staging — banco Postgres separado no Render (custo de 2º banco) vs. banco único com dados de teste. Usuário quer staging; o planner decide o modelo custo×segurança. → *Dados de custo na seção "Open Questions".* → ✅ **RESOLVIDO no plan-phase:** banco separado (ver `## Open Questions (RESOLVED)` Q1 e bloco `D-08-decision` do `01-04-PLAN.md`)

### the agent's Discretion
- Build tool com Maven (premissa do plano mestre, não questionada).
- Estrutura de pacotes, forma do `ProblemDetail`, formato dos erros 401/400 — decidir no plan (alinhado ao `.planning/PLAN.md` §3).
- Configuração do CORS, força do BCrypt, geração/assinatura do JWT.

### Deferred Ideas (OUT OF SCOPE)
- Fluxo "esqueci a senha" / redefinição — capacidade de fase futura.
- Verificação de e-mail no cadastro — fase futura.
- Cadastro de novas empresas (multiempresa real) — já é v2 no REQUIREMENTS.md (GROW-01).
- Domínio próprio na Vercel/Render — decidir quando escalar.
- Novas capacidades (tema de outra fase): comentários de lançamento, importação OFX — já fora de escopo v1.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | Login e-mail/senha → JWT usado em todas as chamadas autenticadas | Padrão 2 (JWT issuance) + Padrão 3 (validação resource-server); testes de integração listados em Validation/Tests |
| AUTH-02 | Senhas apenas como hash BCrypt | Padrão 2: `BCryptPasswordEncoder(12)`, nunca texto puro; Java migration gera hash antes do INSERT |
| AUTH-03 | `empresa_id` exclusivamente do token JWT — nenhum body define tenant | `TenantContext` lê do `SecurityContext` populado pelo `JwtAuthenticationConverter`; teste de integração com body contendo `empresaId` provando que é ignorado (R3) |
| AUTH-04 | Seed de empresa e ADMIN por migration Flyway (V1) | Padrão 4: `V1_1__seed_empresa_admin` Java migration lendo `SEED_ADMIN_PASSWORD` |
| AUTH-05 | Roles ADMIN/USER no schema e no JWT | Claims `role` no JWT + `JwtGrantedAuthoritiesConverter` (claim `role` → `ROLE_ADMIN`/`ROLE_USER`) + regras por rota no `SecurityFilterChain` |
| AUTH-06 | Produção somente HTTPS | Render e Vercel servem HTTPS nativo (verificado na documentação Render); app não precisa de config extra |
| INF-01 | Flyway gerencia schema (V1); migrations imutáveis | `V1__schema.sql` (SQL, checksum) + `V1_1` (Java, sem checksum — imutável por convenção); `ddl-auto=none` |
| INF-03 | API na Render com health check OK em produção | `GET /api/health` custom (controller com `SELECT 1` via JdbcTemplate) + Health Check Path da Render documentado |
| INF-04 | Web na Vercel consumindo API publicada | BFF route handler com env var server-side `API_URL` (nunca `NEXT_PUBLIC_`); env vars por ambiente na Vercel |
</phase_requirements>

## Project Constraints (from AGENTS.md)

Diretrizes extraídas de `./AGENTS.md` — obrigatórias para o planner (mesma autoridade das decisões travadas):

- **Stack definida, não negociável:** Java 21 + Spring Boot 3, Spring Security + JWT, Spring Data JPA, PostgreSQL, Flyway, Testcontainers; Next.js (App Router, TypeScript). Deploy: API + Postgres pago na Render, web na Vercel.
- **Dinheiro:** `BigDecimal`, `NUMERIC(14,2)`, `scale = 2`, `RoundingMode.HALF_EVEN`; nenhum `double`/`float` no código (R1/R7 — nesta fase valem para o DDL: `NUMERIC(14,2)` no `V1__schema.sql`).
- **Tenant:** `empresa_id` vem exclusivamente do token JWT, nunca do body (R3).
- **Lote:** transação tudo-ou-nada (escopo Fase 2 — não planejar agora).
- **Integridade:** categoria ENTRADA não aceita SAÍDA (escopo Fase 2).
- **Segurança:** BCrypt, HTTPS, LGPD prevista antes do uso real.
- **Escopo de IA:** restrito à fase 4.
- **GSD workflow:** commits de docs passam pelo fluxo GSD (`gsd-sdk query commit`); `.planning/` é versionado (`commit_docs: true`).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Login form (UI pt-BR, e-mail+senha) | Browser / Client | — | Página `web/app/login` — única UI desta fase |
| Cookie httpOnly da sessão | Frontend Server (Next BFF) | — | Route handler seta o cookie; browser nunca manipula o JWT (D-02) |
| Proxy autenticado browser→API | Frontend Server (Next BFF) | — | Route handlers forward com `Authorization: Bearer` lido do cookie |
| Emissão/validação de JWT | API / Backend | — | `NimbusJwtEncoder`/`JwtDecoder` no Spring Security — fonte única de verdade |
| Autenticação (BCrypt, credenciais) | API / Backend | — | `POST /api/auth/login` + `BCryptPasswordEncoder` |
| Autorização (roles ADMIN/USER) | API / Backend | — | `SecurityFilterChain` + `@PreAuthorize` — nunca só no front |
| Tenant (`empresa_id`) | API / Backend | — | `TenantContext` lido do `SecurityContext` (claim do JWT) — único ponto de acesso |
| Schema + seed (Flyway V1/V1.1) | Database / Storage | API / Backend | Migrations rodam no boot da API; Flyway é dono do schema (`ddl-auto=none`) |
| Health check `/api/health` | API / Backend | CDN / Static (Render edge) | Endpoint público que valida app + conectividade DB |
| Deploy/infra (Render + Vercel) | CDN / Static | API / Backend | Push → deploy nativo nos dois; sem CI custom (D-07) |

## Standard Stack

### Core — API (`api/`)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot (parent BOM) | **3.5.16** | Framework base Java 21 | Último patch OSS da linha 3.5 [VERIFIED: github.com/spring-projects/spring-boot releases v3.5.16, 2026-06-25] |
| spring-boot-starter-web | via BOM | REST API | Padrão Spring |
| spring-boot-starter-security | via BOM | `SecurityFilterChain`, BCrypt | Padrão Spring |
| spring-boot-starter-oauth2-resource-server | via BOM | JWT via **Nimbus** (decode/validate/encode) — traz `spring-security-oauth2-jose` | "The support for decoding and verifying JWTs is in `spring-security-oauth2-jose`, meaning that both are necessary" [CITED: docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html] |
| spring-boot-starter-data-jpa | via BOM | JPA/Hibernate | Padrão Spring (PLAN §3.3) |
| spring-boot-starter-validation | via BOM | `@Valid` nos DTOs | Padrão Spring (PLAN §3.7.6) |
| flyway-core + **flyway-database-postgresql** | via BOM | Migrations no boot | Desde Flyway 10 o suporte PostgreSQL é módulo separado — sem ele: "No database found to handle jdbc:postgresql" [MEDIUM — verificar na execução] |
| postgresql (driver JDBC) | via BOM | Conexão PG | Padrão |
| spring-boot-starter-test | via BOM | JUnit 5 + AssertJ + MockMvc/TestRestTemplate | Padrão (PLAN §4.2) |
| spring-boot-testcontainers + testcontainers postgresql, junit-jupiter | via BOM | Integração com PG real | `@ServiceConnection` dispensa `@DynamicPropertySource` desde Boot 3.1 [CITED: docs.spring.io/spring-boot/reference/testing/testcontainers.html] |

### Core — Web (`web/`)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| next | **16.4.3** | App Router, route handlers (BFF) | LTS ativo, versão estável atual [VERIFIED: npm view + nextjs.org] |
| react | **19.3.1** | UI | Emparelhada com Next 16.4 [VERIFIED: npm view] |
| typescript | 7.x (resolvido pelo create-next-app) | Tipagem | [VERIFIED: npm view — confirmar compatibilidade no scaffold] |
| eslint + eslint-config-next **16.4.3** | lint | `next lint` foi **removido** no Next 16 — rodar ESLint direto [VERIFIED: nextjs.org/blog/next-16] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spotless (Google Java Format) | via plugin Maven | Formatação Java (PLAN §4.3) | No build do `api/` desde a fase 1 |
| Prettier | latest | Formatação TS (PLAN §4.3) | No `web/` |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| oauth2-resource-server (Nimbus, recomendado) | jjwt 0.12.x / Auth0 java-jwt + `JwtAuthFilter` manual | Lib extra + filtro hand-rolled = mais código de parse/validação para errar (exp, assinatura, alg downgrade). O framework nativo já faz tudo. O `JwtAuthFilter` do PLAN §3.3 é satisfeito pelo `BearerTokenAuthenticationFilter` do Spring configurado via `oauth2ResourceServer()` — não implementar filtro manual. |
| BFF route handler (recomendado) | Cookie setado pela API cross-domain + CORS `credentials: true` | Cross-domain exige `SameSite=None; Secure` + allowlist de origins (previews Vercel dinâmicos) — mais superfície de erro e CSRF. BFF elimina CORS. |
| Java migration para seed (recomendado) | SQL com `${placeholder}` de um hash pré-gerado (`SPRING_FLYWAY_PLACEHOLDERS_...`) | Alternativa válida, mas exige gerar/hashificar a senha offline por ambiente. Java migration faz tudo no boot com fail-fast se a env var faltar. |
| Controller custom `/api/health` (recomendado) | Actuator `/actuator/health` (ou remapeado) | O PLAN diz "(actuator)", mas o caminho exigido é `/api/health`; remapear actuator dá `/api/actuator/health` (base path do management não produz `/api/health`). Controller leve com `SELECT 1` atende o critério e a recomendação da Render de checar o DB. |
| HS256 (recomendado) | RS256 com JWK | Um único serviço emite e valida — HS256 é suficiente e evita gestão de chaves. Segredo ≥ 32 bytes em env var. |

**Installation (api/):**
```bash
# Estrutura sugerida — Spring Initializr (start.spring.io) com Java 21, Maven:
# deps: web, security, oauth2-resource-server, data-jpa, validation, flyway-core,
#       flyway-database-postgresql, postgresql, spring-boot-starter-test,
#       spring-boot-testcontainers, testcontainers(postgresql, junit-jupiter)
```

**Installation (web/):**
```bash
npx create-next-app@16.4.3 web --typescript --app --eslint --no-tailwind --src-dir=false
```

## Package Legitimacy Audit

> `slopcheck` foi instalado, mas só valida **PyPI** — este fase instala pacotes **npm** e **Maven**, fora do alcance da ferramenta. Todos os pacotes abaixo têm proveniência documental oficial (docs oficiais + `npm view`), o que satisfaz a regra `[VERIFIED]` sem slopcheck.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| next | npm | anos (desde 2016) | 9.5M+/sem (16.4.3) | github.com/vercel/next.js | n/a (PyPI only) | Approved [VERIFIED: npm registry + nextjs.org] |
| react | npm | 12+ anos | oficial Meta | github.com/facebook/react | n/a | Approved [VERIFIED: npm registry] |
| eslint-config-next | npm | desde 2020 | oficial Vercel | github.com/vercel/next.js | n/a | Approved [VERIFIED: npm registry] |
| typescript | npm | 10+ anos | oficial Microsoft | github.com/microsoft/TypeScript | n/a | Approved [VERIFIED: npm registry] |
| org.springframework.boot (BOM) | Maven Central | 12+ anos | oficial Spring | github.com/spring-projects/spring-boot | n/a | Approved [VERIFIED: spring.io/blog] |
| org.testcontainers | Maven Central | 10+ anos | padrão do ecossistema | github.com/testcontainers/testcontainers-java | n/a | Approved [VERIFIED: docs.spring.io] |
| org.flywaydb (flyway-core, flyway-database-postgresql) | Maven Central | 15+ anos | oficial Redgate | github.com/flyway/flyway | n/a | Approved [VERIFIED: documentation.red-gate.com] |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

*Obs. de higiene:* a execução do `slopcheck install` (modo PyPI) chegou a baixar os sinônimos PyPI `next`/`react`/`typescript` — eles foram **desinstalados imediatamente** (`pip uninstall`); nada permaneceu no ambiente. Não usar `slopcheck install` para pacotes npm.

## Architecture Patterns

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────┐
│ Browser (usuário)                                                   │
│   POST /login {email, senha} ── same-origin ──┐                     │
│   (cookie httpOnly "sess" nunca exposto a JS) │                     │
└───────────────────────────────────────────────┼─────────────────────┘
                                                ▼
┌────────────────────────────────────────────────────────────────────┐
│ Web — Vercel (Next.js 16 App Router, TS)                           │
│  app/login/page.tsx ─▶ app/api/auth/login/route.ts  (BFF)          │
│  proxy.ts (opcional: redirect / → /login se sem cookie)            │
│  app/api/[...]/route.ts ─▶ lê cookie → forward c/ Bearer ──┐       │
│  Env server-side: API_URL (nunca NEXT_PUBLIC_)              │       │
└────────────────────────────────────────────────────────────┼───────┘
                              HTTPS + Authorization: Bearer   │
┌─────────────────────────────────────────────────────────────┼───────┐
│ API — Render (Spring Boot 3.5.16, Java 21)                  ▼       │
│  POST /api/auth/login ─▶ BCrypt.matches ─▶ NimbusJwtEncoder          │
│      (claims: sub, empresa_id, role, exp=8h) ─▶ cookie via BFF      │
│  JwtAuthFilter (BearerTokenAuthenticationFilter)                    │
│      ─▶ JwtDecoder (HMAC secret env) ─▶ JwtAuthenticationConverter  │
│      ─▶ principal{empresaId, role} ─▶ SecurityContext                │
│  TenantContext.getEmpresaId() ◀── só daqui (R3)                     │
│  GET /api/health ─▶ SELECT 1 (200 OK p/ Render health check)        │
│  Flyway no boot: V1__schema.sql → V1_1__seed (env SEED_ADMIN_PASSWORD)│
└─────────────────────────────────────────────────────────────┬───────┘
                                                              │ JDBC
                              ┌───────────────────────────────▼───────┐
                              │ PostgreSQL (Render, pago, backup)     │
                              └───────────────────────────────────────┘
```

### Recommended Project Structure

```
controle-gastos/
├── api/
│   ├── pom.xml                        # parent: spring-boot-starter-parent 3.5.16
│   └── src/main/
│       ├── java/com/controlefinanceiro/
│       │   ├── config/                # SecurityConfig, JwtConfig (encoder/decoder), ProblemDetail advice
│       │   ├── auth/                  # AuthController (POST /login), JwtService, LoginRequest/Response DTOs
│       │   ├── empresa/               # Empresa entity
│       │   ├── usuario/               # Usuario entity (role ADMIN|USER, ativo)
│       │   ├── common/                # TenantContext, exceptions, ProblemDetail handlers
│       │   └── db/migration/          # V1_1__seed_empresa_admin.java (Java migration)
│       └── resources/
│           ├── application.yml        # server.port=${PORT:8080}, flyway, jpa ddl-auto=none
│           └── db/migration/
│               └── V1__schema.sql     # §3.4 do PLAN (empresa, usuario, categoria, lancamento + índices)
│   └── src/test/java/...              # AuthIntegrationTest (Testcontainers)
├── web/
│   ├── app/
│   │   ├── login/page.tsx             # login minimalista pt-BR (D-05)
│   │   ├── api/auth/login/route.ts    # BFF login: chama API, seta cookie httpOnly
│   │   ├── api/auth/logout/route.ts   # limpa cookie
│   │   └── page.tsx                   # placeholder pós-login (dashboard é Fase 3)
│   ├── proxy.ts                       # redirect protegidas sem cookie → /login
│   └── lib/api.ts                     # fetch server-side c/ API_URL + cookie
├── .gitignore                         # .env, target/, node_modules/
└── .planning/
```

### Pattern 1: BFF de sessão (cookie httpOnly) — satisfaz D-02

**What:** O Next Route Handler é o único que fala com a API. No login, ele chama `POST {API_URL}/api/auth/login`, recebe o JWT no corpo e o grava num cookie `httpOnly`, `sameSite=lax`, `path=/` (e `secure` em produção). Nas demais chamadas, um client server-side lê o cookie e forward com `Authorization: Bearer`.
**When to use:** Sempre — é a decisão D-02 + a arquitetura de apps separados (Vercel/Render).
**Example:**
```ts
// web/app/api/auth/login/route.ts — Source: nextjs.org/docs/app/api-reference/file-conventions/route
import { cookies } from 'next/headers';

export async function POST(request: Request) {
  const body = await request.json();
  const res = await fetch(`${process.env.API_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    // erro genérico p/ o usuário (D-05): "E-mail ou senha inválidos"
    return Response.json({ erro: 'E-mail ou senha inválidos' }, { status: 401 });
  }
  const { token } = await res.json();          // { token: "eyJ..." }
  const cookieStore = await cookies();          // Next 16: acesso SOMENTE assíncrono
  cookieStore.set('sess', token, {
    httpOnly: true, sameSite: 'lax', path: '/',
    secure: process.env.NODE_ENV === 'production',
    maxAge: 8 * 60 * 60,                       // D-01: 8h, alinhado ao exp do JWT
  });
  return Response.json({ ok: true });
}
```

### Pattern 2: Emissão + validação JWT (Spring Security, sem lib extra)

**What:** `POST /api/auth/login` valida credenciais com `BCryptPasswordEncoder`, consulta o `Usuario`, e emite JWT com `NimbusJwtEncoder` (HMAC secret de env var) e claims `sub` (e-mail ou id), `empresa_id` (Long), `role` ("ADMIN"/"USER"), `exp` 8h. Na validação, um `JwtAuthenticationConverter` customizado lê `empresa_id` e `role` e monta o principal carregando o tenant.
**When to use:** Único mecanismo de auth da v1 (AUTH-01, AUTH-05, R3, R6).
**Example:**
```java
// config/SecurityConfig.java — Source: docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
@Configuration
@EnableWebSecurity
class SecurityConfig {

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())               // API stateless com Bearer; cookie httpOnly nunca em request cross-site (SameSite=Lax)
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/login", "/api/health").permitAll()
        .requestMatchers("/api/categorias/**").hasRole("ADMIN")  // seed da regra de permissão — endpoints completam na Fase 2
        .anyRequest().authenticated())
      .oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  @Bean
  Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return jwt -> {
      Long empresaId = jwt.getClaim("empresa_id");     // nunca de request body (R3)
      String role = jwt.getClaim("role");
      UsuarioAuth principal = new UsuarioAuth(jwt.getSubject(), empresaId, role);
      var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
      return new JwtAuthenticationToken(jwt, principal, authorities);
    };
  }
}
```
Emissão (login):
```java
// auth/JwtService.java — emissão com segredo HMAC; segredo ≥ 32 bytes em env JWT_SECRET
// Fonte do padrão: spring-security reference (NimbusJwtEncoder + JWKSource com ImmutableSecret)
JwtClaimsSet claims = JwtClaimsSet.builder()
    .subject(usuario.getEmail())
    .claim("empresa_id", usuario.getEmpresaId())
    .claim("role", usuario.getRole().name())
    .issuedAt(Instant.now())
    .expiresAt(Instant.now().plus(8, ChronoUnit.HOURS))   // D-01
    .build();
JwtEncodingContext ctx = JwtEncodingContext.withClaimsSet(claims) /* + header/algorithm */;
String token = jwtEncoder.encode(JwtEncoderParameters.from(ctx)).getTokenValue();
```
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

### Pattern 3: Testes de integração com Testcontainers

**What:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Testcontainers` + `@Container @ServiceConnection PostgreSQLContainer` — o Flyway roda de verdade no container a cada contexto de teste (valida V1 + seed).
**When to use:** AuthIntegrationTest obrigatório da fase: login ok (200 + token), login inválido (401), token ausente (401), seed Flyway roda limpo (AUTH-04), `empresa_id` do token ignorando body (AUTH-03/R3), roles no JWT (AUTH-05).
**Example:**
```java
// Source: docs.spring.io/spring-boot/reference/testing/testcontainers.html
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {
  @Container
  @ServiceConnection                       // dispensa @DynamicPropertySource (Boot ≥ 3.1)
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate rest;

  @Test void loginComCredenciaisValidasRetornaToken() { /* 200 + claims empresa_id/role */ }
  @Test void loginComSenhaInvalidaRetorna401() { /* corpo genérico, sem dizer qual campo (D-05) */ }
  @Test void rotaSemTokenRetorna401() { }
  @Test void corpoNaoDefineTenant() {
    // POST /api/... com {"empresa_id": 999} no body → tenant usado é o do token, não 999
  }
}
```
*Nota de configuração de teste:* o teste precisa de valores para `JWT_SECRET` e `SEED_ADMIN_PASSWORD` — definir via `@TestPropertySource`/props de teste (nunca valores reais).

### Pattern 4: Seed Flyway via Java migration (D-03/D-04)

**What:** `V1__schema.sql` cria as 4 tabelas do §3.4 do PLAN. `V1_1__seed_empresa_admin` (Java) lê `SEED_ADMIN_PASSWORD` de `System.getenv`, BCrypt-hasha e insere 1 empresa + `admin@controle.com` (role ADMIN, ativo). Falta a env var → startup falha com mensagem clara (fail-fast desejado: cada ambiente tem sua senha).
**When to use:** Sempre que o seed depende de segredo não versionado — SQL puro não lê env vars.
**Example:**
```java
// Source: documentation.red-gate.com/flyway/flyway-concepts/migrations/java-based-migrations
package com.controlefinanceiro.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class V1_1__seed_empresa_admin extends BaseJavaMigration {

  @Override
  public void migrate(Context context) throws Exception {
    String senha = System.getenv("SEED_ADMIN_PASSWORD");   // Flyway instancia a classe
    if (senha == null || senha.isBlank()) {                 // reflexivamente — SEM DI Spring
      throw new IllegalStateException(
          "SEED_ADMIN_PASSWORD não definida — necessária para a migration de seed");
    }
    String hash = new BCryptPasswordEncoder(12).encode(senha);  // custo 12 (discrição; ≥ 10 = R6/PLAN)
    var sql = "INSERT INTO empresa (nome) VALUES ('Controle Financeiro'); "
            + "INSERT INTO usuario (empresa_id, email, senha_hash, nome, role) "
            + "VALUES (currval('empresa_id_seq'), 'admin@controle.com', ?, 'Administrador', 'ADMIN')";
    // executar via PreparedStatement na connection do Context; NÃO fechar a connection
  }
}
```
**Regra de imutabilidade (INF-01):** Java migrations não têm checksum no Flyway (não participam do change detection) — imutabilidade é por convenção; documentar isso no plano. `V2__seed_categorias.sql` (Fase 2) fica livre.

### Anti-Patterns to Avoid

- **`JwtAuthFilter` manual (parse Base64 + HMAC à mão):** usar `oauth2ResourceServer()` — filtros hand-rolled erram validação de `exp`/`alg`/clock skew.
- **`localStorage`/`sessionStorage` para o JWT:** viola D-02 explicitamente.
- **`NEXT_PUBLIC_*` para o token ou para a URL da API:** expõe ao bundle do browser; a API_URL é server-only, o token nunca sai do server.
- **`spring.jpa.hibernate.ddl-auto=update/create`:** Flyway é dono do schema (INF-01); em prod `none`.
- **Injetar Spring (`@Component`/`@Value`) em Java migrations:** Flyway instancia a classe por reflexão — a injeção não funciona de forma confiável; ler env vars direto.
- **Credenciais no body de login como JSON com campo extra:** DTO de login só aceita `email`/`senha` — `enabled=false` em Jackson evita props desconhecidas silenciosas.
- **Hardcodar `server.port=8080`:** quebra no Render (ver Pitfall 1).
- **`double`/`float` em qualquer código (R7):** inclusive em métricas — grep no review.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Parse/validação de JWT | Filtro manual com Base64/HMAC | Spring `oauth2-resource-server` (Nimbus) | Clock skew, `alg=none`, refresher de key, constant-time compare |
| Hash de senha | SHA-256/MD5/qualquer hash cru | `BCryptPasswordEncoder(12)` | Sal + work factor; resistente a brute-force/gpu |
| Gerenciamento de schema | `ddl-auto` ou DDL manual em prod | Flyway (V1 SQL + V1_1 Java) | Imutabilidade, histórico, rollback de deploy |
| Cookie de sessão | `document.cookie` / storage em JS | `cookies()` do Next com flags httpOnly/sameSite/secure | XSS não lê httpOnly; flags erradas = CSRF/XSS |
| Sessão server-side (Redis etc.) | Estado de sessão extra | JWT stateless de 8h (D-02: sem refresh) | Escopo v1; expirou → re-login |
| Health check | — | Controller `/api/health` com `SELECT 1` via JdbcTemplate | 5 linhas; Render recomenda checar o DB |

**Key insight:** toda peça de security client-managed (JWT, senha, cookie flags) tem dezenas de edge cases já resolvidos nas libs acima — reimplementar é a origem clássica de CVE.

## Runtime State Inventory

Não aplicável — fase greenfield (repo apenas com `.opencode/`/`.planning/`); nenhum serviço runtime, dado armazenado ou artefato buildado existe ainda. Único "estado" futuro é o Flyway schema history (criado do zero por esta fase).

## Common Pitfalls

### Pitfall 1: Render `PORT` ≠ 8080 (bloqueia deploy)
**What goes wrong:** API builda, deploy falha no health check — a Render injeta `PORT` (default 10000) e o app escuta 8080.
**How to avoid:** `server.port=${PORT:8080}` no `application.yml` (ou `--server.port=$PORT` no start command). Health Check Path da Render apontar para `/api/health`.
**Warning signs:** deploy "Live" mas health check timeout; log com "Started ... on port 8080" enquanto Render pinga 10000.

### Pitfall 2: URL JDBC do Render
**What goes wrong:** Render fornece `postgresql://user:pass@host:5432/db` — Spring/JDBC não aceita esse esquema.
**How to avoid:** Converter para `jdbc:postgresql://host:5432/db` + usuário/senha separados; usar a **Internal URL** (mesma região/região mesmo workspace, rede privada) se a API estiver na Render, com `?sslmode=require` quando usar a external. Port `:5432` sempre presente.
**Source:** [VERIFIED: render.com/docs] + relatos de deploy Spring Boot na Render.

### Pitfall 3: Flyway 10+ sem `flyway-database-postgresql`
**What goes wrong:** Boot falha com "Unable to determine a database driver to handle jdbc:postgresql".
**How to avoid:** Incluir `org.flywaydb:flyway-database-postgresql` (versão gerida pelo BOM). [MEDIUM — confirmar na execução]

### Pitfall 4: Java migration sem env var derruba TODOS os ambientes
**What goes wrong:** `SEED_ADMIN_PASSWORD` esquecida em staging/produção → app não sobe (fail-fast intencional, mas trava deploy).
**How to avoid:** Checklist de env vars por ambiente no plano (Render: serviço + Vercel: `API_URL`); mensagem de erro explícita na migration; documentar no README/ops.

### Pitfall 5: BCrypt trunca senhas > 72 bytes
**What goes wrong:** senhas longas silenciosamente truncadas.
**How to avoid:** validação de tamanho máximo no DTO de login/seed (≤ 72 bytes) — documentar; não é bloqueante para a fase.

### Pitfall 6: Segredo JWT fraco ou rotacionado sem plano
**What goes wrong:** HS256 com segredo < 32 bytes é quebrável; rotacionar o segredo invalida todas as sessões (aceitável: 8h, sem refresh).
**How to avoid:** segredo ≥ 32 bytes aleatórios (`openssl rand -base64 48`), env var `JWT_SECRET` por ambiente, nunca em git.

### Pitfall 7: Next 16 — APIs síncronas e `middleware.ts`
**What goes wrong:** `cookies()`/`headers()` síncronos foram **removidos** no Next 16; `middleware.ts` está deprecado (renomeado `proxy.ts`, export `proxy`, runtime Node); `next lint` não existe mais (`next build` não roda lint).
**How to avoid:** `const cookieStore = await cookies()`; usar `proxy.ts` só se necessário (redirect de rotas protegidas); ESLint via script direto.
**Source:** [VERIFIED: nextjs.org/docs/app/guides/upgrading/version-16 + blog/next-16]

### Pitfall 8: Conta errada de ambiente na Vercel
**What goes wrong:** previews de branch apontando para a API de produção (dados reais em branch de teste).
**How to avoid:** env var `API_URL` configurada por environment na Vercel: Production → API produção, Preview → API staging (D-06/D-07).

### Pitfall 9: Testcontainers exige Docker rodando
**What goes wrong:** `./mvnw verify` falha em máquina sem Docker Desktop.
**How to avoid:** pré-requisito documentado; Docker verificado disponível neste ambiente (ver Environment Availability).

### Pitfall 10: CSRF "resolvido" só porque API é Bearer
**What goes wrong:** o browser envia o cookie `sess` automaticamente em chamadas same-site; endpoints BFF que mutam estado devem ser POST com `SameSite=Lax` (default já bloqueia cross-site POST) — e a API não aceita cookie, só Bearer (dupla barreira). Se algum dia mudar para cookie direto na API, CSRF volta a ser problema.
**How to avoid:** manter BFF + Bearer; não habilitar `SameSite=None`.

### Pitfall 11: Java 24 local × Java 21 alvo
**What goes wrong:** compilar com features do 24 e falhar no runtime Render (21).
**How to avoid:** `maven.compiler.release=21` (ou toolchain) no `pom.xml`.

## Code Examples

Verificados nas fontes oficiais; padrões adicionais (login BFF, TenantContext, Java migration, teste) na seção Architecture Patterns.

### SecurityFilterChain completo com roles (fonte: Spring Security reference)
```java
.authorizeHttpRequests(auth -> auth
  .requestMatchers("/api/auth/login", "/api/health").permitAll()
  .requestMatchers(HttpMethod.POST, "/api/categorias/**").hasRole("ADMIN")
  .anyRequest().authenticated())
```

### Teste de integração (fonte: docs.spring.io/spring-boot reference/testing/testcontainers)
```java
@Container @ServiceConnection
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
```

### Health check (fonte: render.com/docs/health-checks — Render recomenda "executing a simple database query")
```java
@GetMapping("/api/health")
Map<String, String> health() {
  jdbc.queryForObject("SELECT 1", Integer.class);   // 500 se DB inacessível → Render restarta
  return Map.of("status", "OK");
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Spring Boot 3.4 e anteriores | **3.5.16** (último OSS da linha 3.5) | 3.5.16: jun/2026; OSS da linha 3.5 encerrou (~jun/2026 — 3.5.17+ é só NES/HeroDevs) | Fixar 3.5.16; sem patches OSS futuros na 3.5 — risco aceito pelo lock de stack "Spring Boot 3" (Boot 4.x é a linha ativa atual) |
| Flyway tudo-em-um | `flyway-core` + `flyway-database-postgresql` | Flyway 10 (2023) | Dependência obrigatória ou startup falha |
| `@DynamicPropertySource` em testes | `@ServiceConnection` | Spring Boot 3.1 (2023) | Menos boilerplate nos testes |
| `middleware.ts` | `proxy.ts` (export `proxy`, runtime Node) | Next 16 (out/2025) | Depricação — remover antes da remoção |
| `next lint` | ESLint CLI direto (`next build` não linta) | Next 16 | lint não é mais gate automático do build |
| `cookies()` síncrono | `await cookies()` | Next 16 (remoção do modo compat 15) | Acesso síncrono é erro de runtime/build |
| Render instance types legacy (Starter $7, Standard $20) | Flexible plans (Postgres Basic-256mb $6/mo + storage $0.30/GB) | 2025–2026 | Base de custo do staging (D-08) |

**Deprecated/outdated:**
- Spring Boot 3.5 OSS: linha sem novos patches — justificado pelo lock "Spring Boot 3" (AGENTS.md); documentar risco ao cliente.
- `JwtGrantedAuthoritiesConverter` com prefixo `SCOPE_` default: este app usa claim `role` → converter custom (padrão da própria reference).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Java migrations Flyway não recebem DI Spring (instanciadas reflexivamente) — ler env via `System.getenv` | Pattern 4 | Baixo — fallback `JdbcTemplate`+`SingleConnectionDataSource` documentado nos docs Flyway; se DI funcionar num contexto específico, o padrão env-read continua correto |
| A2 | `flyway-database-postgresql` é necessário com Spring Boot 3.5 | Standard Stack / Pitfall 3 | Baixo — se o BOM já embutir, é no-op; se faltar, startup falha com erro claro |
| A3 | Render native runtime (Java) compila Maven a partir do Root Directory `api/` e roda Java ≥ 21 | Env Availability / Pitfall 2 | Médio — fallback: Dockerfile na Render (runtime Docker), amplamente documentado |
| A4 | `typescript@7.x` resolvido pelo create-next-app é compatível com Next 16.4 | Standard Stack | Baixo — pinar `typescript@^5` se houver incompatibilidade de types |

## Open Questions (RESOLVED)

1. **D-08 — infraestrutura do staging (decisão do planner, com dados de custo)** → ✅ **RESOLVIDO:** banco Postgres **separado** para staging — decisão registrada no bloco `D-08-decision` de `01-04-PLAN.md` e em `01-SKELETON.md`
   - O que se sabe: Render Postgres pago a partir de **$6/mês** (Basic-256mb, compute) + storage $0.30/GB; web service Starter **$7/mês**. Staging completo (2º serviço + 2º banco) ≈ **+$13–25/mês** sobre produção.
   - Recomendação da pesquisa: **banco separado para staging** — env vars (`SEED_ADMIN_PASSWORD`, `JWT_SECRET`) são por serviço; um banco compartilhado exporia dados reais a deploys de teste e quebraria o isolamento que o usuário quer para crescer. Se custo pesar, alternativa: staging compartilha o serviço mas usa database separado no mesmo Postgres Render? — Render não oferece múltiplos databases isolados por instância de forma recomendada; 2ª instância é o caminho limpo.
2. **Spring Boot 3.5 sem patches OSS** → ✅ **RISCO ACEITO** (não é mais pergunta): lock de stack "Spring Boot 3" no AGENTS.md; justificado em State of the Art e visível ao cliente
   - Lock de stack exige Boot 3; risco aceito. Se o usuário permitir, Boot 4.x seria o recomendado para projeto novo em out/2026 — **fora do escopo sem nova decisão**.
3. **Staging seed:** mesma migration roda em staging e produção com senhas diferentes (env var por serviço). Confirmar se o usuário quer mesmo usuário `admin@controle.com` em produção (D-03 diz sim). → ✅ **RESOLVIDO:** D-03 confirmado (`admin@controle.com` em todos os ambientes); senha distinta por serviço via env var `SEED_ADMIN_PASSWORD` (D-04) — checklist na Task 1 do `01-04-PLAN.md`

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | Build/teste API | ✓ (24.0.2) | target `release=21` | instalar JDK 21 se toolchain preferir |
| Apache Maven | Build API | ✓ | 3.9.12 | gerar wrapper `mvnw` (PLAN usa `./mvnw`) |
| Node.js | Build/teste web | ✓ | 24.21.0 | ≥ 20 suportado pelo Next 16 |
| npm | web deps | ✓ | 11.19.0 | — |
| Docker | Testcontainers | ✓ (29.5.3, context desktop-linux) | — | sem Docker, `./mvnw verify` falha (Pitfall 9) |
| git | Deploy push | ✓ | 2.48.1 | — |
| Conta Render + Postgres pago | INF-03/04 | ✗ (não verificável daqui) | — | tarefa humana no plano |
| Conta Vercel conectada ao repo | INF-04 | ✗ (não verificável daqui) | — | tarefa humana no plano |

**Missing dependencies with no fallback:**
- Contas Render (com Postgres pago/backup contratado) e Vercel — pré-requisito humano listado no PLAN §9; o plano deve ter tarefa de checklist/manual de provisionamento (não automatizável sem credenciais).

**Missing dependencies with fallback:**
- JDK 24 vs 21 → `maven.compiler.release=21` cobre; Dockerfile é fallback se o runtime Java da Render não servir (A3).

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | `BCryptPasswordEncoder(12)`; erro 401 genérico (D-05); sem enumeração de usuário |
| V3 Session Management | yes | JWT 8h (D-01) em cookie `httpOnly; SameSite=Lax; Secure` (D-02); logout = limpar cookie; sem refresh token |
| V4 Access Control | yes | `SecurityFilterChain` por rota (ADMIN p/ gestão); `TenantContext` único (R3) |
| V5 Input Validation | yes | DTOs com `@Email @NotBlank`; Jackson `FAIL_ON_UNKNOWN_PROPERTIES`; tamanho de senha ≤ 72 bytes |
| V6 Cryptography | yes | BCrypt (senhas) + HS256 via Nimbus (JWT) — **nunca** hand-roll assinatura/hash |

### Known Threat Patterns for {Spring Boot + Next BFF}

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| XSS roubando token | Tampering | Cookie httpOnly + nunca localStorage (D-02); BFF mantém JWT fora do browser |
| CSRF via cookie | Spoofing | SameSite=Lax + API aceita só Bearer (cookie não vale na API) |
| Vazamento de tenant | Information Disclosure | DTOs sem `empresaId`; `TenantContext` só do token; teste de integração R3 |
| Enumeração de credenciais | Information Disclosure | Mensagem única "E-mail ou senha inválidos" (D-05) |
| Segredo JWT/seed em git | Information Disclosure | Env vars por ambiente; `.env` gitignored; slopcheck-protocol (nada de senha em docs/commits, D-04) |
| Token/credencial em log | Information Disclosure | Nunca logar `Authorization`, cookie ou body de login (PLAN §4.5) |
| downgrade `alg`/token forjado | Tampering | Nimbus valida `alg`/exp/assinatura; segredo ≥ 32 bytes |

## Sources

### Primary (HIGH confidence)
- docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html — resource server JWT, `JwtAuthenticationConverter`
- docs.spring.io/spring-boot/reference/testing/testcontainers.html — `@ServiceConnection`
- documentation.red-gate.com/flyway — Java-based migrations (`BaseJavaMigration`, checksum ausente)
- nextjs.org/docs/app/guides/upgrading/version-16 + blog/next-16 — async `cookies()`, `proxy.ts`, `next lint` removido
- render.com/docs (web-services, health-checks, your-first-deploy) + render.com/pricing — Root Directory, PORT, health check, preços
- github.com/spring-projects/spring-boot releases (v3.5.16, 2026-06-25) + spring.io/blog — versão atual Boot 3.5

### Secondary (MEDIUM confidence)
- spring.io/docs + comunidade (deploy Spring Boot na Render: conversão JDBC URL, SSL) — confirmar em implementação
- Relatos de necessidade de `flyway-database-postgresql` (Pitfall 3)

### Tertiary (LOW confidence)
- Compatibilidade exata `typescript@7.x` × Next 16.4 (A4)
- Runtime Java nativo da Render aceitar Java 21+ sem Dockerfile (A3)

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — versões e recursos verificados em docs oficiais/npm registry
- Architecture: HIGH — BFF + resource-server + Java migration são padrões documentados; encaixe com PLAN §3 validado
- Pitfalls: HIGH/MEDIUM — Pitfalls 1, 2, 7 verificados; Pitfall 3 (MEDIUM) e A3/A4 marcados

**Research date:** 2026-10-30
**Valid until:** 2026-11-30 (Spring/Next têm cadência alta de patch — revalidar versões na execução; há release de segurança Next prevista para 2026-11-03 [VERIFIED: nextjs.org/blog])
