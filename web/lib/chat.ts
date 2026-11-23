import type { ChatMessage, PropostaLancamento } from "./ai/types";
import { centavosDeString } from "./money";

export type PropostaEditavel = Omit<PropostaLancamento, "valor"> & {
  id: string;
  valor: string;
  confirmada: boolean;
};

export type PropostasPorMensagem = Record<string, PropostaEditavel[]>;

export function propostasDaMensagem(mensagem: ChatMessage): PropostaLancamento[] {
  if (mensagem.lancamentoPropostas?.length) {
    return [...mensagem.lancamentoPropostas];
  }
  if (mensagem.lancamentoProposta) {
    return [mensagem.lancamentoProposta];
  }
  return [];
}

export function criarPropostasEditaveis(
  mensagemId: string,
  propostas: PropostaLancamento[],
  encontrarCategoriaId: (sugestao: string) => string | undefined,
): PropostasPorMensagem {
  return {
    [mensagemId]: propostas.map((proposta, indice) => ({
      ...proposta,
      id: `prop-${mensagemId}-${indice}`,
      valor: String(proposta.valor),
      categoriaId:
        proposta.categoriaId ?? encontrarCategoriaId(proposta.categoriaSugerida ?? "") ?? "",
      confirmada: false,
    })),
  };
}

export function atualizarPropostaPorMensagem(
  mapa: PropostasPorMensagem,
  mensagemId: string,
  propostaId: string,
  mudancas: Partial<PropostaEditavel>,
): PropostasPorMensagem {
  const lista = mapa[mensagemId] ?? [];
  return {
    ...mapa,
    [mensagemId]: lista.map((proposta) =>
      proposta.id === propostaId ? { ...proposta, ...mudancas } : proposta,
    ),
  };
}

export function marcarPropostasConfirmadas(
  mapa: PropostasPorMensagem,
  mensagemId: string,
  ids: string[],
): PropostasPorMensagem {
  const lista = mapa[mensagemId] ?? [];
  return {
    ...mapa,
    [mensagemId]: lista.map((proposta) =>
      ids.includes(proposta.id) ? { ...proposta, confirmada: true } : proposta,
    ),
  };
}

export function valorPropostaParaApi(valor: string): number | null {
  const centavos = centavosDeString(valor);
  return centavos === null ? null : Number(centavos) / 100;
}

export function propostasInvalidas(propostas: PropostaEditavel[]): PropostaEditavel[] {
  return propostas.filter(
    (proposta) => !proposta.confirmada && valorPropostaParaApi(proposta.valor) === null,
  );
}
