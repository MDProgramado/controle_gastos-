import { expect, test } from "@playwright/test";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { RagAdminClient } from "../components/RagAdminClient";
import { urlApiSegura } from "../lib/bff-helpers";
import { itensDeNavegacao } from "../lib/navegacao";
import {
  iniciarIngestaoRag,
  obterStatusRag,
  RagAdminError,
} from "../lib/rag-admin";

function comFetch(fetchTemporario: typeof fetch, executar: () => Promise<void>) {
  const fetchOriginal = globalThis.fetch;
  globalThis.fetch = fetchTemporario;
  return executar().finally(() => {
    globalThis.fetch = fetchOriginal;
  });
}

test("BFF aceita rotas RAG e rejeita travessia", () => {
  expect(urlApiSegura("/api/ai/rag/status")).toBe("/api/ai/rag/status");
  expect(urlApiSegura("/api/ai/rag/ingestir")).toBe("/api/ai/rag/ingestir");
  expect(urlApiSegura("/api/ai/rag/../auth/me")).toBeNull();
});

test("status RAG devolve progresso e contagem", async () => {
  await comFetch(
    async () =>
      new Response(
        JSON.stringify({
          chunks: 128,
          caminho: "rag/corpus.jsonl",
          situacao: "EM_ANDAMENTO",
          total: 3142,
          processados: 128,
          erro: null,
          iniciadoEm: "2026-11-25T10:00:00Z",
          finalizadoEm: null,
        }),
        { status: 200, headers: { "content-type": "application/json" } },
      ),
    async () => {
      const status = await obterStatusRag();
      expect(status.chunks).toBe(128);
      expect(status.situacao).toBe("EM_ANDAMENTO");
      expect(status.processados).toBe(128);
      expect(status.total).toBe(3142);
    },
  );
});

test("ingestão RAG usa a rota BFF e o corpo vazio", async () => {
  let urlChamada = "";
  let corpoChamado = "";
  await comFetch(
    async (url, init) => {
      urlChamada = String(url);
      corpoChamado = String(init?.body ?? "");
      return new Response(
        JSON.stringify({
          situacao: "ACEITO",
          processados: 0,
          lotes: 0,
          caminho: "rag/corpus.jsonl",
        }),
        { status: 202, headers: { "content-type": "application/json" } },
      );
    },
    async () => {
      const resposta = await iniciarIngestaoRag();
      expect(urlChamada).toBe("/api/ai/rag/ingestir");
      expect(corpoChamado).toBe("{}");
      expect(resposta.situacao).toBe("ACEITO");
    },
  );
});

test("menu ADMIN inclui a base RAG", () => {
  const rotas = itensDeNavegacao("ADMIN").map((item) => item.href);
  expect(rotas).toContain("/admin/rag");
  expect(itensDeNavegacao("USER").map((item) => item.href)).not.toContain(
    "/admin/rag",
  );
});

test("painel ADMIN mostra progresso e ação de reindexação", () => {
  const html = renderToStaticMarkup(createElement(RagAdminClient));
  expect(html).toContain("Reindexação da base");
  expect(html).toContain("Carregando");
  expect(html).toContain("Chunks indexados");
  expect(html).toContain("Reindexar base");
  expect(html).toContain('role="progressbar"');
});

test("conflito de reindexação vira mensagem específica", async () => {
  await comFetch(
    async () =>
      new Response(JSON.stringify({ erro: "Já existe uma reindexação em andamento." }), {
        status: 409,
        headers: { "content-type": "application/json" },
      }),
    async () => {
      await expect(iniciarIngestaoRag()).rejects.toMatchObject({
        name: "RagAdminError",
        status: 409,
        message: "Já existe uma reindexação em andamento.",
      } satisfies Partial<RagAdminError>);
    },
  );
});

test("erro 402 vira mensagem de créditos insuficientes", async () => {
  await comFetch(
    async () =>
      new Response(JSON.stringify({ detail: "Payment Required" }), {
        status: 402,
        headers: { "content-type": "application/json" },
      }),
    async () => {
      await expect(obterStatusRag()).rejects.toMatchObject({
        name: "RagAdminError",
        status: 402,
        message: "Créditos insuficientes na API de IA. Adicione créditos para reindexar a base.",
      } satisfies Partial<RagAdminError>);
    },
  );
});
