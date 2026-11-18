import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL =
  process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface RouteContext {
  params: Promise<{ id: string }>;
}

export async function PATCH(request: NextRequest, context: RouteContext) {
  try {
    const { id } = await context.params;
    const cookieStore = await cookies();
    const token =
      cookieStore.get("sess")?.value ||
      cookieStore.get("access_token")?.value ||
      cookieStore.get("auth_token")?.value;

    const body = await request.json().catch(() => ({}));

    const res = await fetch(`${BACKEND_URL}/contas-a-pagar/${id}/pagar`, {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(body),
    });

    const data = await res.json().catch(() => ({}));
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json(
      { error: "Erro ao registrar pagamento da conta." },
      { status: 500 }
    );
  }
}
