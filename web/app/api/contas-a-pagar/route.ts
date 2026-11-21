import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

/** BFF de contas a pagar: usa apenas API_URL (server-side) e o cookie de sessão "sess". */
export async function GET(request: NextRequest) {
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
    const { searchParams } = new URL(request.url);
    const query = searchParams.toString();
    const url = `${apiUrl}/api/contas-a-pagar${query ? `?${query}` : ""}`;

    const res = await fetch(url, {
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    const data = await res.json().catch(() => []);
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json(
      { erro: "Falha ao carregar contas a pagar." },
      { status: 502 },
    );
  }
}
