type Parametros = {
  lancamentos: number;
  contas: number;
  falhas: number;
  erros?: string[];
};

export function descreverResultadoConfirmacao({
  lancamentos,
  contas,
  falhas,
  erros = [],
}: Parametros): { titulo: string; descricao: string; parcial: boolean } {
  const total = lancamentos + contas;

  if (falhas === 0) {
    return {
      titulo: "Importação confirmada",
      descricao: `${lancamentos} lançamentos e ${contas} contas a pagar foram gravados. Nada mais será criado se você confirmar de novo.`,
      parcial: false,
    };
  }

  if (total === 0) {
    return {
      titulo: "Nenhum lançamento foi gravado",
      descricao:
        erros.join(" ") ||
        "Nenhum lançamento foi gravado. Verifique a importação e tente novamente.",
      parcial: false,
    };
  }

  return {
    titulo: "Importação parcial",
    descricao: `${lancamentos} lançamentos e ${contas} contas a pagar foram gravados. ${falhas} documento(s) falharam; revise e tente novamente apenas os pendentes.`,
    parcial: true,
  };
}
