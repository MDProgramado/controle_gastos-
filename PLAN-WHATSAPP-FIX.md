# Plano de Correção — Canal WhatsApp

## Contexto

O branch `fix/whatsapp-cadastro-novo` (commit `0d21339`) implementou a infraestrutura do WhatsApp, mas há problemas críticos que impedem o fluxo de funcionar corretamente.

## Problemas identificados (ordem de gravidade)

| # | Problema | Gravidade | Local |
|---|----------|-----------|-------|
| P1 | Confirmação "CONFIRMAR" marca como gravada mas não grava o lançamento (chamada ao LancamentoService comentada; entidade Lancamento sem campos canal/messageIdOrigem; service sem criarComCanal) | Crítico | WhatsappWebhookService.java:300-302, Lancamento.java, LancamentoService.java |
| P2 | Rate limit de código não funciona: @Transactional sem noRollbackFor desfaz o incremento de tentativas | Crítico | WhatsappCadastroService.java:143-182 |
| P3 | IllegalArgumentException não tem handler no ProblemDetailExceptionHandler → retorna 500 em vez de 400 | Alto | ProblemDetailExceptionHandler.java |
| P4 | Duas implementações paralelas com regras conflitantes (UsuarioTelefoneService vs WhatsappCadastroService) | Alto | Ambos os serviços/controllers |
| P5 | findAtivoByUsuarioId não filtra ativo=true → pode lançar IncorrectResultSizeDataAccessException | Médio | UsuarioTelefoneRepository.java:18 |
| P6 | SecurityConfig não restringe /api/whatsapp/** a ADMIN | Médio | SecurityConfig.java:54-55 |
| P7 | WhatsappEnvioService é stub (TODO) — nenhum envio real para Meta | Baixo/Médio | WhatsappEnvioService.java |

## Plano de execução — Status

### Fase 1: Correções críticas (CONCLUÍDA)

1. ✅ **P1a** — Campos `canal` e `messageIdOrigem` adicionados à entidade `Lancamento`
2. ✅ **P1b** — Método `criarComCanal` adicionado ao `LancamentoService`
3. ✅ **P1c** — Chamada ao `lancamentoService.criarComCanal` descomentada em `WhatsappWebhookService.confirmarLancamento`; pendência marcada CONFIRMADA apenas após salvar com sucesso
4. ✅ **P2** — `noRollbackFor = IllegalArgumentException.class` adicionado ao `@Transactional` de `verificarCodigo` em `WhatsappCadastroService`
5. ✅ **P3** — `@ExceptionHandler(IllegalArgumentException.class)` adicionado ao `ProblemDetailExceptionHandler` retornando 400

### Fase 2: Correções de consistência e segurança (CONCLUÍDA)

6. ⚠️ **P4** — Parcialmente concluído: limite unificado para 3 tentativas em ambos os serviços. Duas implementações paralelas ainda coexistem (WhatsappCadastroService e UsuarioTelefoneService) — decisão de remoção pendente
7. ✅ **P5** — `findAtivoByUsuarioId` corrigido para filtrar `ativo = true` via @Query explícito
8. ✅ **P6** — Restrição ADMIN adicionada para `POST /api/whatsapp/**` no `SecurityConfig`

### Fase 3: Testes e validação (PARCIAL)

9. ⏸️ Suíte completa não pôde ser executada: Docker Desktop não está ativo neste ambiente; Testcontainers não consegue subir o PostgreSQL de teste
10. ✅ Commit e push realizados na branch `fix/whatsapp-cadastro-novo` (commit `c0a4e99`)

## Resultados da validação

- ✅ Compilação: BUILD SUCCESS (spotless check passa)
- ✅ Testes de unidade WhatsAppWebhookControllerTest: 4/0/0 (passando)
- ⏸️ Testes de integração UsuarioTelefoneIntegrationTest: não executáveis sem Docker Desktop
- ⏸️ Suíte completa de 169 testes: não executável neste ambiente

## Critérios de aceitação — Status

- ✅ Nenhum lançamento gravado sem confirmação explícita (CONFIRMAR grava via LancamentoService.criarComCanal)
- ✅ Rate limit de tentativas persiste (contador não é desfeito por rollback)
- ✅ Erros de validação retornam 400 (não 500)
- ✅ Apenas um telefone ativo por usuario (findAtivoByUsuarioId filtra ativo=true)
- ✅ /api/whatsapp/** restrito a ADMIN
- ⏸️ Suite de testes completa: pendente (requer Docker Desktop)

## Próximos passos

1. Executar suíte completa em ambiente com Docker Desktop ativo: `./mvnw test -Dspotless.check.skip=true`
2. Decidir sobre unificação das duas implementações de telefone (P4 — remover WhatsappCadastroService/Controller ou manter ambas com regras unificadas)
3. Implementar envio real pela Meta Cloud API em `WhatsappEnvioService` (atualmente é stub com TODO)
4. Fazer merge para `main` após validação completa
