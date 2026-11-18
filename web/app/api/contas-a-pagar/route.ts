import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

const BACKEND_URL =
  process.env.API_URL || process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

async function getAuthHeader() {
  const cookieStore = await cookies();
  const token =
    cookieStore.get("sess")?.value ||
    cookieStore.get("access_token")?.value ||
    cookieStore.get("auth_token")?.value;

  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const query = searchParams.toString();
    const url = `${BACKEND_URL}/contas-a-pagar${query ? `?${query}` : ""}`;

    const authHeader = await getAuthHeader();
    const res = await fetch(url, {
      headers: {
        "Content-Type": "application/json",
        ...(authHeader.Authorization ? { Authorization: authHeader.Authorization } : {}),
      },
      cache: "no-store",
    });

    const data = await res.json().catch(() => []);
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json(
      { error: "Falha ao carregar contas a pagar." },
      { status: 500 }
    );
  }
}
