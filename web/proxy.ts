import { NextResponse, type NextRequest } from "next/server";

/**
 * Next 16: o arquivo de roteamento é `proxy.ts` (o nome antigo está deprecado;
 * ver docs em node_modules/next/dist/docs), runtime Node.
 *
 * Lê SOMENTE a presença do cookie "sess" — o JWT nunca sai do servidor. O
 * assíncrono cookies() de next/headers não é usado neste arquivo: em proxy o
 * acesso correto ao cookie é via `request.cookies` do NextRequest.
 */
export function proxy(request: NextRequest) {
  const temSessao = request.cookies.has("sess");
  const { pathname, searchParams } = request.nextUrl;

  if (pathname === "/login") {
    // Autenticado não fica em /login — exceto quando chegou com ?expirada=1:
    // nesse caso a tela limpa o cookie obsoleto (RSC não pode mutar cookie).
    if (temSessao && searchParams.get("expirada") !== "1") {
      return NextResponse.redirect(new URL("/", request.url));
    }
    return NextResponse.next();
  }

  // Rota protegida sem cookie → /login (a validação real do JWT continua na API)
  if (!temSessao) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  // Páginas entram no gate por cookie; BFF (/api), assets e arquivos com ponto ficam de fora
  matcher: ["/((?!api|_next|favicon\\.ico|.*\\..*).*)"],
};
