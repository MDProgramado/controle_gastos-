# SECURITY.md — Fase 5 (Importação de PDFs + Análise por IA)

**Status:** aprovado (4/4 verificações sem achados de severidade alta não mitigados)

## Escopo da fase

Endpoints `POST/GET /api/importacoes`, `GET/DELETE /api/importacoes/{id}`,
`POST /api/importacoes/{id}/confirmar` + BFFs da web
(`/app/api/importacoes`, `[id]`, `[id]/confirmar`). Staging de importação,
análise por IA (OpenRouter, backend apenas) e confirmação de lançamentos.

---

## Verificação 1 — Autenticação exigida em `/api/importacoes/**`

**Onde:** `api/.../config/SecurityConfig.java` L37-47

```java
auth.requestMatchers("/api/auth/login", "/api/health").permitAll()
...
.anyRequest().authenticated()
```

- `.anyRequest().authenticated()` cobre todos os endpoints de importação — **não há rota pública acidental**.
- Login e health são os únicos `permitAll()` (mesmo padrão das fases anteriores).
- O BFF da web devolve 401 quando não há cookie `sess` (route.ts L23-25, confirmar L22-24, [id] L25-27).
- **Evidência de teste:** `ImportacaoPdfIntegrationTest.semTokenRetorna401` → todos os 5 endpoints retornam 401 sem token.

**Mitigação da ameaça (Alta):** endpoint de importação acessível sem token → bloqueado por `anyRequest().authenticated()` + 401 sem token (coberto por IT).

---

## Verificação 2 — `empresa_id` vem SÓ do JWT (nunca de body/query)

**Onde:**
- `api/.../config/SecurityConfig.java` L62-67 — `jwt.getClaim("empresa_id")` → `UsuarioAuth` principal.
- `api/.../common/TenantContext.java` L18-24 — lê `empresa_id` SOMENTE do `SecurityContext`; lança `NaoAutorizadoException` sem principal.
- `api/.../importacao/ImportacaoService.java` L273-274, L309-310 — `detalhar`, `descartar` e `confirmar` usam `TenantContext.getEmpresaId()`; `findByIdAndEmpresaId` filtra por empresa do token.
- Controller sem parâmetro de empresa nos DTOs (ImportacaoController.java L20-21 comentado).

**Evidência de teste:**
- `ImportacaoPdfIntegrationTest.isolamentoEntreEmpresas`: empresa A não vê histórico nem acessa detalhe/descarte/confirmação da empresa B → 404.
- `AuthTenantIntegrationTest.corpoHostilComEmpresaIdNaoDefineTenant` (fase anterior) — body com `empresa_id: 999` é ignorado; tenant vem do token.

**Mitigação (Alta):** exposição de `empresa_id` no corpo/resposta → nenhum DTO expõe `empresa_id`; resposta `DocumentoResumo` tem id/nome/tipo/status/tamanho/criado_em (sem empresa_id). Isolamento entre empresas provado por IT.

---

## Verificação 3 — Nenhum log sensível (P7) + mapeamento de erros da IA

**Onde:** `api/.../config/ProblemDetailExceptionHandler.java`

- Javadoc L25-26: *"Nunca logar header `Authorization`, cookie ou body de login"*.
- Nenhuma chamada `LOG.*`/`System.out` com `Authorization`, cookie `sess` ou token em qualquer handler.
- `ServicoIaIndisponivelException` → `503 SERVICE_UNAVAILABLE` (L104-107) — D-23.
- `ImportacaoNaoEncontradaException` → `404` (L127-130) — R3, confirmação cruzada.
- `ImportacaoEstadoInvalidoException` → `409` (L133-136) — descarte de já confirmado.
- `ArquivoInvalidoException` → `400` (L110-113) — não-PDF/vazio/acima de 8 MB.
- `MaxUploadSizeExceededException` → `413` (L119-124).

**Mitigação (Alta):** logs vazando Authorization/cookie → ausentes (verificado por leitura do handler + javadoc P7).

---

## Verificação 4 — BFF não expõe chave OpenRouter ao browser

**Onde:** `web/app/api/importacoes/route.ts`, `[id]/route.ts`, `[id]/confirmar/route.ts`

- Cada rota lê `cookieStore.get("sess")?.value` e repassa `Authorization: Bearer ${token}` para `${API_URL}/api/importacoes/...`.
- **Nenhuma referência a `OPENROUTER_API_KEY`/`openrouter` no frontend** — a IA roda exclusivamente no backend (`api/.../ai/AiClientCascata`, `IaExtracaoService`), nunca no browser.
- O browser só chama o próprio BFF (mesma origem). Chave OpenRouter vive só em variável de ambiente do servidor da API (Render), nunca no bundle do cliente.

**Mitigação (Alta):** chave de IA exposta ao cliente → ausente (IA é server-side; BFF repassa apenas Bearer do cookie `sess`).

---

## Ameaças do modelo de ameaças da fase (05-07-PLAN.md)

| Ameaça | Severidade | Status |
|--------|------------|--------|
| Falha de autenticação/JWT nos novos endpoints | Alta | Mitigada — `anyRequest().authenticated()`, 401 sem token (IT coberto) |
| Exposição de `empresa_id` no corpo/resposta | Alta | Mitigada — nenhum DTO expõe; isolamento por IT |
| Endpoint de importação acessível sem token | Alta | Mitigada — SecurityConfig + 401 em todos os 5 endpoints |
| Logs vazando `Authorization` ou cookie `sess` | Alta | Mitigada — P7, nenhum log sensível no `ProblemDetailExceptionHandler` |
| Chave OpenRouter exposta ao browser | Alta | Mitigada — IA server-side; BFF repassa só Bearer do cookie `sess` |

---

## Revisão manual

Concluída sem achados de severidade alta não mitigados. 4/4 verificações confirmadas com evidências de código e testes de integração.
