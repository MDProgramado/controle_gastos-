import type { NextRequest } from "next/server";

type Registro = { quantidade: number; reiniciaEm: number };

const LIMITE_REGISTROS = 10_000;
const registros = new Map<string, Registro>();

function ipCliente(headers: Headers): string {
  const encaminhado = headers.get("x-forwarded-for");
  if (encaminhado) {
    const hops = encaminhado.split(",").map((hop) => hop.trim()).filter(Boolean);
    const ultimo = hops[hops.length - 1];
    if (ultimo) return ultimo;
  }
  return headers.get("x-real-ip")?.trim() || "desconhecido";
}

export function limitarPorIp(
  request: NextRequest,
  chave: string,
  limite: number,
  janelaMs: number,
): boolean {
  const ip = ipCliente(request.headers);
  const identificador = `${chave}:${ip}`;
  const agora = Date.now();

  for (const [registro, valor] of registros) {
    if (agora >= valor.reiniciaEm) registros.delete(registro);
  }

  const atual = registros.get(identificador);
  let excedeu: boolean;
  if (!atual) {
    registros.set(identificador, { quantidade: 1, reiniciaEm: agora + janelaMs });
    excedeu = false;
  } else {
    atual.quantidade += 1;
    excedeu = atual.quantidade > limite;
  }

  while (registros.size > LIMITE_REGISTROS) {
    const maisAntigo = registros.keys().next().value;
    if (maisAntigo === undefined) break;
    registros.delete(maisAntigo);
  }

  return excedeu;
}
