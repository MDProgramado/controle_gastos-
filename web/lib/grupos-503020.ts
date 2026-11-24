export const GRUPOS_503020 = ["NECESSIDADE", "DESEJO", "FUTURO"] as const;

export type Grupo503020 = (typeof GRUPOS_503020)[number];

export const GRUPO_PADRAO_503020: Grupo503020 = "DESEJO";

const ROTULOS: Record<Grupo503020, string> = {
  NECESSIDADE: "Necessidades",
  DESEJO: "Desejos",
  FUTURO: "Futuro",
};

const PERCENTUAIS: Record<Grupo503020, number> = {
  NECESSIDADE: 50,
  DESEJO: 30,
  FUTURO: 20,
};

export function isGrupo503020(value: unknown): value is Grupo503020 {
  return typeof value === "string" && GRUPOS_503020.includes(value as Grupo503020);
}

export function grupoLabel(grupo: Grupo503020): string {
  return ROTULOS[grupo];
}

export function grupoPercentual(grupo: Grupo503020): number {
  return PERCENTUAIS[grupo];
}
