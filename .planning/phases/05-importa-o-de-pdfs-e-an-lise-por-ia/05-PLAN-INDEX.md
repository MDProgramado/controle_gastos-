# Phase 5: Importação de PDFs e Análise por IA - Plan Index

**Phase:** 5
**Name:** Importação de PDFs e Análise por IA
**Mode:** MVP (vertical slices UI→API→DB)
**Status:** Planned
**Created:** 2026-11-13

## Waves

| Wave | Plans | What it builds | Dependencies |
|------|-------|----------------|--------------|
| 1 | 05-01 | Migration V3 (staging + definitivas) + entidades/repositórios de domínio | — |
| 2 | 05-02 | PDFBox + sanitização (CPF/CNPJ/cartão) + classificador de documento | Wave 1 |
| 3 | 05-03 | Extensão do cliente IA: cascata, backoff, JSON estrito, 1 chamada/documento, `modeloUsado`/`degradado` | Wave 2 |
| 4 | 05-04 | Serviço de análise + persistência de staging + resumo/alertas do mês | Wave 2, Wave 3 |
| 5 | 05-05 | Confirmação idempotente + gravação tudo-ou-nada em `lancamento`/`conta_a_pagar` + dedupe | Wave 4 |
| 6 | 05-06 | Web: BFF multipart + página `/importacoes` (upload, revisão, confirmação) | Wave 4, Wave 5 |
| 7 | 05-07 | Testes (unit + integração), `security-audit`, verificação final | Wave 1–6 |

## Requirement Coverage

| Requirement | Covered by plan(s) |
|-------------|--------------------|
| IMP-01 | 05-01, 05-06 |
| IMP-02 | 05-02 |
| IMP-03 | 05-02 |
| IMP-04 | 05-03, 05-04 |
| IMP-05 | 05-04 |
| IMP-06 | 05-03 |
| IMP-07 | 05-03 |
| IMP-08 | 05-03 |
| IMP-09 | 05-04, 05-05 |
| IMP-10 | 05-05 |
| IMP-11 | 05-01, 05-05 |
| IMP-12 | 05-04 |
| IMP-13 | 05-01, 05-04 |
| IMP-14 | 05-02 |
| IMP-15 | 05-06 |

## Decisions covered (D-01..D-32)

D-01 (lote multipart) → 05-01, 05-06 · D-02 (duas etapas) → 05-04, 05-05 · D-03 (UI incluída) → 05-06 · D-04 (validação de arquivo) → 05-01, 05-02 · D-05 (A_PAGAR em tabelas novas) → 05-01 · D-06 (staging) → 05-04 · D-07 (transação+idempotência) → 05-05 · D-08 (origem/chave_dedupe) → 05-01 · D-09 (hash SHA-256) → 05-04 · D-10 (tenant do JWT) → 05-01, 05-04 · D-11 (PDFBox) → 05-02 · D-12 (sem texto = aviso) → 05-02 · D-13 (sanitização) → 05-02 · D-14 (não persistir PDF bruto) → 05-01 · D-15 (cascata gratuita existente) → 05-03 · D-16 (reserva paga último recurso) → 05-03 · D-17 (duas camadas) → 05-03 · D-18 (gatilhos 429/402/5xx/JSON) → 05-03 · D-19 (1 chamada/documento) → 05-03 · D-20 (chat intacto) → 05-03 · D-21 (JSON estrito) → 05-03 · D-22 (modeloUsado/degradado) → 05-03 · D-23 (503 amigável) → 05-03 · D-24 (mapeamento de tipos) → 05-05 · D-25 (categoria do tenant) → 05-04 · D-26 (resumo do mês) → 05-04 · D-27 (gasto atípico) → 05-04 · D-28 (BFF) → 05-06 · D-29 (limite 4MB/5 arquivos) → 05-06 · D-30 (revisão item a item) → 05-06 · D-31 (visual design-taste-frontend) → 05-06 · D-32 (navegação Importar PDF) → 05-06

## Threat Model (fase)

| Ameaça | Severidade | Mitigação (onde) |
|--------|------------|------------------|
| Upload de arquivo disfarçado/malicioso (extensão ≠ conteúdo) | Alta | D-04: magic bytes `%PDF` + content-type + tamanho; 05-01/05-06 |
| Zip-bomb / PDF gigantesco esgotando memória | Média | D-29: 4 MB/arquivo, 5 arquivos; 05-06 |
| CPF/CNPJ/cartão saindo do servidor para OpenRouter | Alta | D-13: sanitização antes do envio; 05-02 |
| Prompt injection via texto do PDF enviado à IA | Média | Prompt com delimitadores rígidos; JSON estrito; 05-03 |
| IDOR entre empresas (empresa_id injetado) | Alta | D-10: tenant só do JWT; 05-01/05-04 |
| XSS via descrição vinda da IA | Média | Escaping no frontend; 05-06 |
| Exaustão de cota 50 req/dia (:free) | Média | D-16: reserva paga último recurso; D-19: 1 chamada/documento; 05-03 |
| DoS na OpenRouter (muitos documentos) | Média | Backoff exponencial; fila sequencial por documento; 05-03 |
