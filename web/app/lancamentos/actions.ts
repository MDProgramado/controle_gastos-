"use server";

import {
  criarLancamento,
  criarLote,
  editarLancamento,
  excluirLancamento,
  type LancamentoData,
  type LancamentoRequest,
  type LoteRequest,
} from "@/lib/api";
import type { ResultadoCriacaoLote } from "@/lib/lancamentos";

export type LancamentoRequestCliente = Omit<LancamentoRequest, "valor"> & {
  valor: string;
};

export async function actionCriarLancamento(
  request: LancamentoRequestCliente,
): Promise<LancamentoData | null> {
  return criarLancamento(request);
}

export async function actionEditarLancamento(
  id: number,
  request: LancamentoRequestCliente,
): Promise<LancamentoData | null> {
  return editarLancamento(id, request);
}

export async function actionExcluirLancamento(id: number): Promise<boolean> {
  return excluirLancamento(id);
}

export async function actionCriarLote(
  linhas: LoteRequest["linhas"],
): Promise<ResultadoCriacaoLote> {
  return criarLote({ linhas });
}
