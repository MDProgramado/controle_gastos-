# MULT_TENANT.md

**Status:** Proposto em 2026-11-21  
**Commit base:** `8b726a9`  
**Objetivo:** transformar o projeto de “single-tenant com coluna `empresa_id`” em uma plataforma multi-tenant real, com cadastro self-service de empresas, gestão de usuários, autenticação via Google e isolamento financeiro por usuário — inclusive sem ADMIN enxergando dados de outros usuários.

## 1. Decisões travadas

| Tema | Decisão |
|------|---------|
| Google | OAuth/OIDC: entrar/cadastrar com conta Google; backend valida ID token e emite JWT próprio |
| Criação de tenant | Self-service no cadastro: empresa + primeiro ADMIN + categorias padrão, em transação |
| Isolamento interno | Por usuário, dentro da empresa; **nem ADMIN vê dados financeiros de outros** |
| Papel ADMIN | Gerencia apenas contas: convidar, desativar, alterar papel; não lê dados financeiros |
| E-mail | Global único; o mesmo e-mail não pode existir em duas empresas |
| E-mail já existente no Google login | Usuário entra na empresa existente; não cria nova empresa com esse e-mail |
| Fluxo Google | Criar empresa nova quando e-mail não existe; aceitar convite quando existe convite para o e-mail |

## 2. Estado atual — baseline

| Área | Situação atual | Evidência |
|------|----------------|-----------|
| Autenticação | `POST /api/auth/login`, JWT HS256 8h, cookie `sess` httpOnly via BFF | `AuthController.java:14-27`, `JwtService.java:37-77`, `web/app/api/auth/login/route.ts:49-56` |
| Identidade no token | `sub = email`, `empresa_id`, `role` em `String` | `JwtService.java:63-77`, `SecurityConfig.java:71-83` |
| Tenant | `TenantContext` lê `empresa_id` somente do JWT; queries filtram por empresa | `TenantContext.java:11-26`, `LancamentoRepository.java:22-78`, `DashboardRepository.java:26-89` |
| Usuários | Não existe cadastro, listagem, edição ou convite via API; só seed `V1_1` | `V1_1__seed_empresa_admin.java:37-59`, `UsuarioRepository.java:15-16` |
| E-mail/senha | `email UNIQUE` global, `senha_hash NOT NULL`, BCrypt(12) | `V1__schema.sql:13-22`, `SecurityConfig.java:85-89` |
| Google | Nenhuma dependência, config ou endpoint OAuth/OIDC | `api/pom.xml:41-111`, `web/package.json:12-41` |
| IA | Isolamento por empresa corrigido no working tree; histórico em memória | `AiMemoryStore.java:18-34`, `AiKnowledgeStore.java:17-23`, `SECURITY.md:43-50` |
| Planejamento | Multiempresa real e social login estavam fora de escopo v1; `GROW-01/02` já existem | `.planning/PROJECT.md:37`, `.planning/REQUIREMENTS.md`, `01-CONTEXT.md:28` |
| Segurança | Correções F-001/F-002/F-003 não commitadas; Docker indisponível para Testcontainers | `SECURITY.md:43-50`, `git status --short` |

## 3. Arquitetura alvo

### 3.1 Modelo de dados

**`empresa`**
- Continua com `id`, `nome`, `criado_em`.
- Não recebe `empresa_id` próprio; é a raiz do tenant.

**`usuario`**
- Mantém `empresa_id NOT NULL` e `email UNIQUE` global.
- Adicionar:
  - `google_sub VARCHAR(255)` — identificador estável da conta Google, `UNIQUE` global quando presente.
  - `email_verificado BOOLEAN NOT NULL DEFAULT false`.
  - `token_version INT NOT NULL DEFAULT 0` — invalida JWTs antigos após mudança de papel/`ativo`/senha.
- `senha_hash` continua `NOT NULL`: usuários Google-only recebem hash BCrypt aleatório, imprevisível, impossível de usar para login por senha.

**`usuario_convite` (nova)**
- `id`, `empresa_id`, `email`, `role`, `token_hash`, `expira_em`, `usado_em`, `criado_por`, `criado_em`.
- Token de convite armazenado apenas como hash; texto claro vai só no link enviado ao usuário.
- Convite é válido para **um e-mail**, expira e é single-use.

**Índices**
- `CREATE INDEX idx_lanc_empresa_usuario_data ON lancamento (empresa_id, usuario_id, data)`.
- `CREATE INDEX idx_importacao_empresa_usuario ON importacao_documento (empresa_id, usuario_id)`.
- `UNIQUE (google_sub)` parcial.

> Migration obrigatória: `V9__multi_tenant_usuarios.sql`. Nunca editar `V1..V8`.

### 3.2 Regra de tenant — exceção controlada

Regra atual: `empresa_id` vem exclusivamente do JWT em endpoints de negócio.

Com onboarding público, é impossível ter JWT antes de criar a empresa. Portanto:

- Endpoints **pré-auth** (`/api/auth/registro`, `/api/auth/google`, `/api/auth/convites/aceitar`) são a única exceção: eles derivam a empresa do estado criado/validado no servidor e emitem o JWT imediatamente.
- Nenhum endpoint de negócio aceita `empresa_id` ou `usuario_id` no body, query ou path.
- Esses valores continuam vindo de `TenantContext` + novo `UsuarioContext`.

### 3.3 Sessão e token

- Cookie `sess` continua httpOnly, SameSite=lax, Secure em produção, 8h.
- Novo claim `usuario_id` no JWT.
- `sub` permanece com e-mail por compatibilidade inicial, mas todos os lookups passam a usar `usuario_id`.
- Introduzir `UsuarioContext` com `(empresaId, usuarioId)`.
- Adicionar validação de `token_version` e `ativo` em filtro de autenticação para que desativação/demissão tenham efeito imediato, sem esperar 8h.
- `role` passa a ser validado contra enum `ADMIN|USER` na emissão e na conversão do JWT — hoje qualquer string não vazia vira `ROLE_<role>`.

### 3.4 Matriz de isolamento

| Recurso | Filtro obrigatório | ADMIN |
|---------|--------------------|-------|
| Lançamentos | `empresa_id + usuario_id` | Não vê os de outros usuários |
| Importações PDF | `empresa_id + usuario_id` | Não vê as de outros usuários |
| Dashboard | `empresa_id + usuario_id` | Não vê números de outros usuários |
| IA — histórico/sessões | `empresa_id + usuario_id + sessionId` | Não vê sessões de outros |
| IA — conhecimento | `empresa_id` | ADMIN gerencia; não é dado financeiro |
| Categorias | `empresa_id` | ADMIN gerencia; dado de referência da empresa |
| Usuários | `empresa_id` | ADMIN gerencia contas, sem dados financeiros |
| WhatsApp | telefone → `empresa_id + usuario_id` | Dono do telefone apenas |
| Contas a pagar / faturas | `empresa_id + usuario_id` | Não vê as de outros usuários |

## 4. Impactos e implicações

| Área | Impacto | Ação |
|------|---------|------|
| Schema | Migrations são imutáveis; `ddl-auto: none` | Criar `V9`, nunca editar antigas |
| Auth | `sub=email` e lookups por e-mail | Adicionar `usuario_id`; trocar `findByEmail` em serviços |
| Repositories | Filtros hoje só por empresa | Adicionar `usuario_id` em todas as queries financeiras |
| IA | Stores em memória e por empresa | Chavear por empresa+usuário; mover para tabela persistente |
| Async WhatsApp | Não há `SecurityContext` | Passar `empresaId`/`usuarioId` explicitamente, como já feito para tenant |
| Frontend | Não há cadastro, convite, gestão de usuários nem botão Google | Novas páginas, BFFs, allowlist do `proxy.ts` |
| Segurança | Sem rate limit em login/registro/callback | Adicionar throttle + `state`/PKCE no OAuth |
| Deploy | Novas env vars Google e isolamento de preview | Render `sync:false`, staging separado |
| LGPD | Google e OpenRouter processam PII | Política de privacidade, consentimento, DPA/retensão |
| Planejamento | Decisões antigas contrárias | Atualizar `.planning/PROJECT.md`, `ROADMAP.md`, `REQUIREMENTS.md`, `STATE.md`, `AGENTS.md`, `01-CONTEXT.md` |
| Dados históricos | `lancamento.usuario_id` já obrigatório e aponta para o admin seed | Decisão: histórico permanece do admin seed; transferência futura seria projeto separado |

## 5. Contratos de endpoint

### Públicos / pré-auth

| Método | Path | Request | Response | Regras |
|--------|------|---------|----------|--------|
| POST | `/api/auth/registro` | `{ empresaNome, nome, email, senha }` | `{ token }` | Cria empresa + ADMIN + categorias; falha se e-mail existir; erro genérico |
| POST | `/api/auth/google` | `{ idToken, intencao: "LOGIN" | "CADASTRO" | "CONVITE", conviteToken? }` | `{ token }` | Valida Google; e-mail existente entra na empresa existente; convite exige e-mail igual |
| POST | `/api/auth/convites/aceitar` | `{ token, nome?, senha? }` | `{ token }` | Consome convite single-use; cria usuário na empresa do convite |

### ADMIN — somente gestão de contas

| Método | Path | Request | Response |
|--------|------|---------|----------|
| GET | `/api/usuarios` | — | `[{ id, nome, email, role, ativo }]` da mesma empresa |
| POST | `/api/usuarios/convites` | `{ email, role }` | `{ token, expiraEm }` |
| PATCH | `/api/usuarios/{id}` | `{ role?, ativo? }` | DTO de conta |
| POST | `/api/usuarios/{id}/desativar` | — | DTO de conta |

> Nenhum desses endpoints retorna lançamentos, importações, dashboard, IA ou WhatsApp.

### BFF

- `POST /api/auth/registro`
- `POST /api/auth/google/init` — gera `state`/PKCE e redireciona.
- `GET /api/auth/google/callback` — troca code, chama `/api/auth/google`, define cookie `sess`.
- `POST /api/auth/convites/aceitar`
- `GET/POST/PATCH /api/usuarios/*`
- Shared `setSessionCookie()` extraído de `web/app/api/auth/login/route.ts:49-56`.

## 6. Fases de execução

### Fase 0 — Estabilização e GSD

- Commitar correções F-001/F-002/F-003.
- Rodar Testcontainers com Docker; corrigir spotless pré-existente em `AiContextoService.java`, `LancamentoService.java`, `UsuarioTelefoneService.java`.
- Atualizar artefatos GSD e criar nova fase `06-multi-tenant`.
- **Aceite:** working tree limpo; 106 testes unitários + 13 integrações passando.

### Fase 1 — Schema e domínio

- Criar `V9__multi_tenant_usuarios.sql`.
- `EmpresaRepository`, `UsuarioRepository.findByIdAndEmpresaId`, `findByEmpresaIdAndEmail`, `findByGoogleSub`, `ConviteRepository`.
- Enum `Role` e validação de `role`.
- **Aceite:** migration aplica em banco limpo e em cópia do schema atual; constraint de e-mail global intacta.

### Fase 2 — Onboarding e usuários

- `TenantOnboardingService`: empresa + ADMIN + categorias padrão em `@Transactional`.
- `ConviteService` e `UsuarioService`.
- Endpoints da seção 5.
- `SecurityConfig`: permitir públicos; ADMIN em `/api/usuarios/**`.
- **Aceite:** cadastro com falha em categoria faz rollback completo; convite expirado/reutilizado é rejeitado.

### Fase 3 — Isolamento por usuário

- Adicionar `usuarioId` em `LancamentoRepository`, `DashboardRepository`, importações, contas a pagar e `AiContextoService`.
- Trocar `findById` + filtro em memória por `findByIdAndEmpresaIdAndUsuarioId`.
- `AiMemoryStore`: chave `empresaId::usuarioId::sessionId`.
- Persistir memória/knowledge em tabelas tenant-scoped para suportar múltiplas réplicas na Render.
- WhatsApp: checar dono do telefone/usuário na confirmação.
- ArchUnit: queries financeiras devem conter empresa+usuário.
- **Aceite:** testes cross-user e cross-tenant, incluindo ADMIN → 404/403 ao tentar ler dados alheios.

### Fase 4 — Google OAuth

- Backend: validar `iss=https://accounts.google.com`, `aud=GOOGLE_CLIENT_ID`, `exp`, `email_verified`; mapear `google_sub`.
- BFF: Authorization Code + PKCE, `state` em cookie httpOnly, callback server-side.
- Nunca enviar ID token/access token Google ao navegador como sessão.
- Rate limit em `/api/auth/google` e callback.
- **Aceite:** token com `aud` errado, expirado ou e-mail não verificado → 401; login com e-mail existente entra no tenant correto.

### Fase 5 — Frontend

- Páginas `/cadastro`, `/convite/[token]`, `/usuários`, botão Google em `/login`.
- `proxy.ts:15-22`: allowlist das rotas públicas.
- Sidebar com “Usuários” apenas para ADMIN; Header/Perfil sem dados financeiros alheios.
- UI seguindo `design-taste-frontend`; e2e no Playwright.
- **Aceite:** fluxos de cadastro, convite, Google e gestão de contas passam no e2e; role UI coerente com backend.

### Fase 6 — Segurança, deploy e LGPD

- Rate limit, security headers, CORS, rotação de segredos.
- Render: Postgres pago com backup; staging separado; `GOOGLE_*` com `sync:false`.
- Vercel: apenas env vars server-side.
- Política de privacidade/LGPD; DPA com Google/OpenRouter/Meta.
- `security-audit` e `seo-audit` finais.
- **Aceite:** nenhuma prévia aponta para produção; nenhum segredo em `NEXT_PUBLIC_*`.

## 7. Testes obrigatórios

- Onboarding transacional: rollback em qualquer falha.
- E-mail duplicado: cadastro por senha retorna erro genérico; Google entra na empresa existente.
- Convite: expirado, reutilizado e e-mail divergente do ID token → rejeitado.
- Google ID token: `iss`, `aud`, `exp`, `email_verified`, `google_sub`.
- Cross-tenant e cross-user em todos os recursos financeiros.
- ADMIN sem acesso a dados financeiros de outros usuários.
- Role tampering no JWT e `token_version` após demissão/desativação.
- IA: mesmo `sessionId` entre usuários/empresas não cruza histórico.
- Testcontainers para integração; Playwright para cadastro/convite/Google/gestão.

## 8. Comandos de validação futuros

```powershell
.\mvnw.cmd "-Dspotless.check.skip=true" "-Dtest=!*IntegrationTest,!*MigrationTest" test
.\mvnw.cmd test
npm --prefix C:\Users\11\controle-gastos\web run lint
npx tsc --noEmit
npm --prefix C:\Users\11\controle-gastos\web run test:e2e
```

## 9. Riscos e pontos em aberto

- **Histórico existente:** permanece associado ao ADMIN seed; confirmar se deve ser transferido.
- **Link automático Google:** proposto vincular `google_sub` quando e-mail verificado coincide com usuário existente.
- **Domínio Google restrito:** não habilitado por padrão; pode ser adicionado depois.
- **`token_version`:** implementado para demissão/desativação imediata; custo de consulta por request.
- **Multi-replica IA:**Store em memória precisa virar tabela antes de escalar API.
- **Preview → produção:** bloquear onboarding em preview até existir staging.

## 10. Próximo passo

Aprovar este documento; na saída do plan mode, escrever em `MULT_TENANT.md` e converter as fases em planos GSD executáveis.
