import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

interface RouteContext {
  params: Promise<{ id: string }>;
}

/** BFF de pagamento: apenas API_URL (server-side) e cookie "sess", sem fallbacks de origem. */
export async function PATCH(request: NextRequest, context: RouteContext) {
  const apiUrl = process.env.API_URL;
  if (!apiUrl) {
    return NextResponse.json({ erro: "API não configurada." }, { status: 503 });
  }

  const cookieStore = await cookies();
  const token = cookieStore.get("sess")?.value;
  if (!token) {
    return NextResponse.json({ erro: "não autenticado" }, { status: 401 });
  }

  try {
    const { id } = await context.params;
    const body = await request.json().catch(() => ({}));

    const res = await fetch(`${apiUrl}/api/contas-a-pagar/${id}/pagar`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json(
      { erro: "Erro ao registrar pagamento da conta." },
      { status: 502 },
    );
  }
}
