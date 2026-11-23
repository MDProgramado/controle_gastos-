import { NextResponse } from "next/server";

type OpcoesFalhaGoogle = {
  erro?: string;
  status?: number;
  manterEstado?: boolean;
  motivo?: string;
};

export function googleFalha(request: Request, opcoes: OpcoesFalhaGoogle = {}) {
  let resposta: NextResponse;
  if (opcoes.erro) {
    resposta = NextResponse.json({ erro: opcoes.erro }, { status: opcoes.status ?? 400 });
  } else {
    const destino = new URL("/login", request.url);
    destino.searchParams.set("erro", "google");
    if (opcoes.motivo) destino.searchParams.set("motivo", opcoes.motivo);
    resposta = NextResponse.redirect(destino, 303);
  }
  if (!opcoes.manterEstado) resposta.cookies.delete("google_oauth");
  return resposta;
}

export function extrairToken(dados: unknown): string | null {
  if (typeof dados !== "object" || dados === null) return null;
  const objeto = dados as { token?: unknown; accessToken?: unknown };
  if (typeof objeto.token === "string" && objeto.token.length > 0) return objeto.token;
  if (typeof objeto.accessToken === "string" && objeto.accessToken.length > 0) {
    return objeto.accessToken;
  }
  return null;
}
