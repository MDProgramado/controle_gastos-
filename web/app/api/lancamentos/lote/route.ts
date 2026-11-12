import { cookies } from "next/headers";

/**
 * Confirmação dos lançamentos propostos pela IA (POST /api/lancamentos/lote).
 *
 * Usa o endpoint de LOTE da API: a gravação é tudo-ou-nada (LOT-01/LOT-02) e o tenant vem
 * exclusivamente do token (R3). O browser não lê o cookie httpOnly "sess", então o Bearer é
 * repassado aqui.
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
    res = await fetch(`${process.env.API_URL}/api/lancamentos/lote`, {
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
