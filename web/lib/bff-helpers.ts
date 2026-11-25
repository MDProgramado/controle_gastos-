export const API_TIMEOUT_MS = 5000;

const CAMINHOS_PERMITIDOS = [
  "/api/auth/me",
  "/api/categorias",
  "/api/lancamentos",
  "/api/ai/chat",
  "/api/importacoes",
  "/api/contas-a-pagar",
  "/api/usuarios",
  "/api/helpdesk/tickets",
  "/api/admin/helpdesk/tickets",
];

export function urlApiSegura(caminho: string): string | null {
  if (!caminho.startsWith("/api/") || caminho.includes("..") || caminho.includes("//")) {
    return null;
  }

  const permitido = CAMINHOS_PERMITIDOS.some(
    (prefixo) => caminho === prefixo || caminho.startsWith(`${prefixo}/`),
  );

  return permitido ? caminho : null;
}

export function respostaErroBff(status: number, mensagem: string): Response {
  return Response.json(
    { erro: mensagem },
    { status, headers: { "cache-control": "no-store" } },
  );
}
