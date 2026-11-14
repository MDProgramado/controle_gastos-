import {
  chatComIA,
  gravarLancamentos,
  type AiChatRequest,
  type AiChatResponse,
  type AiPropostaLancamento,
} from "@/lib/api-client";
import type {
  ChatResponse,
  ConfirmarLancamentoRequest,
  PropostaLancamento,
} from "./types";

const DELAY_MS = 400;

/**
 * Envia uma mensagem para o serviço de IA via API real (POST /api/ai/chat).
 */
export async function enviarMensagem(
  req: AiChatRequest,
): Promise<ChatResponse> {
  // Simula latência de rede para testar o estado "digitando".
  await new Promise((resolve) => setTimeout(resolve, DELAY_MS));

  const resposta = await chatComIA({
    mensagem: req.mensagem,
    // A API aceita apenas {role, content} no histórico (fail-on-unknown-properties):
    // enviar o ChatMessage inteiro (id, criadoEm, lancamentoProposta...) é rejeitado com 400.
    historico: req.historico.map((m) => ({
      role: m.role,
      content: m.content,
    })),
    sessionId: req.sessionId,
  });

  if (!resposta) {
    return {
      intent: "esclarecimento",
      resposta:
        "Não foi possível conectar ao serviço de IA. Tente novamente mais tarde.",
      opcoes: ["Tentar novamente"],
    };
  }

  return mapearResposta(resposta);}

/**
 * Mapeia a resposta da API (AiChatResponse) para o formato esperado pelo
 * frontend (ChatResponse).
 */
function mapearResposta(resposta: AiChatResponse): ChatResponse {
  switch (resposta.intent) {
    case "lancamento": {
      const proposta = resposta.proposta;
      if (!proposta) {
        return {
          intent: "esclarecimento",
          resposta: "Não foi possível entender o lançamento. Tente novamente.",
          opcoes: ["Tentar novamente"],
        };
      }
      const mapear = (p: AiPropostaLancamento): PropostaLancamento => ({
        tipo: p.tipo,
        valor: p.valor,
        data: p.data,
        categoriaId: p.categoriaId,
        categoriaSugerida: p.categoriaSugerida,
        descricao: p.descricao,
      });
      const lista = resposta.propostas?.length ? resposta.propostas : [proposta];
      return {
        intent: "lancamento",
        resposta: resposta.resposta,
        proposta: mapear(proposta),
        propostas: lista.map(mapear),
      };
    }
    case "consulta": {
      return {
        intent: "consulta",
        resposta: resposta.resposta,
        dados: resposta.dados?.map((d) => ({
          label: d.label,
          valor: d.valor,
        })) ?? [],
      };
    }
    case "esclarecimento":
    default:
      return {
        intent: "esclarecimento",
        resposta: resposta.resposta,
        opcoes: resposta.opcoes,
      };
  }
}

/**
 * Confirma o(s) lançamento(s) proposto(s) pela IA.
 *
 * A gravação é feita pelo endpoint de LOTE da API (POST /api/lancamentos/lote): transação
 * tudo-ou-nada (LOT-01/LOT-02) e tenant exclusivamente do token (R3).
 */
export async function confirmarLancamento(
  req: ConfirmarLancamentoRequest,
): Promise<{ id: string }> {
  const categoriaId = Number(req.categoriaId);
  if (!categoriaId) {
    throw new Error("Escolha a categoria do lançamento antes de confirmar.");
  }

  const ids = await gravarLancamentos([
    {
      categoriaId,
      tipo: req.tipo,
      valor: req.valor,
      data: req.data,
      descricao: req.descricao,
    },
  ]);

  if (!ids) {
    throw new Error("Não foi possível registrar o lançamento.");
  }

  return { id: String(ids[0] ?? `confirm-${Date.now()}`) };
}

/**
 * Confirma vários lançamentos de uma vez (mensagem com vários itens). Uma única transação:
 * ou entra tudo, ou nada é gravado.
 */
export async function confirmarLancamentos(
  reqs: ConfirmarLancamentoRequest[],
): Promise<number[]> {
  const linhas = reqs.map((req) => {
    const categoriaId = Number(req.categoriaId);
    if (!categoriaId) {
      throw new Error("Escolha a categoria de todos os lançamentos antes de confirmar.");
    }
    return {
      categoriaId,
      tipo: req.tipo,
      valor: req.valor,
      data: req.data,
      descricao: req.descricao,
    };
  });

  const ids = await gravarLancamentos(linhas);
  if (!ids) {
    throw new Error("Não foi possível registrar os lançamentos.");
  }
  return ids;
}
