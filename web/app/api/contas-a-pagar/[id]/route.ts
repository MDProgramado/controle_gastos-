import type { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

type Contexto = { params: Promise<{ id: string }> };

export async function PATCH(request: NextRequest, { params }: Contexto) {
  const { id } = await params;
  const corpo = await request.text();

  return encaminharParaApi(`/api/contas-a-pagar/${encodeURIComponent(id)}/pagar`, {
    method: "PATCH",
    body: corpo || null,
    contentType: corpo ? "application/json" : null,
    limparCookieEm401: true,
  });
}
