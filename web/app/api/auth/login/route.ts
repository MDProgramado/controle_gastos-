import { urlApiServidor } from "@/lib/api-url";
import { extrairToken } from "@/lib/auth-bff";
import { setSessionCookie } from "@/lib/session";

const MSG_CREDENCIAIS = "E-mail ou senha inválidos";
const MSG_CONEXAO = "Não foi possível conectar. Tente novamente em instantes.";

/**
 * BFF de login (padrão P4 / D-02): o browser nunca vê o JWT — a API devolve o
 * token somente para este route handler, que o grava no cookie httpOnly "sess".
 */
export async function POST(request: Request) {
  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return Response.json({ erro: "Corpo da requisição inválido" }, { status: 400 });
  }

  const apiUrl = urlApiServidor();
  if (!apiUrl) {
    return Response.json({ erro: MSG_CONEXAO }, { status: 502 });
  }

  let res: Response;
  try {
    res = await fetch(`${apiUrl}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
  } catch {
    // erro de rede / API_URL inacessível
    return Response.json({ erro: MSG_CONEXAO }, { status: 502 });
  }

  if (res.status === 401) {
    // anti-enumeração (D-05): mesma mensagem genérica para qualquer falha de credencial
    return Response.json({ erro: MSG_CREDENCIAIS }, { status: 401 });
  }

  if (!res.ok) {
    if (res.status >= 500) {
      return Response.json({ erro: MSG_CONEXAO }, { status: 502 });
    }
    // 4xx de validação da API (corpo malformado, senha > 72 bytes, etc.)
    return Response.json({ erro: "Verifique os dados informados." }, { status: res.status });
  }

  const accessToken = extrairToken(await res.json());
  if (!accessToken) {
    return Response.json({ erro: MSG_CONEXAO }, { status: 502 });
  }

  await setSessionCookie(accessToken);

  return Response.json({ ok: true });
}
