/**
 * API client-side (Client Component).
 * Funções que podem ser chamadas diretamente do navegador.
 * Usa fetch diretamente — o cookie "sess" é enviado automaticamente pelo
 * navegador para o mesmo domínio.
 */

export type AiChatRequest = {
  mensagem: string;
  historico: Array<{ role: string; content: string }>;
  sessionId?: string;
};

export type AiChatResponse = {
  intent: "consulta" | "lancamento" | "esclarecimento";
  resposta: string;
  dados?: Array<{ label: string; valor: number }>;
  proposta?: {
    tipo: "ENTRADA" | "SAIDA";
    valor: number;
    data: string;
    categoriaId: string | null;
    categoriaSugerida?: string;
    descricao?: string;
  };
  opcoes?: string[];
};

/** POST /api/ai/chat — envia mensagem para a IA e obtém resposta estruturada. */
export async function chatComIA(
  request: AiChatRequest,
): Promise<AiChatResponse | null> {
  try {
    const res = await fetch(`${process.env.API_URL}/api/ai/chat`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
      cache: "no-store",
    });
    if (!res.ok) return null;
    return (await res.json()) as AiChatResponse;
  } catch {
    return null;
  }
}
