import { cookies } from "next/headers";
import { NextResponse, type NextRequest } from "next/server";
import { extrairToken, googleFalha } from "@/lib/auth-bff";
import { type IntencaoGoogle } from "@/lib/google-oauth";
import { googleOAuthPronto, tokenRequest } from "@/lib/google-oauth-server";
import { limitarPorIp } from "@/lib/rate-limit";
import { setSessionCookie } from "@/lib/session";

const LIMITE_MINUTO = 30;
const JANELA_MS = 60_000;
const MSG_LIMITE = "Muitas tentativas. Tente novamente em instantes.";

type EstadoOAuth = {
  state: string;
  verifier: string;
  intencao: IntencaoGoogle;
  conviteToken: string;
  redirectUri: string;
};

function lerEstado(valor: string | undefined): EstadoOAuth | null {
  if (!valor) return null;
  let dados: unknown;
  try {
    dados = JSON.parse(valor);
  } catch {
    return null;
  }
  if (typeof dados !== "object" || dados === null) return null;

  const objeto = dados as Record<string, unknown>;
  const intencao = objeto.intencao;
  const redirecionamento = objeto.redirectUri;
  if (
    typeof objeto.state !== "string" ||
    !objeto.state ||
    typeof objeto.verifier !== "string" ||
    !objeto.verifier ||
    (intencao !== "CADASTRO" && intencao !== "LOGIN" && intencao !== "CONVITE") ||
    typeof objeto.conviteToken !== "string" ||
    typeof redirecionamento !== "string"
  ) {
    return null;
  }

  let url: URL;
  try {
    url = new URL(redirecionamento);
  } catch {
    return null;
  }
  if (url.pathname !== "/api/auth/google/callback") return null;

  return {
    state: objeto.state,
    verifier: objeto.verifier,
    intencao,
    conviteToken: objeto.conviteToken,
    redirectUri: redirecionamento,
  };
}

export async function GET(request: NextRequest) {
  if (limitarPorIp(request, "google-callback", LIMITE_MINUTO, JANELA_MS)) {
    return googleFalha(request, { erro: MSG_LIMITE, status: 429, manterEstado: true });
  }
  if (!googleOAuthPronto()) return googleFalha(request, { motivo: "CONFIG" });

  const estado = lerEstado((await cookies()).get("google_oauth")?.value);
  if (!estado) return googleFalha(request, { motivo: "ESTADO" });

  const state = request.nextUrl.searchParams.get("state");
  const code = request.nextUrl.searchParams.get("code");
  if (!state || state !== estado.state || !code) return googleFalha(request, { motivo: "ESTADO" });

  let idToken: string | null = null;
  try {
    const req = tokenRequest(code, estado.verifier, estado.redirectUri);
    const troca = await fetch(req.url, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: req.body.toString(),
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (troca.ok) {
      const dados = (await troca.json()) as { id_token?: unknown };
      if (typeof dados.id_token === "string" && dados.id_token) idToken = dados.id_token;
    }
  } catch {
    return googleFalha(request, { motivo: "TOKEN" });
  }
  if (!idToken) return googleFalha(request, { motivo: "TOKEN" });
  if (!process.env.API_URL) return googleFalha(request, { motivo: "API_URL" });

  let accessToken: string | null = null;
  try {
    const resposta = await fetch(`${process.env.API_URL}/api/auth/google`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        idToken,
        intencao: estado.intencao,
        ...(estado.conviteToken ? { conviteToken: estado.conviteToken } : {}),
      }),
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (resposta.ok) {
      accessToken = extrairToken(await resposta.json());
    }
  } catch {
    return googleFalha(request, { motivo: "API" });
  }
  if (!accessToken) return googleFalha(request, { motivo: "API" });

  await setSessionCookie(accessToken);
  const sucesso = NextResponse.redirect(new URL("/", request.url), 303);
  sucesso.cookies.delete("google_oauth");
  return sucesso;
}
