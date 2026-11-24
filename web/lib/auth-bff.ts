import { NextResponse } from "next/server";
import { COOKIE_CONVITE_OAUTH } from "./google-convite";

type OpcoesFalhaGoogle = {
  erro?: string;
  status?: number;
  manterEstado?: boolean;
  motivo?: string;
  reiniciaEm?: number;
  agora?: number;
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
  if (opcoes.status === 429 && opcoes.reiniciaEm !== undefined) {
    const agora = opcoes.agora ?? Date.now();
    const segundos = Math.max(1, Math.ceil((opcoes.reiniciaEm - agora) / 1000));
    resposta.headers.set("Retry-After", String(segundos));
  }
  if (!opcoes.manterEstado) {
    resposta.cookies.delete("google_oauth");
    resposta.cookies.delete(COOKIE_CONVITE_OAUTH);
  }
  return resposta;
}

export function motivoFalhaApi(status: number | null, comStatusHttp: boolean): string {
  if (status === null) return "API_REDE";
  if (!comStatusHttp) return "API_SEM_TOKEN";
  return `API_${status}`;
}

export function motivoFalhaApi401(detalhe: unknown): string {
  if (detalhe === "Token Google inválido") return "API_TOKEN_INVALIDO";
  if (detalhe === "E-mail ou senha inválidos") return "API_CREDENCIAIS";
  return "API_401_OUTRO";
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
