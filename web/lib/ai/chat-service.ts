import {
  ChatRequest,
  ChatResponse,
  ConfirmarLancamentoRequest,
  PropostaLancamento,
} from "./types";

/**
 * Camada de serviço de chat com IA (fase frontend — Prompt 5).
 *
 * Nesta fase, tudo é mock: não há chamada de rede real.
 * Quando o endpoint `/ai/chat` existir, basta trocar o corpo de
 * `enviarMensagem` e `confirmarLancamento` por chamadas `fetch`.
 */

const DELAY_MS = 800;

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
 * Tenta adivinhar a categoria com base em palavras-chave da mensagem.
 * Retorna { categoriaId, categoriaSugerida } ou null quando não há certeza.
 */
function adivinharCategoria(texto: string): {
  categoriaId: string | null;
  categoriaSugerida?: string;
} {
  const t = texto.toLowerCase();
  const map: Array<[string, string]> = [
    ["almoço|jantar|restaurante|lanche", "Alimentação"],
    ["uber|99|táxi|combustível|gasolina|ônibus|metrô", "Transporte"],
    ["supermercado|feira|mercado", "Supermercado"],
    ["netflix|spotify|assinatura|streaming", "Assinaturas"],
    ["farmácia|remédio|doutor|médico|consulta", "Saúde"],
    ["salário|pagamento|recebimento|freelance", "Receita"],
  ];
  for (const [pattern, sugestao] of map) {
    const re = new RegExp(pattern);
    if (re.test(t)) {
      return { categoriaId: null, categoriaSugerida: sugestao };
    }
  }
  return { categoriaId: null, categoriaSugerida: undefined };
}

function agoraISO(): string {
  return new Date().toISOString().slice(0, 10);
}

/**
 * Envia uma mensagem para o serviço de IA (mock nesta fase).
 *
 * Regras do mock:
 * - Mensagem contendo "gastei" ou "paguei" + um valor → intent "lancamento"
 * - Mensagem contendo "quanto" → intent "consulta"
 * - Qualquer outra coisa → intent "esclarecimento"
 *
 * TODO: TROCAR PELO FETCH AO BACKEND quando /ai/chat existir.
 */
export async function enviarMensagem(
  req: ChatRequest,
): Promise<ChatResponse> {
  // Simula latência de rede para testar o estado "digitando".
  await new Promise((resolve) => setTimeout(resolve, DELAY_MS));

  const texto = req.mensagem.trim().toLowerCase();

  // --- Lançamento: "gastei 45 no almoço" ou "paguei R$ 120" ---
  const palavrasGasto = ["gastei", "paguei", "gastei", "gasto"];
  const temGasto = palavrasGasto.some((p) => texto.includes(p));
  const valor = extrairValor(texto);

  if (temGasto && valor) {
    const categoria = adivinharCategoria(texto);
    const proposta: PropostaLancamento = {
      tipo: "SAIDA",
      valor,
      data: agoraISO(),
      categoriaId: categoria.categoriaId,
      categoriaSugerida: categoria.categoriaSugerida,
      descricao: req.mensagem.trim(),
    };
    return {
      intent: "lancamento",
      resposta: "Entendi este lançamento",
      proposta,
    };
  }

  // --- Consulta: "quanto gastei com..." ---
  if (texto.includes("quanto")) {
    return {
      intent: "consulta",
      resposta:
        "Aqui está um resumo dos seus gastos neste mês (dados de exemplo do mock).",
      dados: [
        { label: "Alimentação", valor: 342.5 },
        { label: "Transporte", valor: 180.0 },
        { label: "Supermercado", valor: 210.75 },
      ],
    };
  }

  // --- Esclarecimento: qualquer outra coisa ---
  return {
    intent: "esclarecimento",
    resposta:
      "Não entendi bem. Você pode dizer, por exemplo: 'gastei 45 no almoço' ou 'quanto gastei com alimentação este mês?'.",
    opcoes: ["Gastei 45 no almoço hoje", "Quanto gastei com alimentação?"],
  };
}

/**
 * Confirma um lançamento junto ao serviço de IA (mock nesta fase).
 *
 * Apenas loga no console e devolve um id fake.
 *
 * TODO: TROCAR PELO FETCH AO BACKEND quando /ai/chat existir.
 */
export async function confirmarLancamento(
  req: ConfirmarLancamentoRequest,
): Promise<{ id: string }> {
  // TODO: TROCAR PELO FETCH AO BACKEND quando /ai/chat existir.
  console.log("[MOCK] confirmarLancamento chamado com:", req);
  return new Promise((resolve) => {
    setTimeout(() => resolve({ id: `mock-${Date.now()}` }), 400);
  });
}
