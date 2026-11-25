import { API_TIMEOUT_MS } from "./bff-helpers";

export type RagSituacao = "PARADO" | "EM_ANDAMENTO" | "SUCESSO" | "ERRO";

export type RagStatus = {
  chunks: number;
  caminho: string;
  situacao: RagSituacao;
  total: number;
  processados: number;
  erro: string | null;
  iniciadoEm: string | null;
  finalizadoEm: string | null;
};

export type RagIngestaoResponse = {
  situacao: string;
  processados: number;
  lotes: number;
  caminho: string;
};

export class RagAdminError extends Error {
  status: number;

  constructor(status: number, mensagem: string) {
    super(mensagem);
    this.name = "RagAdminError";
    this.status = status;
  }
}

async function buscar(url: string, init: RequestInit = {}): Promise<Response> {
  try {
    return await fetch(url, {
      ...init,
      cache: "no-store",
      signal: AbortSignal.timeout(API_TIMEOUT_MS),
    });
  } catch {
    throw new RagAdminError(0, "Não foi possível conectar ao servidor.");
  }
}

async function detalharErro(resposta: Response): Promise<string | null> {
  try {
    const corpo = (await resposta.json()) as { detail?: string; erro?: string };
    return corpo.detail ?? corpo.erro ?? null;
  } catch {
    return null;
  }
}

function mensagemErro(status: number, detalhe: string | null): string {
  if (status === 401) return "Sessão expirada. Entre novamente.";
  if (status === 402) {
    return "Créditos insuficientes na API de IA. Adicione créditos para reindexar a base.";
  }
  if (status === 409) return "Já existe uma reindexação em andamento.";
  if (status === 503) return "API indisponível. Tente novamente em instantes.";
  return detalhe ?? "Não foi possível consultar o serviço de reindexação.";
}

export async function obterStatusRag(): Promise<RagStatus> {
  const resposta = await buscar("/api/ai/rag/status");
  if (!resposta.ok) {
    throw new RagAdminError(
      resposta.status,
      mensagemErro(resposta.status, await detalharErro(resposta)),
    );
  }
  return (await resposta.json()) as RagStatus;
}

export async function iniciarIngestaoRag(): Promise<RagIngestaoResponse> {
  const resposta = await buscar("/api/ai/rag/ingestir", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({}),
  });
  if (!resposta.ok) {
    throw new RagAdminError(
      resposta.status,
      mensagemErro(resposta.status, await detalharErro(resposta)),
    );
  }
  return (await resposta.json()) as RagIngestaoResponse;
}
