import type { NextRequest } from "next/server";
import { encaminharParaApi } from "@/lib/bff";

export async function GET() {
  return encaminharParaApi("/api/usuarios", { limparCookieEm401: true });
}

export async function POST(request: NextRequest) {
  return encaminharParaApi("/api/usuarios/convites", {
    method: "POST",
    body: await request.text(),
    contentType: request.headers.get("content-type"),
    limparCookieEm401: true,
  });
}
