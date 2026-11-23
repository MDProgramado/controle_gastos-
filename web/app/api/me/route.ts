import { encaminharParaApi } from "@/lib/bff";

export async function GET() {
  return encaminharParaApi("/api/auth/me", { limparCookieEm401: true });
}
