# Requirements: Controle Financeiro

**Defined:** 2026-10-30
**Core Value:** Saldo, entradas e saídas corretos ao centavo — números confiáveis por mês, conferidos contra SQL direto.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Autenticação e Tenant (AUTH)

- [x] **AUTH-01**: Usuário faz login com e-mail/senha e recebe JWT usado em todas as chamadas autenticadas
- [x] **AUTH-02**: Senhas são armazenadas apenas como hash BCrypt (nunca em texto puro)
- [x] **AUTH-03**: `empresa_id` é extraído exclusivamente do token JWT — nenhum body define tenant
- [x] **AUTH-04**: Seed de empresa e usuário ADMIN criados por migration Flyway (V1)
- [x] **AUTH-05**: Usuários possuem role `ADMIN` ou `USER` desde a fase 1 e o JWT carrega a role
- [x] **AUTH-06**: Comunicação em produção é somente HTTPS (Render/Vercel)

### Categorias (CAT)

- [x] **CAT-01**: Categoria possui tipo `ENTRADA` ou `SAÍDA`
- [x] **CAT-02**: Seed (Flyway V2) cria 12 categorias de saída e 4 de entrada conforme lista do cliente
- [x] **CAT-03**: Categorias marcadas [P] (Alimentação, Saúde/farmácia, Lazer, Rendimentos e reembolsos) têm `pessoal = true`
- [ ] **CAT-04**: Categoria do tipo ENTRADA rejeita lançamento de SAÍDA e vice-versa
- [ ] **CAT-05**: Usuário ADMIN pode criar, editar e desativar categorias; USER apenas lista
- [x] **CAT-06**: Categorias pertencem à empresa do token e nome é único por (empresa, tipo)

### Lançamentos (LAN)

- [ ] **LAN-01**: Usuário pode criar lançamento com data, tipo, valor, categoria e descrição
- [ ] **LAN-02**: Usuário pode editar lançamento existente (mesmas validações da criação)
- [ ] **LAN-03**: Usuário pode excluir lançamento
- [ ] **LAN-04**: Valor aceita no máximo 2 casas decimais; entrada com mais de 2 casas é **rejeitada** (não arredondada)
- [ ] **LAN-05**: Todo cálculo monetário usa `BigDecimal` com `NUMERIC(14,2)`, `scale = 2` e `RoundingMode.HALF_EVEN`; nenhum `double`/`float` no código
- [ ] **LAN-06**: Usuário pode filtrar lançamentos por mês, tipo e categoria (sempre dentro da empresa do token)
- [ ] **LAN-07**: Valor aceito é sempre positivo — o sinal é dado pelo tipo (ENTRADA/SAÍDA)

### Lote (LOT)

- [ ] **LOT-01**: Endpoint aceita lote de até 500 lançamentos em **transação tudo-ou-nada**
- [ ] **LOT-02**: Em erro, a resposta informa o **índice da linha** inválida e **nenhuma linha** do lote é gravada

### Dashboard (DSH)

- [ ] **DSH-01**: Dashboard mostra total de entradas, total de saídas e saldo do mês solicitado
- [ ] **DSH-02**: Dashboard mostra gasto por categoria do mês
- [ ] **DSH-03**: Dashboard compara o mês com o mês anterior (variação), tratando mês zerado
- [ ] **DSH-04**: Seletor de mês (padrão: mês corrente) atualiza todos os números
- [ ] **DSH-05**: Números do dashboard são idênticos a um SQL de conferência executado direto no banco com seed conhecida

### Infraestrutura e Qualidade (INF)

- [ ] **INF-01**: Flyway gerencia o schema (V1) e o seed de categorias (V2); migrations são imutáveis *(parcial: V1/V1_1 ✓ na fase 1; V2 categorias na fase 2)*
- [ ] **INF-02**: Testes de integração rodam com Testcontainers (PostgreSQL real)
- [x] **INF-03**: API publicada na Render com health check respondendo OK em produção
- [x] **INF-04**: Web publicada na Vercel consumindo a API publicada (login funciona em produção)

### IA (AI) — Fase 4

- [ ] **AI-01**: Consultas fixas via function calling (OpenRouter) retornam os **mesmos números** do dashboard
- [ ] **AI-02**: Categorização sugerida para novo lançamento exige **confirmação explícita** do usuário antes de gravar
- [ ] **AI-03**: Chave da OpenRouter vive somente no servidor (nunca no client)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Crescimento

- **GROW-01**: Cadastro de múltiplas empresas (multiempresa real)
- **GROW-02**: Convite de usuários para a empresa pelo ADMIN
- **GROW-03**: Importação de extrato (OFX/CSV)
- **GROW-04**: Lançamentos recorrentes (mensais automáticos)
- **GROW-05**: App mobile nativo ou PWA offline

### Documentos Legais

- **LGPD-01**: Política de privacidade e termos de uso publicados (texto/contrato — fora do fluxo GSD)

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| IA/ML nas fases 1–3 | Decisão explícita do cliente — IA vem apenas na fase 4 |
| Multiempresa real (v1) | Single-tenant no início; fase 1 só prepara schema com `empresa_id` |
| Importação OFX/CSV | Não mencionada no brief; adiada para v2 |
| Lançamentos recorrentes | Não mencionados no brief; adiados para v2 |
| App mobile nativo | Web-first (Vercel) |
| Documentos legais LGPD | Texto/contrato fora do GSD; app entrega pré-requisitos técnicos (BCrypt, HTTPS, isolamento) |
| CI/CD custom | Premissa: deploy por push nativo da Render/Vercel |
| Categorização automática sem confirmação | IA nunca grava sozinha — confirmação explícita obrigatória |

## Definition of Done

- Implementação concluída cobrindo a regra correspondente do plano (§2 — valor monetário, permissão, tenant)
- Testes automatizados da regra passam (`./mvnw verify` + lint do `web/`)
- Migrations imutáveis; banco sobe do zero
- Revisão do cliente quando o ponto é valor monetário ou permissão
- Commit convencional e `master` verde em produção
- Critério de aceite da fase verificado no `/gsd-verify-work` (incluindo o teste de lote inválido na fase 2)

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 | Verified |
| AUTH-02 | Phase 1 | Verified |
| AUTH-03 | Phase 1 | Verified |
| AUTH-04 | Phase 1 | Verified |
| AUTH-05 | Phase 1 | Verified |
| AUTH-06 | Phase 1 | Verified |
| CAT-01 | Phase 2 | Complete |
| CAT-02 | Phase 2 | Complete |
| CAT-03 | Phase 2 | Complete |
| CAT-04 | Phase 2 | Pending |
| CAT-05 | Phase 2 | Pending |
| CAT-06 | Phase 2 | Complete |
| LAN-01 | Phase 2 | Pending |
| LAN-02 | Phase 2 | Pending |
| LAN-03 | Phase 2 | Pending |
| LAN-04 | Phase 2 | Pending |
| LAN-05 | Phase 2 | Pending |
| LAN-06 | Phase 2 | Pending |
| LAN-07 | Phase 2 | Pending |
| LOT-01 | Phase 2 | Pending |
| LOT-02 | Phase 2 | Pending |
| DSH-01 | Phase 3 | Pending |
| DSH-02 | Phase 3 | Pending |
| DSH-03 | Phase 3 | Pending |
| DSH-04 | Phase 3 | Pending |
| DSH-05 | Phase 3 | Pending |
| INF-01 | Phase 1 | Partial — V1/V1_1 ✓; V2 seed categorias → Phase 2 |
| INF-02 | Phase 2 | Pending |
| INF-03 | Phase 1 | Verified |
| INF-04 | Phase 1 | Verified |
| AI-01 | Phase 4 | Pending |
| AI-02 | Phase 4 | Pending |
| AI-03 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 33 total
- Mapped to phases: 33
- Unmapped: 0 ✓

---
*Requirements defined: 2026-10-30*
*Last updated: 2026-10-30 after initial definition*
