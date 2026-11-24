import { NextResponse } from "next/server";
import { COOKIE_CONVITE_OAUTH, opcoesCookieConvite, validarTokenConvite } from "@/lib/google-convite";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ token: string }> },
) {
  const { token } = await params;
  const conviteToken = validarTokenConvite(token);
  if (!conviteToken) {
    return Response.json({ erro: "Token de convite inválido." }, { status: 400 });
  }

  const destino = new URL("/api/auth/google/start?intencao=CONVITE", request.url);
  const resposta = NextResponse.redirect(destino, 303);
  resposta.cookies.set(COOKIE_CONVITE_OAUTH, conviteToken, opcoesCookieConvite());
  return resposta;
}
