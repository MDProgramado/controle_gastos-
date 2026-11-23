import { centavosDeString } from "./money";

const ZERO = BigInt(0);
const DOIS = BigInt(2);
const CEM = BigInt(100);
const DEZ_MIL = BigInt(10000);

export type CategoriaTotal = {
  id: number;
  nome: string;
  total: string;
};

export type CategoriaAgrupada = {
  id: number | "outros";
  nome: string;
  centavos: bigint;
};

export function formatarBRL(centavos: bigint): string {
  const negativo = centavos < ZERO;
  const texto = (negativo ? -centavos : centavos).toString().padStart(3, "0");
  const inteira = texto.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${negativo ? "-" : ""}R$ ${inteira},${texto.slice(-2)}`;
}

function basisPoints(centavos: bigint, total: bigint): bigint {
  if (total <= ZERO) {
    return ZERO;
  }
  return (centavos * DEZ_MIL * DOIS + total) / (total * DOIS);
}

export function formatarPercentual(centavos: bigint, total: bigint): string {
  const basis = basisPoints(centavos, total);
  const inteira = basis / CEM;
  const decenas = basis % CEM;
  return `${inteira.toString()},${decenas.toString().padStart(2, "0")}%`;
}

export function progressoPercentual(centavos: bigint, total: bigint): number {
  return Math.min(100, Number(basisPoints(centavos, total)) / 100);
}

export function agruparCategorias(
  categorias: CategoriaTotal[],
  limite = 5,
): CategoriaAgrupada[] {
  const ordenadas: CategoriaAgrupada[] = categorias
    .map((categoria) => ({
      id: categoria.id,
      nome: categoria.nome,
      centavos: centavosDeString(categoria.total) ?? ZERO,
    }))
    .sort((a, b) => {
      if (a.centavos === b.centavos) {
        return a.nome.localeCompare(b.nome, "pt-BR");
      }
      return a.centavos > b.centavos ? -1 : 1;
    });

  const top = ordenadas.slice(0, limite);
  const restantes = ordenadas.slice(limite);
  const centavosRestantes = restantes.reduce(
    (total, categoria) => total + categoria.centavos,
    ZERO,
  );

  if (centavosRestantes > ZERO) {
    top.push({ id: "outros", nome: "Outros", centavos: centavosRestantes });
  }

  return top;
}
