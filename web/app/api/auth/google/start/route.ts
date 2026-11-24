import { NextResponse, type NextRequest } from "next/server";
import { googleFalha } from "@/lib/auth-bff";
import { COOKIE_CONVITE_OAUTH, validarTokenConvite } from "@/lib/google-convite";
import {
  authorizeUrl,
  derivarChallenge,
  gerarState,
  gerarVerifier,
  type IntencaoGoogle,
} from "@/lib/google-oauth";
import { googleOAuthHabilitado } from "@/lib/google-oauth-config";
import { verificarLimite } from "@/lib/rate-limit";

const LIMITE_MINUTO = 10;
const JANELA_MS = 60_000;
const MSG_LIMITE = "Muitas tentativas. Tente novamente em instantes.";

function ehIntencao(valor: string | null): valor is IntencaoGoogle {
  return valor === "CADASTRO" || valor === "LOGIN" || valor === "CONVITE";
}

export async function GET(request: NextRequest) {
  const limite = verificarLimite(request, "google-start", LIMITE_MINUTO, JANELA_MS);
  if (limite.excedeu) {
    return googleFalha(request, { erro: MSG_LIMITE, status: 429, reiniciaEm: limite.reiniciaEm });
  }

  const intencao = request.nextUrl.searchParams.get("intencao");
  if (!ehIntencao(intencao)) return googleFalha(request, { erro: "Intenção inválida.", status: 400 });

  let conviteToken = "";
  if (intencao === "CONVITE") {
    const token = validarTokenConvite(request.cookies.get(COOKIE_CONVITE_OAUTH)?.value);
    if (!token) {
      return googleFalha(request, { erro: "Token de convite inválido.", status: 400 });
    }
    conviteToken = token;
  }

  if (!googleOAuthHabilitado(process.env.GOOGLE_CLIENT_SECRET ?? "")) return googleFalha(request);

  const state = gerarState();
  const verifier = gerarVerifier();
  const challenge = await derivarChallenge(verifier);
  const redirectUri = new URL("/api/auth/google/callback", request.url).toString();
  const resposta = NextResponse.redirect(authorizeUrl(state, challenge, redirectUri), 303);

  resposta.cookies.set(
    "google_oauth",
    JSON.stringify({ state, verifier, intencao, conviteToken, redirectUri }),
    {
      httpOnly: true,
      sameSite: "lax",
      path: "/",
      secure: process.env.NODE_ENV === "production",
      maxAge: 600,
    },
  );
  if (conviteToken) resposta.cookies.delete(COOKIE_CONVITE_OAUTH);

  return resposta;
}
