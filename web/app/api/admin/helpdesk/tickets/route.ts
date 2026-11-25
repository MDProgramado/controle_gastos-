import type { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

export async function GET(request: NextRequest) {
  return encaminharParaApi("/api/admin/helpdesk/tickets", {
    query: new URL(request.url).searchParams,
    limparCookieEm401: true,
  });
}
