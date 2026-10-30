# Phase 1: Fundação e Auth em Produção - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-10-30
**Phase:** 1-Fundação e Auth em Produção
**Areas discussed:** Sessão & Token, Seed & Acesso Inicial, Tela de Login, Deploy & Ambientes

---

## Sessão & Token

| Option | Description | Selected |
|--------|-------------|----------|
| JWT 8h + cookie httpOnly | Expira em 8h, token em cookie protegido contra XSS, sem refresh token | ✓ |
| localStorage | Token acessível ao JS — vulnerável a XSS | |
| Expiração mais longa / refresh token | Mais conforto, mais superfície de segurança | |

**User's choice:** 8h ("pode ser 1 esse tempo mesmo") — demais detalhes conforme recomendação do plano.
**Notes:** Sessão mantida via cookie httpOnly; expirou → login novamente.

---

## Seed & Acesso Inicial

| Option | Description | Selected |
|--------|-------------|----------|
| Seed via migration + env var | Usuário criado na V1; senha vem de `SEED_ADMIN_PASSWORD`, fora do git | ✓ |
| Senha fixa no repositório | Simples, mas viola a regra "nada de senha em commits" | |

**User's choice:** Usuário `admin@controle.com` com a senha que forneceu fora do registro (tratamento com env var proposto e aceito sem objeção).
**Notes:** Atenção registrada no CONTEXT.md (D-04): rotacionar a senha após o primeiro login em produção — usuário avisado.

---

## Tela de Login

| Option | Description | Selected |
|--------|-------------|----------|
| Minimalista | Só e-mail + senha, pt-BR, erros genéricos claros | ✓ |
| Com extras | lembrar-me, esqueci a senha, social login | |

**User's choice:** "sim isso msm login e senha simples e minimalista"
**Notes:** Sem extras — "esqueci a senha" é capacidade de outra fase (deferred).

---

## Deploy & Ambientes

| Option | Description | Selected |
|--------|-------------|----------|
| Local + Staging + Produção | Staging prepara escala futura | ✓ |
| Local + Produção | Menos custo/complexidade | |

**User's choice:** "stanting tbm pois eu estou pensando em escalar depois"
**Notes:** Modelo do staging (2º Postgres dedicado vs banco único) fica para o plan-phase decidir (D-08).

---

## the agent's Discretion

- Maven, estrutura de pacotes, formato de erro (ProblemDetail), CORS, BCrypt cost, assinatura do JWT — decidir no plan-phase.

## Deferred Ideas

- Redefinição de senha · verificação de e-mail · cadastro de empresas (GROW-01) · domínio próprio
