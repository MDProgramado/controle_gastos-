import type { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

export async function GET(request: NextRequest) {
  return encaminharParaApi("/api/helpdesk/tickets", {
    query: new URL(request.url).searchParams,
    limparCookieEm401: true,
  });
}

export async function POST(request: NextRequest) {
  return encaminharParaApi("/api/helpdesk/tickets", {
    method: "POST",
    body: await request.text(),
    contentType: request.headers.get("content-type"),
    query: new URL(request.url).searchParams,
    limparCookieEm401: true,
  });
}
