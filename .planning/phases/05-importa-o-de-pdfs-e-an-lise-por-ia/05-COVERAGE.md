# 05-COVERAGE.md — Cobertura de requisitos e decisões da Fase 5

**Gate (05-07-t5):** cada IMP-01..IMP-15 aparece em ≥1 plano de wave; cada decisão D-01..D-32 está citada em um `must_haves.truths` de plano (05-01..05-06) e/ou implementada e testada nesta fase.

Fonte das decisões: `.planning/phases/05-importa-o-de-pdfs-e-an-lise-por-ia/05-CONTEXT.md`.
Fonte dos requisitos: `.planning/REQUIREMENTS.md` (IMP-01..IMP-15).

---

## Requisitos IMP-01..IMP-15

| ID | Requisito | Onde implementado | Onde testado | Plano(s) de wave |
|----|-----------|-------------------|--------------|------------------|
| IMP-01 | Upload em lote: 1..N PDFs multipart (`arquivos`) | `ImportacaoController.analisar` (MultipartFile[]) | `ImportacaoPdfIntegrationTest` (2 PDFs) | 05-04 |
| IMP-02 | Texto extraído no backend (PDFBox), sem OCR | `PdfTextoExtractor` (PDFTextStripper) | `PdfTextoExtractorTest` | 05-02 |
| IMP-03 | CPF/CNPJ/cartão mascarados antes da OpenRouter | `SanitizadorTexto` | `SanitizadorTextoTest` | 05-02 |
| IMP-04 | Classifica item RECEITA / DESPESA_PAGA / A_PAGAR | `IaExtracaoService.parseItem`, `ClassificadorDocumento` | `IaExtracaoServiceTest`, `ClassificadorDocumentoTest` | 05-03 |
| IMP-05 | Resumo do mês (recebido, pago, comprometido, alertas) | `AnalisadorConsolidado.montarResumo` | `AnalisadorConsolidadoTest` | 05-03 |
| IMP-06 | JSON estrito; item inválido → `rejeitados` sem derrubar | `IaExtracaoService.parseResposta` | `IaExtracaoServiceTest` (JSON inválido, cercas) + IT `itemInvalidoVaiParaRejeitados` | 05-03 / 05-07 |
| IMP-07 | Retry/backoff + cascata gratuita em 429/402 | `AiClientCascata` (camada cliente) | `AiClientCascataTest` | 05-03 |
| IMP-08 | Caminho normal só `:free`; pago último recurso, desativável | `application.yml` `ai.openrouter.modelos-reserva`; `AI_OPENROUTER_MODELOS_RESERVA=none` | código + SECURITY.md | 05-03 |
| IMP-09 | Análise NÃO grava lançamentos (apenas staging) | `ImportacaoService.analisar` | `ImportacaoPdfIntegrationTest.analiseGravaStagingNaoGravaDefinitivo` | 05-04 |
| IMP-10 | Confirmação tudo-ou-nada + idempotente | `ImportacaoService.confirmar` (@Transactional) | `ImportacaoConfirmacaoTest` (29/29) + IT `confirmacaoTudoOuNadaEIdempotente` | 05-05 |
| IMP-11 | `A_PAGAR` em `conta_a_pagar`, não altera saldo dashboard | Tabelas `conta_a_pagar`/`fatura_cartao`; filtro dashboard `tipo IN ('ENTRADA','SAIDA')` | `ImportacaoConfirmacaoTest` (conta_a_pagar) | 05-05 |
| IMP-12 | Reenvio mesmo arquivo → DUPLICADO (SHA-256) | `Deduplicador`, `UNIQUE(empresa_id, hash_sha256)` | `DeduplicadorTest` + IT `reenvioMesmoArquivoEhDuplicado` | 05-04 |
| IMP-13 | `empresa_id` exclusivamente do JWT (R3) | `SecurityConfig` (claim), `TenantContext`, `ImportacaoService.usuarioDoToken` | IT `isolamentoEntreEmpresas` + `AuthTenantIntegrationTest` | 05-01 |
| IMP-14 | PDF sem texto → `SEM_TEXTO`, lote continua | `ImportacaoService.analisar` (erro por arquivo não derruba lote) | IT `pdfSemTextoNaoDerrubaLote` | 05-04 |
| IMP-15 | Web: `/importacamentos` upload + revisão + confirmação | `web/app/api/importacoes/*/route.ts`, componentes revisão | build/lint verde; validação visual wave 6 (gate aprovado) | 05-06 |

---

## Decisões D-01..D-32

| ID | Decisão | Implementado em | Testado em | Plano |
|----|---------|-----------------|------------|-------|
| D-01 | Upload em lote 1..N, campo `arquivos` | `ImportacaoController` L41-45 | IT upload 2 PDFs | 05-04 |
| D-02 | Duas etapas: staging primeiro, gravação só na confirmação | `ImportacaoService.analisar` (sem lancamento/conta_a_pagar) | IT `analiseGravaStagingNaoGravaDefinitivo` | 05-04 |
| D-03 | Interface web incluída | `web/app/importacoes/page.tsx` + componentes | build + validação visual wave 6 | 05-06 |
| D-04 | Validação arquivo: content-type + magic bytes %PDF + tamanho + qtd | `ImportacaoController` / `ArquivoInvalidoException` | IT `loteVazioRetorna400`; wave 6 (rejeição .txt) | 05-04 |
| D-05 | `A_PAGAR` em tabela própria; `lancamento.tipo` só ENTRADA/SAIDA | Tabelas `conta_a_pagar`/`fatura_cartao` | `ImportacaoConfirmacaoTest` | 05-05 |
| D-06 | Análise grava em staging (sem estado em memória) | Tabelas `importacao_*` | IT staging counts | 05-04 |
| D-07 | Confirmação transação tudo-ou-nada + idempotente | `ImportacaoService.confirmar` (@Transactional, status IMPORTADO) | IT confirmação idempotente (`"importados":0`, `"duplicados":3`) | 05-05 |
| D-08 | `lancamento` ganha `origem`, `documento_id`, `chave_dedupe` (índice único parcial por empresa) | `V3__importacoes_pdf.sql` + `construirItens` | IT `confirmacaoTudoOuNadaEIdempotente` (chave_dedupe not null, origem/documento_id not null) | 05-04 |
| D-09 | Mesma SHA-256 → DUPLICADO, sem reprocessar | `Deduplicador` + `UNIQUE(empresa_id, hash_sha256)` | IT `reenvioMesmoArquivoEhDuplicado` | 05-04 |
| D-10 | `empresa_id` só do JWT | `TenantContext.getEmpresaId()`, `findByIdAndEmpresaId` | IT `isolamentoEntreEmpresas` | 05-01 |
| D-11 | Extração PDFBox, sem OCR (custo zero) | `PdfTextoExtractor` | `PdfTextoExtractorTest` | 05-02 |
| D-12 | PDF sem texto → `SEM_TEXTO`, lote segue | `ImportacaoService` (status ERRO/SEM_TEXTO por arquivo) | IT `pdfSemTextoNaoDerrubaLote` | 05-04 |
| D-13 | Sanitização obrigatória antes da OpenRouter | `SanitizadorTexto` (CPF/CNPJ/cartão 13-19 dígitos) | `SanitizadorTextoTest` | 05-02 |
| D-14 | PDF bruto não persistido (LGPD); só hash + metadados + resultado | `ImportacaoService` (não guarda bytes do PDF) | inspeção de código | 05-04 |
| D-15 | Cascata modelos gratuitos (`:free` + `openrouter/free`) | `AiClient` (config `ai.openrouter.modelos`) | `AiClientCascataTest` | 05-03 |
| D-16 | Reserva paga último recurso, desativável por env var | `application.yml` `modelos-reserva`; `AI_OPENROUTER_MODELOS_RESERVA` | código | 05-03 |
| D-17 | Cascata 2 camadas (servidor OpenRouter + cliente) + backoff 1.5s→3s→6s | `AiClientCascata` | `AiClientCascataTest` | 05-03 |
| D-18 | Gatilhos: 429, 402, 5xx, JSON inválido | `AiClientCascata` + `IaExtracaoService.parseResposta` | `AiClientCascataTest` + `IaExtracaoServiceTest` | 05-03 |
| D-19 | 1 chamada IA/documento; temperature 0.0; max_tokens 8000→4000 | `AiClient.montarCorpo` parametrizado | `AiServiceTest`/`AiIntegrationTest` (regressão, defaults preservados) | 05-03 |
| D-20 | Chat (`AiService`) delega mesma cascata; defaults preservados | `AiService` → `AiClient.completar` | `AiServiceTest`, `AiIntegrationTest` | 05-03 |
| D-21 | JSON estrito, sem markdown; item inválido → `rejeitados` com motivo | `IaExtracaoService.parseResposta`/`parseItem` | `IaExtracaoServiceTest` + IT `itemInvalidoVaiParaRejeitados` | 05-03 / 05-07 |
| D-22 | Resposta expõe `modeloUsado` e `degradado` | `DocumentoAnalise` (modeloUsado, degradado) | IT `analise...` (corpo contém "modeloUsado"/"degradado") | 05-04 |
| D-23 | Falha total cascata → 503 amigável | `ServicoIaIndisponivelException` → `ProblemDetailExceptionHandler` → 503 | IT `iaIndisponivelNenhumAnalisadoRetorna503` | 05-04 |
| D-24 | Mapeamento: RECEITA→ENTRADA, DESPESA_PAGA→SAIDA, A_PAGAR→conta_a_pagar | `ImportacaoService.construirItens` + confirmação | `ImportacaoConfirmacaoTest` + IT (1 ENTRADA, 1 SAIDA, 1 conta_a_pagar, 0 A_PAGAR) | 05-05 |
| D-25 | Categoria contra categorias reais do tenant + fallback + alerta | `MapeadorCategoria` | `MapeadorCategoriaTest` | 05-03 |
| D-26 | Resumo do mês + alertas (duplicidade, divergência fatura, pagamento própria fatura) | `AnalisadorConsolidado` | `AnalisadorConsolidadoTest` | 05-03 |
| D-27 | Gasto atípico com `BigDecimal` (nunca double) | `AnalisadorConsolidado` | `AnalisadorConsolidadoTest` | 05-03 |
| D-28 | BFF lê cookie httpOnly `sess`, repassa `Authorization: Bearer`; browser não fala API | 3 BFFs `web/app/api/importacoes/*/route.ts` | SECURITY.md verificação 4 | 05-06 |
| D-29 | Limite Vercel: 4 MB/arquivo, 5 arquivos, 4,5 MB total no BFF | `route.ts` POST (limites + mensagens pt-BR) | build/lint + inspeção | 05-06 |
| D-30 | Tela revisão: agrupados por tipo, checkbox/item, select categoria | componentes de revisão (wave 6) | validação visual wave 6 | 05-06 |
| D-31 | Visual conforme `design-taste-frontend` + `05-UI-SPEC.md`; tokens `globals.css` | `web/app/importacoes/page.tsx` + componentes | gate design-taste APROVADO (wave 6) | 05-06 |
| D-32 | Item "Importar PDF" na navegação (sidebar + TopNav) | `components/ui/sidebar.tsx`, `components/TopNav.tsx` | snapshot wave 6 (link presente) | 05-06 |

---

## Critérios de verificação (t5)

- [x] `mvnw verify` verde (API: 100 testes unitários, 0 falhas, spotless check OK). ITs Testcontainers compilados; execução **pendente** sem Docker local (critério da wave admite).
- [x] `npm run build` verde (web) — rotas `/importacoes` e BFFs presentes.
- [x] `npm run lint` verde — 0 erros (5 warnings em assets vendidos pré-existentes).
- [x] `SECURITY.md` da fase 5 escrito com 4 verificações confirmadas + evidências.
- [x] Tabela de cobertura acima preenchida (IMP-01..15 e D-01..32).
- [ ] Commit dos artefatos da fase (manual, `gsd-sdk commit` falha) — ver `STATE.md`.
