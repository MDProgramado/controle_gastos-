import type { NextRequest } from "next/server";

type Registro = { quantidade: number; reiniciaEm: number };

const LIMITE_REGISTROS = 10_000;
export const CHAVE_REGISTROS_RATE_LIMIT = "__controleGastosRateLimitRegistros";

type GlobalComRegistros = typeof globalThis & {
  [CHAVE_REGISTROS_RATE_LIMIT]?: Map<string, Registro>;
};

function mapaRegistros(): Map<string, Registro> {
  const global = globalThis as GlobalComRegistros;
  if (!global[CHAVE_REGISTROS_RATE_LIMIT]) {
    global[CHAVE_REGISTROS_RATE_LIMIT] = new Map<string, Registro>();
  }
  return global[CHAVE_REGISTROS_RATE_LIMIT]!;
}

function ipCliente(headers: Headers): string {
  const encaminhado = headers.get("x-forwarded-for");
  if (encaminhado) {
    const hops = encaminhado.split(",").map((hop) => hop.trim()).filter(Boolean);
    const ultimo = hops[hops.length - 1];
    if (ultimo) return ultimo;
  }
  return headers.get("x-real-ip")?.trim() || "desconhecido";
}

export function verificarLimite(
  request: NextRequest,
  chave: string,
  limite: number,
  janelaMs: number,
  agora: number = Date.now(),
): { excedeu: boolean; reiniciaEm: number } {
  const registros = mapaRegistros();
  const ip = ipCliente(request.headers);
  const identificador = `${chave}:${ip}`;

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

  return { excedeu, reiniciaEm: registros.get(identificador)!.reiniciaEm };
}

export function limitarPorIp(
  request: NextRequest,
  chave: string,
  limite: number,
  janelaMs: number,
): boolean {
  return verificarLimite(request, chave, limite, janelaMs).excedeu;
}
