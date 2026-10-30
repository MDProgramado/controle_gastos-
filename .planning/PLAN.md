# PLANO MESTRE — Controle Financeiro

> Documento único com visão geral, regras de negócio, arquitetura, práticas de engenharia e o detalhamento das 4 fases (5 semanas).
> Artefatos GSD correspondentes: `.planning/PROJECT.md` (contexto), `.planning/REQUIREMENTS.md` (requisitos com REQ-IDs), `.planning/ROADMAP.md` (fases), `.planning/STATE.md` (memória).
> **Revisão do cliente:** este plano + o `PLAN.md` gerado por fase. Procurar lacunas de regra de negócio, especialmente em **valor monetário** e **permissão**.

---

## 1. Visão geral

| Item | Definição |
|------|-----------|
| Produto | Controle financeiro de entradas e saídas para uma empresa cliente |
| Multi-tenant | Single-tenant no início; `empresa_id` em **todas** as tabelas para multiusuário futuro |
| Usuários (v1) | Seed de 1 empresa + usuários com roles `ADMIN`/`USER` |
| Stack | Spring Boot 3 / Java 21 · Spring Security + JWT · Spring Data JPA · PostgreSQL · Flyway · Testcontainers · Next.js (App Router, TypeScript) |
| Deploy | API + Postgres pago (com backup) na **Render**; web na **Vercel** |
| Timeline | 5 semanas → Fases 1+2 (sem. 1–2) · Fase 3 (sem. 3–4) · Fase 4 (sem. 5) |
| Fora do escopo v1 | IA até a fase 4, multiempresa real, importação OFX/CSV, recorrentes, mobile nativo |

**Core Value:** saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, conferidos contra SQL direto.

---

## 2. Regras de negócio não negociáveis

Estas regras devem aparecer **como tarefas** no `PLAN.md` de cada fase e serem verificadas no `/gsd-verify-work`.

| # | Regra | Onde vale | Como garantir |
|---|-------|-----------|----------------|
| R1 | Valores em `BigDecimal`, coluna `NUMERIC(14,2)`, `scale = 2`, `RoundingMode.HALF_EVEN` | Service de lançamento, lote, dashboard | DTOs usam `BigDecimal`; validação `@Digits(integer = 12, fraction = 2)` + checagem explícita no service; aritmética sempre via `setScale(2, HALF_EVEN)` no service; **zero** `double`/`float` (grep no code review) |
| R2 | Entrada com mais de 2 casas decimais é **rejeitada** (não arredondada) | Endpoint de criação/edição/lote | Validação na entrada → HTTP 400 com mensagem clara; teste unitário cobre 3 casas decimais |
| R3 | `empresa_id` vem **sempre do token**, nunca do body | Todas as rotas autenticadas | `SecurityContext`/`JwtService` expõe o claim; filtro JPA por `empresa_id` do contexto; DTOs de request **não** têm campo `empresaId`; teste de integração prova que o body é ignorado |
| R4 | Lote = transação **tudo-ou-nada**; erro informa o **índice da linha** | `POST /api/lancamentos/lote` | `@Transactional` no service (rollback em qualquer falha); resposta de erro `{ linha: <index>, erro: "<msg>" }`; **teste obrigatório:** lote com 1 linha inválida → nada gravado (contador do banco inalterado) |
| R5 | Categoria ENTRADA não aceita lançamento SAÍDA e vice-versa | Criação/edição/lote de lançamento | Checagem no service (`categoria.getTipo() != lancamento.getTipo()` → 422/400); teste unitário + de integração |
| R6 | Senha com BCrypt, trânsito HTTPS, LGPD prevista antes do uso real | Auth / deploy | `BCryptPasswordEncoder`; Render/Vercel só servem HTTPS; app entrega os pré-requisitos técnicos — documentos legais ficam fora do fluxo GSD |
| R7 | Nenhum `double`/`float` no código | Backend inteiro | Code review + grep `float|double` nos pontos de valor; `NUMERIC(14,2)` impede imprecisão no banco |

---

## 3. Arquitetura

### 3.1 Diagrama de componentes

```
                       ┌─────────────────────────────────────────────┐
                       │  Web — Vercel (Next.js App Router, TS)      │
                       │  /login  /(dashboard)  /lancamentos  /cats  │
                       └────────────────────┬────────────────────────┘
                                            │ HTTPS + JWT (Bearer)
                       ┌────────────────────▼────────────────────────┐
                       │  API — Render (Spring Boot 3, Java 21)      │
                       │  ┌──────────────────────────────────────┐   │
                       │  │ Controller (REST, validação)         │   │
                       │  ├──────────────────────────────────────┤   │
                       │  │ Service (regras: dinheiro, tipo,     │   │
                       │  │  tenant, lote atômico)               │   │
                       │  ├──────────────────────────────────────┤   │
                       │  │ Repository (JPA, filtro empresa_id)  │   │
                       │  └──────────────────────────────────────┘   │
                       │  Security: JWT filter · BCrypt · Roles     │
                       └────────────────────┬────────────────────────┘
                                            │ JDBC
                       ┌────────────────────▼────────────────────────┐
                       │  PostgreSQL (Render, pago, com backup)      │
                       │  Flyway: V1 schema · V2 seed categorias     │
                       └─────────────────────────────────────────────┘

  Fase 4 (única): API ──HTTPS──▶ OpenRouter (function calling, servidor p/ servidor)
```

### 3.2 Monorepo

```
controle-gastos/
├── api/                  # Spring Boot (Maven)
│   ├── src/main/java/...
│   ├── src/main/resources/db/migration/   # Flyway: V1__schema.sql, V2__seed_categorias.sql
│   └── src/test/java/...                  # Testcontainers
├── web/                  # Next.js App Router
├── .planning/            # Documentos GSD
└── AGENTS.md             # Instruções do projeto
```

Decisão: **Maven** (padrão Spring, sem curva de aprendizado). Ajustável.

### 3.3 Backend — estrutura de pacotes e camadas

```
com.controlefinanceiro
├── config/        SecurityConfig, JwtAuthFilter, OpenApiConfig, ProblemDetail advice
├── auth/          login, emissão/validação de JWT, claims (empresa_id, role)
├── empresa/       entidade Empresa
├── usuario/       entidade Usuario (role ADMIN/USER)
├── categoria/     entidade Categoria (tipo ENTRADA|SAÍDA, flag pessoal) + seed
├── lancamento/    entidade Lancamento, service CRUD + lote, validações R1/R2/R4/R5
├── dashboard/     consultas agregadas (native queries) mês, categoria, comparação
├── ia/            (fase 4) OpenRouter client, tools fixas, sugestão de categoria
└── common/        erros (ProblemDetail), utils de dinheiro, exceptions, audit fields
```

- **Controller** → valida formato, monta DTO, delega. Nunca contém regra de negócio.
- **Service** → regras dos itens 2 (dinheiro, tipo, tenant, lote `@Transactional`).
- **Repository** → Spring Data JPA; **toda** query filtra por `empresa_id` vindo do contexto do token.
- **Padrão de erro:** RFC 7807 `ProblemDetail` (`type`, `title`, `status`, `detail`) em `@RestControllerAdvice`.

### 3.4 Modelo de dados (Flyway V1 — rascunho)

```sql
CREATE TABLE empresa (
  id          BIGSERIAL PRIMARY KEY,
  nome        VARCHAR(120) NOT NULL,
  criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE usuario (
  id             BIGSERIAL PRIMARY KEY,
  empresa_id     BIGINT NOT NULL REFERENCES empresa(id),
  email          VARCHAR(160) NOT NULL UNIQUE,
  senha_hash     VARCHAR(100) NOT NULL,          -- BCrypt
  nome           VARCHAR(120) NOT NULL,
  role           VARCHAR(16)  NOT NULL DEFAULT 'USER',  -- ADMIN | USER
  ativo          BOOLEAN NOT NULL DEFAULT true,
  criado_em      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categoria (
  id          BIGSERIAL PRIMARY KEY,
  empresa_id  BIGINT NOT NULL REFERENCES empresa(id),
  nome        VARCHAR(80) NOT NULL,
  tipo        VARCHAR(10) NOT NULL,              -- ENTRADA | SAIDA
  pessoal     BOOLEAN NOT NULL DEFAULT false,    -- marca [P]
  ativo       BOOLEAN NOT NULL DEFAULT true,
  UNIQUE (empresa_id, nome, tipo)
);

CREATE TABLE lancamento (
  id           BIGSERIAL PRIMARY KEY,
  empresa_id   BIGINT NOT NULL REFERENCES empresa(id),   -- sempre do token
  usuario_id   BIGINT NOT NULL REFERENCES usuario(id),
  categoria_id BIGINT NOT NULL REFERENCES categoria(id),
  tipo         VARCHAR(10) NOT NULL,             -- ENTRADA | SAIDA
  valor        NUMERIC(14,2) NOT NULL CHECK (valor > 0), -- R1
  data         DATE NOT NULL,
  descricao    VARCHAR(200),
  criado_em    TIMESTAMPTZ NOT NULL DEFAULT now(),
  atualizado_em TIMESTAMPTZ
);

CREATE INDEX idx_lanc_empresa_data ON lancamento (empresa_id, data);
CREATE INDEX idx_lanc_empresa_cat  ON lancamento (empresa_id, categoria_id);
CREATE INDEX idx_cat_empresa_tipo  ON categoria (empresa_id, tipo);
```

- **20 dígitos inteiros + 2 decimais** = 14 no total (`NUMERIC(14,2)` → até 999.999.999.999,99).
- `tipo` duplicado em `lancamento` evita join só para validar filtro — e deve **bater** com o tipo da categoria (R5).
- `CHECK (valor > 0)` — valor negativo nunca entra; o sinal é o `tipo`.
- Sem tabela de lote: o lote é um array processado numa única transação (R4).

### 3.5 Superfície da API (v1)

| Método | Rota | Descrição | Permissão |
|--------|------|-----------|-----------|
| POST | `/api/auth/login` | e-mail/senha → JWT (claims: `sub`, `empresa_id`, `role`) | público |
| GET | `/api/health` | health check (actuator) | público |
| GET | `/api/categorias` | lista por empresa (do token) | autenticado |
| POST | `/api/categorias` | cria categoria | ADMIN |
| PUT | `/api/categorias/{id}` | edita/ativa/desativa | ADMIN |
| GET | `/api/lancamentos?mes=YYYY-MM&tipo=&categoriaId=` | lista/filtra | autenticado |
| POST | `/api/lancamentos` | cria (R1, R2, R5) | autenticado |
| PUT | `/api/lancamentos/{id}` | edita (R1, R2, R5) | dono/ADMIN |
| DELETE | `/api/lancamentos/{id}` | exclui | dono/ADMIN |
| POST | `/api/lancamentos/lote` | lote até 500, tudo-ou-nada (R4) | autenticado |
| GET | `/api/dashboard?mes=YYYY-MM` | entradas, saldo, por-categoria, vs mês anterior | autenticado |
| POST | `/api/ia/categorizar` | sugere categoria (fase 4) | autenticado |
| POST | `/api/ia/consulta` | query fixa via function calling (fase 4) | autenticado |

Nenhum request body aceita `empresaId` (R3).

### 3.6 Frontend (Next.js App Router)

```
web/
├── app/
│   ├── login/            # página pública
│   ├── (app)/            # rota protegida (layout com sessão)
│   │   ├── page.tsx      # dashboard (mês corrente + seletor)
│   │   ├── lancamentos/  # lista + criar/editar/excluir + botão "lote"
│   │   └── categorias/   # gestão (ADMIN)
├── lib/api.ts            # client fetch tipado + token
├── lib/auth.tsx          # contexto de sessão (JWT em cookie httpOnly via BFF ou memory)
└── components/           # UI compartilhada
```

- Token guardado em **cookie httpOnly** (via rota de login do Next) ou em memória — **nunca** `localStorage` exposto a XSS. Decisão fechada no discuss-phase 1.
- Erros da API exibidos com o padrão `ProblemDetail` (inclusive `{ linha, erro }` do lote).
- Sem servidor de dados financeiros no client: tudo via API (fonte única de verdade = banco).

### 3.7 Segurança

1. **Autenticação:** POST `/api/auth/login` → JWT assinado (HS256/RS256), expiração curta (ex.: 8h — fechar na fase 1), claims `empresa_id` e `role`.
2. **Autorização:** `SecurityConfig` com regras por rota (ADMIN para gestão de categorias); `JwtAuthFilter` popula o contexto; `@PreAuthorize`/interceptor nos services.
3. **Tenant:** helper `TenantContext.getEmpresaId()` lido **só** do token — único ponto de acesso ao tenant.
4. **Senhas:** `BCryptPasswordEncoder` (custo padrão ≥ 10); hash nunca logado.
5. **Trânsito:** HTTPS obrigatório (Render/Vercel), HSTS por padrão.
6. **Validação:** `@Valid` em todos os DTOs; regras de dinheiro no service (R1/R2).
7. **LGPD:** app entrega isolamento por empresa, criptografia em trânsito e mínimo de dados; textos legais ficam fora do GSD (decisão mapeada em Out of Scope do PROJECT.md).

### 3.8 IA (Fase 4 — OpenRouter)

```
Usuário ─▶ /api/ia/consulta ─▶ service monta messages + tools (JSON Schema)
                                    │
                                    ▼
                             OpenRouter (modelo à escolher, ex.: gpt-4o-mini class)
                                    │  function call (apenas tools FIXAS)
                                    ▼
                       service executa query real (mesmos services do dashboard)
                                    │
                                    ▼
                       resposta em texto com os números vindos do banco
```

- **Tools fixas (nada livre):** `resumo_mes(empresa, mes)`, `gastos_por_categoria(empresa, mes)`, `comparar_meses(empresa, mes, mes_ref)`.
- **Categorização sugerida:** ao criar lançamento, front pode chamar `ia/categorizar` → modelo devolve `categoria_sugerida` → **UI mostra sugestão com confirmação explícita** → só grava após confirmar. Sugestão nunca grava sozinha.
- Chave da OpenRouter **somente no servidor** (env var da Render), nunca no Next.
- Critério de aceite: as tools retornam **exatamente** os mesmos números do dashboard (mesma query subjacente — não reimplementar cálculo).

---

## 4. Práticas de engenharia de software

### 4.1 Versionamento e fluxo

- **Git:** `master` como tronco; feature branches por fase/feature (`fase-2/lote`, `fase-3/dashboard`), merge via PR/revisão do cliente nos pontos críticos.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `test:`, `chore:`, `docs:`) — mensagem explícita sobre regra tocada (ex.: `feat(lote): atomicidade com indice da linha`).
- **`.planning/` versionado** (`commit_docs: true`) — decisões e requisitos versionam junto com o código.
- **Deploy:** push em `master` → deploy automático (Render + Vercel). Sem pipeline custom na fase 1 (premissa — ver §7).

### 4.2 Estratégia de testes (pirâmide enxuta)

| Camada | Ferramenta | Cobre |
|--------|-----------|-------|
| Unitário | JUnit 5 + AssertJ | regras de dinheiro (R1/R2 — `HALF_EVEN`, rejeição de 3 casas), validação de tipo de categoria (R5), cálculo de saldo |
| Integração API | `@SpringBootTest` + **Testcontainers** (Postgres real) | login + JWT, `empresa_id` do token (R3), CRUD, seed Flyway, permissões ADMIN/USER |
| Integração lote | Testcontainers | **R4:** lote com 1 linha inválida → 400 `{linha}` e **contagem do banco inalterada** (transação) |
| Conferência SQL | Testcontainers | dashboard devolve o mesmo que SQL direto escrito à mão (meses aleatórios com seed conhecida) |
| Front | lint + revisão manual guiada pelo critério de aceite | fluxos de login, criação e dashboard |

- **Testes obrigatórios por fase** listados no `PLAN.md` da fase; o `/gsd-verify-work` roda os do critério de aceite.
- **Ponto de controle do cliente no verify:** rodar o teste do lote com linha inválida e confirmar nada gravado (mapeado na fase 2 e re-executado nas fases seguintes).

### 4.3 Qualidade de código

- **Formatação/lint:** Spotless (Google Java Style) no `api/`; ESLint + Prettier no `web/`.
- **Code review humano** (cliente) nos pontos de valor e permissão: `lancamento service`, `lote`, `TenantContext`/filtros de `empresa_id`, queries do `dashboard`.
- **Gates de regra:** checklist do §2 obrigatório no `PLAN.md` de cada fase (fase `plan_check: true` — verificado antes de executar).
- **Sem duplicação de cálculo:** totais sempre calculados em um único service/consulta por contexto (dashboard e IA compartilham o mesmo código).

### 4.4 Padrões de código

- **Dinheiro:** tipo de domínio `BigDecimal` sempre; `setScale(2, HALF_EVEN)` único e centralizado (ex.: `Money.scale(BigDecimal)`); nenhum construtor de `BigDecimal(double)`.
- **Erros:** `ProblemDetail` + exceptions de domínio (`RegraNegocioException`, `NaoAutorizadoException`); mensagem de negócio sem vazar stack.
- **Migrations Flyway:** imutáveis — nunca editar migration já aplicada; sempre `V<n>__`. Seed de categorias = `V2`.
- **DTOs:** request/response separados da entidade; validação em `jakarta.validation`.
- **Datas:** `LocalDate` para data do lançamento; `mes` como `YYYY-MM` na API; timezone fixo (America/Sao_Paulo) no app.
- **Auditoria:** `criado_em`/`usuario_id` preenchidos no service (nunca no body).

### 4.5 CI/CD e observabilidade

- **Local (pré-push):** `./mvnw verify` + lint no `web/` — o cliente roda antes de push (premissa sem CI custom na fase 1).
- **Deploy:** push `master` → Render (API) e Vercel (web); migrations rodam no boot da Render.
- **Health:** `/api/health` verificado pós-deploy (marco da fase 1).
- **Logs:** nível INFO sem dados financeiros individualizados em produção; erros de validação sem valor completo da senha... (senhas nem entram em log).
- **Backup:** Postgres pago da Render já inclui backup (contratado pelo cliente).

### 4.6 Definition of Done (global)

Uma tarefa/funcionalidade está **pronta** quando:

1. Regras do §2 aplicáveis estão implementadas (sem `double`, com `empresa_id` do token, etc.)
2. Testes unitários + integração cobrindo a regra passam (`./mvnw verify`)
3. Migração Flyway, quando aplicável, é imutável e sobe do zero
4. Revisão do cliente nos pontos de valor/permissão (se for um desses pontos)
5. Commit com mensagem convencional; deploy de `master` sobe verde
6. Critério de aceite da fase verificado no `/gsd-verify-work`

---

## 5. Fases (4 fases · 5 semanas · Vertical MVP)

> Cada fase passa por: `/gsd-discuss-phase N` → `/gsd-plan-phase N` → `/gsd-execute-phase N` → `/gsd-verify-work N`.

### Fase 1 — Fundação e Auth em Produção · semanas 1–2

**Objetivo:** monorepo, Flyway V1, auth JWT, seed de empresa/usuário, deploy API na Render e web na Vercel.
**Marco verificável:** login funciona em produção; health check OK.

Tarefas:

- [ ] Monorepo `api/` (Maven, Spring Boot 3, Java 21) + `web/` (Next.js TS) — build verde nos dois
- [ ] `SecurityConfig` + `JwtAuthFilter` + endpoint `POST /api/auth/login` (BCrypt, claims `empresa_id`/`role`) — **R6**
- [ ] Flyway `V1__schema.sql` (§3.4: empresa, usuario, categoria, lancamento + índices) — **R1** (`NUMERIC(14,2)`)
- [ ] Flyway `V1.1`/`V1` seed: 1 empresa + 1 usuário ADMIN — login sai do zero
- [ ] `TenantContext` lendo `empresa_id` **só do token** + teste de integração provando que body não manda — **R3**
- [ ] Roles `ADMIN`/`USER` no schema e no JWT; regras por rota no `SecurityConfig` (permissão) — **permissão**
- [ ] Endpoint `GET /api/health`
- [ ] Página `web/app/login` consumindo a API + sessão (cookie httpOnly — decisão da fase discuss)
- [ ] Deploy: repo conectado à Render (API + Postgres pago com backup) e à Vercel (web); HTTPS ativo — **R6**
- [ ] Testes: login ok, login inválido 401, token ausente 401, seed Flyway roda limpo (Testcontainers)

Critérios de aceite: login com e-mail/senha funciona **na web publicada**; `/api/health` OK em produção; `empresa_id` sempre do token (teste); seed criado por migration.

**UI hint:** sim (login).

---

### Fase 2 — Categorias e Lançamentos · semanas 1–2

**Objetivo:** categorias (seed V2), lançamento unitário e lote em transação.
**Marco verificável:** testes de integração passando; lote com erro não grava nada.

Tarefas:

- [ ] Flyway `V2__seed_categorias.sql`: 12 saídas + 4 entradas, `pessoal = true` em Alimentação, Saúde/farmácia, Lazer e Rendimentos/reembolsos — **flag [P]**
- [ ] CRUD de categorias (ADMIN) com tipo `ENTRADA`/`SAÍDA` — **permissão**
- [ ] CRUD de lançamentos: data, tipo, valor, categoria, descrição — **R5** (tipo da categoria × tipo do lançamento)
- [ ] Valoração: `@Digits(integer=12, fraction=2)` + rejeição explícita de >2 casas no service, `HALF_EVEN`, `BigDecimal` — **R1, R2** (zero `double`/`float`)
- [ ] `empresa_id` do token em todos os repositórios (herdado do fase 1) — **R3**
- [ ] `POST /api/lancamentos/lote`: array ≤ 500, `@Transactional` tudo-ou-nada, erro `{ linha, erro }` — **R4**
- [ ] Filtros da lista: mês, tipo, categoria
- [ ] Front: telas de lançamentos (criar/editar/excluir) + fluxo de lote com feedback de `{ linha, erro }`
- [ ] Testes: **lote com linha inválida → nada gravado** (contador inalterado), rejeição de 3 casas decimais, rejeição de categoria com tipo errado, CRUD happy path (Testcontainers)

Critérios de aceite: `./mvnw verify` verde com os testes acima; lote com erro **não grava nada** (ponto de controle do cliente); rejeição de valor com 3 casas; regra de tipo da categoria.

**UI hint:** sim (formulários financeiros).

---

### Fase 3 — Dashboard · semanas 3–4

**Objetivo:** entradas, saídas, saldo por mês, gasto por categoria e comparação mensal.
**Marco verificável:** números batem com SQL de conferência.

Tarefas:

- [ ] `GET /api/dashboard?mes=` com: total entradas, total saídas, saldo, gasto por categoria, %/variação vs mês anterior — **R1** (`BigDecimal` no agregado, `COALESCE`)
- [ ] Queries com índice `idx_lanc_empresa_data` + filtro por `empresa_id` — **R3**
- [ ] Front: página dashboard com seletor de mês, cards + barra por categoria + comparação com mês anterior (premissa Q5)
- [ ] **Teste de conferência SQL:** seed conhecida → compara JSON da API com SQL direto (mês corrente, mês zerado, mês com entradas e saídas, mês futuro)
- [ ] Consistência: mesmos `BigDecimal`/`HALF_EVEN` do lançamento (sem novo cálculo duplicado)
- [ ] Front: estados vazios (mês sem lançamentos) e carregamento

Critérios de aceite: números do dashboard **idênticos** ao SQL de conferência; comparação mensal correta em mês zerado (divisão por zero tratada); filtros por `empresa_id` (usuário de outra empresa não vê nada).

**UI hint:** sim (tela principal).

---

### Fase 4 — IA via OpenRouter · semana 5

**Objetivo:** function calling sobre consultas fixas + categorização sugerida com confirmação.
**Marco verificável:** consultas de teste retornam os mesmos números do dashboard.

Tarefas:

- [ ] Cliente OpenRouter no servidor (env var na Render; chave nunca no front) — **R6**
- [ ] Tools fixas `resumo_mes`, `gastos_por_categoria`, `comparar_meses` → executam **os mesmos services/queries do dashboard** (sem cálculo duplicado)
- [ ] `POST /api/ia/consulta`: valida parâmetros (mês `YYYY-MM`, empresa do token) e devolve resposta com números do banco — **R3**
- [ ] `POST /api/ia/categorizar`: devolve `categoria_sugerida` a partir da descrição; **front exibe sugestão e só grava após confirmação explícita** (nunca automático)
- [ ] UI: chat/consulta simples no dashboard + sugestão de categoria no form de lançamento
- [ ] Testes: tools fixas devolvem os mesmos números do dashboard (mock da OpenRouter + comparação), recusa de tool não listada, prompt sem chave não quebra a API
- [ ] Rate/erro: chave ausente/limite atingido → mensagem amigável, app segue funcionável sem IA

Critérios de aceite: consultas de teste retornam os **mesmos números** do dashboard; categorização exige confirmação; app degrada sem IA.

**UI hint:** sim (chat + sugestão inline).

---

### Timeline consolidada

| Semana | Fase | Marco |
|--------|------|-------|
| 1–2 | 1 · Fundação e Auth | Login em produção, health check OK |
| 1–2 | 2 · Categorias e Lançamentos | Testes passando; lote com erro não grava nada |
| 3–4 | 3 · Dashboard | Números batem com SQL de conferência |
| 5 | 4 · IA (OpenRouter) | Consultas = números do dashboard; categorização confirmada |

---

## 6. Pontos de controle do cliente

| Momento | O que verificar | Contra-ataque se falhar |
|---------|-----------------|--------------------------|
| `/gsd-plan-phase N` | O plano toca em **valor monetário** (R1/R2) e **permissão** (role/tenant)? As regras do §2 aparecem como tarefas? | Ajustar o plano **antes** de executar |
| `/gsd-verify-work N` | Rodar o **teste de lote com linha inválida** → confirmar que nada foi gravado | Bug silencioso — bloquear a fase |
| Revisão de código | service de lançamento, lote, filtro `empresa_id`, queries do dashboard | Corrigir antes do merge |

---

## 7. Premissas (defaults aplicados — confirmar ou ajustar)

O questioning foi encerrado pelo pedido de plano direto; estas premissas cobrem as perguntas não respondidas:

| # | Pergunta | Premissa aplicada |
|---|----------|-------------------|
| Q1 | Ciclo de vida do lançamento | **CRUD completo** (criar/editar/excluir) |
| Q2 | Limite do lote | **500 linhas** |
| Q3 | Usuários/roles | **Roles `ADMIN`/`USER` modeladas desde a fase 1** |
| Q4 | Marcador `[P]` | **Boolean `pessoal`** na tabela `categoria` |
| Q5 | Período do dashboard | **Mês corrente + seletor de mês + comparação com mês anterior** |
| Q6 | LGPD | **Documentos legais fora do GSD**; app entrega os pré-requisitos técnicos |
| Q7 | Deploy fase 1 | **Push → deploy automático** (Render/Vercel nativo), sem CI custom |
| — | Lista de categorias | **12 saídas + 4 entradas** como listadas (o brief dizia "10 saídas"; a lista tem 12) |
| — | Estrutura de fases | **Vertical MVP** — cada fase entrega capacidade ponta a ponta |
| — | Config GSD | `interactive` · `coarse` · `sequential` · commit de docs · `inherit` (modelo da sessão) · pesquisa off · plan_check e verifier **on** |

## 8. Risks

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| Bug silencioso no lote (grava parcial) | Alto — corrompe saldo | Transação + teste obrigatório no verify (§6) |
| Divergência dashboard × conferência | Alto — quebra confiança do cliente | Teste de SQL de conferência com seed conhecida |
| Precisão monetária (arredondamento/float) | Alto | R1/R2 + grep proibindo `double`/`float` no review |
| Vazamento de tenant (`empresa_id` errado) | Crítico — dados cruzados | `TenantContext` único + teste de integração (R3) |
| Escopo de IA apertado na semana 5 | Médio — atraso | Tools fixas, reusa queries do dashboard, app funciona sem IA |
| Deploy Render/Vercel no fase 1 | Médio | Health check é critério de aceite da fase 1 |
| Quantidade de revisões do cliente | Médio — gargalo | Janelas de revisão fixas: plano e verify por fase |

## 9. Dependencies

- Conta **Render** com projeto Postgres **pago (backup)** ativo
- Conta **Vercel** conectada ao repo
- Push em `master` com deploy automático habilitado nos dois
- Chave da API **OpenRouter** (só para a fase 4)
- Definição do seed: e-mail/senha do usuário ADMIN inicial
- Lista final de categorias confirmada (12 saídas + 4 entradas)

## 10. Fora de escopo (resumo)

IA antes da fase 4 · multiempresa real · importação OFX/CSV · lançamentos recorrentes · app mobile nativo · documentos legais LGPD (fora do GSD) · CI/CD custom (premissa: deploy por push).

---
*Plano mestre criado: 2026-10-30 · atualizado a cada /gsd-plan-phase e transição de fase*
