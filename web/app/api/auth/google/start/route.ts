import { NextResponse, type NextRequest } from "next/server";
import { googleFalha } from "@/lib/auth-bff";
import {
  authorizeUrl,
  derivarChallenge,
  gerarState,
  gerarVerifier,
  type IntencaoGoogle,
} from "@/lib/google-oauth";
import { googleOAuthPronto } from "@/lib/google-oauth-server";
import { limitarPorIp } from "@/lib/rate-limit";

const LIMITE_MINUTO = 10;
const JANELA_MS = 60_000;
const CONVITE_TOKEN = /^[A-Za-z0-9_-]{20,512}$/;
const MSG_LIMITE = "Muitas tentativas. Tente novamente em instantes.";

function ehIntencao(valor: string | null): valor is IntencaoGoogle {
  return valor === "CADASTRO" || valor === "LOGIN" || valor === "CONVITE";
}

export async function GET(request: NextRequest) {
  if (limitarPorIp(request, "google-start", LIMITE_MINUTO, JANELA_MS)) {
    return googleFalha(request, { erro: MSG_LIMITE, status: 429 });
  }

  const intencao = request.nextUrl.searchParams.get("intencao");
  if (!ehIntencao(intencao)) return googleFalha(request, { erro: "Intenção inválida.", status: 400 });

  const conviteToken = request.nextUrl.searchParams.get("conviteToken") ?? "";
  if (intencao === "CONVITE" && !CONVITE_TOKEN.test(conviteToken)) {
    return googleFalha(request, { erro: "Token de convite inválido.", status: 400 });
  }

  if (!googleOAuthPronto()) return googleFalha(request);

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

  return resposta;
}
