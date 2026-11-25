import type { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

type Contexto = { params: Promise<{ id: string }> };

export async function GET(request: NextRequest, { params }: Contexto) {
  const { id } = await params;

  return encaminharParaApi(`/api/helpdesk/tickets/${encodeURIComponent(id)}`, {
    query: new URL(request.url).searchParams,
    limparCookieEm401: true,
  });
}
