# Relatório de Execução — Plano de Correção WhatsApp

**Branch:** `fix/whatsapp-verificacao-plano`  
**Base:** `a7c15ce` (`main`)  
**Data:** 2026-11-19  
**Status final:** Fases 0, 1, 2, 3, 4 e 6 concluídas; Fase 5 não executada. **Pronto para revisão**; o risco de colisão da V8 está encerrado para deploy automático, pois a V8 antiga nunca esteve na cadeia de primeiro pai de `main` (detalhes abaixo).

## Resumo executivo

- Corrigido o fluxo WhatsApp de ponta a ponta no backend: mapeamento JPA, validade da pendência, cadastro unificado de telefone, cooldown/limite de tentativas e confirmação atômica de lançamento.
- Removidas as implementações legadas conflitantes (`WhatsappCadastroController`, `WhatsappCadastroService` e DTOs associados).
- Confirmação por `sim`/`não` grava o lançamento com `canal = WHATSAPP`, `messageIdOrigem` e atualização condicional da pendência.
- Os três testes exigidos na auditoria foram adicionados e passaram: número não verificado via `processarWebhook`, duplicata concorrente com threads reais e cross-tenant com telefone verificado de outra empresa.
- `LancamentoIntegrationTest` não tem mais o teste vazio `lancamentoRequestNaoExisteAindaRed`, e a limpeza de categorias usa os ids criados pelo próprio teste.
- Suíte completa executada duas vezes após a estabilização do teste assíncrono: **180 testes, 0 falhas, 0 erros, 0 skips**, 27 classes em ambas. `spotless:check` e `git diff --check` passaram.
- Checksums das migrations alteradas apenas por EOL foram validados com Flyway `11.7.2` (versão resolvida pelo BOM do Spring Boot `3.5.16`) contra um Postgres temporário: **CHECKSUM_VALIDADO_OK**.
- Working tree: dois testes modificados, Javadoc documental em `WhatsappWebhookService` e este relatório; sem mudança de comportamento em produção.
- **Não houve push nem abertura de PR.**
- Descrição do PR preparada com os três avisos obrigatórios: nova V8 de rename, Fase 5 fora e envio pela Meta ainda indisponível ao usuário final.

## Revisão pré-merge — pontos levantados

1. **V8 x histórico do Render — encerrado para deploy automático:** `c9ee1ab` adicionou `V8__empresa_nome_unique.sql` e `e5d7c10` removeu ainda na branch `fix/whatsapp-cadastro-novo`, antes do merge `f0444ce` em `main`. Cada push em `main` cria um novo HEAD na cadeia de primeiro pai; `git log --first-parent main -- 'api/src/main/resources/db/migration/V8__*'` não retorna nada, e `git ls-tree f0444ce api/src/main/resources/db/migration` contém apenas V1/V3/V5/V6/V7. Logo, nenhum estado publicado de `main` teve a V8 antiga. `render.yaml:29-32` define serviço Git-based `runtime: docker` na `branch: main` e não define `autoDeploy`/`autoDeployTrigger`. A especificação oficial da Render (https://render.com/docs/blueprint-spec) diz que omitir `autoDeployTrigger` usa `commit` para serviço novo e mantém o valor atual para serviço existente; o projeto registra D-07 (`push → deploy automático`) em `.planning/PLAN.md:238` e o smoke da fase 1 validou a publicação. Conclusão: o merge em `main` dispara a subida com V6/V7/V8 e a migration deste branch permanece `V8__renomeia_coluna_codigo.sql`. O YAML está marcado como inerte (`render.yaml:21-24`), então o valor definitivo do serviço existente é o do dashboard; se ele estiver `On Commit` (padrão), o cenário acima se aplica.
2. **Espera do teste de duplicata:** adequada. `aguardarResultado`/`aguardarResultados` fazem polling no banco a cada `Thread.sleep(20)` até `ESPERA_ASSINCRONA_SEGUNDOS = 20`, retornando assim que a condição aparece (`resultado` esperado ou `count(*) == 2`); não é espera fixa cega. `verify(..., timeout(20000))` segue o mesmo princípio. O `0xC0000142` apareceu já após o resumo verde da segunda suíte; não há `*.dump*` em `api/target/surefire-reports`, indicando ruído de encerramento do fork no Windows, não falha de asserção.
3. **Self-invocation:** os métodos `@Transactional` internos são chamados dentro do próprio bean; a atomicidade efetiva depende de `@Transactional` em `processarWebhook` e do `UPDATE ... WHERE status = 'PENDENTE'`. Essa dependência foi documentada no Javadoc de `WhatsappWebhookService` para evitar remoção acidental da transação externa.
4. **Versão do Flyway:** a aplicação resolve Flyway `11.7.2` via BOM do Spring Boot `3.5.16` (`spring-boot-dependencies-3.5.16.pom:57`); é a mesma versão usada na prova de checksum com `flyway-maven-plugin:11.7.2`.
5. **Fase 5:** fora deste PR. `WhatsappEnvioService` continua stub; o PR deve declarar explicitamente que o usuário final ainda não recebe resposta real da Meta.
6. **Working tree:** commits separados para testes, documentação do Javadoc e relatório; código de produção sem mudança de comportamento.

## Descrição sugerida do PR

```markdown
Correções de consistência do fluxo WhatsApp: mapeamento JPA, validade da pendência, cadastro unificado de telefone, cooldown/limite por hora e confirmação atômica de lançamento, com testes de regressão e estabilização do teste concorrente.

- A migration `V8__renomeia_coluna_codigo.sql` é a nova V8 deste branch: renomeia `telefone_id` para `usuario_telefone_id`. Ela não é a antiga `V8__empresa_nome_unique.sql`, removida antes do merge em `main`.
- A Fase 5 (envio real pela Meta Cloud API) não está incluída neste PR.
- Consequentemente, o envio pela Meta ainda não funciona para o usuário final: `WhatsappEnvioService` permanece como stub/TODO.
```

## Execução por fase

### Fase 0 — Preparação

- Criada a branch `fix/whatsapp-verificacao-plano` a partir de `main`.
- Ajuste de `core.autocrlf = false` e criação de `.gitattributes` com `*.java text eol=lf`.
- Aplicação de `spotless:apply` e normalização dos finais de linha.
- Validação inicial: `compile` e `test-compile` OK.

### Fase 1 — Correção de mapeamento e validade

- Criada a migration `V8__renomeia_coluna_codigo.sql`:
  - `ALTER TABLE whatsapp_codigo_verificacao RENAME COLUMN telefone_id TO usuario_telefone_id;`
- `UsuarioTelefone.numeroE164` passou a mapear explicitamente `numero_e164`, `nullable = false`, `length = 20`, `unique = true`.
- `WhatsappConfirmacaoPendente.proposta` passou a usar `@JdbcTypeCode(SqlTypes.JSON)` com `@Column(nullable = false)`.
- Validade da pendência unificada em 24h (`CONFIRMACAO_VALIDADE_HORAS = 24`).
- Teste `UsuarioTelefoneIntegrationTest#gravaCodigoEPendenciaComMapeamentoCorreto` verde; classe completa verde.

### Fase 2 — Consolidação do cadastro de telefone

- Removidos:
  - `WhatsappCadastroController.java`
  - `WhatsappCadastroService.java`
  - `WhatsappTelefoneRequest.java`
  - `WhatsappTelefoneResponse.java`
  - `WhatsappVerificarRequest.java`
- Renomeado `findAtivoByUsuarioId` para `findFirstByUsuarioIdAndAtivoTrue` e atualizadas as chamadas.
- Removida a regra `POST /api/whatsapp/**` de `SecurityConfig.java`.
- Removido o método morto `findValidByUsuarioTelefoneId` e imports não utilizados.
- Confirmedo por busca de código que não restaram referências ao fluxo legado.
- `compile` e `test-compile` OK.

### Fase 3 — Cooldown, conflito e limite por hora

- Criada `CodigoVerificacaoCooldownException`.
- `ProblemDetailExceptionHandler` passou a responder:
  - `429` para cooldown de envio.
  - `409` para telefone já vinculado a outra empresa.
- `UsuarioTelefoneService` passou a usar:
  - `COOLDOWN_ENVIO_MINUTOS = 2`
  - `JANELA_TENTATIVAS_HORAS = 1`
  - `MAX_TENTATIVAS_POR_HORA = 3`
- `WhatsappCodigoVerificacaoRepository` ganhou:
  - `findFirstByUsuarioTelefone_EmpresaIdAndUsuarioTelefone_NumeroE164OrderByCriadoEmDesc`
  - `contarTentativasRecentes(...)`
- Contagem de tentativas feita por `usuario_telefone_id` somando `tentativas` de códigos criados na última hora.
- Testes de cooldown, conflito e janela de tentativas verdes em `UsuarioTelefoneIntegrationTest`.

### Fase 4 — Confirmação atômica por `sim`/`não`

- `LancamentoService` ganhou a sobrecarga:
  - `criarComCanal(Long empresaId, Long usuarioId, LancamentoRequest request, String canal, String messageIdOrigem)`
  - A versão antiga delega usando `usuarioAtual().getId()`.
  - Usuário informado é validado contra o `empresaId` do tenant.
- `WhatsappConfirmacaoPendenteRepository` ganhou atualizações condicionais:
  - `confirmar`, `expirar`, `cancelar`
  - `countByUsuarioTelefoneIdAndStatus`
- `WhatsappWebhookService` passou a:
  - Interpretar `sim`, `não`/`nao`, com ou sem `<id>`.
  - Exigir `sim <id>` / `não <id>` quando há múltiplas pendências.
  - Incluir o id da pendência no resumo enviado ao usuário.
  - Usar `saveAndFlush` para garantir o id antes da confirmação.
  - Gravar o wamid da mensagem `sim` como `messageIdOrigem`.
  - Capturar escalares antes do `clear()` do `EntityManager`.
- `WhatsappConfirmacaoIntegrationTest` passou de 6 para 9 cenários.
- Novos cenários:
  - `mensagemSimDeNumeroNaoVerificadoNaoConfirmaPendencia`: entra por `processarWebhook`, espera `NAO_VERIFICADO`, mantém a pendência `PENDENTE` e não grava lançamento.
  - `duplicataConcorrenteComDoisWamidDiferentesGravaUmUnicoLancamento`: 5 repetições, `CyclicBarrier(2)`, duas threads reais, dois wamid, espera até as duas mensagens serem processadas e afirma 1 lançamento, 1 texto “já foi processado” e 1 texto “gravado”.
  - `confirmacaoComTelefoneVerificadoDeOutraEmpresaNaoGravaNaEmpresaDaPendencia`: segundo telefone verificado em outra empresa envia `sim` pelo webhook; pendência original permanece `PENDENTE` e nenhuma empresa recebe lançamento.

### Fase 5 — Envio real pela Meta

- **Não executada neste PR.**
- `WhatsappEnvioService` permanece como stub/TODO.
- Decisão registrada: envio real ficará em PR separado, para não misturar correção de consistência com integração externa e credenciais.

### Fase 6 — Validação final

- `spotless:apply` aplicado e `spotless:check` executado com sucesso.
- A primeira re-verificação encontrou uma instabilidade no teste `duplicataConcorrenteComDoisWamidDiferentesGravaUmUnicoLancamento`: falha `mensagens processadas wamid-race-1-a e wamid-race-1-b expected: 2 but was: 1`. A espera fixa de 5s expirava antes do commit da segunda transação `@Async` sob carga da suíte completa.
- Correção aplicada apenas no teste: constantes `ESPERA_ASSINCRONA_SEGUNDOS = 20` e `ESPERA_ASSINCRONA_MS = 20_000L`, usadas em `aguardarResultado`, `aguardarResultados` e nos `verify(..., timeout(...))`. A mensagem de falha agora inclui resultados e erros capturados para diagnóstico.
- Após o ajuste, a suíte completa passou duas vezes: `SUITE_COMPLETA_OK` e `SUITE_REPETIDA_OK`, ambas com **180 testes, 0 falhas, 0 erros, 0 skips**.
- A segunda execução emitiu, já após o resumo verde, aviso do Surefire ao encerrar a JVM (`exit code 0xC0000142`, `errno 11`, `Resource temporarily unavailable`). Não há `*.dump*` em `api/target/surefire-reports`; a leitura é ruído de shutdown/fork do Windows, não falha de teste. `spotless:check` e `git diff --check` foram repetidos separadamente com sucesso.
- Testcontainers subiu `postgres:16-alpine`; as 8 migrations, incluindo `V8`, foram aplicadas e validadas.
- Mutation checks executados com mutações temporárias e revertidas:
  - Remover a checagem `NAO_VERIFICADO` → teste falhou com `GRAVADA` no lugar de `NAO_VERIFICADO`.
  - Remover `p.status = 'PENDENTE'` do UPDATE atômico → teste falhou com `2` lançamentos no lugar de `1`.
  - Remover os filtros de tenant na busca e no UPDATE → teste falhou com `GRAVADA` no lugar de `DUPLICADA`.
- `LancamentoIntegrationTest#listarLancamentosFiltradoPorTipoRetornaApenasDaEmpresa` continua ajustado ao contrato paginado: `body.get("lancamentos")`.
- Contrato paginado confirmado no front: `web/lib/api.ts:110-131` consome `{ lancamentos, total }` e `web/app/lancamentos/page.tsx:55-66` usa `lancamentosResp?.lancamentos`.

## Validação de checksum Flyway

- `./mvnw flyway:validate` direto falhou apenas por ausência de datasource configurado; isso não provou nada sobre checksum.
- Prova executada:
  - Postgres temporário `postgres:16-alpine`.
  - Migrations SQL do commit `a7c15ce` extraídas com `git archive` e aplicadas com `flyway-maven-plugin:11.7.2:migrate`.
  - Migrations SQL do commit `5c419a0` validadas com `flyway-maven-plugin:11.7.2:validate` e `-Dflyway.ignoreMigrationPatterns=*:pending`.
  - Resultado: `CHECKSUM_VALIDADO_OK`.
- `git diff --ignore-cr-at-eol a7c15ce..5c419a0 -- application.yml V3 V5 V6 V7` não produziu diff: as alterações nesses arquivos são apenas finais de linha.
- `application.yml` não é migration Flyway; V3/V5/V6/V7 são as que poderiam ter checksum alterado.
- Versão confirmada: `~/.m2/repository/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom:57` define `<flyway.version>11.7.2</flyway.version>`, e `flyway-core`, `flyway-database-postgresql` e `flyway-maven-plugin` estão em `11.7.2` no cache local.
- `flyway_schema_history` do Render não foi consultado por falta de credenciais. A verificação por Git mostrou que a V8 antiga esteve apenas na branch de feature e foi removida antes do merge em `main`, que é a única branch publicada pela Render (`render.yaml:32`); risco residual restrito a deploy manual da branch.

## Assinaturas relevantes

- `WhatsappWebhookService.java:76`: `@Async @Transactional public void processarWebhook(String payload, String signature)`.
- `WhatsappWebhookService.java:108`: `private void processarMensagem(Map<String, Object> message)`.
- `WhatsappWebhookService.java:141-145`: checagem `!telefone.isVerificado() || !telefone.isAtivo()` envia instrução de cadastro, registra `NAO_VERIFICADO` e retorna.
- `WhatsappWebhookService.java:147-153`: `sim`, `não`, `nao` e `(?i)^(sim|não|nao)\\s+\\d+$` são roteados para `processarConfirmacaoTexto`.
- `WhatsappWebhookService.java:251`: `@Transactional void processarConfirmacaoTexto(String messageId, String from, String texto)`.
- Payload aceito pelo webhook: `entry[].changes[].value.messages[]`, com `id`, `from` e `text.body` (ou `button.payload`).

## Commits

| Commit | Descrição |
|---|---|
| `0f796c7` | `chore: normaliza finais de linha para LF` |
| `2576a77` | `fix(whatsapp): corrige mapeamento e validade da pendencia` |
| `55f6537` | `refactor(whatsapp): consolida cadastro de telefone em um unico fluxo` |
| `1d360cb` | `fix(whatsapp): aplica cooldown, limite por hora e status 409/429` |
| `6e13a86` | `fix(whatsapp): confirma lancamento de forma atomica por sim/nao` |
| `5c419a0` | `test(lancamento): ajusta listagem ao contrato paginado` |

## Validação final

- Execuções 1 e 2 de `./mvnw -q clean test`, antes da revisão: `SUITE_COMPLETA_OK` e `SUITE_REPETIDA_OK`, ambas com **27 classes, 180 testes, 0 falhas, 0 erros, 0 skips**.
- Validação fresca após a revisão pré-merge: `./mvnw -q spotless:apply` e `git diff --check` OK; `./mvnw -q clean test` terminou com `TEST_EXIT=0`, Surefire **27 classes, 180 testes, 0 falhas, 0 erros, 0 skips** e `DUMPS=0`.
- `./mvnw -q spotless:check` — `SPOTLESS_CHECK_EXIT=0`.
- `git diff --check` — `DIFF_CHECK_EXIT=0`.
- `./mvnw dependency:tree -Dincludes=org.flywaydb` — `flyway-core:jar:11.7.2:compile`, `flyway-database-postgresql:jar:11.7.2:compile`, `BUILD SUCCESS`.
- `git status --short` antes dos commits:
  - `M api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappWebhookService.java`
  - `M api/src/test/java/com/controlefinanceiro/LancamentoIntegrationTest.java`
  - `M api/src/test/java/com/controlefinanceiro/whatsapp/WhatsappConfirmacaoIntegrationTest.java`
  - `?? RELATORIO-PLANO-WHATSAPP-VERIFICACAO.md`
- Código de produção: apenas Javadoc documental, sem mudança de comportamento após os mutation checks.

## Pendências e riscos

- Push e PR não realizados: apenas commit local, conforme solicitado.
- Fase 5 (envio real via Meta Cloud API) permanece para PR separado; sem ela o usuário final não recebe resposta real da Meta.
- V6 e V7 estão presentes no histórico de `main` e no commit base `a7c15ce`; V8 foi adicionada neste branch. Checksums das migrations touchadas por EOL foram provados. A V8 antiga (`empresa_nome_unique`) esteve apenas na branch de feature e foi removida antes do merge em `main`, que é a única branch publicada pela Render (`render.yaml:32`); risco de colisão encerrado para deploy automático (cadeia de primeiro pai sem V8 antiga + `autoDeployTrigger` omitido, cujo padrão é `commit`).
- Relatório Surefire antigo (`WhatsAppIntegrationTest.txt`) foi substituído pela execução `clean test` e não pertence ao resultado final.

## Arquivos centrais alterados

- `api/src/main/resources/db/migration/V8__renomeia_coluna_codigo.sql`
- `api/src/main/java/com/controlefinanceiro/whatsapp/UsuarioTelefone.java`
- `api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappConfirmacaoPendente.java`
- `api/src/main/java/com/controlefinanceiro/whatsapp/UsuarioTelefoneService.java`
- `api/src/main/java/com/controlefinanceiro/whatsapp/UsuarioTelefoneRepository.java`
- `api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappCodigoVerificacaoRepository.java`
- `api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappConfirmacaoPendenteRepository.java`
- `api/src/main/java/com/controlefinanceiro/whatsapp/WhatsappWebhookService.java`
- `api/src/main/java/com/controlefinanceiro/common/CodigoVerificacaoCooldownException.java`
- `api/src/main/java/com/controlefinanceiro/config/ProblemDetailExceptionHandler.java`
- `api/src/main/java/com/controlefinanceiro/config/SecurityConfig.java`
- `api/src/main/java/com/controlefinanceiro/lancamento/LancamentoService.java`
- `api/src/test/java/com/controlefinanceiro/UsuarioTelefoneIntegrationTest.java`
- `api/src/test/java/com/controlefinanceiro/whatsapp/WhatsappConfirmacaoIntegrationTest.java`
- `api/src/test/java/com/controlefinanceiro/LancamentoIntegrationTest.java`
