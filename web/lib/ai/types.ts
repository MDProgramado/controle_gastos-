/**
 * Contrato do módulo de IA (fase frontend — Prompt 5).
 *
 * Estes tipos definem a interface entre o frontend e o backend de IA
 * (OpenRouter). O backend ainda não existe: este arquivo é o contrato que
 * será implementado posteriormente.
 */

export type Intent = "consulta" | "lancamento" | "esclarecimento";

export type ChatMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
  criadoEm: Date;
  /** Preenchido apenas quando role === "assistant" e intent === "lancamento" */
  lancamentoProposta?: PropostaLancamento;
  /** Preenchido apenas quando role === "assistant" e intent === "consulta" */
  consultaDados?: ConsultaResultado[];
  /** Preenchido apenas quando role === "assistant" e intent === "esclarecimento" */
  opcoes?: string[];
};

export type ChatRequest = {
  mensagem: string;
  historico: ChatMessage[];
};

export type ConsultaResultado = {
  label: string;
  valor: number;
};

export type PropostaLancamento = {
  tipo: "ENTRADA" | "SAIDA";
  valor: number; // em reais, sempre > 0
  data: string; // ISO yyyy-mm-dd
  categoriaId: string | null; // null = IA não tem certeza, usuário escolhe
  categoriaSugerida?: string;
  descricao?: string;
};

export type ConfirmarLancamentoRequest = PropostaLancamento;

export type ChatResponse =
  | {
      intent: "consulta";
      resposta: string;
      dados?: ConsultaResultado[];
    }
  | {
      intent: "lancamento";
      resposta: string;
      proposta: PropostaLancamento;
    }
  | {
      intent: "esclarecimento";
      resposta: string;
      opcoes?: string[];
    };
