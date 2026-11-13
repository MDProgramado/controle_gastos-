import { cookies } from "next/headers";

/**
 * BFF da confirmação da importação (fase 5, D-28).
 *
 * POST /api/importacoes/{id}/confirmar — repassa o corpo JSON com a seleção do
 * usuário para `${API_URL}/api/importacoes/{id}/confirmar`. A gravação é
 * tudo-ou-nada na API (05-05); aqui nada é interpretado — só autenticação e
 * repasse.
 */

type Contexto = { params: Promise<{ id: string }> };

export async function POST(request: Request, { params }: Contexto) {
  const { id } = await params;
  const idValido = Number(id);
  if (!Number.isInteger(idValido) || idValido <= 0) {
    return Response.json({ erro: "id inválido" }, { status: 400 });
  }

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
    res = await fetch(`${process.env.API_URL}/api/importacoes/${idValido}/confirmar`, {
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
