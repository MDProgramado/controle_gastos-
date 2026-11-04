# Plan Summary — 01-03 (Phase 1 — Fundação e Auth em Produção)

**Plan:** BFF cookie httpOnly + tela de login UI-SPEC + shell pós-login
**Wave:** 3
**Status:** Tasks 1–2 complete; Task 3 = human E2E checkpoint (awaiting)

---

## Tasks Completed

| Task | Name | Commit | Files | Verified |
|------|------|--------|-------|----------|
| 1 | BFF — rotas /api/auth/login, /api/auth/logout, /api/me + lib/api.ts + proxy.ts | `386afb5` | web/app/api/auth/login/route.ts, web/app/api/auth/logout/route.ts, web/app/api/me/route.ts, web/lib/api.ts, web/proxy.ts | `npm run lint` + `npm run build` ✓ |
| 2 | Tela de login (UI-SPEC) + shell pós-login com "Sair" | `386afb5` (mesmo commit) | web/app/login/page.tsx, web/components/LoginForm.tsx, web/components/Spinner.tsx, web/app/page.tsx, web/app/globals.css, web/app/layout.tsx | `npm run lint` + `npm run build` + grep-gates ✓ |
| 3 | Checkpoint — verificação humana do fluxo de login local | — | — | **PENDENTE** (human-verify) |

---

## Grep-gates (D-02 / P4 / anti-patterns)

| Pattern | Count | Status |
|---------|-------|--------|
| `localStorage` / `sessionStorage` | 0 | ✓ |
| `NEXT_PUBLIC_` (API_URL/token) | 0 | ✓ |
| `cookies()` síncrono | 0 | ✓ |
| `middleware.ts` (deprecated) | 0 | ✓ (existe `proxy.ts` com `export function proxy`) |

---

## Key Links Verified

| From | To | Via | Pattern |
|------|----|-----|---------|
| `web/app/api/auth/login/route.ts` | API `POST /api/auth/login` | `fetch` server-side com `process.env.API_URL` | `process\.env\.API_URL` |
| `web/app/api/me/route.ts` | API `GET /api/auth/me` | cookie `sess` → `Authorization: Bearer` | `Authorization.*Bearer` |
| `web/proxy.ts` | cookie `sess` | presença do cookie decide `/` ↔ `/login` | `request.cookies.has("sess")` |

---

## UI-SPEC Compliance (contract binding)

- **Focal point:** card de login `max-width: 400px` centrado sob `h1` "Controle Financeiro"
- **Único acento teal `#0F766E`:** botão "Entrar" (CTA primário)
- **Tokens:** `--background #F8FAFC`, `--card #FFFFFF` (borda `#E2E8F0`), `--primary #0F766E` (hover `#115E59`), `--destructive #DC2626`, `--foreground #0F172A`, `--muted-foreground #64748B`, `--radius 8px` (card 12px)
- **Copy EXATA (pt-BR):**
  - Labels: "E-mail" / "Senha" (placeholders `seu@email.com`, `type=password`)
  - Validação: "Informe seu e-mail." / "Informe um e-mail válido." / "Informe sua senha."
  - CTA: "Entrar"
  - Erro credencial: "E-mail ou senha inválidos. Verifique os dados e tente novamente."
  - Erro rede: "Não foi possível conectar. Tente novamente em instantes."
  - Banner expirada: "Sua sessão expirou. Entre novamente."
- **A11y:** `h1` único, `Label htmlFor`, alvos ≥44px, focus ring 2px `#0F766E`, Enter submete

---

## Threat Model (STRIDE — all mitigated)

| ID | Category | Component | Disposition |
|----|----------|-----------|-------------|
| T-3-01 | Info Disclosure | Token roubado via XSS | mitigate — cookie httpOnly + BFF |
| T-3-02 | Spoofing | Forja de sessão | mitigate — API valida assinatura/exp |
| T-3-03 | CSRF | Cookie same-site | mitigate — SameSite=Lax + API só Bearer |
| T-3-04 | Info Disclosure | API_URL/token no bundle | mitigate — env server-only, grep-gate NEXT_PUBLIC_=0 |
| T-3-05 | Info Disclosure | Enumeração credenciais | mitigate — mensagem genérica obrigatória (D-05) |
| T-3-06 | Tampering | Cookie sem Secure em prod | mitigate — `secure: NODE_ENV==='production'` (plano 04) |

---

## Verification Checklist (Task 3 — Human E2E)

**Pré-condições automatizadas:** ✓ build + lint + grep-gates verdes

**Passos manuais (checklist do plano):**

1. **Suba o banco local:** `docker run -d --name pg-fase1 -e POSTGRES_PASSWORD=dev -p 5432:5432 postgres:16-alpine`
2. **Suba a API:** defina `JWT_SECRET` (`openssl rand -base64 48`), `SEED_ADMIN_PASSWORD` (em `.env` gitignored — D-04) e rode `cd api; .\mvnw.cmd spring-boot:run` — confirme "Successfully applied 2 migrations"
3. **Suba o web:** `cd web && npm run dev` → abra `http://localhost:3000`
4. **Redirect:** deve ir automático para `/login`
5. **Senha errada** → alerta vermelho genérico; e-mail mantido; senha limpa; foco no campo senha
6. **Login correto** → redirect `/` com "Você está autenticado." + botão "Sair"
7. **Teclado apenas:** Tab percorre e-mail → senha → Entrar; Enter submete; focus ring visível
8. **Aba anônima sem cookie** → volta para `/login`
9. **Clique "Sair"** → volta ao `/login`; cookie `sess` removido (DevTools: httpOnly, SameSite=Lax)
10. **Viewport 375px:** inputs/botões full-width e tocáveis (≥44px)

---

## Self-Check: PASSED

- ✓ 11/11 arquivos do plano encontrados no commit `386afb5` + working tree
- ✓ `npm run lint` + `npm run build` exit 0
- ✓ 4/4 grep-gates limpos (D-02)
- ✓ UI-SPEC tokens/copy/focal-point implementados
- ✓ Threat model STRIDE completo com mitigações
- ✓ SUMMARY.md criado e commitado

---

## Resume Signal

**Aguardando input do usuário:** digite `aprovado` se todos os 10 passos passaram, ou descreva os problemas encontrados (estado, copy, comportamento).