export type ResultadoCriacaoLote =
  | { ok: true; criados: number }
  | { ok: false; mensagem: string; indiceLinha?: number };

type CorpoErro = {
  detail?: unknown;
  erro?: unknown;
  message?: unknown;
  indiceLinha?: unknown;
};

function texto(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() ? value.trim() : undefined;
}

export function resultadoErroLote(corpo: unknown, _status: number): ResultadoCriacaoLote {
  void _status;
  const dados = (corpo ?? {}) as CorpoErro;
  const mensagem =
    texto(dados.detail) ?? texto(dados.erro) ?? texto(dados.message) ?? "Não foi possível salvar o lote.";
  const indice =
    typeof dados.indiceLinha === "number" && Number.isInteger(dados.indiceLinha)
      ? dados.indiceLinha
      : undefined;

  return { ok: false, mensagem, ...(indice === undefined ? {} : { indiceLinha: indice }) };
}
