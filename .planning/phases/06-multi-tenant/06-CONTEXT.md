# Phase 6: Multi-tenant, Usuários e Google - Context

**Gathered:** 2026-11-21
**Status:** Ready for planning
**Source:** `MULT_TENANT.md` (documento aprovado em 2026-11-21, commit base `8b726a9`)

<domain>
## Phase Boundary

Esta fase transforma o projeto de "single-tenant com coluna `empresa_id`" em **plataforma multi-tenant real**: cadastro self-service de empresas, gestão de usuários pelo ADMIN, autenticação via Google (OIDC) e **isolamento financeiro por usuário dentro da empresa** — inclusive com ADMIN impedido de ler dados financeiros de outros usuários. Promove `GROW-01` (multiempresa real) e `GROW-02` (convite de usuários) de v2 para esta fase, e entrega os pré-requisitos técnicos de LGPD (consentimento, DPA, política de privacidade).

**In scope (Fases 0–6 do documento):**
- Fase 0 — Estabilização: commit das correções F-001/F-002/F-003, spotless pré-existente, Testcontainers com Docker, artefatos GSD desta fase.
- Fase 1 — Schema e domínio: migration `V9__multi_tenant_usuarios.sql`, `google_sub`/`email_verificado`/`token_version`, tabela `usuario_convite`, enum `Role`, repositórios, índices.
- Fase 2 — Onboarding e usuários: `TenantOnboardingService`, `ConviteService`, `UsuarioService`, endpoints públicos (`/api/auth/registro`, `/api/auth/google`, `/api/auth/convites/aceitar`) e ADMIN (`/api/usuarios/**`), regras de `SecurityConfig`.
- Fase 3 — Isolamento por usuário: `usuario_id` em **todas** as queries financeiras (lançamentos, importações, dashboard, contas a pagar, IA), `UsuarioContext`, validação de `token_version`/`ativo` no filtro, persistência tenant-scoped dos stores de IA, checagem de dono no WhatsApp, ArchUnit.
- Fase 4 — Google OAuth: validação do ID token (`iss`, `aud`, `exp`, `email_verified`, `google_sub`), BFF com Authorization Code + PKCE e `state` em cookie httpOnly, rate limit.
- Fase 5 — Frontend: `/cadastro`, `/convite/[token]`, `/usuários`, botão Google no `/login`, allowlist em `proxy.ts`, UI com role coerente, e2e Playwright.
- Fase 6 — Segurança, deploy e LGPD: rate limit, headers, CORS, rotação de segredos, Render/Vercel, política de privacidade/DPA, `security-audit` e `seo-audit`.

**Out of scope (não pertence a esta fase):**
- Transferência do histórico financeiro existente do ADMIN seed para novos usuários (decisão: permanece no seed; transferência seria projeto separado — D-17).
- Domínio Google restrito (`hd` claim) — pode ser adicionado depois; não habilitado por padrão.
- Login com Apple/Microsoft ou qualquer outro provedor que não Google.
- Recuperação de senha / "esqueci minha senha", 2FA/MFA, verificação de e-mail por link (fora do escopo do documento).
- `GROW-03` (OFX/CSV), `GROW-04` (lançamentos recorrentes), `GROW-05` (app mobile) — permanecem em v2.
- Texto/contrato da política de privacidade e DPA (LGPD-01 é tratado como pré-requisito técnico/processual; o texto jurídico fica fora do fluxo GSD).
- Reescrita das migrations V1..V8 (são imutáveis).
</domain>

<constraints>
## Constraints (não negociáveis)

- **Stack** (AGENTS.md): Java 21 + Spring Boot 3, Spring Security + JWT (Nimbus), Spring Data JPA, PostgreSQL, Flyway, Testcontainers; Next.js App Router + TypeScript.
- **Dinheiro**: `BigDecimal` + `NUMERIC(14,2)`, `scale=2`, `HALF_EVEN`, rejeição de >2 casas; nenhum `double`/`float`. Nenhuma task desta fase introduce tipo float.
- **Tenant**: `empresa_id` vem exclusivamente do JWT; único ponto de acesso é `TenantContext` (e o novo `UsuarioContext`). Exceção controlada apenas nos 3 endpoints pré-auth.
- **Migrations imutáveis**: criar `V9`, nunca editar V1..V8; `ddl-auto: none`.
- **Fluxo GSD**: backend via TDD rigoroso (JUnit + Testcontainers) antes de implementar; frontend via `design-taste-frontend`; `security-audit` e `seo-audit` antes de concluir/deploy.
- **E-mail é chave global**: `usuario.email UNIQUE` global — o mesmo e-mail não pode pertencer a duas empresas (é o mecanismo de resolução de tenant no login Google e no convite).
</constraints>

<locked_decisions>
## Locked Decisions (de `MULT_TENANT.md` — NON-NEGOTIABLE)

### Decisões travadas (§1)
- **D-01:** Google OAuth/OIDC — entrar/cadastrar com conta Google; o backend valida o ID token e **emite JWT próprio** (nunca usa o token do Google como sessão).
- **D-02:** Criação de tenant **self-service no cadastro**: empresa + primeiro ADMIN + categorias padrão, **em transação** (rollback completo se qualquer passo falhar).
- **D-03:** Isolamento **por usuário**, dentro da empresa; **nem ADMIN enxerga dados financeiros de outros usuários**.
- **D-04:** Papel ADMIN **gerencia apenas contas** (convidar, desativar, alterar papel); não lê lançamentos/importações/dashboard/IA/WhatsApp de ninguém.
- **D-05:** E-mail **global único**; o mesmo e-mail não pode existir em duas empresas.
- **D-06:** E-mail já existente no Google login → usuário **entra na empresa existente**; não cria nova empresa com esse e-mail.
- **D-07:** Fluxo Google: **criar empresa nova** quando o e-mail não existe; **aceitar convite** quando existe convite para o e-mail.

### Schema e tenant (§3)
- **D-08:** Migration obrigatória `V9__multi_tenant_usuarios.sql`; nunca editar V1..V8.
- **D-09:** `usuario` ganha `google_sub VARCHAR(255)` (UNIQUE parcial quando presente), `email_verificado BOOLEAN NOT NULL DEFAULT false`, `token_version INT NOT NULL DEFAULT 0`; `senha_hash` **continua NOT NULL** — usuários Google-only recebem hash BCrypt aleatório/imprevisível, impossível de usar por senha.
- **D-10:** Nova tabela `usuario_convite` (`id`, `empresa_id`, `email`, `role`, `token_hash`, `expira_em`, `usado_em`, `criado_por`, `criado_em`); token claro **só no link**, gravado apenas como hash; válido para **um e-mail**, expira e é **single-use**.
- **D-11:** Índices: `idx_lanc_empresa_usuario_data (empresa_id, usuario_id, data)`, `idx_importacao_empresa_usuario (empresa_id, usuario_id)`, `UNIQUE(google_sub)` parcial.
- **D-12:** Exceção controlada: apenas `/api/auth/registro`, `/api/auth/google`, `/api/auth/convites/aceitar` derivam a empresa do estado criado/validado no servidor e emitem JWT na hora. **Nenhum endpoint de negócio aceita `empresa_id` ou `usuario_id`** em body/query/path — esses valores vêm de `TenantContext` + `UsuarioContext`.
- **D-13:** Cookie `sess` permanece httpOnly, SameSite=lax, Secure em produção, 8h. Novo claim `usuario_id`; `sub` permanece e-mail por compatibilidade, mas **todos os lookups passam a usar `usuario_id`**. Introduzir `UsuarioContext` com `(empresaId, usuarioId)`.
- **D-14:** Validar `token_version` e `ativo` no filtro de autenticação para desativação/demissão com efeito imediato (sem esperar as 8h do JWT).
- **D-15:** `role` validado contra enum `ADMIN|USER` na emissão e na conversão do JWT — hoje qualquer string não vazia vira `ROLE_<role>`.
- **D-16:** Matriz de isolamento: Lançamentos/Importações/Dashboard/IA-histórico/Contas a pagar/WhatsApp → `empresa_id + usuario_id`; IA-conhecimento e Categorias → `empresa_id` (dado de referência, ADMIN gerencia); Usuários → `empresa_id` (só contas).
- **D-17:** Dados históricos: `lancamento.usuario_id` já é obrigatório e aponta para o ADMIN seed; **histórico permanece do seed**.

### Endpoints e BFF (§5)
- **D-18:** Contratos exatos: públicos — `POST /api/auth/registro` `{empresaNome,nome,email,senha}`→`{token}` (falha se e-mail existe, erro genérico); `POST /api/auth/google` `{idToken,intencao:"LOGIN"|"CADASTRO"|"CONVITE",conviteToken?}`→`{token}` (e-mail existente entra na empresa existente; convite exige e-mail igual); `POST /api/auth/convites/aceitar` `{token,nome?,senha?}`→`{token}` (single-use, cria usuário na empresa do convite). ADMIN — `GET /api/usuarios`, `POST /api/usuarios/convites` `{email,role}`→`{token,expiraEm}`, `PATCH /api/usuarios/{id}` `{role?,ativo?}`, `POST /api/usuarios/{id}/desativar`. **Nenhum retorna dado financeiro.**
- **D-19:** BFF: extrair `setSessionCookie()` compartilhado de `web/app/api/auth/login/route.ts:49-56`; Google via Authorization Code + PKCE com `state` em cookie httpOnly e callback server-side; **nunca enviar ID token/access token Google ao navegador como sessão**.
- **D-20:** Rate limit em `/api/auth/google` e no callback; `state`/PKCE obrigatórios; adicionar throttle em login/registro/callback.

### Deploy, segurança e aceite (§4, §6, §7)
- **D-21:** Render com Postgres pago + backup, staging separado, `GOOGLE_*` com `sync:false`; Vercel apenas env vars server-side; bloquear onboarding em preview até existir staging; nenhuma prévia aponta para produção; nenhum segredo em `NEXT_PUBLIC_*`.
- **D-22:** Política de privacidade/LGPD, consentimento e DPA/retensão com Google, OpenRouter e Meta antes do uso real.
- **D-23:** Testes obrigatórios: rollback transacional do onboarding; e-mail duplicado (cadastro genérico / Google entra na empresa existente); convite expirado, reutilizado e com e-mail divergente do ID token; ID token `iss`/`aud`/`exp`/`email_verified`/`google_sub`; cross-tenant e cross-user em **todos** os recursos financeiros; ADMIN → 404/403 ao ler dados alheios; role tampering no JWT e `token_version` após demissão/desativação; mesmo `sessionId` entre usuários/empresas não cruza histórico de IA; Testcontainers + Playwright.
- **D-24:** Aceites por fase: F0 = working tree limpo, 106 unitários + 13 integrações passando; F1 = migration aplica em banco limpo e sobre schema atual, constraint de e-mail global intacta; F2 = falha em categoria faz rollback completo, convite expirado/reutilizado rejeitado; F3 = testes cross-user/cross-tenant inclusive ADMIN→404/403; F4 = token com `aud` errado/expirado/e-mail não verificado → 401, login com e-mail existente entra no tenant correto; F5 = e2e de cadastro/convite/Google/gestão passa, role UI coerente com backend; F6 = nenhuma prévia aponta para produção, nenhum segredo em `NEXT_PUBLIC_*`.

### Agente tem discrição sobre
- Formato/TTL exato do token de convite (desde que hash, single-use e expirável) e shape exato dos DTOs.
- Limiares exatos de rate limit e a mensagem de erro genérica (desde que não enumere e-mails).
- Estrutura da tabela de persistência dos stores de IA (desde que chaveie por `empresa_id + usuario_id + sessionId`).
- Estratégia de link automático de `google_sub` quando e-mail verificado coincide com usuário existente — **proposta em aberto (§9)**: implementar a forma mais segura e documentar; domínio Google restrito fica desligado.
</locked_decisions>

<dependencies>
## Dependencies e ordem das Fases

| Fase | Depende de | Entrega / desbloqueia |
|------|-----------|------------------------|
| F0 — Estabilização | baseline `8b726a9` | Docker + Testcontainers verdes, spotless limpo, tree limpo → desbloqueia todas |
| F1 — Schema e domínio | F0 | `V9`, entidades, repositórios, `Role` → desbloqueia F2 e F3 |
| F2 — Onboarding e usuários | F1 | endpoints pré-auth + ADMIN, Services → desbloqueia F5 (BFF/UI) |
| F3 — Isolamento por usuário | F1 | `UsuarioContext`, filtros, token_version, stores IA, ArchUnit → desbloqueia F5/F6 |
| F4 — Google OAuth | F2, F3 | validação ID token + PKCE → desbloqueia F5 (botão Google) |
| F5 — Frontend | F2, F3, F4 | páginas, BFFs, allowlist, e2e → desbloqueia F6 |
| F6 — Segurança/deploy/LGPD | F1–F5 | rate limit final, env vars, política, auditorias → fecha a fase |

**Dependências externas / pré-condições:**
- `GROW-01`/`GROW-02` (REQUIREMENTS.md, antes em v2) são promovidos e **devem ser marcados como cobertos** por esta fase (fora desta pasta, conforme restrição de escrita do pedido atual).
- Correções F-001/F-002/F-003 (SECURITY.md) e spotless de `AiContextoService.java`, `LancamentoService.java`, `UsuarioTelefoneService.java` são pré-condição de F0.
- Docker é pré-condição para Testcontainers; hoje indisponível.
- Novas env vars Google (`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`) exigem configuração humana no Render (`sync:false`) e no Vercel (server-side) — registrar em `user_setup` dos planos de F4/F6.
</dependencies>

<canonical_refs>
## Canonical References (downstream agents DEVEM ler antes de implementar)

### Documento-fonte e planejamento
- `MULT_TENANT.md` — documento aprovado; fonte de todas as decisões travadas (D-01..D-24), contratos de endpoint, matriz de isolamento, fases e testes obrigatórios.
- `.planning/PROJECT.md`, `.planning/ROADMAP.md`, `.planning/REQUIREMENTS.md` (GROW-01/02, LGPD-01, AUTH-01/03/05, INF-01/02), `AGENTS.md` (fluxo GSD, skills, BigDecimal, tenant).

### Código existente (analógos a seguir / pontos de extensão)
- `api/src/main/java/com/controlefinanceiro/auth/JwtService.java` — emissão HS256 (claims `sub`/`empresa_id`/`role`, +8h); adicionar `usuario_id` (D-13).
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java` — `permitAll` em `/api/auth/login`, converter `Jwt`→`UsuarioAuth`, BCrypt(12); adicionar públicos, ADMIN em `/api/usuarios/**`, validação de `role`/`token_version`/`ativo` (D-14/D-15).
- `api/src/main/java/com/controlefinanceiro/common/TenantContext.java` + `common/UsuarioAuth.java` + `common/UsuarioAuthAuthenticationToken.java` — padrão de principal contextual; espelhar em `UsuarioContext`.
- `api/src/main/java/com/controlefinanceiro/usuario/Usuario.java` + `UsuarioRepository.java` + `empresa/Empresa.java` — entidades; adicionar colunas e métodos de busca (D-09).
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoRepository.java`, `dashboard/DashboardRepository.java`, `importacao/ImportacaoDocumentoRepository.java`, `importacao/ImportacaoItemRepository.java`, `conta/ContaAPagarRepository.java`, `conta/FaturaCartaoRepository.java` — filtros hoje só por empresa; adicionar `usuario_id` (D-16).
- `api/src/main/java/com/controlefinanceiro/ai/AiMemoryStore.java`, `ai/AiKnowledgeStore.java`, `ai/AiContextoService.java` — chaves atuais por empresa; trocar por empresa+usuário(+sessionId) e persistir (D-16).
- `api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappWebhookService.java` + `whatsapp/UsuarioTelefoneService.java` — passar contexto explícito na async (já feito para tenant).
- `api/src/main/resources/db/migration/V1__schema.sql` (DDL de referência), `V1_1__seed_empresa_admin.java`, `V2__seed_categorias.java` (padrão de seed de categorias padrão), `V3__importacoes_pdf.sql` … `V8__renomeia_coluna_codigo.sql` — imutáveis; V9 é a próxima.
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java` — padrão de erro RFC 7807 para novas exceções.
- `api/pom.xml` — adicionar dependências de validação de ID token Google e ArchUnit.
- `web/app/api/auth/login/route.ts` — padrão de BFF e `setSessionCookie()` a extrair (D-19).
- `web/proxy.ts` — allowlist de rotas públicas (D-05/Fase 5, linhas 15–22).
- `web/app/login/page.tsx` (+ componentes em `web/components/`), `web/components/ui/sidebar.tsx`, `web/components/TopNav.tsx` — pontos de extensão da UI.
- `web/e2e/*.spec.ts`, `web/playwright.config.ts` — padrão de testes e2e; adicionar specs de cadastro/convite/Google/gestão.
- `SECURITY.md` — correções F-001/F-002/F-003 (pré-condição F0).
</canonical_refs>

<verification>
## Comandos de validação (MULT_TENANT.md §8)

```powershell
.\mvnw.cmd "-Dspotless.check.skip=true" "-Dtest=!*IntegrationTest,!*MigrationTest" test
.\mvnw.cmd test
npm --prefix C:\Users\11\controle-gastos\web run lint
npx tsc --noEmit
npm --prefix C:\Users\11\controle-gastos\web run test:e2e
```

Comandos específicos por task (unitários rápidos, <60s) aparecem em cada `<verify>` dos planos.
</verification>

<risks>
## Riscos e pontos em aberto (MULT_TENANT.md §9)

- **Histórico existente** permanece no ADMIN seed — confirmar com o cliente se deve ser transferido (D-17).
- **Link automático Google** quando e-mail verificado coincide com usuário existente — proposto, definir implementação segura.
- **Domínio Google restrito** — desligado por padrão.
- **`token_version`** — validação por request adiciona uma consulta; aceitar o custo para desativação imediata.
- **Multi-replica IA** — stores em memória precisam virar tabela antes de escalar na Render.
- **Preview → produção** — bloquear onboarding em preview até existir staging.
- **Docker** — indisponível no baseline; F0 depende dele para os 13 testes de integração.
</risks>

---
*Phase: 06-multi-tenant*
*Context gathered: 2026-11-21 a partir de `MULT_TENANT.md` (aprovado)*
