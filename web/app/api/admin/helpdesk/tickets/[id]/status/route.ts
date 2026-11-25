import type { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

type Contexto = { params: Promise<{ id: string }> };

export async function PATCH(request: NextRequest, { params }: Contexto) {
  const { id } = await params;

  return encaminharParaApi(
    `/api/admin/helpdesk/tickets/${encodeURIComponent(id)}/status`,
    {
      method: "PATCH",
      body: await request.text(),
      contentType: request.headers.get("content-type"),
      query: new URL(request.url).searchParams,
      limparCookieEm401: true,
    },
  );
}
