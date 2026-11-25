# Quick summary — documentação de arquitetura (25/11/2026)

## Entregue
- `ARCHITECTURE.md` na raiz, em pt-BR, com visão C4, invariantes R1–R7, mapa do backend e do frontend, fluxos ponta a ponta (OAuth, lote, PDF, IA/RAG, WhatsApp, helpdesk), integrações externas, CI/deploy, 40 decisões A1–A40 com racional, dívidas, riscos aceitos e mapa de documentação desatualizada.
- Diagramas Mermaid: contexto, camadas da API, Google OAuth, lote atômico, IA/RAG e pipeline de build/deploy.

## Validação
- Todos os caminhos completos citados foram verificados no working tree.
- Tabelas com contagem de colunas consistente; IDs A1–A40 sem duplicatas ou lacunas.
- Nenhum arquivo de código, config, plano existente ou `AGENTS.md` foi alterado.

## Observações
- Durante a escrita havia trabalho não commitado em andamento (`261125-correcoes-pos-deploy`: `api/Dockerfile`, `render.yaml`, corpus em `api/src/main/resources/rag/` e role padrão do onboarding). O documento reflete o estado atual do working tree e registra contas a pagar e a mudança de role como em andamento.
- Não houve commit, push ou deploy.
