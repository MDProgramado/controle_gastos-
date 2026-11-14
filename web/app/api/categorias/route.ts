import { cookies } from "next/headers";

/**
 * Lista de categorias do tenant (GET /api/categorias).
 *
 * BFF: o browser não lê o cookie httpOnly "sess", então a chamada autenticada é feita aqui.
 */
export async function GET() {
  const cookieStore = await cookies(); // Next 16: acesso SOMENTE assíncrono
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return Response.json({ erro: "não autenticado" }, { status: 401 });
  }

  let res: Response;
  try {
    res = await fetch(`${process.env.API_URL}/api/categorias`, {
      headers: { Authorization: `Bearer ${token}` },
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
