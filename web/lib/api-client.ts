/**
 * API client-side (Client Component).
 * Funções que podem ser chamadas diretamente do navegador.
 *
 * A API (Spring Boot) exige o header Authorization: Bearer, e o cookie "sess" é httpOnly —
 * ou seja, o JavaScript da página não o lê. Por isso as chamadas passam pelas rotas BFF do
 * Next (/api/ai/chat, /api/categorias), que repassam o Bearer para a API.
 */

export type AiChatRequest = {
  mensagem: string;
  historico: Array<{ role: string; content: string }>;
  sessionId?: string;
};

export type AiPropostaLancamento = {
  tipo: "ENTRADA" | "SAIDA";
  valor: number;
  data: string;
  categoriaId: string | null;
  categoriaSugerida?: string;
  descricao?: string;
};

export type AiChatResponse = {
  intent: "consulta" | "lancamento" | "esclarecimento";
  resposta: string;
  dados?: Array<{ label: string; valor: number }>;
  proposta?: AiPropostaLancamento;
  /** Todos os lançamentos retornados na mensagem (pode ter mais de um). */
  propostas?: AiPropostaLancamento[];
  opcoes?: string[];
};

/**
 * POST /api/ai/chat — envia mensagem para a IA e obtém resposta estruturada.
 *
 * Quando a API responde com erro (503 do serviço de IA, por exemplo), devolve a mensagem do
 * servidor para o chat mostrar algo útil em vez de um erro genérico.
 */
export async function chatComIA(
  request: AiChatRequest,
): Promise<AiChatResponse | null> {
  try {
    const res = await fetch("/api/ai/chat", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) {
      const detalhe = await extrairDetalhe(res);
      throw new Error(
        detalhe ??
          "Não foi possível falar com o serviço de IA. Tente novamente mais tarde.",
      );
    }
    return (await res.json()) as AiChatResponse;
  } catch (e) {
    if (e instanceof Error) throw e;
    return null;
  }
}

/** Lê o campo "detail" de um ProblemDetail (RFC 7807) devolvido pela API. */
async function extrairDetalhe(res: Response): Promise<string | null> {
  try {
    const corpo = (await res.json()) as { detail?: string };
    return typeof corpo?.detail === "string" ? corpo.detail : null;
  } catch {
    return null;
  }
}

export type CategoriaDisponivel = {
  id: number;
  nome: string;
  tipo: string;
  ativo: boolean;
};
/** GET /api/categorias — categorias reais do tenant (via BFF). */
export async function listarCategorias(): Promise<CategoriaDisponivel[]> {
  try {
    const res = await fetch("/api/categorias", { cache: "no-store" });
    if (!res.ok) return [];
    const dados = (await res.json()) as CategoriaDisponivel[];
    return Array.isArray(dados) ? dados : [];
  } catch {
    return [];
  }
}

export type LancamentoParaGravar = {
  categoriaId: number;
  tipo: "ENTRADA" | "SAIDA";
  valor: number;
  data: string;
  descricao?: string;
};

/**
 * POST /api/lancamentos/lote — grava os lançamentos confirmados em UMA transação
 * (tudo-ou-nada). Devolve os ids criados ou null em caso de falha.
 */
export async function gravarLancamentos(
  linhas: LancamentoParaGravar[],
): Promise<number[] | null> {
  try {
    const res = await fetch("/api/lancamentos/lote", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ linhas }),
      cache: "no-store",
    });
    if (!res.ok) return null;
    const dados = (await res.json()) as Array<{ id: number }>;
    return dados.map((l) => l.id);
  } catch {
    return null;
  }
}
