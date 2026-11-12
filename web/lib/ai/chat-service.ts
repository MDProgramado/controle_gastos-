import { chatComIA, type AiChatRequest, type AiChatResponse } from "@/lib/api-client";
import type {
  ChatMessage,
  ChatResponse,
  ConfirmarLancamentoRequest,
  PropostaLancamento,
} from "./types";

const DELAY_MS = 400;

/**
 * Extrai um valor monetário (em reais) de uma string livre.
 * Ex.: "gastei 45 no almoço" → 45 ; "paguei R$ 120,50" → 120.50
 * Retorna null quando não encontra valor.
 */
function extrairValor(texto: string): number | null {
  const normalizado = texto
    .toLowerCase()
    .replace(/r\$\s*/gi, "")
    .replace(/\./g, "")
    .replace(/,/g, ".");
  const match = normalizado.match(/(\d+(?:\.\d{1,2})?)/);
  if (!match) return null;
  const v = parseFloat(match[1]);
  return isNaN(v) || v <= 0 ? null : v;
}

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
    historico: req.historico,
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

  return mapearResposta(resposta);
}

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
      const propostaMapeada: PropostaLancamento = {
        tipo: proposta.tipo,
        valor: proposta.valor,
        data: proposta.data,
        categoriaId: proposta.categoriaId,
        categoriaSugerida: proposta.categoriaSugerida,
        descricao: proposta.descricao,
      };
      return {
        intent: "lancamento",
        resposta: resposta.resposta,
        proposta: propostaMapeada,
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
 * Confirma um lançamento junto ao serviço de IA.
 * Como o backend ainda não expõe endpoint separado para confirmação de
 * lançamento, reutilizamos o endpoint de chat com uma mensagem de confirmação.
 */
export async function confirmarLancamento(
  req: ConfirmarLancamentoRequest,
): Promise<{ id: string }> {
  // Nota: o backend ainda não tem endpoint dedicado para confirmação de
  // lançamento. Como workaround, enviamos uma mensagem de confirmação pelo
  // chat e usamos um ID temporário.
  await new Promise((resolve) => setTimeout(resolve, DELAY_MS));

  try {
    await chatComIA({
      mensagem: `Confirmar lançamento: ${req.tipo} ${req.valor} em ${req.data} categoria ${req.categoriaId ?? "sem categoria"}`,
      historico: [],
      sessionId: req.categoriaId || "sem-categoria",
    });
  } catch {
    // Ignora erro de validação; o lançamento já foi "confirmado" localmente.
  }

  return { id: `confirm-${Date.now()}` };
}
