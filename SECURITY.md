# SECURITY.md

## Scope

- **Repository**: `C:\Users\11\controle-gastos`
- **Reviewed commit**: `8b726a9a1c2e8d26ba707c8b4fb8c424c0ba83ec` (`git status --short` clean at review start)
- **Review date**: 2026-11-21
- **Method**: Source-first, read-only static review. No live/deployed probes, no exploit execution, no credential use. Deployment/provider controls were not observed directly.
- **Stack reviewed**: Spring Boot 3 / Java 21 API (`api/`), Next.js App Router / TypeScript BFF (`web/`), Flyway migrations, Docker/Render config.

## Coverage

| Area | Status | Notes |
|------|--------|-------|
| Auth/JWT/session | Reviewed | `SecurityConfig`, `JwtService`, `AuthService`, login/logout/me BFF routes, `web/proxy.ts` |
| Tenant isolation | Reviewed | `TenantContext`, repository/service scoping, `findById` usages, AI session/knowledge, WhatsApp confirmation lookup |
| BFF/API proxy routes | Reviewed | All `web/app/api/**/route.ts` files |
| File upload/import | Reviewed | Import controller/service, multipart limits, PDF magic/hash validation |
| WhatsApp webhook | Reviewed | Signature verification, idempotency, async processing, confirmation flow |
| AI/OpenRouter integration | Reviewed | Controller/service/memory/knowledge/client/context |
| Secrets/config | Reviewed (names only) | `.env` files are gitignored/untracked; key names observed, values not reported |
| Logging/error handling | Reviewed | ProblemDetail handler, AI/WhatsApp log statements |
| Dependencies/CVEs | Not deeply audited | No SBOM/CVE scan executed; versions recorded as evidence only |
| Deployed TLS/WAF/rate limits/backups/provider settings | Needs validation | No live observation |

## Findings Summary

| ID | Title | State | Severity | Affected principal/resource | Trust boundary |
|----|-------|-------|----------|-----------------------------|----------------|
| F-001 | AI chat sessions can be poisoned across tenants by client-supplied `sessionId` | confirmed | High | Any authenticated user -> another tenant's AI session/history/context | Authenticated API user -> tenant-scoped AI session data |
| F-002 | AI knowledge base is globally readable/writable by any authenticated user | confirmed | High | Any authenticated user -> all tenants' AI system prompt/context | Authenticated API user -> global AI knowledge store |
| F-003 | Contas-a-pagar BFF routes bypass `/api` path and use unsafe env/cookie fallbacks | confirmed | Medium | Authenticated browser user / misconfigured deployment -> backend API routing and token forwarding | Browser/BFF -> backend API |
| N-001 | No application-level rate limiting/throttling observed for auth, AI, webhook, and upload endpoints | needs_validation | - | Internet/authenticated users -> API availability/cost | External clients -> API |
| N-002 | No explicit CORS/security-header hardening observed in source | needs_validation | - | Browser users -> web/API responses | Browser -> web/API edge |
| N-003 | Transport encryption, backup encryption, secret storage, and provider data retention not verifiable from source | needs_validation | - | Operators/users -> sensitive data at rest/in transit/third parties | Deployment/provider boundary |
| N-004 | Vercel preview-to-production API isolation not verifiable from source | needs_validation | - | Preview deployment/user -> production API/data | Preview environment -> production backend |
| N-005 | WhatsApp/AI sensitive-content logging and retention controls incomplete in source | needs_validation | - | Operators/third-party providers -> PII/financial message content | App -> logs/providers |

## Remediation Status (2026-11-21, working tree after `8b726a9`)

| ID | Status | Change | Verification |
|----|--------|--------|--------------|
| F-001 | **Fixed in working tree** | `AiMemoryStore` keys sessions by `empresaId::sessionId`; `obter/adicionarMensagem/obterHistorico/limpar` validate the session's `empresaId`; `AiService`/`AiController` propagate `TenantContext` empresa | `AiTenantIsolationTest` (2 tests) is RED before and GREEN after the fix; `AiServiceTest` updated |
| F-002 | **Fixed in working tree** | Knowledge store keyed by `empresaId`; `AiService` injects only the caller's knowledge into prompts; `SecurityConfig` requires `ADMIN` for `/api/ai/chat/conhecimento` and `/api/ai/chat/conhecimento/**` | `AiTenantIsolationTest` covers cross-tenant prompt leakage; `AiIntegrationTest` has USER->403 and tenant-isolation cases (Testcontainers, not run locally) |
| F-003 | **Fixed in working tree** | BFF routes and `web/app/contas-a-pagar/page.tsx` now use only `API_URL` (server-side), `/api/contas-a-pagar...` paths, and cookie `sess` only; `NEXT_PUBLIC_API_URL`, `localhost`, `access_token`/`auth_token` fallbacks removed; explicit 401/502/503 | `npm run lint` (0 errors, 8 pre-existing warnings) and `npx tsc --noEmit` pass |

- Functional note: no backend controller for `/api/contas-a-pagar` was found in source; the BFF path is now consistent with the rest of the app, but endpoint availability still needs product/owner validation. This was already broken before the fix and is not a new exposure.
- Test results: `.\mvnw.cmd "-Dspotless.check.skip=true" "-Dtest=!*IntegrationTest,!*MigrationTest" test` -> **106 tests, 0 failures/errors**.
- Not executed locally: 13 Testcontainers integration tests (Docker daemon unavailable: `npipe:////./pipe/dockerDesktopLinuxEngine` not found).
- `spotless:check` still fails on 3 pre-existing files (`AiContextoService.java`, `LancamentoService.java`, `UsuarioTelefoneService.java`); files changed for these fixes were formatted with `spotless:apply` and those pre-existing files were restored unchanged.

## Confirmed Findings

### F-001 — AI chat sessions can be poisoned across tenants by client-supplied `sessionId`

- **State**: confirmed
- **Severity**: High
- **Affected principal/resource**: Any authenticated user; another tenant's AI conversation session and prompt context.
- **Trust boundary**: Authenticated API caller supplies `sessionId` and crosses tenant boundary into AI memory keyed globally.

**Evidence**

- `api/src/main/java/com/controlefinanceiro/ai/AiController.java:39-46` accepts `sessionId` from request body, defaults only when absent, and passes it to `AiService.processar`.
- `api/src/main/java/com/controlefinanceiro/ai/AiController.java:70-74` exposes `POST /api/ai/chat/sessao/{sessionId}/limpar` with no tenant check.
- `api/src/main/java/com/controlefinanceiro/ai/AiService.java:54-75` obtains/creates session by `sessionId`, appends user message, builds prompt from session history, and stores assistant response in same session.
- `api/src/main/java/com/controlefinanceiro/ai/AiMemoryStore.java:18-46` stores sessions in a global `ConcurrentHashMap<String, AiSession>` keyed only by `sessionId`; `obterOuCriar` uses `computeIfAbsent` and never validates `empresaId` for existing sessions; `obter/adicionarMensagem/obterHistorico/limpar` have no tenant check.
- `api/src/main/java/com/controlefinanceiro/ai/AiSession.java:14-47` stores `empresaId` but it is never enforced by store/controller.
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java:38-57` requires authentication for non-permitted routes; `/api/ai/**` falls under `anyRequest().authenticated()`.

**Safe local reproduction (source-based, no live exploit)**

1. As tenant A authenticated user, call `POST /api/ai/chat` with `{"mensagem":"...","sessionId":"known-id"}`; session is created under that key.
2. As tenant B authenticated user, call `POST /api/ai/chat` with the same `sessionId`. `AiMemoryStore.obterOuCriar` returns the existing session regardless of tenant B's `empresaId`; `AiService` appends tenant B's message into that session and includes prior session history in prompt construction.
3. As tenant B authenticated user, call `POST /api/ai/chat/sessao/known-id/limpar`; `AiMemoryStore.limpar` clears the session without tenant validation.

**Impact**

- Cross-tenant session poisoning/tampering: an authenticated user who knows or guesses another session ID can append messages to or clear another tenant's AI session.
- Context leakage risk: victim session messages are included in prompt assembly for subsequent requests using the same session ID. The response object does not directly return history, but history is sent to the AI model/provider as context.

**Severity rationale**

High because any authenticated user can act across tenant boundaries by reusing a client-controlled identifier. Exploitation requires session ID knowledge/guessability, but the identifier is externally controlled and the store performs no authorization check.

**Smallest effective fix**

- Make session ownership server-side and tenant-bound: key sessions by `(empresaId, sessionId)` or generate server-owned opaque session IDs mapped to `empresaId`.
- In `AiMemoryStore.obterOuCriar/obter/adicionarMensagem/obterHistorico/limpar`, require the requesting `empresaId` and reject/miss when it does not match the stored session.
- In `AiController`, do not trust client-supplied session IDs as authorization; for clear-session endpoint, resolve tenant from JWT and only clear sessions owned by that tenant.

### F-002 — AI knowledge base is globally readable/writable by any authenticated user

- **State**: confirmed
- **Severity**: High
- **Affected principal/resource**: Any authenticated user; global AI knowledge injected into all tenants' prompts.
- **Trust boundary**: Authenticated API caller -> globally shared AI knowledge store.

**Evidence**

- `api/src/main/java/com/controlefinanceiro/ai/AiController.java:53-68` exposes `GET /api/ai/chat/conhecimento`, `POST /api/ai/chat/conhecimento`, and `POST /api/ai/chat/conhecimento/limpar` with no role/tenant checks.
- `api/src/main/java/com/controlefinanceiro/ai/AiService.java:354-364` delegates knowledge read/write directly to `AiKnowledgeStore`.
- `api/src/main/java/com/controlefinanceiro/ai/AiKnowledgeStore.java:10-26` stores knowledge as a single mutable global `String`, not scoped by empresa/tenant.
- `api/src/main/java/com/controlefinanceiro/ai/AiService.java:103-133` injects `knowledgeStore.obterConhecimento()` into the system prompt for every chat request.
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java:38-57` leaves `/api/ai/**` as authenticated-only; no ADMIN restriction.

**Safe local reproduction (source-based, no live exploit)**

1. Authenticate as any regular USER in tenant A.
2. `POST /api/ai/chat/conhecimento` with attacker-controlled text. `AiKnowledgeStore.definirConhecimento` overwrites the global string.
3. Any authenticated user in tenant B calls `POST /api/ai/chat`; `AiService.montarMensagens` includes the same global knowledge in the system prompt.
4. Any authenticated user can `GET /api/ai/chat/conhecimento` to read the current global knowledge.

**Impact**

- Cross-tenant prompt poisoning/integrity compromise: one authenticated user can alter AI behavior/context for all tenants.
- Information disclosure: any authenticated user can read knowledge intended for administrative/global configuration.

**Severity rationale**

High because the writable global prompt context crosses tenant boundaries and affects AI outputs for other tenants. The endpoint is authenticated-only but not restricted to ADMIN or scoped per tenant.

**Smallest effective fix**

- Restrict knowledge endpoints to `ADMIN` (or remove them from production).
- Scope knowledge by `empresaId` if tenants need custom knowledge, and persist/read it through tenant-filtered storage.
- Add audit logging and input size/format limits for admin-managed knowledge.

### F-003 — Contas-a-pagar BFF routes bypass `/api` path and use unsafe env/cookie fallbacks

- **State**: confirmed
- **Severity**: Medium
- **Affected principal/resource**: Authenticated web users; backend API tokens/requests.
- **Trust boundary**: Browser -> Next.js BFF route -> backend API.

**Evidence**

- `web/app/api/contas-a-pagar/route.ts:4-36` uses `BACKEND_URL = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"`, accepts `sess`, `access_token`, or `auth_token` cookies, and fetches `${BACKEND_URL}/contas-a-pagar...` (missing `/api`).
- `web/app/api/contas-a-pagar/[id]/pagar/route.ts:4-49` uses same env/cookie fallback and fetches `${BACKEND_URL}/contas-a-pagar/${id}/pagar` (missing `/api`).
- Other BFF routes consistently use `${process.env.API_URL}/api/...` and only `sess` cookie (e.g., `web/app/api/ai/chat/route.ts:16-34`, `web/app/api/categorias/route.ts:16-33`, `web/app/api/importacoes/route.ts:89-107`, `web/app/api/me/route.ts:16-34`).
- No backend controller mapping for contas-a-pagar was found by source search (`git grep` for `contas-a-pagar|ContaAPagarController|/api/contas` returned no matches under `api/src/main/java`).

**Safe local reproduction (source-based, no live exploit)**

1. Authenticate in web app so `sess` cookie exists.
2. Call `GET /api/contas-a-pagar` or `PATCH /api/contas-a-pagar/{id}/pagar` through Next.js BFF.
3. BFF forwards Authorization Bearer token to `${BACKEND_URL}/contas-a-pagar...` without `/api`; compared with existing API pattern `/api/...`, this is an inconsistent backend path. With missing backend controller, request cannot reach intended protected resource.

**Impact**

- Reliability/authorization-flow break: authenticated feature requests are routed inconsistently and likely fail.
- Security hardening gap: fallback to `NEXT_PUBLIC_API_URL` and extra cookie names increases token-forwarding attack surface if environment/cookies are misconfigured. The Bearer token is forwarded by server-side code; misrouting to an unintended backend origin would expose credentials to that origin.

**Severity rationale**

Medium. No direct data breach is proven from source alone, but the route forwards auth credentials using inconsistent/unsafe fallbacks and an inconsistent backend path. Impact becomes higher if `NEXT_PUBLIC_API_URL` is pointed to an unintended origin in deployment.

**Smallest effective fix**

- Align both routes to the standard BFF pattern: require only `sess`, use `process.env.API_URL`, call `${process.env.API_URL}/api/contas-a-pagar...`, and return clear 401/502 errors.
- Remove `NEXT_PUBLIC_API_URL`, `access_token`, and `auth_token` fallbacks.
- Add a backend route/controller or remove dead BFF routes until implemented.

## Needs Validation

### N-001 — No application-level rate limiting/throttling observed for auth, AI, webhook, and upload endpoints

- **State**: needs_validation
- **Affected principal/resource**: Internet clients/authenticated users; login, AI, webhook, and PDF import availability/cost.
- **Trust boundary**: External/client requests -> API/BFF.

**Why not confirmed**

Source shows no rate-limit filter/interceptor/dependency (`git grep` for rate limit/bucket/resilience returned none; `api/pom.xml` has no rate-limit dependency). However, Render/Vercel/WAF/CDN/provider throttling may exist outside source.

**Exact missing facts**

- Are rate limits enforced at edge/provider for `/api/auth/login`, `/api/ai/*`, `/webhooks/whatsapp`, and `/api/importacoes/pdf`?
- Are per-IP/per-user quotas and burst limits configured?
- Is there alerting for abuse/cost spikes (OpenRouter/Meta/file upload)?

**Safe owner-observed validation plan**

- In staging, owner runs authorized low-volume burst tests against login/AI/import endpoints and confirms 429/throttling behavior.
- Owner exports Render/Vercel/WAF rate-limit rules and confirms coverage for public and authenticated expensive endpoints.
- Owner confirms monitoring/alerts for 429s, AI spend, webhook volume, and upload errors.

### N-002 — No explicit CORS/security-header hardening observed in source

- **State**: needs_validation
- **Affected principal/resource**: Browser users; web/API responses.
- **Trust boundary**: Browser -> edge/app responses.

**Why not confirmed**

No CORS config or security headers were found in Java source, `web/proxy.ts`, or `web/next.config.ts`. These may be set by Vercel/Render/reverse proxy.

**Exact missing facts**

- Are `Content-Security-Policy`, `Strict-Transport-Security`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, and frame protections set on web responses?
- Is CORS restricted to trusted origins for API endpoints?
- Are HSTS and HTTPS redirects enforced at edge?

**Safe owner-observed validation plan**

- Owner captures response headers from production/staging web and API endpoints using browser devtools or `curl -I`.
- Owner reviews Vercel/Render header/CORS configuration.
- Owner validates CSP does not break required assets and that API CORS is not wildcard for credentialed routes.

### N-003 — Transport encryption, backup encryption, secret storage, and provider data retention not verifiable from source

- **State**: needs_validation
- **Affected principal/resource**: Password hashes, JWT secret, database credentials, OpenRouter/WhatsApp tokens, financial/PII data.
- **Trust boundary**: App <-> users, app <-> database, app <-> OpenRouter/Meta, deployment/storage providers.

**Why not confirmed**

Source shows good baseline practices: BCrypt password hashing (`SecurityConfig.java:82-86`, `V1_1__seed_empresa_admin.java:33-65`), JWT HS256 secret required (`JwtService.java:31-52`), secrets via env vars (`application.yml`, `render.yaml` uses `sync:false`), `.env` gitignored. Source cannot prove TLS termination, DB encryption at rest, backup encryption/retention, log retention, or third-party retention.

**Exact missing facts**

- Is HTTPS enforced end-to-end from browser to Vercel and Vercel to Render/API and API to Postgres?
- Is PostgreSQL encrypted at rest and are backups encrypted/retention-limited?
- Are OpenRouter/Meta configured with acceptable data retention/training/privacy settings?
- Are secrets stored only in provider secret managers/dashboards with rotation policy?

**Safe owner-observed validation plan**

- Owner verifies Vercel/Render domains redirect HTTP->HTTPS and HSTS is present.
- Owner checks Render/Postgres provider settings for encryption at rest, backup schedule, retention, and restore access controls.
- Owner reviews OpenRouter/Meta account/data-processing settings and contracts for retention/training.
- Owner confirms secret rotation/runbook and that no secrets appear in logs/build artifacts.

### N-004 — Vercel preview-to-production API isolation not verifiable from source

- **State**: needs_validation
- **Affected principal/resource**: Production API/data.
- **Trust boundary**: Preview deployment -> production backend.

**Why not confirmed**

`render.yaml` comment notes a known deviation: single environment and preview deployments may point to production API. Source does not show Vercel environment mapping.

**Exact missing facts**

- Do Vercel preview deployments use production `API_URL`/secrets?
- Are preview deployments protected from untrusted access?

**Safe owner-observed validation plan**

- Owner inspects Vercel project environment variables by environment (Production/Preview/Development).
- Owner confirms preview deployments require authentication or use isolated staging backend.

### N-005 — WhatsApp/AI sensitive-content logging and retention controls incomplete in source

- **State**: needs_validation
- **Affected principal/resource**: PII, phone numbers, financial messages, AI prompts/responses.
- **Trust boundary**: App -> logs/monitoring/AI provider/WhatsApp provider.

**Why not confirmed**

Current code avoids obvious body logging in several places (`WhatsappEnvioService` masks numbers and logs sizes; AI client logs model/status; exception handler has comment about not logging auth/cookies/login body). However, no centralized redaction policy/log retention config is visible, and message content is sent to OpenRouter/Meta.

**Exact missing facts**

- Are application/provider logs redacted and retention-limited?
- Are AI prompts containing financial data minimized/anonymized where possible?
- Are WhatsApp message bodies stored only when necessary and protected?

**Safe owner-observed validation plan**

- Owner reviews Render/Vercel/log sink retention and access controls.
- Owner samples logs for PII/financial content in staging with test data.
- Owner reviews provider DPAs/settings for OpenRouter and Meta WhatsApp Cloud API.

## Rejected / Not Findings

| Candidate | Result | Reason |
|-----------|--------|--------|
| `ImportacaoService` `faturaCartaoRepository.findById(faturaId)` | rejected | `faturaId` is produced inside the same tenant-scoped confirmation flow after loading `ImportacaoDocumento` by `idAndEmpresaId`; no external ID reaches this lookup. |
| `LancamentoService`/`UsuarioTelefoneService` `findById` followed by tenant/role checks | rejected | IDs come from request paths but service filters by `empresaId` or validates role/ownership before mutation; no cross-tenant write/read was evidenced. |
| Missing CSRF tokens on API | rejected | API is stateless Bearer/JWT resource server (`SecurityConfig` disables CSRF by design); browser uses BFF with httpOnly sameSite=lax cookie and server-side Bearer forwarding. CSRF still needs review if cookie-authenticated endpoints are added. |
| Password hashing absent | rejected | BCrypt cost 12 is configured in `SecurityConfig` and seed migration; passwords validated with `PasswordEncoder.matches`. |

## Dados enviados à IA

**Quais dados são enviados** (OpenRouter, chat, extração de PDF e embeddings):
- Contexto fixo de cada mensagem do chat: totais de entradas, saídas e saldo do mês atual e do anterior, e o resumo por categoria do mês atual (nome da categoria + total). Não inclui descrição de lançamento.
- A mensagem digitada pelo usuário e o histórico da sessão (até 20 mensagens).
- Sob demanda, quando o modelo chama uma ferramenta: agregados por mês e categoria; `detectar_ralos_e_recorrencias` devolve também a descrição de gastos recorrentes.
- O conhecimento cadastrado pelo admin da empresa e trechos da base pública de educação financeira (RAG).
- Na importação de PDF: o texto extraído do extrato/fatura, já sanitizado.
- **Nunca** são montados no prompt: nome, e-mail, telefone ou CPF do usuário, nem `empresa_id`/`usuario_id` (o tenant vem do JWT e fica só no backend).

**Política** (por requisição, configurável em `ai.openrouter.data-collection` / `ai.openrouter.zdr`):
- Toda requisição à OpenRouter envia `"provider": {"data_collection": "deny", "zdr": true}`: só provedores que não coletam nem treinam com os dados, e só endpoints Zero Data Retention (sem retenção).
- Os padrões de modelo são pagos; modelos `:free` só via env var, apenas em desenvolvimento e nunca com dados reais.
- Se nenhum provedor atende à política para um modelo, a cascata avança para o próximo e registra WARN.
- Logs não registram conteúdo de mensagens nem respostas da IA, só modelo, finish_reason, tokens, latência e resultado.

**Como desligar a IA por empresa:** **pendente.** Não existe hoje um flag por empresa. A IA só pode ser desligada globalmente, removendo `OPENROUTER_API_KEY`. Falta um campo em `empresa` com checagem em `AiController`, `WhatsappWebhookService` e `ImportacaoService`.

## Sensitive Data & Encryption Statement

**Source-evidence only:** Sensitive data has partial protection in code, but end-to-end encryption/protection cannot be fully confirmed from source.

- **Passwords**: Stored as BCrypt hashes (`usuario.senha_hash`, cost 12 in `SecurityConfig.java` and `V1_1__seed_empresa_admin.java`). Plaintext passwords are not persisted by reviewed code.
- **Auth tokens**: JWTs are signed HS256 with required secret (`JwtService.java`); browser receives httpOnly `sess` cookie through BFF login route (`web/app/api/auth/login/route.ts`). Cookie is `secure` in production mode.
- **Sensitive config/secrets**: Expected via environment variables; `.env` files are gitignored/untracked. No tracked secret files were found by name scan.
- **Financial/PII data**: Stored in PostgreSQL schema fields (users, phones, transactions, imports). Source shows no field-level encryption for financial records, phone numbers, WhatsApp message metadata/content, or AI conversation content. Protection at rest depends on database/provider configuration (needs validation).
- **Data in transit**: Code uses HTTPS-capable deployment assumptions, but actual TLS enforcement/certificates/HSTS are deployment concerns not provable from source (needs validation).
- **Third-party processing**: AI prompts/context are sent to OpenRouter; WhatsApp integration sends/receives data through Meta Cloud API. Source does not prove provider-side encryption settings, retention, or data-use restrictions (needs validation).

---

## End-to-End Validation (2026-11-21, commit `8b726a9`)

> Baseline inventory below reflects the audited commit before the remediations listed in **Remediation Status**. Re-run this inventory after the fixes are merged.


### Direct Answers

- **Is any endpoint unintentionally exposed?** **Yes, from an application-authorization perspective.** `GET/POST /api/ai/chat/conhecimento`, `POST /api/ai/chat/conhecimento/limpar`, and `POST /api/ai/chat/sessao/{sessionId}/limpar` are reachable to any authenticated user and allow cross-tenant/global AI state access (F-001/F-002). All other backend endpoints are either intentionally public (`/api/auth/login`, `/api/health`, `/webhooks/whatsapp`), authenticated, or admin-scoped by source config. Network-level exposure of the backend itself is **needs_validation**.
- **Is sensitive data exposed?** **Confirmed sensitive-data exposure risk exists in AI session/knowledge flows (cross-tenant context/prompt poisoning; history sent to model context).** No confirmed plaintext password/token leak in API responses/DTOs was found; `Usuario.senhaHash` is `@JsonIgnore`. Sensitive-data handling still has **needs_validation** gaps for TLS/at-rest encryption, logs, provider retention, and deployment isolation.

### A. Complete Endpoint Inventory & Exposure Verdicts

Legend: **Auth** = required auth; **Tenant** = tenant scoping evidenced; **Verdict** = intended / unintended exposure / needs_validation.

#### Backend (Spring)

| Method | Path | Source | Auth | Tenant | Exposed data / behavior | Verdict |
|--------|------|--------|------|--------|-------------------------|---------|
| POST | `/api/auth/login` | `api/src/main/java/com/controlefinanceiro/auth/AuthController.java:24` | Public (`SecurityConfig.java:41`) | N/A (issues token) | Accepts email/senha; returns `{token}` on success; generic 401 | Intended |
| GET | `/api/auth/me` | `api/src/main/java/com/controlefinanceiro/auth/AuthMeController.java:19` | Authenticated (`SecurityConfig.java:56`) | Yes (from JWT principal) | Returns email, empresa_id, role | Intended |
| GET | `/api/health` | `api/src/main/java/com/controlefinanceiro/health/HealthController.java:24` | Public (`SecurityConfig.java:41`) | N/A | Health status only | Intended |
| GET | `/webhooks/whatsapp` | `api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappWebhookController.java:55` | Public + Meta verify token check | N/A | Meta challenge verification; 403 on bad token | Intended |
| POST | `/webhooks/whatsapp` | `api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappWebhookController.java:75` | Public + HMAC SHA256 signature | Indirect (phone->empresa resolution) | Receives Meta payload; 401 invalid signature; async processing | Intended (needs validation for replay/rate limits) |
| POST | `/api/ai/chat` | `api/src/main/java/com/controlefinanceiro/ai/AiController.java:38` | Authenticated | Partial / broken | Accepts message + client `sessionId`; uses tenant for data context but session store not tenant-validated | **Unintended exposure** (F-001) |
| GET | `/api/ai/chat/conhecimento` | `api/src/main/java/com/controlefinanceiro/ai/AiController.java:53` | Authenticated | **No (global)** | Returns global AI knowledge | **Unintended exposure** (F-002) |
| POST | `/api/ai/chat/conhecimento` | `api/src/main/java/com/controlefinanceiro/ai/AiController.java:58` | Authenticated | **No (global)** | Overwrites global AI knowledge | **Unintended exposure** (F-002) |
| POST | `/api/ai/chat/conhecimento/limpar` | `api/src/main/java/com/controlefinanceiro/ai/AiController.java:64` | Authenticated | **No (global)** | Clears global AI knowledge | **Unintended exposure** (F-002) |
| POST | `/api/ai/chat/sessao/{sessionId}/limpar` | `api/src/main/java/com/controlefinanceiro/ai/AiController.java:70` | Authenticated | **No (global session key)** | Clears session by ID without tenant check | **Unintended exposure** (F-001) |
| GET | `/api/categorias` | `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java:31` | ADMIN/USER (`SecurityConfig.java:49`) | Yes (`TenantContext`) | Tenant categories | Intended |
| POST | `/api/categorias` | `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java:37` | ADMIN (`SecurityConfig.java:51`) | Yes | Creates category | Intended |
| PUT | `/api/categorias/{id}` | `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java:44` | ADMIN (`SecurityConfig.java:51`) | Yes | Edits category | Intended |
| DELETE | `/api/categorias/{id}` | `api/src/main/java/com/controlefinanceiro/categoria/CategoriaController.java:51` | ADMIN (`SecurityConfig.java:51`) | Yes | Soft-deactivates category | Intended |
| GET | `/api/dashboard` | `api/src/main/java/com/controlefinanceiro/dashboard/DashboardController.java:27` | Authenticated | Yes | Aggregated financial dashboard for tenant | Intended |
| POST | `/api/importacoes/pdf` | `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoController.java:41` | Authenticated | Yes | PDF upload/staging analysis; no raw PDF persisted by design | Intended |
| GET | `/api/importacoes` | `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoController.java:48` | Authenticated | Yes | Tenant import history | Intended |
| GET | `/api/importacoes/{id}` | `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoController.java:58` | Authenticated | Yes (`findByIdAndEmpresaId`) | Import detail/items/summary | Intended |
| POST | `/api/importacoes/{id}/confirmar` | `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoController.java:70` | Authenticated | Yes | Confirms staged items transactionally | Intended |
| DELETE | `/api/importacoes/{id}` | `api/src/main/java/com/controlefinanceiro/importacao/ImportacaoController.java:80` | Authenticated | Yes | Discards pending import | Intended |
| POST | `/api/lancamentos` | `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java:33` | Authenticated | Yes | Creates lancamento | Intended |
| POST | `/api/lancamentos/lote` | `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java:44` | Authenticated | Yes | Batch create, all-or-nothing | Intended |
| PUT | `/api/lancamentos/{id}` | `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java:52` | Authenticated | Yes (service filters by empresa) | Updates lancamento | Intended |
| DELETE | `/api/lancamentos/{id}` | `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java:59` | Authenticated | Yes | Deletes lancamento | Intended |
| GET | `/api/lancamentos` | `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java:68` | Authenticated | Yes | Lists tenant lancamentos | Intended |
| GET | `/api/lancamentos/{id}` | `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoController.java:79` | Authenticated | Yes (filters in-memory from tenant list) | Fetches one lancamento; inefficient implementation but tenant-scoped | Intended (performance debt) |
| POST | `/api/usuarios/{usuarioId}/telefone` | `api/src/main/java/com/controlefinanceiro/whatsapp/UsuarioTelefoneController.java:51` | ADMIN (`SecurityConfig.java:54` + service checks) | Yes | Links phone to user, sends verification flow | Intended |
| POST | `/api/telefones/verificar` | `api/src/main/java/com/controlefinanceiro/whatsapp/UsuarioTelefoneController.java:79` | Authenticated | Yes | Verifies phone code for authenticated user | Intended |

#### Web BFF Routes (`web/app/api/**/route.ts`)

| Method | Path | Source | Auth (BFF) | Backend target | Verdict |
|--------|------|--------|------------|----------------|---------|
| POST | `/api/ai/chat` | `web/app/api/ai/chat/route.ts:11` | Requires `sess` cookie | `${API_URL}/api/ai/chat` | Intended proxy; inherits F-001 backend risk |
| POST | `/api/auth/login` | `web/app/api/auth/login/route.ts:10` | Public | `${API_URL}/api/auth/login` | Intended |
| POST | `/api/auth/logout` | `web/app/api/auth/logout/route.ts:4` | None needed (deletes cookie) | none | Intended |
| GET | `/api/categorias` | `web/app/api/categorias/route.ts:8` | Requires `sess` | `${API_URL}/api/categorias` | Intended |
| GET | `/api/contas-a-pagar` | `web/app/api/contas-a-pagar/route.ts:17` | Weak cookie check/fallbacks | `${BACKEND_URL}/contas-a-pagar` (missing `/api`) | **Unintended exposure/routing risk** (F-003) |
| PATCH | `/api/contas-a-pagar/[id]/pagar` | `web/app/api/contas-a-pagar/[id]/pagar/route.ts:11` | Weak cookie check/fallbacks | `${BACKEND_URL}/contas-a-pagar/{id}/pagar` (missing `/api`) | **Unintended exposure/routing risk** (F-003) |
| GET | `/api/importacoes` | `web/app/api/importacoes/route.ts:110` | Requires `sess` | `${API_URL}/api/importacoes?...` | Intended |
| POST | `/api/importacoes` | `web/app/api/importacoes/route.ts:21` | Requires `sess` | `${API_URL}/api/importacoes/pdf` | Intended |
| GET | `/api/importacoes/[id]` | `web/app/api/importacoes/[id]/route.ts:18` | Requires `sess` | `${API_URL}/api/importacoes/{id}` | Intended |
| DELETE | `/api/importacoes/[id]` | `web/app/api/importacoes/[id]/route.ts:51` | Requires `sess` | `${API_URL}/api/importacoes/{id}` | Intended |
| POST | `/api/importacoes/[id]/confirmar` | `web/app/api/importacoes/[id]/confirmar/route.ts:14` | Requires `sess` | `${API_URL}/api/importacoes/{id}/confirmar` | Intended |
| POST | `/api/lancamentos/lote` | `web/app/api/lancamentos/lote/route.ts:10` | Requires `sess` | `${API_URL}/api/lancamentos/lote` | Intended |
| GET | `/api/me` | `web/app/api/me/route.ts:8` | Requires `sess`; clears cookie on 401 | `${API_URL}/api/auth/me` | Intended |

#### Web Pages / Public Reachability

| Path / area | Source | Public? | Notes | Verdict |
|-------------|--------|---------|-------|---------|
| `/login` | `web/app/login/page.tsx`, `web/proxy.ts:15-22` | Yes | Intended public page; authenticated users redirected unless `?expirada=1` | Intended |
| Other app pages (e.g. `/`, `/lancamentos`, `/categorias`, `/importacoes`, `/contas-a-pagar`) | `web/app/**/page.tsx`, `web/proxy.ts:24-33` | No by cookie gate | `proxy.ts` only checks cookie presence; real authz is backend JWT validation | Intended (cookie gate is UX, not security boundary) |
| `/api/**` BFF routes | `web/proxy.ts:37` matcher excludes `api` | Reachable directly | BFF routes perform their own cookie checks; backend still enforces JWT on protected endpoints | Intended; individual route verdicts above |
| Static assets / public files | `web/public/**`, matcher excludes `_next`, files with dot | Public | No secrets found; includes `public/assets/motion/*` | Intended |

### B. Sensitive-Data Exposure Review

#### Confirmed / Strong Evidence

| ID | State | Evidence | Impact | Smallest fix |
|----|-------|----------|--------|--------------|
| F-001 | confirmed | Session history keyed by client `sessionId`; history inserted into prompts (`AiService.java:63-75,130`) | Cross-tenant conversation context can be poisoned; history may be disclosed to AI model context | Tenant-bind session keys and validate ownership |
| F-002 | confirmed | Global `AiKnowledgeStore` string injected into system prompt (`AiKnowledgeStore.java:13-26`, `AiService.java:130`) | Any authenticated user can read/overwrite global AI instructions/data | ADMIN-only + tenant-scoped knowledge |
| SD-001 | needs_validation | AI sends financial context/messages to OpenRouter (`AiClient.java:91-113`, `AiContextoService.java` builds real financial data) | Financial/PII data leaves system boundary to third party | Confirm OpenRouter DPA/retention/training settings; minimize data; document consent/legal basis |

#### Rejected / Not Confirmed

| Candidate | State | Evidence | Reason |
|-----------|-------|----------|--------|
| Password hash exposure in responses | rejected | `Usuario.senhaHash` has `@JsonIgnore` (`api/src/main/java/com/controlefinanceiro/usuario/Usuario.java:38-41`); controllers return DTOs | No response path serializing password hash was found |
| Hardcoded production secrets in tracked source | rejected | `render.yaml` uses `sync:false`; `application.yml` reads env vars; `.env*` gitignored (`.gitignore:5-6`) | No tracked secret values found; local `.env` files are untracked |
| Browser-exposed sensitive `NEXT_PUBLIC_*` secrets | rejected / low | Only `NEXT_PUBLIC_API_URL` fallback appears in server-side BFF/page code (`web/app/api/contas-a-pagar/*`, `web/app/contas-a-pagar/page.tsx:13`); no secret-bearing NEXT_PUBLIC vars found | Not a confirmed browser secret leak, but F-003 fallback should be removed |
| XSS via Markdown rendering | rejected | `react-markdown` used without `rehype-raw`/`allowDangerousHtml` (`web/components/ai/markdown.tsx:13-61`) | Raw HTML not rendered |
| `dangerouslySetInnerHTML` code injection | rejected | Only static constant `HEAD_LINE` injected in layout (`web/app/layout.tsx:14-51`) | No user-controlled input flows into HTML sink |
| SQL injection in repositories/services | rejected | JPA repositories/derived queries and parameterized `@Query` patterns observed; no string-concatenated SQL found in reviewed paths | No concrete injection sink identified |

### C. Attack Sensitivity / Threat Checks

| Check | Result | Evidence | Notes |
|-------|--------|----------|-------|
| Broken auth / JWT validation | Mostly good; needs validation | `JwtService` HS256, >=32-byte secret, exp/iat; `SecurityConfig` oauth2ResourceServer validates JWT; `jwtAuthenticationConverter` requires `empresa_id`/`role` | No refresh/revocation/blacklist observed; logout is cookie deletion only |
| IDOR / tenant bypass | **Confirmed issues F-001/F-002**; other reviewed ID lookups scoped | Tenant from `TenantContext`; import/lancamento/whatsapp services use empresa filters; WhatsApp confirmation lookup checks phone ownership (`WhatsappWebhookService.java:405-452`) | Prioritize AI fixes |
| CSRF | Not confirmed | API stateless Bearer; BFF uses `sess` httpOnly cookie + sameSite=lax and forwards Bearer server-side | Reassess if cookie-auth endpoints added |
| XSS | Not confirmed | React escaping default; Markdown no raw HTML; static `dangerouslySetInnerHTML` | Monitor future HTML sinks |
| SQL/JPQL injection | Not confirmed | Spring Data/JPA parameterized queries | Continue parameterization discipline |
| SSRF | Low / needs validation | Backend fetches fixed OpenRouter/Meta APIs by config; BFF forwards to `process.env.API_URL` except F-003 fallbacks | Remove URL fallbacks; validate env governance |
| File upload abuse | Partially mitigated / needs validation | PDF magic-byte, size, batch limits, hash dedupe (`ImportacaoService.java:67-75,850-877,631-642`); no raw PDF stored | No antivirus/malware scan; parser DoS/resource limits depend on PDFBox/runtime |
| Webhook authenticity | Good in source | HMAC SHA256 over raw body, constant-time compare (`WhatsappWebhookController.java:95-126`) | Replay/rate limiting/idempotency window needs validation |
| Brute force/rate limiting | Needs validation | No app-level throttling found | N-001 |
| Security headers/CORS | Needs validation | No explicit config in source | N-002 |
| Secrets in git history | Not fully confirmed | Current tree has no tracked `.env`; prior `git ls-files`/name scan clean for common secret files | Recommend full secret scan with gitleaks/trufflehog in CI |

### D. Updated Findings / Validation Notes

- **F-001, F-002, F-003 were confirmed** as of `8b726a9` with unchanged source evidence; see **Remediation Status** for the post-audit fixes.
- **New confirmed finding:** none beyond existing F-001..F-003 after end-to-end inventory.
- **New needs_validation focus:** backend direct network exposure is unknown; BFF cookie gate is not an auth boundary; third-party AI/WhatsApp data retention and logging controls need owner validation.

### Endpoint Exposure Verdict

- **Backend endpoints inventoried:** 27
- **Public backend endpoints:** 4 (`POST /api/auth/login`, `GET /api/health`, `GET /webhooks/whatsapp`, `POST /webhooks/whatsapp`)
- **Authenticated backend endpoints:** 18
- **Admin-only backend endpoints:** 4 (`POST/PUT/DELETE /api/categorias/**`, `POST /api/usuarios/{usuarioId}/telefone`) plus admin-expected but currently mis-scoped AI knowledge endpoints
- **Unintentionally exposed by authorization design:** 5 (`POST /api/ai/chat` tenant-binding gap, `GET/POST /api/ai/chat/conhecimento`, `POST /api/ai/chat/conhecimento/limpar`, `POST /api/ai/chat/sessao/{sessionId}/limpar`)
- **BFF endpoints inventoried:** 13; **unintended/risky:** 2 (`/api/contas-a-pagar`, `/api/contas-a-pagar/[id]/pagar`)
