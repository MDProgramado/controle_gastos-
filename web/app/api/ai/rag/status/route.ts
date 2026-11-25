import { encaminharParaApi } from "@/lib/bff";

export async function GET() {
  return encaminharParaApi("/api/ai/rag/status", { limparCookieEm401: true });
}
