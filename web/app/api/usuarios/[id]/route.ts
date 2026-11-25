import type { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

type Contexto = { params: Promise<{ id: string }> };

export async function PATCH(request: NextRequest, { params }: Contexto) {
  const { id } = await params;

  return encaminharParaApi(`/api/usuarios/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: await request.text(),
    contentType: request.headers.get("content-type"),
    limparCookieEm401: true,
  });
}

export async function DELETE(_request: NextRequest, { params }: Contexto) {
  const { id } = await params;

  return encaminharParaApi(`/api/usuarios/${encodeURIComponent(id)}`, {
    method: "DELETE",
    limparCookieEm401: true,
  });
}
