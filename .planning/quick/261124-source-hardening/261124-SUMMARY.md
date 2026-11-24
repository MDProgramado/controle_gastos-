# 261124 source hardening — resumo

Status: concluído no source. Nenhum commit/push realizado.

## Aplicado

- `conviteToken` saiu da query de `/api/auth/google/start`; agora entra por cookie httpOnly via `/convite/[token]`, é copiado para o estado OAuth e removido. Falha limpa ambos os cookies.
- Headers estáticos no Next (`nosniff`, `DENY`, `Referrer-Policy`, `Permissions-Policy`, HSTS) e CORS explicitamente fechado + headers no `SecurityConfig`, sem alterar regras JWT/ADMIN.
- `LogRedacao` centraliza redação de CPF/CNPJ/cartão/telefone/e-mail/JWT/Bearer/`conviteToken`, com truncamento; aplicado em IA, importação e webhook.
- Rate limit usa singleton em `globalThis`, renova janela e expõe `reiniciaEm` para `Retry-After`.
- `urlApiServidor()`falha fechado em preview: exige `API_URL_PREVIEW` e `API_URL_PRODUCAO`, compara origens e rejeita valores inválidos ou iguais.
- Testes adicionados/estendidos em `web/e2e/google-oauth.spec.ts`, `web/e2e/auth-bff.spec.ts`, `api/.../LogRedacaoTest.java` e `api/.../SecurityHeadersTest.java`.

## Verificação

- API: `.\mvnw.cmd "-Dspotless.check.skip=true" test` → `Tests run: 226, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- API: `.\mvnw.cmd -q spotless:check` → `API_SPOTLESS_OK`.
- Web: `npm run lint` → 0 erros, 6 warnings preexistentes.
- Web: `npx tsc --noEmit` → OK.
- Web: `npx playwright test e2e/frontend-fixes.spec.ts e2e/google-oauth.spec.ts e2e/auth-bff.spec.ts --reporter=line` → `49 passed`.
- Web: `npm run build` → sucesso.
- Revisão focada pós-fix (`security-audit`, guidance mode): 6/6 PASS após duas correções de revisão (nome de arquivo nos logs e guard de preview).

## Pendências do owner

- Definir `API_URL_PREVIEW` e `API_URL_PRODUCAO` na Vercel; preview não pode apontar para a origem de produção. `API_URL` continua obrigatório em local/produção.
- Rotacionar/confirmar `GOOGLE_CLIENT_SECRET` vazado anteriormente.
- Provider/TLS/backup/retention/WAF e limites reais continuam `needs_validation` fora do source.

## Observações

- A auditoria FULL anterior remains em `C:\Users\11\AppData\Local\Temp\opencode\security-audit-skill\controle-gastos\run-1\`.
- O working tree também contém mudanças RAG não relacionadas (`rag/`, `AiService`, `AiResponse`, `application.yml`, V11, testes RAG etc.); foram preservadas. `spotless:apply` formatou arquivos Java RAG para destravar o gate.
- Não há confirmação de vulnerabilidade explorável nesta rodada; as mudanças são hardening defensivo.
