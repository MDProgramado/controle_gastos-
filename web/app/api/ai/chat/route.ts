import { cookies } from "next/headers";

/**
 * BFF do chat com a IA (fase 4).
 *
 * O browser não pode chamar a API direto: ela exige o header Authorization (Bearer) e o
 * cookie "sess" é httpOnly (o JavaScript da página não o lê). Esta rota lê o cookie,
 * repassa a requisição para a API com o Bearer e devolve a resposta.
 */
export async function POST(request: Request) {
  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

  let corpo: string;
  try {
    corpo = await request.text();
  } catch {
    return Response.json({ erro: "corpo inválido" }, { status: 400 });
  }

  let res: Response;
  try {
    res = await fetch(`${process.env.API_URL}/api/ai/chat`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: corpo,
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { erro: "Não foi possível conectar. Tente novamente em instantes." },
      { status: 502 },
    );
  }

  const body = await res.text();
  return new Response(body, {
    status: res.status,
    headers: { "Content-Type": res.headers.get("content-type") ?? "application/json" },
  });
}
