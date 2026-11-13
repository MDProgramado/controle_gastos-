/**
 * Tipos e helpers compartilhados dos componentes de importação (fase 5).
 * Módulo puro (sem React, sem server imports) — usado só por Client Components.
 */
import type { ImportacaoItemRevisao } from "@/lib/api-client";

/** Arquivo válido na lista do envio (validação em `ImportacaoClient`). */
export type ArquivoValido = {
  id: string;
  file: File;
  origem: "CARTAO" | "EXTRATO" | "Documento";
};

/** Item de staging consolidado com o contexto do documento de origem. */
export type ItemRevisao = ImportacaoItemRevisao & {
  /** Documento de origem do item (chave da confirmação por documento). */
  documentoId: number | null;
  documentoNome: string;
  documentoDegradado: boolean;
};

/** Estado de um item na revisão: marcado + categoria escolhida. */
export type SelecaoItem = {
  incluir: boolean;
  categoriaId: number | null;
  /** true quando o usuário escolheu a categoria manualmente (some o badge "Revisar categoria"). */
  manual: boolean;
};

/** Alerta pronto para render (copy do 05-UI-SPEC §6.2). */
export type AlertaLista = {
  id: string;
  kind: string;
  titulo: string;
  descricao: string;
  /** Aviso não bloqueante → Badge variant="warning" à direita. */
  aviso?: boolean;
  /** id do item para o alerta de "revisar categoria" rolar/focar a linha. */
  alvoItemId?: number;
};

/** Soma valores monetários em centavos inteiros — regra do centavo (sem drift float). */
export function somarCentavos(valores: number[]): number {
  return valores.reduce((total, valor) => total + Math.round(valor * 100), 0);
}

/** Centavos → número para `MONETARY.format`. */
export function centavos(valor: number): number {
  return valor / 100;
}

/** "2,4 MB" / "812 KB" (pt-BR, padrão 05-UI-SPEC §6.1). */
export function formatarBytes(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1).replace(".", ",")} MB`;
  }
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

/** ISO yyyy-MM-dd → "dd/mm/aaaa"; null/inválido → "—". */
export function formatarDataIso(iso: string | null | undefined): string {
  if (!iso) return "—";
  const partes = iso.split("-");
  if (partes.length < 3) return "—";
  return `${partes[2].slice(0, 2)}/${partes[1]}/${partes[0].slice(0, 4)}`;
}

/** Heurística do nome do arquivo para o badge de origem (§6.1). */
export function origemInferida(nomeArquivo: string): ArquivoValido["origem"] {
  const nome = nomeArquivo.toLowerCase();
  if (/fatura|cart[aã]o|cartao/.test(nome)) return "CARTAO";
  if (/extrato|banco|saldo/.test(nome)) return "EXTRATO";
  return "Documento";
}

/** Rótulo do badge de origem do item (CARTAO/EXTRATO do staging). */
export function rotuloOrigem(origem: string | null | undefined): string {
  if (origem === "CARTAO") return "Cartão";
  if (origem === "EXTRATO") return "Extrato";
  return "Documento";
}

/** Rótulo do badge de origem inferida do arquivo (§6.1: heurística pelo nome). */
export function rotuloOrigemArquivo(
  origem: ArquivoValido["origem"],
): string {
  if (origem === "CARTAO") return "Fatura de cartão";
  if (origem === "EXTRATO") return "Extrato bancário";
  return "Documento";
}

/** Tags de alerta que o backend concatena em `ImportacaoItem.alerta`. */
export function itemTemTag(alerta: string | null, tag: string): boolean {
  return alerta != null && alerta.includes(tag);
}
