# Quick summary — documentação de segurança arquitetural (25/11/2026)

## Entregue
- `SECURITY-ARCHITECTURE.md` na raiz, em pt-BR, com 14 seções cobrindo modelo de confiança, autenticação local/Google, JWT, convites, autorização, isolamento por `empresa_id`/`usuario_id`, BFF/cookies/OAuth, webhook HMAC, IA/RAG, upload de PDF, segredos, headers/CORS/rate limit, LGPD, CI/deploy, ameaças e verificações.
- Decisões S1–S6 com racional e evidências; diagramas Mermaid para OAuth, BFF, isolamento de dados, webhook e fluxo de deploy.
- Matriz de achados `SECURITY.md` (`F-001`–`F-003`, `N-001`–`N-005`) atualizada para o estado atual do código, sem alterar o baseline.

## Validação
- 66 referências com `caminho/arquivo:linha` extraídas e verificadas no working tree; todas existem.
- Nenhum valor de segredo foi impresso; apenas nomes de env vars e padrões.
- `git diff -- SECURITY.md` vazio; nenhum arquivo de código, config, plano existente ou `AGENTS.md` alterado.
- `.\mvnw.cmd -q spotless:check` OK; `npm run lint` sem erros (6 warnings em arquivos não alterados nesta task); `npx tsc --noEmit` OK.
- `.\mvnw.cmd "-Dspotless.check.skip=true" test`: 268 testes, 1 falha em `ContaAPagarServiceTest.pagarTruncaDescricaoDoLancamentoParaLimiteDoBanco`, parte do trabalho não commitado de contas-a-pagar e fora do escopo desta task.

## Observações
- O documento registra o working tree corrente com contas-a-pagar, V13/V14 e mudanças de UI como trabalho não commitado; itens dependentes estão marcados como **em andamento**.
- TLS/HSTS no edge, WAF, backup/criptografia em repouso, retenção de logs e DPAs ficaram como pendências do owner, por não serem verificáveis apenas por código.
- Não houve commit, push ou deploy.
